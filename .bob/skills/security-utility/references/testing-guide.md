# Security Utility Testing Guide

## Overview

The security utility project uses two types of tests:
1. **Unit Tests** - Fast, isolated tests with mocked dependencies
2. **FAT Tests** - Full integration tests with real Liberty servers

## Unit Testing

### Location
`dev/com.ibm.ws.security.utility/test/com/ibm/ws/security/utility/tasks/`

### Framework
- JUnit 4
- JMock for mocking

### Running Unit Tests

```bash
cd dev
./gradlew com.ibm.ws.security.utility:test
```

### Unit Test Structure

```java
public class MyTaskTest {
    @Rule
    public JUnitRuleMockery context = new JUnitRuleMockery();
    
    private MyTask task;
    private ConsoleWrapper stdin;
    private PrintStream stdout;
    private PrintStream stderr;
    private IFileUtility fileUtil;
    
    @Before
    public void setUp() {
        stdin = context.mock(ConsoleWrapper.class);
        stdout = context.mock(PrintStream.class);
        stderr = context.mock(PrintStream.class);
        fileUtil = context.mock(IFileUtility.class);
        
        task = new MyTask("securityUtility");
        task.setFileUtility(fileUtil);
    }
    
    @Test
    public void testSuccessPath() {
        // Setup expectations
        context.checking(new Expectations() {{
            oneOf(stdin).readMaskedText("Enter password: ");
            will(returnValue("password"));
            
            oneOf(stdout).println(with(any(String.class)));
        }});
        
        // Execute
        String[] args = {"--password=password"};
        SecurityUtilityReturnCodes rc = task.handleTask(stdin, stdout, stderr, args);
        
        // Verify
        assertEquals(SecurityUtilityReturnCodes.OK, rc);
    }
}
```

### Debugging Unit Test Failures

#### Analyze Mock Expectations First

When a test fails, check:
1. Are all expected method calls being made?
2. Are methods being called in the correct order?
3. Are the correct parameters being passed?

```java
// If test fails with "unexpected invocation", check:
context.checking(new Expectations() {{
    // Make sure this matches actual call
    oneOf(stdout).println("Expected exact text");
    
    // Or use matchers for flexibility
    oneOf(stdout).println(with(containsString("partial text")));
}});
```

#### Console I/O Debugging Pattern

```java
// Capture actual output for debugging
final StringBuilder output = new StringBuilder();
context.checking(new Expectations() {{
    allowing(stdout).println(with(any(String.class)));
    will(new Action() {
        public Object invoke(Invocation invocation) {
            output.append(invocation.getParameter(0)).append("\n");
            return null;
        }
        public void describeTo(Description description) {
            description.appendText("captures output");
        }
    });
}});

// After test, print captured output
System.out.println("Actual output: " + output.toString());
```

#### File Operation Test Debugging

```java
// Mock file operations carefully
context.checking(new Expectations() {{
    oneOf(fileUtil).exists(with(any(String.class)));
    will(returnValue(false));  // File doesn't exist
    
    oneOf(fileUtil).resolveFile("output.keys");
    will(returnValue(new File("/tmp/output.keys")));
}});
```

#### Return Code Debugging

```java
// Test should verify correct return code
SecurityUtilityReturnCodes rc = task.handleTask(stdin, stdout, stderr, args);
assertEquals("Task should succeed", SecurityUtilityReturnCodes.OK, rc);

// If wrong return code, check error messages
final List<String> errors = new ArrayList<>();
context.checking(new Expectations() {{
    allowing(stderr).println(with(any(String.class)));
    will(new Action() {
        public Object invoke(Invocation invocation) {
            errors.add((String) invocation.getParameter(0));
            return null;
        }
        public void describeTo(Description description) {}
    });
}});

// Print errors after test
for (String error : errors) {
    System.out.println("Error: " + error);
}
```

## FAT (Feature Acceptance Test) Testing

### Location
`dev/com.ibm.ws.security.utility_fat/`

### Running FAT Tests

