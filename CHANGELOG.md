# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [3.0.0] - 2023-09-15

### Added

- Complete redesign of the library
- Support for parsing Maven versions and version constraints
- Support for parsing Gradle versions and version constraints
- Support for parsing NPM versions and version constraints
- Support for parsing Semantic versions
- Support for parsing Calendar versions

### Removed

- The original `Version` class has been removed. Use one of the new versioning classes instead.

## [2.0.0] - 2022-07-12

### Changed

- Migrate to Java 17

## [1.0.0] - 2016-12-27

### Added

- Parsing versions of various formats using the `Version` class.

[3.0.0]: https://github.com/cthing/versionparser/releases/tag/3.0.0
[2.0.0]: https://github.com/cthing/versionparser/releases/tag/2.0.0
[1.0.0]: https://github.com/cthing/versionparser/releases/tag/1.0.0
