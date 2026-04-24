# Plan: Remove Default XOR Encoding from securityUtility Tasks

## Overview

Currently, the `securityUtility` commands default to XOR encoding when no encoding parameter is specified. This plan outlines the changes needed to remove this default behavior across all affected tasks and require customers to explicitly specify their preferred encoding method (with AES encryption being the recommended option).

## Affected Commands

1. **`encode`** - Uses `--encoding` parameter (defaults to XOR)
2. **`createSSLCertificate`** - Uses `--passwordEncoding` parameter (defaults to XOR)
3. **`createLTPAKeys`** - Uses `--passwordEncoding` parameter (defaults to XOR)

## Current Behavior

### encode Command
- When `--encoding` is not specified, defaults to XOR encoding
- Code location: [`EncodeTask.java:113`](dev/com.ibm.ws.security.utility/src/com/ibm/ws/security/utility/tasks/EncodeTask.java:113)
  ```java
  ret = PasswordUtil.encode(plaintext, encodingType == null ? PasswordUtil.getDefaultEncoding() : encodingType, properties);
  ```

### createSSLCertificate Command
- When `--passwordEncoding` is not specified, defaults to XOR encoding
- Code location: [`CreateSSLCertificateTask.java:200`](dev/com.ibm.ws.security.utility/src/com/ibm/ws/security/utility/tasks/CreateSSLCertificateTask.java:200)
  ```java
  String encoding = getArgumentValue(BaseCommandTask.ARG_PASSWORD_ENCODING, args, PasswordUtil.getDefaultEncoding());
  ```

### createLTPAKeys Command
- When `--passwordEncoding` is not specified, defaults to XOR encoding
- Code location: [`CreateLTPAKeysTask.java:177`](dev/com.ibm.ws.security.utility/src/com/ibm/ws/security/utility/tasks/CreateLTPAKeysTask.java:177)
  ```java
  String encoding = getArgumentValue(BaseCommandTask.ARG_PASSWORD_ENCODING, args, PasswordUtil.getDefaultEncoding());
  ```

## Proposed Changes

### 1. Code Changes

#### 1.1 EncodeTask.java

**File**: [`dev/com.ibm.ws.security.utility/src/com/ibm/ws/security/utility/tasks/EncodeTask.java`](dev/com.ibm.ws.security.utility/src/com/ibm/ws/security/utility/tasks/EncodeTask.java)

**Changes Required**:

1. **Implement `checkRequiredArguments()` method** (lines 285-287):
   - Currently empty with comment "checkRequiredArguments is not used by this implementation"
   - **CRITICAL**: Following [`coding-rules.md:89-127`](.bob/skills/security-utility/references/coding-rules.md:89-127), we MUST implement validation here
   
   ```java
   @Override
   void checkRequiredArguments(String[] args) {
       String message = "";
       if (args.length < 2) {
           message = getMessage("insufficientArgs");
       }
       
       boolean encodingFound = false;
       
       for (String arg : args) {
           String key = arg.split("=")[0];
           if (key.equals(BaseCommandTask.ARG_ENCODING)) {
               encodingFound = true;
               break;
           }
       }
       
       if (!encodingFound) {
           if (!message.isEmpty()) {
               message += " ";
           }
           message += getMessage("encode.encodingRequired");
       }
       
       if (!message.isEmpty()) {
           throw new IllegalArgumentException(message);
       }
   }
   ```

2. **Remove default encoding logic** (line 113):
   - Change from: `encodingType == null ? PasswordUtil.getDefaultEncoding() : encodingType`
   - Change to: `encodingType`
   - Since validation ensures `encodingType` is never null, we can use it directly

3. **Update `getTaskHelp()` call** (lines 86-91):
   - Move `--encoding` from optional to required section
   ```java
   return getTaskHelp("encode.desc", "encode.usage.options",
                      "encode.required-key.", "encode.required-desc.",
                      "encode.option-key.", "encode.option-desc.",
                      null, null,
                      scriptName, customAlgorithm, customDescription);
   ```

#### 1.2 CreateSSLCertificateTask.java

**File**: [`dev/com.ibm.ws.security.utility/src/com/ibm/ws/security/utility/tasks/CreateSSLCertificateTask.java`](dev/com.ibm.ws.security.utility/src/com/ibm/ws/security/utility/tasks/CreateSSLCertificateTask.java)

