# Security Utility Coding Rules and Conventions

This document defines coding practices, patterns, and conventions for the `com.ibm.ws.security.utility` project.

## Task Implementation Rules

### Task Class Structure

All task classes MUST:
- Extend `BaseCommandTask`
- Implement `SecurityUtilityTask` interface
- Be placed in `com.ibm.ws.security.utility.tasks` package
- Follow naming convention: `[Action]Task.java` (e.g., `EncodeTask`, `CreateLTPAKeysTask`)

```java
public class MyNewTask extends BaseCommandTask {
    public MyNewTask(String scriptName) {
        super(scriptName);
    }
    
    @Override
    public String getTaskName() {
        return "myCommand";
    }
    
    @Override
    public String getTaskDescription() {
        return getOption("myCommand.desc", true);
    }
    
    @Override
    public String getTaskHelp() {
        return getTaskHelp("myCommand.desc", "myCommand.usage.options",
                          "myCommand.required-key.", "myCommand.required-desc.",
                          "myCommand.option-key", "myCommand.option-desc",
                          null, null, scriptName);
    }
    
    @Override
    public SecurityUtilityReturnCodes handleTask(ConsoleWrapper stdin, 
                                                  PrintStream stdout, 
                                                  PrintStream stderr, 
                                                  String[] args) throws Exception {
        // Implementation
        return SecurityUtilityReturnCodes.OK;
    }
}
```

### Task Registration

New tasks MUST be registered in `SecurityUtility.main()`:

```java
// Register in desired order (affects help display)
util.registerTask(new MyNewTask(SCRIPT_NAME));
```

## Argument Handling Rules

### Argument Constants

Argument names MUST:
- Be defined as `static final String` constants
- Use `--` prefix for long-form arguments
- Follow naming pattern: `ARG_[NAME]` in UPPER_SNAKE_CASE

```java
static final String ARG_PASSWORD = "--password";
static final String ARG_SERVER = "--server";
static final String ARG_FILE = "--file";
```

### Argument Validation

Tasks MUST implement `isKnownArgument()` to validate that command-line arguments are recognized by the task. This method returns `true` if the argument is valid for this task, `false` otherwise. The framework calls this before executing the task to catch typos and invalid arguments early.

```java
@Override
boolean isKnownArgument(String arg) {
    return arg.equals(ARG_PASSWORD) ||
           arg.equals(ARG_SERVER) ||
           arg.equals(ARG_FILE);
}
```

### Required Arguments Check

**CRITICAL**: Tasks with required arguments MUST override `checkRequiredArguments()` and perform ALL validation there. DO NOT perform null checks or default value assignments for required arguments in `handleTask()`.

```java
@Override
void checkRequiredArguments(String[] args) {
    String message = "";
    if (args.length < 2) {
        message = getMessage("insufficientArgs");
    }
    
    boolean passwordFound = false;
    boolean encodingFound = false;
    
    for (String arg : args) {
        String key = arg.split("=")[0];
        if (key.equals(ARG_PASSWORD)) {
            passwordFound = true;
        }
        if (key.equals(ARG_PASSWORD_ENCODING)) {
            encodingFound = true;
        }
    }
    
    if (!passwordFound) {
        message += " " + getMessage("missingArg", ARG_PASSWORD);
    }
    
    if (!encodingFound) {
        if (!message.isEmpty()) {
            message += " ";
        }
        message += getMessage("encodingRequired");
    }
    
    if (!message.isEmpty()) {
        throw new IllegalArgumentException(message);
    }
}
```

Then in `handleTask()`, assume required arguments exist:

```java
// CORRECT: No null check, no default - validation already done
String password = getArgumentValue(ARG_PASSWORD, args, null);
String encoding = getArgumentValue(ARG_PASSWORD_ENCODING, args, null);

// WRONG: Don't validate in handleTask()
if (encoding == null) {  // ❌ NO! Validate in checkRequiredArguments()
    throw new IllegalArgumentException(getMessage("encodingRequired"));
}
```

### Mutually Exclusive Arguments

Define groups of arguments that cannot be used together. Each set represents arguments that are mutually exclusive within that group.