```bash
cd dev

# Run all FAT tests
./gradlew com.ibm.ws.security.utility_fat:buildandrun

# Run specific test class
./gradlew com.ibm.ws.security.utility_fat:buildandrun --tests SecurityUtilityEncodeTest

# Run single test method
./gradlew com.ibm.ws.security.utility_fat:buildandrun --tests SecurityUtilityEncodeTest.testCustomEncode

# Debug FAT tests
export DEBUG_PORT=7777
./gradlew com.ibm.ws.security.utility_fat:buildandrun --debug-jvm
```

### FAT Test Framework Components

#### 1. LibertyServer

```java
private static LibertyServer server = LibertyServerFactory.getLibertyServer("ServerName");

@BeforeClass
public static void setUp() throws Exception {
    server.installUserBundle("bundle.name");
    server.installUserFeature("feature-1.0");
}

@AfterClass
public static void tearDown() throws Exception {
    server.uninstallUserBundle("bundle.name");
}
```

**Key Methods**:
- `getInstallRoot()` - Get Liberty install directory
- `getMachine()` - Get Machine object for command execution
- `copyFileToLibertyInstallRoot()` - Copy files to Liberty install
- `installUserBundle()` / `uninstallUserBundle()` - Manage OSGi bundles
- `installUserFeature()` / `uninstallUserFeature()` - Manage features

#### 2. Machine.execute()

```java
Machine machine = server.getMachine();
Properties env = new Properties();

ProgramOutput po = machine.execute(
    securityUtilityPath,                    // Command path
    new String[] { "encode", "password" },  // Arguments
    installRoot,                            // Working directory
    env                                     // Environment variables
);

// Check results
assertEquals("Command should succeed", 0, po.getReturnCode());
assertTrue("Output should contain encoded password",
           po.getStdout().contains("{xor}"));
```

**ProgramOutput Methods**:
- `getReturnCode()` - Exit code (0 = success)
- `getStdout()` - Standard output as String
- `getStderr()` - Standard error as String
- `getCommand()` - Full command executed

#### 3. SecurityUtilityScriptUtils

```java
// Execute with environment variables
List<String> output = SecurityUtilityScriptUtils.execute(
    Arrays.asList(new SecurityUtilityScriptUtils.EnvVar("JVM_ARGS", "-Xmx512m")),
    Arrays.asList("encode", "--encoding=aes", "password"),
    false  // ignoreError
);

// Find matching output line
boolean found = SecurityUtilityScriptUtils.findMatchingLine(
    output,
    ".*\\{aes\\}.*"  // Regex pattern
);
```

### Common FAT Test Patterns

#### Pattern 1: Basic Command Execution

```java
@Test
public void testEncodeBasic() throws Exception {
    String password = "myPassword";
    
    ProgramOutput po = machine.execute(
        securityUtilityPath,
        new String[] { "encode", password },
        installRoot,
        new Properties()
    );
    
    assertEquals("Command should succeed", 0, po.getReturnCode());
    assertTrue("Output should contain encoded password",
               po.getStdout().contains("{xor}"));
    
    Log.info(thisClass, testName.getMethodName(),
             "Encoded output: " + po.getStdout());
}
```

#### Pattern 2: File Creation Validation

```java
@Test
public void testCreateLTPAKeys() throws Exception {
    String keyFile = "test_ltpa.keys";
    String keyPath = installRoot + "/" + keyFile;
    
    // Ensure file doesn't exist
    File ltpaFile = new File(keyPath);
    if (ltpaFile.exists()) {
        ltpaFile.delete();
    }
    
    // Execute command
    ProgramOutput po = machine.execute(
        securityUtilityPath,
        new String[] { "createLTPAKeys", "--file=" + keyFile, "--password=WebAS" },
        installRoot,
        new Properties()
    );
    
    assertEquals("Command should succeed", 0, po.getReturnCode());
    assertTrue("LTPA key file should be created", ltpaFile.exists());
    
    // Validate file contents
    Properties ltpaProps = new Properties();
    try (FileInputStream fis = new FileInputStream(ltpaFile)) {
        ltpaProps.load(fis);
    }
    
    assertNotNull("Should have private key",
                  ltpaProps.getProperty("com.ibm.websphere.ltpa.PrivateKey"));
    assertNotNull("Should have public key",
                  ltpaProps.getProperty("com.ibm.websphere.ltpa.PublicKey"));
    
    // Cleanup
    ltpaFile.delete();
}
```

