# Security Utility Architecture

## Task-Based Command Structure

The utility follows a **plugin-style task architecture** where each command is implemented as a separate task class:

```
SecurityUtility.java (main class)
    │
    ├─ Registers tasks via addTask()
    │   ├─ EncodeTask (encode command)
    │   ├─ CreateSSLCertificateTask (createSSLCertificate command)
    │   ├─ CreateLTPAKeysTask (createLTPAKeys command)
    │   ├─ TLSProfilerTask (tlsProfiler command)
    │   ├─ ConfigureFIPSTask (configureFIPS command)
    │   └─ GenerateAesKeyTask (generateAesKey command)
    │
    ├─ HelpTask (help command - built-in)
    │
    └─ Routes CLI arguments to appropriate task's handleTask() method
```

## Key Components

### 1. SecurityUtility
Main controller that:
- Registers all available tasks
- Parses command-line arguments
- Routes execution to appropriate task
- Handles I/O streams (stdin, stdout, stderr)

**Location**: `dev/com.ibm.ws.security.utility/src/com/ibm/ws/security/utility/SecurityUtility.java`

### 2. SecurityUtilityTask Interface
Contract for all tasks:
- `getTaskName()` - Returns command name
- `getTaskDescription()` - Returns brief description
- `getTaskHelp()` - Returns detailed help text
- `handleTask()` - Executes the task logic

**Location**: `dev/com.ibm.ws.security.utility/src/com/ibm/ws/security/utility/SecurityUtilityTask.java`

### 3. BaseCommandTask
Abstract base class providing:
- Common argument parsing utilities
- Password prompting functionality
- Encoding/encryption helpers
- Message formatting utilities

**Location**: `dev/com.ibm.ws.security.utility/src/com/ibm/ws/security/utility/tasks/BaseCommandTask.java`

### 4. SecurityUtilityReturnCodes
Enum defining exit codes:
- `OK(0)` - Success
- `ERR_GENERIC(1)` - General error
- `ERR_SERVER_NOT_FOUND(2)` - Server not found
- `ERR_CLIENT_NOT_FOUND(3)` - Client not found
- `ERR_PATH_CANNOT_BE_CREATED(4)` - Path creation failed
- `ERR_FILE_EXISTS(5)` - File already exists

**Location**: `dev/com.ibm.ws.security.utility/src/com/ibm/ws/security/utility/SecurityUtilityReturnCodes.java`

## Project Structure

```
com.ibm.ws.security.utility/
├── src/com/ibm/ws/security/utility/
│   ├── SecurityUtility.java          # Main entry point
│   ├── SecurityUtilityTask.java      # Task interface
│   ├── SecurityUtilityReturnCodes.java # Exit codes enum
│   ├── IFileUtility.java             # File operations interface
│   ├── tasks/                        # Task implementations
│   │   ├── BaseCommandTask.java      # Abstract base for tasks
│   │   ├── EncodeTask.java           # Password encoding
│   │   ├── CreateLTPAKeysTask.java   # LTPA key generation
│   │   ├── CreateSSLCertificateTask.java # SSL cert creation
│   │   ├── TLSProfilerTask.java      # TLS profiling
│   │   ├── ConfigureFIPSTask.java    # FIPS configuration
│   │   ├── GenerateAesKeyTask.java   # AES key generation
│   │   └── HelpTask.java             # Help system
│   └── utils/                        # Utility classes
│       ├── ConsoleWrapper.java       # Console I/O abstraction
│       ├── FileUtility.java          # File operations
│       └── SecurityUtilityResourceBundle.java # i18n support
├── test/com/ibm/ws/security/utility/ # Unit tests
│   └── tasks/                        # Task unit tests
│       ├── EncodeTaskTest.java
│       ├── CreateLTPAKeysTaskTest.java
│       └── CreateSSLCertificateTaskTest.java
├── resources/                        # Resource files
│   └── com/ibm/ws/security/utility/resources/
│       ├── UtilityMessages.nlsprops  # Error messages
│       └── UtilityOptions.nlsprops   # Help text
└── bnd.bnd                          # OSGi bundle configuration
```

## OSGi Bundle Configuration

The security utility is packaged as an OSGi bundle with specific configuration in `bnd.bnd`:

```properties
Bundle-Name: WebSphere Security Utility
Bundle-SymbolicName: com.ibm.ws.security.utility
Bundle-Description: Security utility command-line tool

# Main class for jar execution
Main-Class: com.ibm.ws.security.utility.SecurityUtility

# Export packages for other bundles
Export-Package: \
  com.ibm.ws.security.utility;version=1.0, \
  com.ibm.ws.security.utility.tasks;version=1.0

# Import required packages
Import-Package: \
  com.ibm.ws.crypto.certificateutil, \
  com.ibm.ws.crypto.ltpakeyutil, \
  com.ibm.ws.crypto.passwordutil, \
  org.osgi.framework;version="[1.6,2)"
```

## Task Registration Pattern

Tasks are registered in `SecurityUtility.main()`:

```java
public static void main(String[] args) {
    SecurityUtility util = new SecurityUtility(SCRIPT_NAME);
    
    // Register tasks in desired order (affects help display)
    util.registerTask(new EncodeTask(SCRIPT_NAME));
    util.registerTask(new CreateSSLCertificateTask(SCRIPT_NAME));
    util.registerTask(new CreateLTPAKeysTask(SCRIPT_NAME));
    util.registerTask(new TLSProfilerTask(SCRIPT_NAME));
    util.registerTask(new ConfigureFIPSTask(SCRIPT_NAME));
    util.registerTask(new GenerateAesKeyTask(SCRIPT_NAME));
    
    // Run the program
    util.runProgram(args);
}
```

## Command-Line Interface Flow

1. User executes: `securityUtility <command> [options] [arguments]`
2. `SecurityUtility.main()` receives arguments
3. First argument is parsed as command name
4. Registered tasks are searched for matching command
5. If found, task's `handleTask()` is invoked with remaining arguments
6. Task returns `SecurityUtilityReturnCodes` value
7. Program exits with corresponding exit code

## Documentation (i18n)

- **UtilityMessages.nlsprops**: Error messages, prompts, informational messages (English only, translations handled externally)
- **UtilityOptions.nlsprops**: Help text, command descriptions, option descriptions (English only, translations handled externally)

Messages are retrieved using:
```java
String message = getMessage("message.key", arg1, arg2);
String option = getOption("option.key", true);
```

## Key Design Principles

1. **Separation of Concerns** - Each task is independent
2. **Extensibility** - Easy to add new tasks
3. **Testability** - I/O abstraction enables unit testing
4. **Internationalization** - All user-facing text externalized
5. **Error Handling** - Consistent return codes and error messages
6. **User Experience** - Interactive prompts, clear help text