**Changes Required**:

1. **Update `checkRequiredArguments()` method** (lines 255-287):
   - Add validation for `--passwordEncoding` parameter
   - Insert after password validation (around line 283):
   
   ```java
   boolean passwordEncodingFound = false;
   for (String arg : args) {
       if (arg.startsWith(ARG_PASSWORD_ENCODING)) {
           passwordEncodingFound = true;
           break;
       }
   }
   
   if (!passwordEncodingFound) {
       message += " " + getMessage("missingArg", ARG_PASSWORD_ENCODING);
   }
   ```

2. **Remove default encoding** (line 200):
   - Change from: `getArgumentValue(BaseCommandTask.ARG_PASSWORD_ENCODING, args, PasswordUtil.getDefaultEncoding())`
   - Change to: `getArgumentValue(BaseCommandTask.ARG_PASSWORD_ENCODING, args, null)`
   - Since validation ensures encoding is never null, we can safely use null as default

3. **Update `getTaskHelp()` call** (lines 100-103):
   - Move `--passwordEncoding` from optional to required section
   ```java
   return getTaskHelp("sslCert.desc", "sslCert.usage.options",
                      "sslCert.required-key.", "sslCert.required-desc.",
                      "sslCert.option-key.", "sslCert.option-desc.",
                      null, null, scriptName, ...);
   ```

#### 1.3 CreateLTPAKeysTask.java

**File**: [`dev/com.ibm.ws.security.utility/src/com/ibm/ws/security/utility/tasks/CreateLTPAKeysTask.java`](dev/com.ibm.ws.security.utility/src/com/ibm/ws/security/utility/tasks/CreateLTPAKeysTask.java)

**Changes Required**:

1. **Update `checkRequiredArguments()` method** (lines 99-120):
   - Add validation for `--passwordEncoding` parameter
   - Insert after password validation:
   
   ```java
   boolean passwordEncodingFound = false;
   for (String arg : args) {
       if (arg.startsWith(ARG_PASSWORD_ENCODING)) {
           passwordEncodingFound = true;
           break;
       }
   }
   
   if (!passwordEncodingFound) {
       message += " " + getMessage("missingArg", ARG_PASSWORD_ENCODING);
   }
   ```

2. **Remove default encoding** (line 177):
   - Change from: `getArgumentValue(BaseCommandTask.ARG_PASSWORD_ENCODING, args, PasswordUtil.getDefaultEncoding())`
   - Change to: `getArgumentValue(BaseCommandTask.ARG_PASSWORD_ENCODING, args, null)`

3. **Update `getTaskHelp()` call** (lines 82-87):
   - Move `--passwordEncoding` from optional to required section
   ```java
   return getTaskHelp("createLTPAKeys.desc", "createLTPAKeys.usage.options",
                      "createLTPAKeys.required-key.", "createLTPAKeys.required-desc.",
                      "createLTPAKeys.option-key", "createLTPAKeys.option-desc",
                      null, null, scriptName);
   ```

#### 1.4 UtilityOptions.nlsprops

**File**: [`dev/com.ibm.ws.security.utility/resources/com/ibm/ws/security/utility/resources/UtilityOptions.nlsprops`](dev/com.ibm.ws.security.utility/resources/com/ibm/ws/security/utility/resources/UtilityOptions.nlsprops)

**Changes Required**:

1. **Update encode command description** (lines 55-60):
   ```properties
   encode.desc=\
   \tEncode the provided text.\n\
   \tYou must specify an encoding method using the --encoding option.\n\
   \tAES encryption is recommended for security.\n\n\
   \tIf your password includes special characters, you must escape each special character to help ensure that the password is properly encoded.\n\
   \tSpecial characters and escape characters might vary according to your operating system.\n\
   \tFor example, on UNIX systems, pa$$W0rd must be provided as pa\\$\\$W0rd.\n\
   \tFor more information, see the Open Liberty securityUtility encode documentation.
   ```

