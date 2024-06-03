# ![C Thing Software](https://www.cthing.com/branding/CThingSoftware-57x60.png "C Thing Software") versionparser

[![CI](https://github.com/cthing/versionparser/actions/workflows/ci.yml/badge.svg)](https://github.com/cthing/versionparser/actions/workflows/ci.yml)
[![Maven Central](https://maven-badges.herokuapp.com/maven-central/org.cthing/versionparser/badge.svg)](https://maven-badges.herokuapp.com/maven-central/org.cthing/versionparser)
[![javadoc](https://javadoc.io/badge2/org.cthing/versionparser/javadoc.svg)](https://javadoc.io/doc/org.cthing/versionparser)

A Java library for parsing and working with versions and version constraints. Versions and  constraints can be
expressed in a number of common formats. The parsed versions can be queried for their components and are ordered.
Operations on parsed constraints include testing whether a given version satisfies the constraint and set operations
such as intersection and union.

The following version and version constraint schemes are supported:

* [Maven](https://maven.apache.org/)
* [Gradle](https://gradle.org/)
* [NPM](https://www.npmjs.com/)
* [RubyGems](https://rubygems.org/)
* [Semantic Versioning](https://semver.org/)
* [Calendar Versioning](https://calver.org/)

## Usage
See the [examples folder](examples) for complete working code demonstrating the usage of this library. The
library is available from [Maven Central](https://repo.maven.apache.org/maven2/org/cthing/versionparser/) using the
following Maven dependency:
```xml
<dependency>
  <groupId>org.cthing</groupId>
  <artifactId>versionparser</artifactId>
  <version>4.4.0</version>
</dependency>
```
or the following Gradle dependency:
```kotlin
implementation("org.cthing:versionparser:4.4.0")
```

### Versioning Overview

| Scheme   | Version Factory                                    | Version Constraint Factory                    |
|----------|----------------------------------------------------|-----------------------------------------------|
| Maven    | `MvnVersionScheme.parseVersion(String)`            | `MvnVersionScheme.parseConstraint(String)`    |                           
| Gradle   | `GradleVersionScheme.parseVersion(String)`         | `GradleVersionScheme.parseConstraint(String)` |                           
| Npm      | `NpmVersionScheme.parseVersion(String)`            | `NpmVersionScheme.parseConstraint(String)`    |                           
| RubyGems | `GemVersionScheme.parseVersion(String)`            | `GemVersionScheme.parseConstraint(String)`    |                           
| Semantic | `SemanticVersion.parseVersion(String)`             | N/A                                           |                           
| Calendar | `CalendarVersionScheme.parse(String)`<sup>1</sup>  | N/A                                           |

<sup>1</sup> A `CalendarVersionScheme` instance must be created to define the version format. Call the `parse` method
on that instance to create a version instance.

All version factories create an instance of a scheme-specific version class (e.g. `MvnVersion`) that implements the
`Version` interface. All version constraint factories create an instance of the `VersionConstraint` class. A
`VersionConstraint` class consists of one or more `VersionRange` instances. If desired, both the `VersionConstraint`
and `VersionRange` classes can be directly constructed. Do not mix version schemes (e.g. do not use Maven version
instances to create NPM version constraints).

### Maven Versioning
Support is provided for parsing Maven versions and dependency version constraints.

```java
// Parse versions
final MvnVersion version1 = MvnVersionScheme.parseVersion("1.2.3");
final Version version2 = MvnVersionScheme.parseVersion("2.0.7");

// Obtain information from the parsed version
assertThat(version1.getOriginalVersion()).isEqualTo("1.2.3");
assertThat(version1.isPreRelease()).isFalse();
assertThat(version1.getComponents()).containsExactly("1", "2", "3");

// Verify ordering
assertThat(version1.compareTo(version2)).isEqualTo(-1);

// Parse version constraints
final VersionConstraint constraint1 = MvnVersionScheme.parseConstraint("[1.0.0,2.0.0)");
final VersionConstraint constraint2 = MvnVersionScheme.parseConstraint("[1.5.0,3.0.0)");

// Perform constraint checking
assertThat(constraint1.allows(version1)).isTrue();
assertThat(constraint1.allows(version2)).isFalse();

// Perform constraint set operations
assertThat(constraint1.intersect(constraint2)).isEqualTo(MvnVersionScheme.parseConstraint("[1.5.0,2.0.0)"));
assertThat(constraint1.union(constraint2)).isEqualTo(MvnVersionScheme.parseConstraint("[1.0.0,3.0.0)"));
```

### Gradle Versioning
Support is provided for parsing Gradle versions and dependency version constraints.

```java
// Parse versions
final GradleVersion version1 = GradleVersionScheme.parseVersion("1.2.3-SNAPSHOT");
final Version version2 = GradleVersionScheme.parseVersion("2.0.7");

// Obtain information from the parsed version
assertThat(version1.getOriginalVersion()).isEqualTo("1.2.3-SNAPSHOT");
assertThat(version1.isPreRelease()).isTrue();
assertThat(version1.getComponents()).containsExactly("1", "2", "3", "SNAPSHOT");

// Verify ordering
assertThat(version1.compareTo(version2)).isEqualTo(-1);

// Parse version constraints
final VersionConstraint constraint1 = GradleVersionScheme.parseConstraint("[1.0.0,2.0.0[");
final VersionConstraint constraint2 = GradleVersionScheme.parseConstraint("[1.5.0,2.+]");

// Perform constraint checking
assertThat(constraint1.allows(version1)).isTrue();
assertThat(constraint1.allows(version2)).isFalse();

// Perform constraint set operations
assertThat(constraint1.intersect(constraint2)).isEqualTo(GradleVersionScheme.parseConstraint("[1.5.0,2.+]"));
assertThat(constraint1.union(constraint2)).isEqualTo(GradleVersionScheme.parseConstraint("[1.0.0,2.0.0)"));
```

### NPM Versioning
Support is provided for parsing semantic versions and NPM dependency version constraints.

```java
// Parse versions
final SemanticVersion version1 = NpmVersionScheme.parseVersion("1.2.3");
final Version version2 = NpmVersionScheme.parseVersion("2.0.7");

// Obtain information from the parsed version
assertThat(version1.getOriginalVersion()).isEqualTo("1.2.3");
assertThat(version1.isPreRelease()).isFalse();
assertThat(version1.getMajor()).isEqualTo(1);
assertThat(version1.getMinor()).isEqualTo(2);
assertThat(version1.getPatch()).isEqualTo(3);

// Verify ordering
assertThat(version1.compareTo(version2)).isEqualTo(-1);

// Parse version constraints
final VersionConstraint constraint1 = NpmVersionScheme.parseConstraint("^1.0.0");
final VersionConstraint constraint2 = NpmVersionScheme.parseConstraint(">=1.5.0 <3.0.0");

// Perform constraint checking
assertThat(constraint1.allows(version1)).isTrue();
assertThat(constraint1.allows(version2)).isFalse();

// Perform constraint set operations
assertThat(constraint1.intersect(constraint2)).isEqualTo(NpmVersionScheme.parseConstraint(">=1.5.0 <2.0.0-0"));
assertThat(constraint1.union(constraint2)).isEqualTo(NpmVersionScheme.parseConstraint(">=1.0.0 <3.0.0"));
```

### RubyGems Versioning
Support is provided for parsing RubyGems versions and version requirements.

```java
// Parse versions
final GemVersion version1 = GemVersionScheme.parseVersion("1.2.3");
final Version version2 = GemVersionScheme.parseVersion("2.0.7");

// Obtain information from the parsed version
assertThat(version1.getOriginalVersion()).isEqualTo("1.2.3");
assertThat(version1.isPreRelease()).isFalse();
assertThat(version1.getComponents()).containsExactly("1", "2", "3");

// Verify ordering
assertThat(version1.compareTo(version2)).isEqualTo(-1);

// Parse version constraints
final VersionConstraint constraint1 = GemVersionScheme.parseConstraint("~>1.0");
final VersionConstraint constraint2 = GemVersionScheme.parseConstraint(">=1.5.0", "<3.0.0");

// Perform constraint checking
assertThat(constraint1.allows(version1)).isTrue();
assertThat(constraint1.allows(version2)).isFalse();

// Perform constraint set operations
assertThat(constraint1.intersect(constraint2)).isEqualTo(GemVersionScheme.parseConstraint(">=1.5.0", "<2.ZZZ"));
assertThat(constraint1.union(constraint2)).isEqualTo(GemVersionScheme.parseConstraint(">=1.0.0", "<3.0.0"));
```

### Semantic Versioning
Support is provided for parsing semantic versions.

```java
// Parse versions
final SemanticVersion version1 = NpmVersionScheme.parseVersion("1.2.3");
final SemanticVersion version2 = NpmVersionScheme.parseVersion("2.0.7");

// Obtain information from the parsed version
assertThat(version1.getOriginalVersion()).isEqualTo("1.2.3");
assertThat(version1.isPreRelease()).isFalse();
assertThat(version1.getMajor()).isEqualTo(1);
assertThat(version1.getMinor()).isEqualTo(2);
assertThat(version1.getPatch()).isEqualTo(3);

// Verify ordering
assertThat(version1.compareTo(version2)).isEqualTo(-1);
```

Pre-release and build metadata is supported, and a "v" prefix is ignored.

```java
final SemanticVersion version = SemanticVersion.parse("v1.2.3-beta.1+56709");
        
assertThat(version.getOriginalVersion()).isEqualTo("v1.2.3-beta.1+56709");
assertThat(version).hasToString("v1.2.3-beta.1+56709");
assertThat(version.getNormalizedVersion()).isEqualTo("1.2.3-beta.1+56709");
assertThat(version.getCoreVersion()).isEqualTo("1.2.3");
assertThat(version.isPreRelease()).isTrue();
assertThat(version.getMajor()).isEqualTo(1);
assertThat(version.getMinor()).isEqualTo(2);
assertThat(version.getPatch()).isEqualTo(3);
assertThat(version.getPreReleaseIdentifiers()).containsExactly("beta", "1");
assertThat(version.getBuild()).containsExactly("56709");
```

A convenience parsing method is provided for the common case of specifying a core version
and a separate pre-release identifier.

```java
final SemanticVersion version = SemanticVersion.parse("1.2.3", "beta.1");

assertThat(version.getOriginalVersion()).isEqualTo("1.2.3-beta.1");
assertThat(version).hasToString("1.2.3-beta.1");
assertThat(version.getNormalizedVersion()).isEqualTo("1.2.3-beta.1");
assertThat(version.getCoreVersion()).isEqualTo("1.2.3");
assertThat(version.isPreRelease()).isTrue();
assertThat(version.getMajor()).isEqualTo(1);
assertThat(version.getMinor()).isEqualTo(2);
assertThat(version.getPatch()).isEqualTo(3);
assertThat(version.getPreReleaseIdentifiers()).containsExactly("beta", "1");
```

Another convenience parsing method is provided to accommodate snapshot builds. This method takes a core version
and a flag to indicate whether the version represents a snapshot. Snapshot versions are given a pre-release
component which is the number of milliseconds since the Unix Epoch.

```java
final SemanticVersion version1 = SemanticVersion("1.2.3", true);

assertThat(version1.getOriginalVersion()).isEqualTo("1.2.3-1717386681940");
assertThat(version1).hasToString("1.2.3-1717386681940");
assertThat(version1.getNormalizedVersion()).isEqualTo("1.2.3-1717386681940");
assertThat(version1.getCoreVersion()).isEqualTo("1.2.3");
assertThat(version1.isPreRelease()).isTrue();
assertThat(version1.getMajor()).isEqualTo(1);
assertThat(version1.getMinor()).isEqualTo(2);
assertThat(version1.getPatch()).isEqualTo(3);
assertThat(version1.getPreReleaseIdentifiers()).containsExactly("1717386681940");

final SemanticVersion version2 = SemanticVersion("1.2.3", false);

assertThat(version2.getOriginalVersion()).isEqualTo("1.2.3");
assertThat(version2).hasToString("1.2.3");
assertThat(version2.getNormalizedVersion()).isEqualTo("1.2.3");
assertThat(version2.getCoreVersion()).isEqualTo("1.2.3");
assertThat(version2.isPreRelease()).isFalse();
assertThat(version2.getMajor()).isEqualTo(1);
assertThat(version2.getMinor()).isEqualTo(2);
assertThat(version2.getPatch()).isEqualTo(3);
assertThat(version2.getPreReleaseIdentifiers()).isEmpty();
```

### Calendar Versioning
Support is provided for parsing calendar versions.

```java
// Parse a single version
final CalendarVersion version1 = CalendarVersionScheme.parse("YYYY.MM.0D-MAJOR", "2023.2.03-4");

// Parse multiple versions using the same format
final CalendarVersionScheme scheme = new CalendarVersionScheme("yyyy.major.minor");
final Version version2 = scheme.parse("2022.1.0");
final Version version3 = scheme.parse("2022.1.1");

// Obtain information from the parsed version
assertThat(version1.getOriginalVersion()).isEqualTo("2023.2.03-4");
assertThat(version1.isPreRelease()).isFalse();

// Obtain information about the first component of the version
final Component component1 = version1.getComponents().get(0);
assertThat(component1.getValueStr()).isEqualTo("2023");
assertThat(component1.getValue()).isEqualTo(2023);
assertThat(component1.getCategory()).isEqualTo(ComponentCategory.YEAR);

// Verify ordering
assertThat(version2.compareTo(version3)).isEqualTo(-1);
```

## Additional Information
In preparation for creating this library, a [survey of many popular versioning schemes](docs/VersioningSurvey.md) was conducted.
Among other things, this lead to the recognition that all version constraint specifications could be expressed using a single notation.
This in turn allows version constraints to be handled in a version scheme independent manner. To work with a constraint, the only
requirement of a version is that it be ordered.

## Building
The library is compiled for Java 17. If a Java 17 toolchain is not available, one will be downloaded.

Gradle is used to build the library:
```bash
./gradlew build
```
The Javadoc for the library can be generated by running:
```bash
./gradlew javadoc
```

## Releasing
This project is released on the [Maven Central repository](https://central.sonatype.com/artifact/org.cthing/versionparser).
Perform the following steps to create a release.

- Commit all changes for the release
- In the `build.gradle.kts` file, edit the `ProjectVersion` object
    - Set the version for the release. The project follows [semantic versioning](https://semver.org/).
    - Set the build type to `BuildType.release`
- Commit the changes
- Wait until CI builds the release candidate
- Run the command `mkrelease versionparser <version>`
- In a browser go to the [Maven Central Repository Manager](https://s01.oss.sonatype.org/)
- Log in
- Use the `Staging Upload` to upload the generated artifact bundle `versionparser-bundle-<version>.jar`
- Click on `Staging Repositories`
- Once it is enabled, press `Release` to release the artifacts to Maven Central
- Log out
- Wait for the new release to be available on Maven Central
- In a browser, go to the project on GitHub
- Generate a release with the tag `<version>`
- In the build.gradle.kts file, edit the `ProjectVersion` object
    - Increment the version patch number
    - Set the build type to `BuildType.snapshot`
- Update the `CHANGELOG.md` with the changes in the release and prepare for next release changes
- Update the `Usage` section in the `README.md` with the latest artifact release version
- Commit these changes
