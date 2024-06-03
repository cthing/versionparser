# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [unreleased]

### Added

- The `SemanticVersion.parse(String coreVersion, String preReleaseIdentifier)` and
  `SemanticVersion.parse(String coreVersion, boolean snapshot)` convenience methods.

## [4.3.0] - 2024-05-25

### Added

- The `SemanticVersion.getCoreVersion()` method to obtain the major, minor and patch version without
  pre-release and build metadata
- [Dependency analysis Gradle plugin](https://github.com/autonomousapps/dependency-analysis-gradle-plugin)
- The `check` task now depends on the `buildHealth` task and will fail the build on health violations

### Changed
 
- Changed JSR-305 dependency from `implementation` to `api`
- Use cthing-projectversion library `ProjectVersion` object for project version

## [4.2.1] - 2023-12-22

### Added

- Dependency on org.cthing:cthing-annotations library

### Changed

- Migrated from internal annotations to using cthing-annotations library

### Removed

- The internal package org.cthing.versionparser.internal has been removed

## [4.2.0] - 2023-11-23

### Added

- Added RubyGems versioning

## [4.1.0] - 2023-10-02

### Added

- Added a `weak` property to a `VersionConstraint` to support Maven's concept of a `soft` constraint (i.e. an
  undecorated dependency verson).

## [4.0.0] - 2023-09-20

### Changed

- Annotations moved to the `org.cthing.versionparser.internal` package since they should not be
  considered part of the public API.

## [3.0.0] - 2023-09-18

### Added

- First version published to Maven Central
- Complete redesign of the library
- Support for parsing Maven versions and version constraints
- Support for parsing Gradle versions and version constraints
- Support for parsing NPM versions and version constraints
- Support for parsing Semantic versions
- Support for parsing Calendar versions

### Removed

- The original `Version` class has been removed. Use one of the new versioning classes instead.

## 2.0.0 - 2022-07-12

### Changed

- Migrate to Java 17

## 1.0.0 - 2016-12-27

### Added

- Parsing versions of various formats using the `Version` class.

[unreleased]: https://github.com/cthing/versionparser/compare/4.3.0...HEAD
[4.3.0]: https://github.com/cthing/versionparser/releases/tag/4.3.0
[4.2.1]: https://github.com/cthing/versionparser/releases/tag/4.2.1
[4.2.0]: https://github.com/cthing/versionparser/releases/tag/4.2.0
[4.1.0]: https://github.com/cthing/versionparser/releases/tag/4.1.0
[4.0.0]: https://github.com/cthing/versionparser/releases/tag/4.0.0
[3.0.0]: https://github.com/cthing/versionparser/releases/tag/3.0.0