```java
private static final List<Set<String>> EXCLUSIVE_ARGUMENTS = Arrays.asList(
    new HashSet<String>(Arrays.asList(ARG_KEY, ARG_BASE64_KEY, ARG_AES_CONFIG_FILE)),
    new HashSet<String>(Arrays.asList(ARG_SERVER, ARG_FILE))
);
```

Then validate in `handleTask()` by calling `validateArgumentList()` with any required arguments:
```java
validateArgumentList(args, Arrays.asList(ARG_PASSWORD));
```

This will check that only one argument from each exclusive group is provided and report an error if multiple are used together.

### Argument Parsing

Use `getArgumentValue()` to extract argument values from the command-line args array. This method handles the `--arg=value` format and returns the value portion.

```java
String password = getArgumentValue(ARG_PASSWORD, args, null);  // No default, returns null if not provided
String server = getArgumentValue(ARG_SERVER, args, null);      // No default
String file = getArgumentValue(ARG_FILE, args, DEFAULT_FILE_NAME);  // Uses default if not provided
```

**Parameters**:
- `argumentName` - The argument to look for (e.g., `--password`)
- `args` - The command-line arguments array
- `defaultValue` - Value to return if argument not found (use `null` for required args)

## Return Code Rules

Tasks MUST:
- Return `SecurityUtilityReturnCodes` enum values
- Use `SecurityUtilityReturnCodes.OK` for success
- Use appropriate error codes for failures
- NEVER return `null`

**Available Return Codes**:
- `OK(0)` - Successful completion
- `ERR_GENERIC(1)` - General error
- `ERR_SERVER_NOT_FOUND(2)` - Server directory not found
- `ERR_CLIENT_NOT_FOUND(3)` - Client directory not found
- `ERR_PATH_CANNOT_BE_CREATED(4)` - Cannot create required path
- `ERR_FILE_EXISTS(5)` - File already exists

```java
if (!fileUtility.exists(serverDir)) {
    stderr.println(getMessage("serverNotFound", serverName, serverDir));
    return SecurityUtilityReturnCodes.ERR_SERVER_NOT_FOUND;
}
```

## Internationalization (i18n) Rules

### Message Externalization

ALL user-facing text MUST be externalized to English `.nlsprops` files.

**Message Files**:
- `UtilityMessages.nlsprops` - Error messages, prompts (English only, translations handled externally)
- `UtilityOptions.nlsprops` - Help text, option descriptions (English only, translations handled externally)

### Message Key Naming

**For UtilityMessages.nlsprops**:
- Error messages: `error.[description]`
- Task-specific messages: `[taskName].[description]`
- Common messages: `[description]` (no prefix)

**For UtilityOptions.nlsprops**:
- Task description: `[taskName].desc`
- Usage: `[taskName].usage.options`
- Required options: `[taskName].required-key.[argName]` and `[taskName].required-desc.[argName]`
- Optional options: `[taskName].option-key.[argName]` and `[taskName].option-desc.[argName]`

**Example** (UtilityMessages.nlsprops):
```properties
# Error messages
error.missingIO=Error, missing I/O device: {0}.
error.inputConsoleNotAvailable=Input console is not available.

# Task-specific messages
encode.enterText=Enter text:
encode.reenterText=Re-enter text:
encode.entriesDidNotMatch=Entries did not match.

# Common messages
insufficientArgs=Insufficient arguments.
missingArg=Missing argument {0}.
```

**Example** (UtilityOptions.nlsprops):
```properties
# Task description
encode.desc=\
\tEncode a password for use in server configuration.

# Usage
encode.usage.options=\
\t{0} encode [options]

# Required options
encode.required-key.password=\ \ \ \ --password
encode.required-desc.password=\
\tThe password to encode.

# Optional options
encode.option-key.encoding=\ \ \ \ --encoding=[xor|aes|hash{1}]
encode.option-desc.encoding=\
\tSpecify how to encode the password. Supported encodings are xor, \n\
\taes, and hash. The default encoding is xor.
```

### Message Retrieval

```java
// In task classes
String message = getMessage("messageKey", arg1, arg2);
String option = getOption("optionKey", true, arg1, arg2);

// Direct access
String message = CommandUtils.getMessage("messageKey", args);
String option = CommandUtils.getOption("optionKey", forceFormat, args);
```

### Help Text Formatting