#### Pattern 3: Error Condition Testing

```java
@Test
public void testInvalidArgument() throws Exception {
    ProgramOutput po = machine.execute(
        securityUtilityPath,
        new String[] { "encode", "--invalidOption=value", "password" },
        installRoot,
        new Properties()
    );
    
    assertNotEquals("Command should fail", 0, po.getReturnCode());
    assertTrue("Should report invalid argument error",
               po.getStdout().contains("Invalid argument") ||
               po.getStderr().contains("Invalid argument"));
}
```

#### Pattern 4: Server Integration Test

```java
@Test
public void testServerLTPAKeys() throws Exception {
    String serverName = "LTPAKeysTestServer";
    String keyFile = "resources/security/ltpa.keys";
    
    // Create LTPA keys for server
    ProgramOutput po = machine.execute(
        securityUtilityPath,
        new String[] {
            "createLTPAKeys",
            "--server=" + serverName,
            "--password=WebAS"
        },
        installRoot,
        new Properties()
    );
    
    assertEquals("Command should succeed", 0, po.getReturnCode());
    
    // Verify file created in server directory
    String serverKeyPath = installRoot + "/usr/servers/" + serverName + "/" + keyFile;
    File serverKeyFile = new File(serverKeyPath);
    assertTrue("LTPA keys should exist in server directory",
               serverKeyFile.exists());
    
    // Cleanup
    serverKeyFile.delete();
}
```

### Troubleshooting FAT Test Failures

#### Issue 1: Command Execution Fails with Return Code 32

**Symptom**: Test fails with return code 32, often on Windows

**Cause**: Java process cannot start, usually due to classpath or manifest issues

**Solution**: Add diagnostic method to print manifest

```java
private void printManifestIfRC32(ProgramOutput po) throws Exception {
    if (po.getReturnCode() == 32) {
        String jarPath = installRoot + "/lib/com.ibm.ws.security.utility.jar";
        try (JarFile jar = new JarFile(jarPath)) {
            Manifest manifest = jar.getManifest();
            Log.info(thisClass, "printManifest",
                     "Manifest: " + manifest.getMainAttributes());
        }
    }
}
```

#### Issue 2: File Not Found After Creation

**Symptom**: Command succeeds but file doesn't exist

**Solution**: Use absolute paths and check multiple locations

```java
@Test
public void testFileCreation() throws Exception {
    String absolutePath = installRoot + "/test.keys";
    
    ProgramOutput po = machine.execute(
        securityUtilityPath,
        new String[] { "createLTPAKeys", "--file=" + absolutePath, "--password=WebAS" },
        installRoot,
        new Properties()
    );
    
    // Log actual command and working directory
    Log.info(thisClass, testName.getMethodName(),
             "Command: " + po.getCommand());
    Log.info(thisClass, testName.getMethodName(),
             "Working dir: " + installRoot);
    
    // Check multiple possible locations
    File[] possibleLocations = {
        new File(absolutePath),
        new File(installRoot + "/test.keys"),
        new File("test.keys")
    };
    
    boolean found = false;
    for (File f : possibleLocations) {
        if (f.exists()) {
            Log.info(thisClass, testName.getMethodName(),
                     "Found file at: " + f.getAbsolutePath());
            found = true;
            break;
        }
    }
    
    assertTrue("File should be created", found);
}
```

#### Issue 3: Test Interference

**Symptom**: Test fails because file already exists or has wrong content

**Solution**: Implement proper cleanup

