# ![C Thing Software](https://www.cthing.com/branding/CThingSoftware-57x60.png "C Thing Software") versionparser

[![CI](https://github.com/cthing/versionparser/actions/workflows/ci.yml/badge.svg)](https://github.com/cthing/versionparser/actions/workflows/ci.yml)

A Java library for parsing and working with versions and version constraints. Versions and  constraints can be
expressed in a number of common formats. The parsed versions can be queried for their components and are ordered.
Operations on parsed constraints include testing whether a given version satisfies the constraint and set operations
such as intersection and union.

The following version and version constraint schemes are supported:

* [Maven](https://maven.apache.org/)
* [Gradle](https://gradle.org/)
* [NPM](https://www.npmjs.com/)
* [Semantic Versioning](https://semver.org/)
* [Calendar Versioning](https://calver.org/)

### Usage
See the [examples folder](examples) for complete working code demonstrating the usage of this library.

#### Maven Versioning
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

#### Gradle Versioning
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

#### NPM Versioning
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

#### Semantic Versioning
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

#### Calendar Versioning
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

### Additional Information
In preparation for creating this library, a [survey of many popular versioning schemes](docs/VersioningSurvey.md) was conducted.
Among other things, this lead to the recognition that all version constraint specifications could be expressed using a single notation.
This in term allows version constraints to be handled in a version scheme independent manner. To work with a constraint, the only
requirement of a version is that it be ordered.

### Building
The libray is compiled for Java 17. If a Java 17 toolchain is not available, one will be downloaded.

Gradle is used to build the library:
```bash
./gradlew build
```
The Javadoc for the library can be generated by running:
```bash
./gradlew javadoc
```