Help text MUST:
- Limit lines to 80 characters
- Use `\n\` for line continuations in `.nlsprops` files
- Use `\t` for indentation
- Begin option keys with `\ \ \ \ ` (4 spaces) for leading whitespace
- Begin descriptions with `\t`

## Console I/O Rules

### Console Wrapper Usage

ALWAYS use `ConsoleWrapper`, NEVER use `System.console()` directly:

```java
// Correct
ConsoleWrapper stdin = ...;
String password = stdin.readMaskedText("Enter password: ");

// Incorrect - DO NOT DO THIS
Console console = System.console();
char[] password = console.readPassword("Enter password: ");
```

**Reason**: `ConsoleWrapper` enables unit testing by abstracting console operations.

### Stream Parameters

Tasks MUST accept and use provided streams:

```java
@Override
public SecurityUtilityReturnCodes handleTask(ConsoleWrapper stdin, 
                                              PrintStream stdout, 
                                              PrintStream stderr, 
                                              String[] args) {
    // Use provided streams
    stdout.println("Success message");
    stderr.println("Error message");
    
    // NEVER use System.out or System.err directly
}
```

### Password Prompting

Use `BaseCommandTask` helper methods:

```java
// Prompt for password with confirmation
String password = promptForPassword(stdin, stdout, 
                                   "Enter password: ", 
                                   "Re-enter password: ");

// Prompt for text with confirmation
String text = promptForText(stdin, stdout, 
                           "Enter text: ", 
                           "Re-enter text: ");
```

## File Operations Rules

### File Utility Usage

Use `IFileUtility` for all file operations:

```java
IFileUtility fileUtil = ...;

// Check existence
if (fileUtil.exists(path)) { ... }

// Get directories
String serversDir = fileUtil.getServersDirectory();
String clientsDir = fileUtil.getClientsDirectory();

// Resolve paths
File outputFile = fileUtil.resolveFile(filename);
```

### Path Separators

ALWAYS use `File.separatorChar` for path construction:

```java
static final String SLASH = String.valueOf(File.separatorChar);

String serverDir = usrServers + serverName + SLASH;
String resourcesDir = serverDir + "resources" + SLASH + "security" + SLASH;
```

**DO NOT** hardcode `/` or `\\` in paths.

### File Existence Checks

Check file/directory existence before operations:

```java
if (!fileUtility.exists(serverDir)) {
    stderr.println(getMessage("serverNotFound", serverName, serverDir));
    return SecurityUtilityReturnCodes.ERR_SERVER_NOT_FOUND;
}

if (fileUtility.exists(outputFile)) {
    stderr.println(getMessage("fileExists", outputFile));
    return SecurityUtilityReturnCodes.ERR_FILE_EXISTS;
}
```

## Testing Rules

### Test Class Naming

Test classes MUST:
- Be named `[ClassName]Test.java`
- Be placed in same package as class under test (in `test/` directory)
- Test all public methods

**Example**: `EncodeTask.java` → `EncodeTaskTest.java`

### Mock Objects

Use JMock for mocking dependencies:

```java
@Rule
public JUnitRuleMockery context = new JUnitRuleMockery();

private ConsoleWrapper stdin;
private PrintStream stdout;
private PrintStream stderr;

@Before
public void setUp() {
    stdin = context.mock(ConsoleWrapper.class);
    stdout = context.mock(PrintStream.class);
    stderr = context.mock(PrintStream.class);
}
```

### Test Method Naming

Test methods MUST:
- Use descriptive names indicating what is being tested
- Follow pattern: `methodName_scenario` or `scenario`
- Use underscores for readability

**Examples**:
```java
@Test
public void getTaskName() { ... }

@Test
public void handleTask_providedPassword_fileCreated() { ... }

