# Ask Mode Documentation Rules (Non-Obvious Only)

## Project Structure Context

- `dev/` contains ALL development code (counterintuitive - not at root)
- Each `com.ibm.ws.*` or `io.openliberty.*` directory is a separate OSGi bundle
- Projects ending in `_fat` are test projects (FAT = Feature Acceptance Test)

## Documentation Locations

- Bundle documentation in `bnd.bnd` files (OSGi metadata)
- Package documentation in `package-info.java` files (common pattern)
- Test documentation in `fat/src/` subdirectories
- Build documentation in `dev/wlp-gradle/` scripts

## Non-Standard Naming

- FAT tests use custom `@RunWith(FATRunner.class)` not standard JUnit
- Test modes: `@Mode(TestMode.LITE)` vs `@Mode(TestMode.FULL)`
- `publish/` directories contain test artifacts, not build outputs

## Hidden Build Context

- Gradle wrapper location: `.gradle-wrapper/` (non-standard)
- Build must start from `dev/` directory
- Initial setup: `./gradlew cnf:initialize` required before builds
- Memory settings in `dev/gradle.properties` (6400m default)

## Java Version Context

- Requires Java 17 or 21 in JAVA_HOME
- If Java 17, also need JAVA_21_HOME set
- Some tests have `@MinimumJavaLevel` or `@MaximumJavaLevel` restrictions

## Commit Context

- PRs target `integration` branch (NOT `release` or `master`)
- All commits require GPG/SSH signing
- CLA required for non-trivial changes