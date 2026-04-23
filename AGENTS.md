# AGENTS.md

This file provides guidance to agents when working with code in this repository.

## Build System (Non-Obvious)

- Build commands MUST be run from `dev/` directory, not project root
- Gradle wrapper is in `.gradle-wrapper/` (non-standard location)
- Initial setup requires: `cd dev && ./gradlew cnf:initialize` before any build
- Memory: Default 6400m in gradle.properties; increase if OOM occurs, then run `./gradlew --stop`

## Test Execution (FAT Tests)

- FAT (Feature Acceptance Test) projects end with `_fat` suffix
- Run single FAT: `./gradlew <project>_fat:buildandrun` (e.g., `./gradlew build.example_fat:buildandrun`)
- FAT tests use `@RunWith(FATRunner.class)` annotation (custom runner, not standard JUnit)
- Test modes: `@Mode(TestMode.LITE)` runs by default, `@Mode(TestMode.FULL)` requires explicit flag
- FAT tests are in `fat/src/` subdirectory within `_fat` projects

## Project Structure (Non-Standard)

- All development code is under `dev/` directory
- Each bundle/feature is a separate Gradle subproject under `dev/`
- Bundle projects use `.bnd` files (OSGi metadata) - REQUIRED for all bundles
- `-sub: *.bnd` in bnd files means "include all .bnd files in this directory"
- Project naming: `com.ibm.ws.*` for core, `io.openliberty.*` for newer features

## Code Style (Discovered)

- Copyright header REQUIRED: EPL 2.0 format with year range (see dev/cnf/resources/bnd/bundle.props)
- Package-info.java files are common and contain package-level documentation
- Import order: `!*.internal.*` excluded first (see bundle.props defaultPackageImport)

## Java Version Requirements

- JAVA_HOME must point to Java 17 or Java 21
- If using Java 17, JAVA_21_HOME environment variable also required
- Some tests have `@MinimumJavaLevel` or `@MaximumJavaLevel` annotations

## Commit Requirements (Critical)

- ALL commits MUST be GPG/SSH signed (enforced at repo level)
- PRs target `integration` branch, NOT `release` or `master`
- CLA (Contributor License Agreement) required for non-trivial changes

## Testing Artifacts

- Test servers/clients publish configs to `publish/` directories within test projects
- Server configs are XML files in `publish/servers/<servername>/`
- Client configs in `publish/clients/<clientname>/`