```java
@Before
public void cleanupBeforeTest() {
    String[] testFiles = {
        installRoot + "/ltpa.keys",
        installRoot + "/custom_ltpa.keys",
        installRoot + "/test.keys"
    };
    
    for (String path : testFiles) {
        File f = new File(path);
        if (f.exists()) {
            f.delete();
            Log.info(thisClass, "cleanup", "Deleted: " + path);
        }
    }
}

@After
public void cleanupAfterTest() {
    cleanupBeforeTest();
}
```

#### Issue 4: Platform-Specific Path Issues

**Symptom**: Test passes on Linux but fails on Windows (or vice versa)

**Solution**: Use `File.separator` and normalize paths

```java
@Test
public void testCrossPlatform() throws Exception {
    // Use File.separator for platform-independent paths
    String serverPath = "usr" + File.separator + "servers" +
                       File.separator + "testServer";
    
    // Or use File API
    File serverDir = new File(installRoot, "usr/servers/testServer");
    String normalizedPath = serverDir.getAbsolutePath();
    
    ProgramOutput po = machine.execute(
        securityUtilityPath,
        new String[] { "createLTPAKeys", "--server=testServer", "--password=WebAS" },
        installRoot,
        new Properties()
    );
    
    assertEquals("Should succeed", 0, po.getReturnCode());
}
```

#### Issue 5: Output Validation Fails Due to Locale

**Symptom**: Error message assertions fail on non-English systems

**Solution**: Force English locale or use regex patterns

```java
@Test
public void testErrorMessage() throws Exception {
    // Force English locale
    List<String> output = SecurityUtilityScriptUtils.execute(
        Arrays.asList(new SecurityUtilityScriptUtils.EnvVar("JVM_ARGS", "-Duser.language=en")),
        Arrays.asList("encode", "--invalidOption=value", "password"),
        true  // ignoreError
    );
    
    // Use regex for flexible matching
    boolean found = SecurityUtilityScriptUtils.findMatchingLine(
        output,
        ".*Invalid argument.*"
    );
    
    assertTrue("Should report invalid argument", found);
}
```

#### Issue 6: Timing Issues with File System

**Symptom**: Intermittent failures where file exists check fails

**Solution**: Add retry logic

```java
private boolean waitForFile(File file, int maxWaitMs) throws InterruptedException {
    int waited = 0;
    while (!file.exists() && waited < maxWaitMs) {
        Thread.sleep(100);
        waited += 100;
    }
    return file.exists();
}

@Test
public void testFileCreationWithWait() throws Exception {
    String keyPath = installRoot + "/ltpa.keys";
    File keyFile = new File(keyPath);
    
    ProgramOutput po = machine.execute(
        securityUtilityPath,
        new String[] { "createLTPAKeys", "--file=ltpa.keys", "--password=WebAS" },
        installRoot,
        new Properties()
    );
    
    assertEquals("Command should succeed", 0, po.getReturnCode());
    assertTrue("File should be created within 5 seconds",
               waitForFile(keyFile, 5000));
}
```

## FAT Test Best Practices

1. **Always Clean Up**: Use `@Before` and `@After` to ensure clean test state
2. **Log Everything**: Use `Log.info()` to record command execution and results
3. **Use Absolute Paths**: Avoid relative path issues across platforms
4. **Test Error Conditions**: Don't just test success paths
5. **Verify Side Effects**: Check files created, server state, etc.
6. **Handle Platform Differences**: Test on both Windows and Linux
7. **Use Descriptive Names**: Test method names should describe what's being tested
8. **Document Complex Tests**: Add comments explaining non-obvious test logic
9. **Avoid Hard-Coded Values**: Use constants for passwords, file names, etc.
10. **Test Cleanup**: Verify cleanup methods actually remove all artifacts

## FAT vs Unit Test Decision Guide

**Use Unit Tests When**:
- Testing individual task logic
- Mocking I/O is sufficient
- Fast feedback needed
- Testing error handling paths
- No Liberty server required

**Use FAT Tests When**:
- Testing complete command execution
- Verifying file system operations
- Testing Liberty server integration
- Validating cross-platform behavior
- Testing actual crypto operations
- Verifying help text and messages