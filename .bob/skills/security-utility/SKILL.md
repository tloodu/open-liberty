---
name: security-utility
description: Work with Open Liberty securityUtility CLI tool - creating and debugging unit and FAT tests, implementing new security commands, modifying existing tasks, understanding OSGi task architecture. Use this skill when debugging test failures, implementing new security utility commands, modifying existing tasks, troubleshooting crypto operations, or understanding the security utility architecture.
---

# Security Utility Project Context

## Purpose

This skill provides comprehensive guidance for working with the `com.ibm.ws.security.utility` command-line tool in Open Liberty. It supports debugging test failures (unit and FAT), implementing new security commands, modifying existing tasks, and understanding the task-based CLI architecture.

**Project Location**: `dev/com.ibm.ws.security.utility`

## Inputs

### Required Inputs

- **Task Type**: The type of work being performed
  - Debugging unit test failures
  - Debugging FAT test failures
  - Creating a new task
  - Modifying an existing task
  - Understanding architecture
- **Context**: Relevant files, error messages, or requirements depending on task type

### Optional Inputs

- **Test Output**: Test failure logs and stack traces (for debugging)
- **Requirements**: Specifications for new features (for implementation)
- **Error Messages**: Specific error messages to investigate

## Outputs

**Artifact Location:** Work is performed directly in the project, no artifacts generated

**Deliverables:**
- Fixed unit or FAT tests
- New task implementation with tests
- Modified task with updated tests
- Architecture understanding documentation

## Steps