@Test
public void handleTask_specifiedServer_serverDoesNotExist() { ... }
```

### Test Expectations

Set up expectations before calling method under test:

```java
@Test
public void handleTask_promptPassword_fileCreated() {
    // Setup expectations
    context.checking(new Expectations() {{
        oneOf(stdin).readMaskedText("Enter password: ");
        will(returnValue("password"));
        
        oneOf(stdin).readMaskedText("Re-enter password: ");
        will(returnValue("password"));
        
        oneOf(stdout).println(with(any(String.class)));
    }});
    
    // Execute
    SecurityUtilityReturnCodes rc = task.handleTask(stdin, stdout, stderr, args);
    
    // Verify
    assertEquals(SecurityUtilityReturnCodes.OK, rc);
}
```

## Error Handling Rules

### Exception Handling in Tasks

Tasks MUST:
- Catch specific exceptions when possible
- Convert exceptions to appropriate return codes
- Print meaningful error messages to stderr
- NEVER let exceptions propagate to main controller (except `IllegalArgumentException`)

```java
try {
    // Task logic
    return SecurityUtilityReturnCodes.OK;
} catch (IllegalArgumentException e) {
    // Let IllegalArgumentException propagate for argument validation
    throw e;
} catch (IOException e) {
    stderr.println(getMessage("error.fileOperation", e.getMessage()));
    return SecurityUtilityReturnCodes.ERR_GENERIC;
} catch (Exception e) {
    stderr.println(getMessage("error", e.toString()));
    e.printStackTrace(stderr);
    return SecurityUtilityReturnCodes.ERR_GENERIC;
}
```

### Argument Validation Errors

For invalid arguments, throw `IllegalArgumentException`:

```java
if (invalidCondition) {
    throw new IllegalArgumentException(getMessage("invalidArg", argName));
}
```

## Code Style Rules

### Copyright Headers

ALL source files MUST include EPL 2.0 copyright header:

```java
/*******************************************************************************
 * Copyright (c) [Creation Year],[Modified Year] IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
```

### Code Style Consistency

**CRITICAL**: ALWAYS examine existing similar code in the same file BEFORE writing new code. Match the exact formatting, patterns, and conventions (whitespace, brace placement, error message building, etc.). Don't assume - look at the actual code.

## Common Anti-Patterns to Avoid

### DO NOT: Hardcode Strings

```java
// Wrong
stdout.println("Password encoded successfully");

// Correct
stdout.println(getMessage("encode.success"));
```

### DO NOT: Use System.console() Directly

```java
// Wrong
Console console = System.console();
char[] password = console.readPassword("Enter password: ");

// Correct
String password = stdin.readMaskedText(getMessage("encode.enterPassword"));
```

### DO NOT: Hardcode File Paths

```java
// Wrong
String path = serverDir + "/resources/security/ltpa.keys";

// Correct
String path = serverDir + "resources" + SLASH + "security" + SLASH + "ltpa.keys";
```

### DO NOT: Return null

```java
// Wrong
public SecurityUtilityReturnCodes handleTask(...) {
    return null;  // NEVER DO THIS
}

// ✅ Correct
public SecurityUtilityReturnCodes handleTask(...) {
    return SecurityUtilityReturnCodes.OK;
}
```

### DO NOT: Ignore Exceptions

```java
// Wrong
try {
    // code
} catch (Exception e) {
    // Silent failure
}

// Correct
try {
    // code
} catch (Exception e) {
    stderr.println(getMessage("error", e.toString()));
    e.printStackTrace(stderr);
    return SecurityUtilityReturnCodes.ERR_GENERIC;
}
```

## Checklists

### Checklist for New Tasks

- [ ] Task class extends `BaseCommandTask`
- [ ] Task implements `SecurityUtilityTask` interface
- [ ] Task registered in `SecurityUtility.main()`
- [ ] Argument constants defined
- [ ] `isKnownArgument()` implemented
- [ ] `checkRequiredArguments()` implemented if needed
- [ ] `handleTask()` returns appropriate return codes
- [ ] All user-facing text externalized to default English `.nlsprops` files (translations handled externally)
- [ ] Help text formatted correctly (80 char limit)
- [ ] Unit tests created with JMock
- [ ] All public methods tested
- [ ] Copyright header added
- [ ] Code follows existing patterns in file

### Checklist for Modifying Existing Tasks

- [ ] New arguments added to constants
- [ ] `isKnownArgument()` updated
- [ ] `checkRequiredArguments()` updated if needed
- [ ] Backward compatibility maintained
- [ ] Help text updated
- [ ] New messages added to default English `.nlsprops` files (translations handled externally)
- [ ] Existing unit tests updated and new unit tests added for new functionality
- [ ] All existing tests still pass
- [ ] Breaking changes documented
- [ ] Code style matches existing code