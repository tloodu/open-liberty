# Plan Mode Architecture Rules (Non-Obvious Only)

## OSGi Bundle Architecture

- Each bundle is independent Gradle subproject under `dev/`
- Bundles use `.bnd` files for OSGi metadata (not MANIFEST.MF directly)
- `-sub: *.bnd` pattern includes all .bnd files (recursive configuration)
- Bundle dependencies via Import-Package, not traditional Maven/Gradle deps

## Build System Architecture

- Gradle wrapper in non-standard `.gradle-wrapper/` location
- Build system requires `cnf:initialize` task before first build
- All builds MUST run from `dev/` directory (not project root)
- Build outputs to `dev/cnf/release/dev/openliberty/<version>/`

## Test Architecture (FAT)

- FAT (Feature Acceptance Test) projects are separate Gradle subprojects
- Test configs published to `publish/` directories (not standard resources)
- Custom test runner: `@RunWith(FATRunner.class)` not standard JUnit
- Test modes control execution: LITE (default) vs FULL (explicit)

## Module Organization

- Core features: `com.ibm.ws.*` namespace
- New features: `io.openliberty.*` namespace
- Internal packages: `*.internal` or `*.internal.*` (not exported)
- Each feature can have multiple bundles

## Dependency Constraints

- Java 17 or 21 required (JAVA_HOME)
- If Java 17, JAVA_21_HOME also required
- Memory: 6400m default (configurable in gradle.properties)
- Some tests have Java version restrictions via annotations

## Integration Workflow

- PRs target `integration` branch (NOT release/master)
- All commits require GPG/SSH signing
- CLA required for contributions
- Build verification runs on PR submission