2. **Move encoding to required options** (add after line 69):
   ```properties
   encode.required-key.encoding=\ \ \ \ --encoding=[xor|aes|hash{1}]
   encode.required-desc.encoding=\
   \tSpecify how to encode the password. Supported encodings are xor, aes, aes-128,\n\
   \tand hash. AES encryption is recommended for security. Aes encrypts by using an aes-256 bit key.\n\
   \taes-128 can be used for compatibility with server versions before 25.0.0.2. {2}
   ```

3. **Remove or update old encoding option** (lines 70-75):
   - Remove these lines since encoding is now required, not optional

4. **Update createSSLCertificate passwordEncoding** (lines 174-179):
   - Move from optional to required section
   - Add new required entries:
   ```properties
   sslCert.required-key.passwordEncoding=\ \ \ \ --passwordEncoding=[xor|aes]
   sslCert.required-desc.passwordEncoding=\
   \tSpecify how to encode the keystore password. Supported encodings are\n\
   \txor and aes. AES encryption is recommended for security.\n\
   \tUse securityUtility encode --listCustom command to see if any\n\
   \tadditional custom encryptions are supported.
   ```
   - Remove old optional entries (lines 174-179)

5. **Update createLTPAKeys passwordEncoding** (lines 275-280):
   - Move from optional to required section
   - Add new required entries:
   ```properties
   createLTPAKeys.required-key.passwordEncoding=\ \ \ \ --passwordEncoding=[xor|aes]
   createLTPAKeys.required-desc.passwordEncoding=\
   \tSpecify how to encode the LTPA keys password in the server.xml.\n\
   \tSupported encodings are xor and aes. AES encryption is recommended for security.\n\
   \tUse securityUtility encode --listCustom command to see if any\n\
   \tadditional custom encryptions are supported.
   ```
   - Remove old optional entries (lines 275-280)

#### 1.5 UtilityMessages.nlsprops

**File**: [`dev/com.ibm.ws.security.utility/resources/com/ibm/ws/security/utility/resources/UtilityMessages.nlsprops`](dev/com.ibm.ws.security.utility/resources/com/ibm/ws/security/utility/resources/UtilityMessages.nlsprops)

**Changes Required**:

1. **Add new error message** (after line 47):
   ```properties
   encode.encodingRequired=The --encoding argument is required. Specify an encoding method such as xor, aes, or hash. AES encryption is recommended for security.
   ```

### 2. Unit Test Changes

#### 2.1 EncodeTaskTest.java

**File**: [`dev/com.ibm.ws.security.utility/test/com/ibm/ws/security/utility/tasks/EncodeTaskTest.java`](dev/com.ibm.ws.security.utility/test/com/ibm/ws/security/utility/tasks/EncodeTaskTest.java)

**Changes Required**:

1. **Update `handleTask_promptWhenNoConsole()` test** (line 79):
   - Change: `String[] args = { "encode" };`
   - To: `String[] args = { "encode", "--encoding=xor" };`

2. **Update `handleTask_multiArgText()` test** (line 143):
   - Change: `String[] args = { "encode", plaintext, "extraArg" };`
   - To: `String[] args = { "encode", "--encoding=xor", plaintext, "extraArg" };`

3. **Add new test for missing encoding**:
   ```java
   @Test(expected = IllegalArgumentException.class)
   public void handleTask_missingEncodingParameter() throws Exception {
       String[] args = { "encode", plaintext };
       encode.handleTask(stdin, stdout, stderr, args);
   }
   
   @Test(expected = IllegalArgumentException.class)
   public void handleTask_missingEncodingInteractive() throws Exception {
       String[] args = { "encode" };
       encode.handleTask(stdin, stdout, stderr, args);
   }
   ```

#### 2.2 CreateSSLCertificateTaskTest.java

**File**: [`dev/com.ibm.ws.security.utility/test/com/ibm/ws/security/utility/tasks/CreateSSLCertificateTaskTest.java`](dev/com.ibm.ws.security.utility/test/com/ibm/ws/security/utility/tasks/CreateSSLCertificateTaskTest.java)

**Changes Required**:

1. **Update ALL test methods** that create SSL certificates:
   - Add `--passwordEncoding=xor` or `--passwordEncoding=aes` to all args arrays
   - Search for patterns like `new String[] { "createSSLCertificate", "--server=...`
   - Example change:
     ```java
     // Before
     String[] args = { "createSSLCertificate", "--server=myServer", "--password=myPass" };
     
     // After
     String[] args = { "createSSLCertificate", "--server=myServer", "--password=myPass", "--passwordEncoding=xor" };
     ```

