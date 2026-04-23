# Code Mode Rules (Non-Obvious Only)

## OSGi Bundle Requirements

- Every bundle MUST have a `.bnd` file with proper OSGi metadata
- Use `-sub: *.bnd` to include all .bnd files in directory (common pattern)
- Bundle-Version format: `${bFullVersion}.${version.qualifier}` (auto-generated)
- Import-Package MUST exclude internal packages: `!*.internal.*` comes first

## Build Artifacts Location

- Gradle wrapper in `.gradle-wrapper/` not standard `gradle/wrapper/`
- Build outputs go to `dev/cnf/release/dev/openliberty/<version>/`
- Must run `./gradlew cnf:initialize` before first build

## Test File Organization

- FAT test configs in `publish/servers/<name>/` and `publish/clients/<name>/`
- Test source in `fat/src/` subdirectory (not `src/test/`)
- Server XML configs are in publish directories, not resources

## Copyright Headers

- MUST use EPL 2.0 format with year range
- Template in `dev/cnf/resources/bnd/bundle.props`
- Format: `Copyright (c) YYYY, ${copyrightBuildYear} IBM Corporation and others`

## Package Naming Conventions

- Core features: `com.ibm.ws.*` or `com.ibm.websphere.*`
- New features: `io.openliberty.*`
- Internal packages: `*.internal` or `*.internal.*` (excluded from exports)

## No Access to MCP and Browser Tools

This mode does not have access to MCP servers or browser automation tools.