```
<Steps>
<Step>
Identify the Task Type:
- Determine which type of work is needed:
  - **Debugging Unit Tests**: JUnit test failures with JMock
  - **Debugging FAT Tests**: Integration test failures with Liberty servers
  - **Creating New Task**: Implementing a new security utility command
  - **Modifying Existing Task**: Adding options or functionality to existing command
  - **Understanding Architecture**: Learning the task-based CLI structure
- Read the appropriate reference documentation:
  - Architecture: `references/architecture.md`
  - Coding Rules: `references/coding-rules.md`
  - Testing: `references/testing-guide.md`
  - Crypto Components: `references/crypto-components.md`
</Step>

<Step>
For Debugging Unit Test Failures:
- Read the test failure output and stack trace
- Identify which task class is being tested
- Check `references/testing-guide.md` for unit testing patterns
- Analyze mock expectations:
  - Are all expected method calls being made?
  - Are methods called in correct order?
  - Are correct parameters being passed?
- Common issues:
  - Mock expectations don't match actual calls
  - Console I/O expectations incorrect
  - File operation mocks not set up properly
  - Return code assertions failing
- Use debugging patterns from `references/testing-guide.md`
- Fix the test or implementation as needed
- Run tests to verify: `./gradlew com.ibm.ws.security.utility:test`
</Step>

<Step>
For Debugging FAT Test Failures:
- Read the FAT test failure output
- Check `references/testing-guide.md` for FAT testing patterns
- Common issues:
  - Return code 32 (manifest/classpath issues)
  - File not found after creation (path issues)
  - Test interference (cleanup issues)
  - Platform-specific path problems
  - Locale-dependent error messages
  - File system timing issues
- Use troubleshooting patterns from `references/testing-guide.md`
- Check test artifacts in `build/libs/autoFVT/`
- Fix the test or implementation
- Run FAT tests: `./gradlew com.ibm.ws.security.utility_fat:buildandrun --tests TestClass`
</Step>

<Step>
For Creating a New Task:
- Read `references/coding-rules.md` for task implementation rules
- Follow the implementation checklist:
  1. Create task class extending `BaseCommandTask`
  2. Implement `SecurityUtilityTask` interface methods
  3. Define argument constants
  4. Implement `isKnownArgument()` for validation
  5. Implement `checkRequiredArguments()` if needed
  6. Implement `handleTask()` with task logic
  7. Add i18n messages to English `.nlsprops` files
  8. Register task in `SecurityUtility.main()`
  9. Create unit tests with JMock
  10. Create FAT tests if needed
- Follow patterns from existing tasks in `dev/com.ibm.ws.security.utility/src/com/ibm/ws/security/utility/tasks/`
- Use crypto components from `references/crypto-components.md` if needed
- Ensure all coding rules from `references/coding-rules.md` are followed
- Build and test: `./gradlew com.ibm.ws.security.utility:build`
</Step>

<Step>
For Modifying an Existing Task:
- Read `references/coding-rules.md` for modification guidelines
- Identify the task class to modify
- Follow the modification checklist:
  1. Add new argument constants if needed
  2. Update `isKnownArgument()` validation
  3. Update `checkRequiredArguments()` if adding required args
  4. Add mutually exclusive argument handling if needed
  5. Implement new logic in `handleTask()`
  6. Add new i18n messages to English `.nlsprops` files
  7. Update help text
  8. Update existing unit tests and add new unit tests for new functionality
  9. Update FAT tests and add new FAT tests for new functionality
  10. Verify backward compatibility
- **CRITICAL**: When removing defaults or requiring new arguments, ALL unit tests MUST be updated accordingly if a breaking change
- Test existing functionality still works
- Build and test: `./gradlew com.ibm.ws.security.utility:build`
</Step>

<Step>
For Understanding Architecture:
- Read `references/architecture.md` for complete architecture overview
- Key concepts to understand:
  - Task-based command structure
  - SecurityUtility main controller
  - SecurityUtilityTask interface
  - BaseCommandTask abstract class
  - SecurityUtilityReturnCodes enum
  - OSGi bundle configuration
  - Task registration pattern
- Review existing task implementations in `dev/com.ibm.ws.security.utility/src/com/ibm/ws/security/utility/tasks/`
- Understand crypto component integration from `references/crypto-components.md`
</Step>

<Step>
Execute and Validate:
- Run appropriate tests based on changes made
- For unit tests: `./gradlew com.ibm.ws.security.utility:test`
- For FAT tests: `./gradlew com.ibm.ws.security.utility_fat:buildandrun`
- For specific test: Add `--tests ClassName.methodName`
- Verify all tests pass
- Check for regressions in existing functionality
- Review code against coding rules checklist
- Ensure all user-facing text is externalized to default English `.nlsprops` files (translations handled externally)
- Verify help text follows 80-character limit
- Confirm copyright headers are present
</Step>
</Steps>
```

## Notes

### Key Design Principles

1. **Separation of Concerns** - Each task is independent
2. **Extensibility** - Easy to add new tasks
3. **Testability** - I/O abstraction enables unit testing
4. **Internationalization** - All user-facing text externalized
5. **Error Handling** - Consistent return codes and error messages
6. **User Experience** - Interactive prompts, clear help text

### Important Reminders

- All file paths use `File.separatorChar` for cross-platform compatibility
- Console operations wrapped in `ConsoleWrapper` for testability
- Return codes must be from `SecurityUtilityReturnCodes` enum
- Help text must fit within 80 characters per line
- All exceptions caught and converted to appropriate return codes
- **Code Style**: ALWAYS examine existing similar code in the same file BEFORE writing new code

### Reference Documentation

All detailed documentation is in the `references/` folder:
- **Architecture**: Complete system architecture and component details
- **Coding Rules**: All coding standards, patterns, and anti-patterns
- **Testing Guide**: Unit and FAT testing patterns and troubleshooting
- **Crypto Components**: Integration with crypto utilities

### Related Projects

- `com.ibm.ws.security.utility.securityutil` - Additional security utilities
- `com.ibm.ws.crypto.certificateutil` - SSL certificate creation
- `com.ibm.ws.crypto.ltpakeyutil` - LTPA key generation
- `com.ibm.ws.crypto.passwordutil` - Password encoding
- `com.ibm.ws.crypto.aeskeyutil` - AES key management