2. **Add new test for missing passwordEncoding**:
   ```java
   @Test(expected = IllegalArgumentException.class)
   public void handleTask_missingPasswordEncoding() throws Exception {
       String[] args = { "createSSLCertificate", "--server=testServer", "--password=testPass" };
       task.handleTask(stdin, stdout, stderr, args);
   }
   ```

#### 2.3 CreateLTPAKeysTaskTest.java

**File**: [`dev/com.ibm.ws.security.utility/test/com/ibm/ws/security/utility/tasks/CreateLTPAKeysTaskTest.java`](dev/com.ibm.ws.security.utility/test/com/ibm/ws/security/utility/tasks/CreateLTPAKeysTaskTest.java)

**Changes Required**:

1. **Update ALL test methods** that create LTPA keys:
   - Add `--passwordEncoding=xor` or `--passwordEncoding=aes` to all args arrays
   - Example change:
     ```java
     // Before
     String[] args = { "createLTPAKeys", "--password=myPass" };
     
     // After
     String[] args = { "createLTPAKeys", "--password=myPass", "--passwordEncoding=xor" };
     ```

2. **Add new test for missing passwordEncoding**:
   ```java
   @Test(expected = IllegalArgumentException.class)
   public void handleTask_missingPasswordEncoding() throws Exception {
       String[] args = { "createLTPAKeys", "--password=testPass" };
       task.handleTask(stdin, stdout, stderr, args);
   }
   ```

### 3. FAT Test Changes

#### 3.1 SecurityUtilityEncodeTest.java

**File**: [`dev/com.ibm.ws.security.utility_fat/fat/src/com/ibm/ws/security/utility/test/SecurityUtilityEncodeTest.java`](dev/com.ibm.ws.security.utility_fat/fat/src/com/ibm/ws/security/utility/test/SecurityUtilityEncodeTest.java)

**Changes Required**:

1. **Update `testCustomEncode()` test** (line 196):
   - Change: `new String[] { "encode", textToEncode }`
   - To: `new String[] { "encode", "--encoding=xor", textToEncode }`

2. **Add new tests**:
   ```java
   @Test
   public void testEncodeMissingEncodingParameter() throws Exception {
       env = new Properties();
       ProgramOutput po = machine.execute(
           securityUtilityPath,
           new String[] { "encode", "testPassword" },
           installRoot,
           env);
       
       assertEquals("encode without --encoding should fail", FAILURE_RC, po.getReturnCode());
       assertTrue("Error should mention encoding required", 
                  po.getStdout().contains("--encoding") || po.getStderr().contains("--encoding"));
   }
   
   @Test
   public void testHelpShowsEncodingRequired() throws Exception {
       env = new Properties();
       ProgramOutput po = machine.execute(
           securityUtilityPath,
           new String[] { "help", "encode" },
           installRoot,
           env);
       
       assertEquals("help should succeed", SUCCESS_RC, po.getReturnCode());
       assertTrue("Help should show encoding as required", 
                  po.getStdout().contains("Required") && po.getStdout().contains("--encoding"));
   }
   ```

#### 3.2 SecurityUtilityCreateSSLCertificateTest.java

**File**: [`dev/com.ibm.ws.security.utility_fat/fat/src/com/ibm/ws/security/utility/test/SecurityUtilityCreateSSLCertificateTest.java`](dev/com.ibm.ws.security.utility_fat/fat/src/com/ibm/ws/security/utility/test/SecurityUtilityCreateSSLCertificateTest.java)

**Changes Required**:

1. **Update ALL test methods** that execute createSSLCertificate:
   - Add `--passwordEncoding=xor` or `--passwordEncoding=aes` to command arrays
   - Search for all `machine.execute()` calls with "createSSLCertificate"

2. **Add new test**:
   ```java
   @Test
   public void testCreateSSLCertificateMissingPasswordEncoding() throws Exception {
       ProgramOutput po = machine.execute(
           securityUtilityPath,
           new String[] { "createSSLCertificate", "--server=testServer", "--password=testPass" },
           installRoot,
           new Properties());
       
       assertEquals("createSSLCertificate without --passwordEncoding should fail", 
                    FAILURE_RC, po.getReturnCode());
   }
   ```

#### 3.3 SecurityUtilityCreateLTPAKeysTest.java

**File**: [`dev/com.ibm.ws.security.utility_fat/fat/src/com/ibm/ws/security/utility/test/SecurityUtilityCreateLTPAKeysTest.java`](dev/com.ibm.ws.security.utility_fat/fat/src/com/ibm/ws/security/utility/test/SecurityUtilityCreateLTPAKeysTest.java)

**Changes Required**:

1. **Update ALL test methods** that execute createLTPAKeys:
   - Add `--passwordEncoding=xor` or `--passwordEncoding=aes` to command arrays
   - Search for all `machine.execute()` calls with "createLTPAKeys"

2. **Add new test**:
   ```java
   @Test
   public void testCreateLTPAKeysMissingPasswordEncoding() throws Exception {
       ProgramOutput po = machine.execute(
           securityUtilityPath,
           new String[] { "createLTPAKeys", "--password=testPass" },
           installRoot,
           new Properties());
       
       assertEquals("createLTPAKeys without --passwordEncoding should fail", 
                    FAILURE_RC, po.getReturnCode());
   }
   ```

### 4. Implementation Checklist

Following [`coding-rules.md:606-615`](.bob/skills/security-utility/references/coding-rules.md:606-615):

**For EncodeTask**:
- [ ] Update `checkRequiredArguments()` to validate --encoding
- [ ] Remove default encoding logic in `handleTask()`
- [ ] Update `getTaskHelp()` to move encoding to required section
- [ ] Add `encode.encodingRequired` message to UtilityMessages.nlsprops
- [ ] Update help text in UtilityOptions.nlsprops
- [ ] Update all unit tests to include --encoding
- [ ] Add unit tests for missing encoding validation
- [ ] Update all FAT tests to include --encoding
- [ ] Add FAT tests for missing encoding validation

**For CreateSSLCertificateTask**:
- [ ] Update `checkRequiredArguments()` to validate --passwordEncoding
- [ ] Remove default encoding in line 200
- [ ] Update `getTaskHelp()` to move passwordEncoding to required section
- [ ] Update help text in UtilityOptions.nlsprops
- [ ] Update all unit tests to include --passwordEncoding
- [ ] Add unit tests for missing passwordEncoding validation
- [ ] Update all FAT tests to include --passwordEncoding
- [ ] Add FAT tests for missing passwordEncoding validation

**For CreateLTPAKeysTask**:
- [ ] Update `checkRequiredArguments()` to validate --passwordEncoding
- [ ] Remove default encoding in line 177
- [ ] Update `getTaskHelp()` to move passwordEncoding to required section
- [ ] Update help text in UtilityOptions.nlsprops
- [ ] Update all unit tests to include --passwordEncoding
- [ ] Add unit tests for missing passwordEncoding validation
- [ ] Update all FAT tests to include --passwordEncoding
- [ ] Add FAT tests for missing passwordEncoding validation

### 5. Breaking Change Impact

**This is an intentional breaking change** affecting three commands:

1. **encode command**: Scripts using `securityUtility encode` without `--encoding`
2. **createSSLCertificate command**: Scripts without `--passwordEncoding`
3. **createLTPAKeys command**: Scripts without `--passwordEncoding`

**Recommended Migration Messages**:

For `encode`:
```
The securityUtility encode command now requires the --encoding parameter.
Previously defaulted to XOR encoding (less secure than AES).

Migration:
  Old: securityUtility encode myPassword
  New: securityUtility encode --encoding=aes --key=myKey myPassword
```

For `createSSLCertificate`:
```
The securityUtility createSSLCertificate command now requires --passwordEncoding.
Previously defaulted to XOR encoding (less secure than AES).

Migration:
  Old: securityUtility createSSLCertificate --server=myServer --password=myPass
  New: securityUtility createSSLCertificate --server=myServer --password=myPass --passwordEncoding=aes --passwordKey=myKey
```

For `createLTPAKeys`:
```
The securityUtility createLTPAKeys command now requires --passwordEncoding.
Previously defaulted to XOR encoding (less secure than AES).

Migration:
  Old: securityUtility createLTPAKeys --password=myPass
  New: securityUtility createLTPAKeys --password=myPass --passwordEncoding=aes --passwordKey=myKey
```

### 6. Files to Modify Summary

| File | Lines | Changes |
|------|-------|---------|
| [`EncodeTask.java`](dev/com.ibm.ws.security.utility/src/com/ibm/ws/security/utility/tasks/EncodeTask.java) | 86-91, 113, 285-287 | Update getTaskHelp, remove default, add validation |
| [`CreateSSLCertificateTask.java`](dev/com.ibm.ws.security.utility/src/com/ibm/ws/security/utility/tasks/CreateSSLCertificateTask.java) | 100-103, 200, 255-287 | Update getTaskHelp, remove default, add validation |
| [`CreateLTPAKeysTask.java`](dev/com.ibm.ws.security.utility/src/com/ibm/ws/security/utility/tasks/CreateLTPAKeysTask.java) | 82-87, 177, 99-120 | Update getTaskHelp, remove default, add validation |
| [`UtilityOptions.nlsprops`](dev/com.ibm.ws.security.utility/resources/com/ibm/ws/security/utility/resources/UtilityOptions.nlsprops) | Multiple sections | Move encoding options to required, update descriptions |
| [`UtilityMessages.nlsprops`](dev/com.ibm.ws.security.utility/resources/com/ibm/ws/security/utility/resources/UtilityMessages.nlsprops) | After 47 | Add encode.encodingRequired message |
| [`EncodeTaskTest.java`](dev/com.ibm.ws.security.utility/test/com/ibm/ws/security/utility/tasks/EncodeTaskTest.java) | 79, 143, new tests | Update existing tests, add validation tests |
| [`CreateSSLCertificateTaskTest.java`](dev/com.ibm.ws.security.utility/test/com/ibm/ws/security/utility/tasks/CreateSSLCertificateTaskTest.java) | All tests | Add --passwordEncoding to all tests |
| [`CreateLTPAKeysTaskTest.java`](dev/com.ibm.ws.security.utility/test/com/ibm/ws/security/utility/tasks/CreateLTPAKeysTaskTest.java) | All tests | Add --passwordEncoding to all tests |
| [`SecurityUtilityEncodeTest.java`](dev/com.ibm.ws.security.utility_fat/fat/src/com/ibm/ws/security/utility/test/SecurityUtilityEncodeTest.java) | 196, new tests | Update tests, add validation tests |
| [`SecurityUtilityCreateSSLCertificateTest.java`](dev/com.ibm.ws.security.utility_fat/fat/src/com/ibm/ws/security/utility/test/SecurityUtilityCreateSSLCertificateTest.java) | All tests | Add --passwordEncoding to all tests |
| [`SecurityUtilityCreateLTPAKeysTest.java`](dev/com.ibm.ws.security.utility_fat/fat/src/com/ibm/ws/security/utility/test/SecurityUtilityCreateLTPAKeysTest.java) | All tests | Add --passwordEncoding to all tests |

### 7. Build and Test Commands

```bash
# Build the security utility
./gradlew com.ibm.ws.security.utility:build

# Run unit tests
./gradlew com.ibm.ws.security.utility:test

# Run FAT tests
./gradlew com.ibm.ws.security.utility_fat:buildandrun

# Run specific FAT test class
./gradlew com.ibm.ws.security.utility_fat:buildandrun --tests SecurityUtilityEncodeTest
./gradlew com.ibm.ws.security.utility_fat:buildandrun --tests SecurityUtilityCreateSSLCertificateTest
./gradlew com.ibm.ws.security.utility_fat:buildandrun --tests SecurityUtilityCreateLTPAKeysTest
```

## Summary

This plan removes the default XOR encoding behavior from three `securityUtility` commands:
- `encode` (--encoding parameter)
- `createSSLCertificate` (--passwordEncoding parameter)
- `createLTPAKeys` (--passwordEncoding parameter)

All three commands will now require customers to explicitly specify their preferred encoding method, with AES encryption being the recommended option for better security. The implementation follows all coding rules from the security-utility skill, including proper argument validation in `checkRequiredArguments()`, comprehensive test coverage, and clear error messages.