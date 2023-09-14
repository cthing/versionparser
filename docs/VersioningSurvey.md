# Versioning Survey

This document surveys the schemes used by various specifications, build tools and package managers to specify 
version numbers, constraints and precedence.

- **Version number** - The format for specifying the version of an artifact. Even though a version may consist
of a combination of numbers, letters, hyphens and dots, the entire combination is referred to as a version number.
An example of a version number is `1.0.1`.

- **Version constraint** - The notation used to specify restrictions on version numbers of dependencies. An example
of a version constraint is `[1.0.0,2.0.0)`.

- **Version precedence** - The rules for ordering version numbers. For example, version `2.0.0` is considered greater
than version `1.0.0`.

## Semantic Versioning
[Semantic Versioning](https://semver.org/) is a specification describing how to construct version numbers.

### Numbering
Version numbers consist of three required components and an optional fourth component. The format is
`<major>.<minor>.<patch>[[-<pre-release>][+<build>]]`. Where:

- `<major>` - Positive number indicating backwards compatibility.

- `<minor>` - Positive number indicating a compatible feature release.

- `<patch>` - Positive number indicating a bug fix release.

- `<pre-release>` - Alphanumeric identifier taken into account in version precedence.

- `<build>` - Alphanumeric identifier ignored in version precedence.

### Constraints
Semantic Versioning does not address version constraints.

### Precedence
- Precedence is calculated by separating the version into major, minor, patch and pre-release identifiers in that order
(build metadata does not figure into precedence).

- Precedence is determined by the first difference when comparing each of these identifiers from left to right as
follows: Major, minor, and patch versions are always compared numerically.

- When major, minor, and patch are equal, a pre-release version has lower precedence than a normal version.

- Precedence for two pre-release versions with the same major, minor, and patch version is determined by comparing each
dot separated identifier from left to right until a difference is found as follows:

  - Identifiers consisting of only digits are compared numerically.

  - Identifiers with letters or hyphens are compared lexically in ASCII sort order.

  - Numeric identifiers always have lower precedence than non-numeric identifiers.

  - A larger set of pre-release fields has a higher precedence than a smaller set, if all the preceding identifiers
    are equal.

The following versions are sorted in descending order.

| Version            |
|--------------------|
| `2.0.0`            |
| `1.1.1`            |
| `1.1.0`            |
| `1.0.1`            |
| `1.0.0`            |
| `1.0.0-rc.2`       |
| `1.0.0-rc.1`       |
| `1.0.0-beta.11`    |
| `1.0.0-beta.2`     |
| `1.0.0-beta`       |
| `1.0.0-alpha.beta` |
| `1.0.0-alpha.1`    |
| `1.0.0-alpha`      |

## Calendar Versioning
[Calendar Versioning](https://calver.org/) is a specification describing how to construct version numbers based on dates.

### Numbering

Unlike the other versioning methodologies in this survey, Calendar Versioning does not have a prescribed format for the version
components. Instead, the specification provides a number of identifiers (e.g. `YYYY`, `MM`, `MAJOR`) that can be combined by
users to form their own version layout. The user constructs a version format (e.g. "YYYY.MAJOR") and parses version strings
against that format. The identifiers are grouped into those that represent date elements and those that represent semantic
version elements. In addition, the specification allows for an optional trailing modifier. While the specification does not
provide any details on interpreting the optional modifier, for sort order purposes, it can be treated in the same manner as
the semantic version optional pre-release metadata.

### Constraints
Calendar Versioning does not address version constraints.

### Precedence

The Calendar Versioning specification does not indicate anything with regard to ordering. However, based on the meaning of the
components that make up a calendar version, the following order precedence can be inferred.

1. `YYYY`/`YY`/`0Y`
2. `WW`/`0W` **or** `MM`/`0M` and `DD`/`0D`
3. `MAJOR`
4. `MINOR`
5. `PATCH`
6. Modifier according to the [Semantic Version](https://semver.org/) specification for pre-release versions (specification item 9)

The following versions are sorted in descending order.

| Version                    | Format                         |
|----------------------------|--------------------------------|
| `2023.11.15-2.1.4`         | `YYYY.MM.DD-MAJOR.MINOR.PATCH` |
| `2023.11.15-2.1.4-alpha.1` | `YYYY.MM.DD-MAJOR.MINOR.PATCH` |
| `2023.11.15-2.1`           | `YYYY.MM.DD-MAJOR.MINOR`       |
| `2023.11.15-2`             | `YYYY.MM.DD-MAJOR`             |
| `2023.11.15`               | `YYYY.MM.DD`                   |
| `2023.11`                  | `YYYY.MM`                      |
| `2023`                     | `YYYY`                         |

Note that the use of `MM`/`DD` is mutually exclusive with `WW`. Replacing `MM`/`DD` with `WW` in the above example would result in the
same sort order.

## Maven
[Maven](https://maven.apache.org/) is a build tool which uses an [XML](https://www.w3.org/standards/xml/core) file,
`pom.xml`, to declaratively configure builds.

### Numbering
Maven places no requirements on the format of a version number. It applies various heuristics to parse a version
number into components.

### Constraints
Maven provides dependency version range notation.

| Constraint      | Equivalent          | Definition                          | Description                                             |
|-----------------|---------------------|-------------------------------------|---------------------------------------------------------|
| `(,1.0]`        | `(,1.0]`            | x &#x2264; 1.0                      | Less than or equal to                                   |
| `1.0`           | `[1.0,)`            | x &#x2265; 1.0                      | Greater than or equal to                                |
| `[1.0]`         | `[1.0]`             | x = 1.0                             | Exactly equal to                                        |
| `[1.2,1.3]`     | `[1.2,1.3]`         | 1.2 &#x2264; x &#x2264; 1.3         | Inclusive range                                         |
| `(1.2,1.5)`     | `(1.2,1.5)`         | 1.2 < x < 1.5                       | Exclusive range                                         |
| `[1.0,2.0)`     | `[1.0,2.0)`         | 1.0 &#x2264; x < 2.0                | Inclusive lower bound, exclusive upper bound            |
| `[1.5,)`        | `[1.5,)`            | x &#x2265; 1.5                      | Greater than or equal to                                |
| `(,1.0],[1.2,)` | `(,1.0],[1.2,)`     | x &#x2264; 1.0 or x &#x2265; 1.2    | Disjoint range. Multiple sets are separated by a comma. |
| `(,1.1),(1.1,)` | `(,1.1),(1.1,)`     | x < 1.1 or x > 1.1                  | Exclusion range                                         |
| `1.2.min`       | `[1.2.min]`         | x = 1.2.min                         | Minimum version in the 1.2 line                         |
| `1.2.max`       | `[1.2.max]`         | x = 1.2.max                         | Maximum version in the 1.2 line                         |
| `1.2.*`         | `[1.2.min,1.2.max]` | 1.2.min &#x2264; x &#x2265; 1.2.max | Minimum to maximum in the 1.2 line                      |

### Precedence
The ordering of versions in Maven is determined by the
[ComparableVersion](https://maven.apache.org/ref/current/maven-artifact/apidocs/org/apache/maven/artifact/versioning/ComparableVersion.html)
class. The following rules are listed in that class.

* An unlimited number of version components.
* Components can be digits or strings.
* Component separators are `-` (hyphen), `.` (dot), or a transition between characters and digits (e.g. `1.0alpha1`
  consists of the four components `1`, `0`, `alpha`, and `1`).
* Strings are checked for well-known qualifiers, and the qualifier ordering is used for version ordering. Well-known
qualifiers (case-insensitive) in ascending order are:
  * `alpha` or `a`
  * `beta` or `b`
  * `milestone` or `m`
  * `rc` or `cr`
  * `snapshot`
  * the empty string or `ga` or `final`
  * `sp`
  * Unknown qualifiers are considered after known qualifiers, with lexical order (always case-insensitive).
* A hyphen usually precedes a qualifier, and is always less important than something preceded with a dot.

The following versions are sorted in descending order.

| Version                                               |
|-------------------------------------------------------|
| `2.0.0`                                               |
| `1.0.1.0.1`                                           |
| `1.0.1`                                               |
| `1.0.1-SNAPSHOT`                                      |
| `1.0.0.1`                                             |
| `1-something`                                         |
| `1_0_0`                                               |
| `1-sp`                                                |
| `1`, `1.0.0`, `1-0-0`, `1-0.0`, `1-final`, `1.0.0-ga` |
| `1-SNAPSHOT`                                          |
| `1.0.0-rc`, `1.0.0-cr`                                |
| `1.0.0-milestone`                                     |
| `1-beta2`                                             |
| `1beta1`, `1.beta.1`                                  |
| `1beta`                                               |
| `1.0alpha1`                                           |
| `NotAVersionSting`                                    |

## Gradle
[Gradle](https://gradle.org/) is a build tool which uses either the [Groovy](https://groovy-lang.org/) or
[Kotlin](https://kotlinlang.org/) language to configure builds.

### Numbering
Gradle places no requirements on the format of a version number. It applies various heuristics to parse a version
number into components.

### Constraints
Gradle [is similar to the Maven](https://docs.gradle.org/current/userguide/single_versions.html) version constraint scheme.
Gradle does not support multiple ranges (e.g. `[1.0,2.0),[3.0,4.0)`). Gradle range notation more closely conform to the
<a href="https://en.wikipedia.org/wiki/ISO_31-11#Sets">ISO 31-11</a> standard set notation, and to provide the
`strictly`, `require`, `prefer`, and `rejects` version resolution modes. In addition, Gradle provides programmatic
intervention in the dependency resolution process. While all this provides maximum flexibility for dependency version
resolution, it can present a high learning curve, and lead to resolution decisions that are difficult to debug.

| Constraint            | Equivalent              | Definition                  | Description                                                                             |
|-----------------------|-------------------------|-----------------------------|-----------------------------------------------------------------------------------------|
| `(,1.0]`              | `(,1.0]`                | x &#x2264; 1.0              | Less than or equal to                                                                   |
| `1.0`                 | `[1.0,)`                | x &#x2265; 1.0              | Greater than or equal to                                                                |
| `[1.0]`               | `[1.0]`                 | x = 1.0                     | Exactly equal to                                                                        |
| `1.0!!`               | `[1.0]`                 | x = 1.0                     | Exactly equal to                                                                        |
| `[1.2,1.3]`           | `[1.2,1.3]`             | 1.2 &#x2264; x &#x2264; 1.3 | Inclusive range                                                                         |
| `(1.2,1.5)`           | `(1.2,1.5)`             | 1.2 < x < 1.5               | Exclusive range                                                                         |
| `]1.2,1.5[`           | `(1.2,1.5)`             | 1.2 < x < 1.5               | Exclusive range                                                                         |
| `[1.0,2.0)`           | `[1.0,2.0)`             | 1.0 &#x2264; x < 2.0        | Inclusive lower bound, exclusive upper bound                                            |
| `[1.5,)`              | `[1.5,)`                | x &#x2265; 1.5              | Greater than or equal to                                                                |
| `1.0.+`               |                         | x = 1.0.*                   | Exactly match leading portion allowing anything after                                   |
| `+`                   | `(,)`                   |                             | Match any version                                                                       |
| `latest.integration`  | `[latest.integration]`  | x = latest.integration      | Latest revision of the dependency                                                       |
| `latest.<any status>` | `[latest.<any status>]` | x = latest.<any status>     | Latest revision of the dependency with at least the specified status (e.g. "milestone") |

### Precedence
Gradle version ordering is described in the
[Declaring Versions and Ranges](https://docs.gradle.org/current/userguide/single_versions.html#version_ordering)
document.

* Each version is split into components:
  * The characters `.`, `-`, `_`, `+` are used to delimit the components of a version.
  * Any component which contains both digits and letters is split into separate components: `1a1` == `1.a.1`
  * Only the components of a version are compared. The actual separator characters are not significant:
   `1.a.1` == `1-a+1` == `1.a-1` == `1a1`
* The equivalent components of two versions are compared using the following rules:
  * If both components are numeric, the highest numeric value is considered higher: `1.1` < `1.2`
  * If one component is numeric, it is considered higher than the non-numeric component: `1.a` < `1.1`
  * If both components are non-numeric, they are compared alphabetically, case-sensitive: `1.A` < `1.B` < `1.a` < `1.b`
  * A version with an extra numeric component is considered higher than a version without: `1.1` < `1.1.0`
  * A version with an extra non-numeric component is considered lower than a version without: `1.1.a` < `1.1`
* Certain string values have special meaning for the purposes of ordering:
  * The string `dev` is considered lower than any other string component: `1.0-dev` < `1.0-alpha` < `1.0-rc`
  * The strings `rc`, `release` and `final` are considered higher than any other string component (sorted in that
   order): `1.0-zeta` < `1.0-rc` < `1.0-release` < `1.0-final` < `1.0`.
  * The string `SNAPSHOT` will be ordered higher than `rc`: `1.0-RC` < `1.0-SNAPSHOT` < `1.0`
  * The string `GA` will be ordered next to `FINAL` and `RELEASE`, in alphabetical order:
   `1.0-RC` < `1.0-FINAL` < `1.0-GA` < `1.0-RELEASE` < 1.0
  * The string `SP` will be ordered higher than `RELEASE`. However, it remains lower than an unqualified version,
   limiting its use to versioning schemes using either `FINAL`, `GA` or `RELEASE`: `1.0-RELEASE` < `1.0-SP1` < `1.0`
  * Numeric snapshot versions have no special meaning, and are sorted like any other numeric component:
   `1.0` < `1.0-20150201.121010-123` < `1.1`

The following versions are sorted in descending order.

| Version                                               |
|-------------------------------------------------------|
| `1.0.1`                                               |
| `1.0.1-SNAPSHOT`                                      |
| `1.0.0.1`                                             |
| `1-something`                                         |
| `1-SOMETHING`                                         |
| `1_0_0`                                               |
| `1-sp`                                                |
| `1`, `1.0.0`, `1-0-0`, `1-0.0`, `1-final`, `1.0.0-ga` |
| `1-SNAPSHOT`                                          |
| `1.0.0-rc`                                            |
| `1.0.0-milestone`                                     |
| `1-beta2`                                             |
| `1beta1`                                              |
| `1beta`                                               |
| `1.0alpha1`                                           |
| `1.0dev`                                              |

## Apache Ivy
[Ivy](https://ant.apache.org/ivy/) is a dependency manager offering a very high degree of flexibility in repository
layout, artifact naming, artifact publishing, and dependency specification.

### Numbering
Ivy places no requirements on the format of a version number. A so-called
[latest strategy](https://ant.apache.org/ivy/history/2.5.1/settings/latest-strategies.html) can be specified for Ivy
to use when it needs to parse a version number.

### Constraints
| Constraint            | Equivalent              | Definition                  | Description                                                                             |
|-----------------------|-------------------------|-----------------------------|-----------------------------------------------------------------------------------------|
| `[1.0,2.0]`           | `[1.0,2.0]`             | 1.0 &#x2264; x &#x2265; 2.0 | Inclusive range                                                                         |
| `[1.0,2.0[`           | `[1.0,2.0[`             | 1.0 &#x2264; x < 2.0        | Inclusive lower bound, exclusive upper bound                                            |
| `]1.0,2.0]`           | `(1.0,2.0]`             | 1.0 < x &#x2265; 2.0        | Exclusive lower bound, inclusive upper bound                                            |
| `]1.0,2.0[`           | `(1.0,2.0)`             | 1.0 < x < 2.0               | Exclusive range                                                                         |
| `[1.0,)`              | `[1.0,)`                | 1.0 &#x2264; x              | Inclusive lower bound, infinite upper bound                                             |
| `]1.0,)`              | `(1.0,)`                | 1.0 < x                     | Exclusive lower bound, infinite upper bound                                             |
| `(,2.0]`              | `(,2.0]`                | 0.0 &#x2264; x &#x2264; 2.0 | Infinite lower bound, inclusive upper bound                                             |
| `(,2.0[`              | `(,2.0)`                | 0.0 &#x2264; x < 2.0        | Infinite lower bound, exclusive upper bound                                             |
| `1.0.+`               |                         | x = 1.0.*                   | Exactly match leading portion allowing anything after                                   |
| `+`                   | `(,)`                   |                             | Match any version                                                                       |
| `latest.integration`  | `[latest.integration]`  | x = latest.integration      | Latest revision of the dependency                                                       |
| `latest.<any status>` | `[latest.<any status>]` | x = latest.<any status>     | Latest revision of the dependency with at least the specified status (e.g. "milestone") |

### Precedence
Ordering of version numbers is determined by the
[latest strategy](https://ant.apache.org/ivy/history/2.5.1/settings/latest-strategies.html) that is specified as part
of Ivy configuration. Ivy provides the following strategies:

* `latest-time` - Compares the revision dates to know which is the latest.
* `latest-revision` - Compares the revisions as strings, using an algorithm close to the one used in the PHP
  `version_compare` function. The
  [LatestRevisionStrategy](https://github.com/apache/ant-ivy/blob/c63ce79f52133857d1146cd2335d51178714effd/src/java/org/apache/ivy/plugins/latest/LatestRevisionStrategy.java)
  class defines the ordering.
* `latest-lexico` - Compares the revisions as strings using lexicographic order

The following versions are sorted in descending order using the `latest-revision` strategy.

| Version      |
|--------------|
| `2.0`        |
| `1.0.1`      |
| `1.0`        |
| `1.0-rc2`    |
| `1.0-rc1`    |
| `1.0-gamma`  |
| `1.0-beta2`  |
| `1.0-beta1`  |
| `1.0-alpha2` |
| `1.0-alpha1` |
| `1.0-dev2`   |
| `1.0-dev1`   |
| `0.2-final`  |
| `0.2rc1`     |
| `0.2_b`      |
| `0.2a`       |

## SBT Coursier
[Coursier](https://get-coursier.io/) is a dependency resolution package used by the [SBT](https://www.scala-sbt.org/)
build tool and the [bazel-deps](https://github.com/bazeltools/bazel-deps) package for resolving Maven transitive
dependencies when using the [Bazel](https://bazel.build/) build tool.

### Numbering
Coursier places no requirements on the format of a version number. It applies various heuristics similar to Maven to
parse a version number into components.

### Constraints
Coursier supports a subset of the Maven version range syntax (i.e. unions of ranges are not currently supported).

| Constraint  | Equivalent  | Definition                  | Description                                  |
|-------------|-------------|-----------------------------|----------------------------------------------|
| `(,1.0]`    | `(,1.0]`    | x &#x2264; 1.0              | Less than or equal to                        |
| `1.0`       | `[1.0,)`    | x &#x2265; 1.0              | Greater than or equal to                     |
| `[1.0]`     | `[1.0]`     | x = 1.0                     | Exactly equal to                             |
| `[1.2,1.3]` | `[1.2,1.3]` | 1.2 &#x2264; x &#x2264; 1.3 | Inclusive range                              |
| `(1.2,1.5)` | `(1.2,1.5)` | 1.2 < x < 1.5               | Exclusive range                              |
| `[1.0,2.0)` | `[1.0,2.0)` | 1.0 &#x2264; x < 2.0        | Inclusive lower bound, exclusive upper bound |
| `[1.5,)`    | `[1.5,)`    | x &#x2265; 1.5              | Greater than or equal to                     |

### Precedence
[Coursier's ordering scheme](https://docs.gradle.org/current/userguide/single_versions.html#version_ordering) is
adapted from Maven. The
[Version.scala](https://github.com/coursier/coursier/blob/master/modules/core/shared/src/main/scala/coursier/core/Version.scala)
class determines the ordering. Note that this class is adapted from the Aether
[GenericVersion.java](https://github.com/eclipse/aether-core/blob/master/aether-util/src/main/java/org/eclipse/aether/util/version/GenericVersion.java)
class.

For comparison, versions are split into components. The version with the least components is padded with additional
zero components so that both versions have the same number of components.

Components are obtained by splitting a version at a `.`, `-`, and `_` character. Those separators are discarded.
Components are also obtained at letter-to-digit and digit-to-letter boundaries.

Numeric components are compared numerically. Non-numeric components are compared lexicographically, ignoring the case.
Non-numeric components go before non-zero numeric components, and after zero numeric components.

Certain non-numeric components have a special meaning, and go before other non-numeric, zero and non-zero numeric
components. These are, in comparison order:

* `dev`
* `alpha` (or `a` if directly followed by a digit)
* `beta` (or `b` if directly followed by a digit)
* `milestone` (or `m` if directly followed by a digit)
* `cr` or `rc`
* `snapshot`
* `ga` or `final`
* `sp`, `bin`

Note that the case is not considered (e.g. `RC` is equivalent to `rc`).

Note that `1.1a` is not equivalent to `1.1-alpha`, as `a` is not followed by a digit. On the other hand, `1.1a1` is
equivalent to `1.1-alpha-1`, and `1.1a-1` is not, as `a` is followed by `-`, not by a digit.

A last rule consists in ignoring any 0 components before a non-numeric component.

The following versions are sorted in descending order.

| Version          |
|------------------|
| `1.1-foo`        |
| `1.1a`           |
| `1.1.0`, `1.1`   |
| `1.1-final`      |
| `1.1-rc`         |
| `1.1-beta`       |
| `1.1-alpha-1`    |
| `1.0.1.2`        |
| `1.0.1e`         |
| `1.0.1`, `1.0-1` |

## NPM
The [Node Package Manager](https://www.npmjs.com/) (NPM) is the package manager for the [Node.js](https://nodejs.org/)
JavaScript development ecosystem.

### Numbering
NPM requires [Semantic Versioning](#semantic-versioning) for all version numbers.

### Constraints
NPM has two levels of version constraint notations. The low level notation is:

| Low Level Constraint                | Equivalent              | Definition                                | Description                 |
|-------------------------------------|-------------------------|-------------------------------------------|-----------------------------|
| `1.0.0`                             | `[1.0.0]`               | x = 1.0.0                                 | Fixed version               |
| `=1.0.0`                            | `[1.0.0]`               | x = 1.0.0                                 | Fixed version               |
| `<1.0.0`                            | `(,1.0.0)`              | x < 1.0.0                                 | Less than                   |
| `<=1.0.0`                           | `(,1.0.0]`              | x &#x2264; 1.0.0                          | Less than or equal to       |
| `>1.0.0`                            | `(1.0.0,)`              | x > 1.0.0                                 | Greater than                |
| `>=1.0.0`                           | `[1.0.0,)`              | x &#x2265; 1.0.0                          | Greater than or equal to    |
| `>=1.2.7 <1.3.0`                    | `[1.2.7,1.3.0)`         | 1.2.7 &#x2264; x < 1.3.0                  | Intersection comparator set |
| `1.2.7`&#124;&#124;`>=1.2.9 <2.0.0` | `[1.2.7],[1.2.9,2.0.0)` | (x = 1.2.7) or (1.2.9 &#x2264; x < 2.0.0) | Multiple comparator sets    |

The high level notation is:

| High Level Constraint | Low Level Equivalent | Equivalent        | Definition                      | Description                              |
|-----------------------|----------------------|-------------------|---------------------------------|------------------------------------------|
| `1.2.3 - 2.3.4`       | `>=1.2.3 <=2.3.4`    | `[1.2.3,2.3.4]`   | 1.2.3 &#x2264; x &#x2264; 2.3.4 | Hyphen version range                     |
| `1.2 - 2.3.4`         | `>=1.2.0 <=2.3.4`    | `[1.2.0,2.3.4]`   | 1.2.0 &#x2264; x &#x2264; 2.3.4 | Hyphen range with partial version        |
| `1.2 - 2.3`           | `>=1.2.0 <=2.4.0-0`  | `[1.2.0,2.4.0-0)` | 1.2.0 &#x2264; x < 2.4.0-0      | Hyphen range with partial version        |
| `1.2 - 2`             | `>=1.2.0 <=3.0.0-0`  | `[1.2.0,3.0.0-0)` | 1.2.0 &#x2264; x < 3.0.0-0      | Hyphen range with partial version        |
| `*` or empty string   | `>=0.0.0`            | `(,)`             | x &#x2265; 0.0.0                | X-Range using asterisk                   |
| `1.*`                 | `>=1.0.0 <2.0.0-0`   | `[1.0.0,2.0.0-0)` | 1.0.0 &#x2264; x < 2.0.0-0      | X-Range using asterisk                   |
| `1.x`                 | `>=1.0.0 <2.0.0-0`   | `[1.0.0,2.0.0-0)` | 1.0.0 &#x2264; x < 2.0.0-0      | X-Range using 'x'                        |
| `1.2.X`               | `>=1.2.0 <1.3.0-0`   | `[1.2.0,1.3.0-0)` | 1.2.0 &#x2264; x < 1.3.0-0      | X-Range using 'X'                        |
| `1.2`                 | `>=1.2.0 <1.3.0-0`   | `[1.2.0,1.3.0-0)` | 1.2.0 &#x2264; x < 1.3.0-0      | Partial versions are treated as X-Ranges |
| `~1.2.3`              | `>=1.2.3 <1.3.0-0`   | `[1.2.3,1.3.0-0)` | 1.2.3 &#x2264; x < 1.3.0-0      | Tilde range                              |
| `~1.2`                | `>=1.2.0 <1.3.0-0`   | `[1.2.0,1.3.0-0)` | 1.2.0 &#x2264; x < 1.3.0-0      | Tilde range with partial version         |
| `^1.2.3`              | `>=1.2.3 <2.0.0-0`   | `[1.2.3,2.0.0-0)` | 1.2.3 &#x2264; x < 2.0.0-0      | Caret range (major)                      |
| `^0.2.3`              | `>=0.2.3 <0.3.0-0`   | `[0.2.3,0.3.0-0)` | 0.2.3 &#x2264; x < 0.3.0-0      | Caret range (minor)                      |
| `^0.0.3`              | `>=0.0.3 <0.0.4-0`   | `[0.0.3,0.0.4-0)` | 0.0.3 &#x2264; x < 0.0.4-0      | Caret range (patch)                      |
| `^0.2`                | `>=0.2.0 <0.3.0-0`   | `[0.2.0,0.3.0-0)` | 0.2.0 &#x2264; x < 0.3.0-0      | Caret range with partial version         |

Note that the lowest priority pre-release version (i.e. "-0") is appended to the maximum version in a number of cases.
This ensures that the pre-release versions of an excluded version are not included in the set. For example, without appending
"-0" to the maximum version, the range {@code ^4.6.0} would allow {@code 5.0.0-beta}. This is shown in the examples in
the [node-semver project README](https://github.com/npm/node-semver/blob/main/README.md) but is not discussed.

### Precedence
Because the NPM versioning scheme requires the use of Semantic Versioning, the version ordering is provided by
that specification. See the [Semantic Versioning](#semantic-versioning) section above.

## Python Pip
[Pip](https://pip.pypa.io) is the package manager for the [Python](https://www.python.org/) programming language.

### Numbering
The Pip version numbering format is specified in [PEP-440](https://peps.python.org/pep-0440/). In Pip, version numbers
are called version identifiers. The following pattern describes a version identifier:

`[N!]N(.N)*[{a|b|rc}N][.postN][.devN][+<local version label>]`

where:

* `N!` - Epoch segment
* `N(.N)*` - Release segment
* `{a|b|rc}N` - Pre-release segment
  * `a` - Alpha
  * `b` - Beta
  * `rc` - Release candidate
* `.postN` - Post-release segment
* `.devN` - Development release segment
* `+<local version label>` - Local version label consisting of alphanumeric characters and periods

### Constraints
| Constraint        | Equivalent          | Definition                                                 | Description              |
|-------------------|---------------------|------------------------------------------------------------|--------------------------|
| `== 1.2.3`        | `[1.2.3]`           | x = 1.2.3                                                  | Fixed version            |
| `=== 1.2.3Z`      |                     | x = "1.2.3Z"                                               | String equality          |
| `~= 3.0.3`        | `[3.0.3,3.1.0)`     | 3.0.3 &#x2264; x < 3.1                                     | Minor compatible         |
| `~= 1.1`          | `[1.1,2.0)`         | >= 1.1 and < 2.0                                           | Major compatible         |
| `!= 1.2.3`        | `(,1.2.3),(1.2.3,)` | x &#x2260; 1.2.3                                           | Exclude version          |
| `>= 1.2.3`        | `[1.2.3,)`          | 1.2.3 &#x2264; x                                           | Greater than or equal to |
| `> 1.2.3`         | `(1.2.3,)`          | 1.2.3 < x                                                  | Greater than             |
| `<= 1.2.3`        | `(,1.2.3]`          | x &#x2264; 1.2.3                                           | Less than or equal to    |
| `< 1.2.3`         | `(,1.2.3)`          | x < 1.2.3                                                  | Less than                |
| `>= 2.2.0, < 3.0` | `[2.2.0,3.0)`       | 2.2.0 &#x2264; x < 3.0                                     | Multiple constraints     |
| `== 1.2.*`        | `[1.2.0,1.3.0)`     | Any version that starts with 1.2, equivalent to `~= 1.2.0` | Wildcard                 |

### Precedence
The following versions are sorted in descending order.

| Version                |
|------------------------|
| `1.1.dev1`             |
| `1.0.15`               |
| `1.0.post456`          |
| `1.0.post456.dev34`    |
| `1.0+5`                |
| `1.0+abc.7`            |
| `1.0+abc.5`            |
| `1.0`                  |
| `1.0rc1`               |
| `1.0rc1.dev456`        |
| `1.0b2.post345`        |
| `1.0b2.post345.dev456` |
| `1.0b2`                |
| `1.0b1.dev456`         |
| `1.0a12`               |
| `1.0a12.dev456`        |
| `1.0a2.dev456`         |
| `1.0a1`                |
| `1.0.dev456`           |
| `1.dev0`               |

## Ruby Bundler
[Bundler](https://bundler.io/) is the package (i.e. Gem) manager for the [Ruby](https://www.ruby-lang.org) programming
language.

### Numbering
Versions consist of numbers separated by periods. Any number of components is allowed. If the final component is
the keyword `pre`, the package is considered to be prerelease.

### Constraints
| Constraint        | Equivalent          | Definition             | Description              |
|-------------------|---------------------|------------------------|--------------------------|
| `1.2.3`           | `[1.2.3]`           |  x = 1.2.3             | Fixed version            |
| `>= 1.2.3`        | `[1.2.3,)`          | 1.2.3 &#x2264; x       | Greater than or equal to |
| `> 1.2.3`         | `(1.2.3,)`          | 1.2.3 < x              | Greater than             |
| `<= 1.2.3`        | `(,1.2.3]`          | x &#x2264; 1.2.3       | Less than or equal to    |
| `< 1.2.3`         | `(,1.2.3)`          | x < 1.2.3              | Less than                |
| `>= 2.2.0, < 3.0` | `[2.2.0,3.0)`       | 2.2.0 &#x2264; x < 3.0 | Multiple constraints     |
| `~> 3.0.3`        | `[3.0.3,3.1)`       | 3.0.3 &#x2264; x < 3.1 | Minor compatible         |
| `~> 1.1`          | `[1.1,2.0)`         | >= 1.1 and < 2.0       | Major compatible         |
| `!= 1.2.3`        | `(,1.2.3),(1.2.3,)` | x &#x2260; 1.2.3       | Exclude version          |
| the empty string  | `(,)`               | 0 &#x2264; x           | Any version              |

### Precedence
Version ordering is defined by the
[Gem::Version](https://github.com/rubygems/rubygems/blob/master/lib/rubygems/version.rb) class. Based on the comments
in that class, version order is defined as:

* Versions are sorted numerically by component
* If any part contains letters (currently only a-z are supported) then that version is considered prerelease
* Versions with a prerelease part in the Nth part sort less than versions with N-1 parts
* Prerelease parts are sorted alphabetically using the normal Ruby string sorting rules
* If a prerelease part contains both letters and numbers, it will be broken into multiple parts to provide expected
  sort behavior (1.0.a10 becomes 1.0.a.10, and is greater than 1.0.a9)
* Prerelease versions sort between real releases

The following versions are sorted in descending order.

| Version   |
|-----------|
| `3.10`    |
| `3.2`     |
| `3.0.2`   |
| `3.0.1`   |
| `1.0`     |
| `1.0.b1`  |
| `1.0a3`   |
| `1.0.a.2` |
| `0.9`     |

## Rust Cargo
[Cargo](https://doc.rust-lang.org/stable/cargo/) is the dependency package manager for the
[Rust](https://www.rust-lang.org/) language.

### Numbering
Cargo requires [Semantic Versioning](#semantic-versioning) for all version numbers. It differs from Semantic Versioning
in the way it treats versions before 1.0.0. While Semantic Versioning says there is no compatibility before 1.0.0,
Cargo considers 0.x.y to be compatible with 0.x.z, where y &#x2265; z and x > 0.

### Constraints
| Constraint       | Equivalent      | Definition               | Description                                             |
|------------------|-----------------|--------------------------|---------------------------------------------------------|
| `=1.0.0`         | `[1.0.0]`       | x = 1.0.0                | Fixed version                                           |
| `<1.0.0`         | `(,1.0.0)`      | x < 1.0.0                | Less than                                               |
| `<=1.0.0`        | `(,1.0.0]`      | x &#x2264; 1.0.0         | Less than or equal to                                   |
| `>1.0.0`         | `(1.0.0,)`      | x > 1.0.0                | Greater than                                            |
| `>=1.0.0`        | `[1.0.0,)`      | x &#x2265; 1.0.0         | Greater than or equal to                                |
| `>=1.0.0,<1.5.0` | `[1.0.0,1.5.0)` | 1.0.0 &#x2264; x < 1.5.0 | Multiple requirements                                   |
| `1.2.3`          | `[1.2.3,)`      | 1.2.3 &#x2264; x < 2.0   | Greater than or equal to but less than next major       |
| `^1.2.3`         | `[1.2.3,2.0)`   | 1.2.3 &#x2264; x < 2.0   | Greater than or equal to but less than next major       |
| `~1.2.3`         | `[1.2.3,1.3.0)` | 1.2.3 &#x2264; x < 1.3.0 | Greater than or equal to but less than next minor       |
| `1.2`            | `[1.2,2.0)`     | 1.2.0 &#x2264; x < 2.0.0 | Missing components are zero                             |
| `~1.2`           | `[1.2,1.3)`     | 1.2.0 &#x2264; x < 1.3.0 | Missing components are zero, tilde less than next minor |
| `1.2.*`          | `[1.2,1.3)`     | 1.2.0 &#x2264; x < 1.3.0 | Wildcard is same as tilde                               |
| `1`              | `[1,2)`         | 1.0.0 &#x2264; x < 2.0.0 | Missing components are zero                             |
| `1.*`            | `[1.0,2.0)`     | 1.0.0 &#x2264; x < 2.0.0 | Wildcard is same as tilde                               |
| `~1`             | `[1,2)`         | 1.0.0 &#x2264; x < 2.0.0 | Tilde less than next major                              |
| `0.2.3`          | `[0.2.3,0.3.0)` | 0.2.3 &#x2264; x < 0.3.0 | Zero major, less than next minor                        |
| `0.2`            | `[0.2.0,0.3.0)` | 0.2.0 &#x2264; x < 0.3.0 | Zero major, missing is zero, less than next minor       |
| `0.0.3`          | `[0.0.3,0.0.4)` | 0.0.3 &#x2264; x < 0.0.4 | Zero major, zero minor, less than next patch            |
| `0.0`            | `[0.0,0.1)`     | 0.0.0 &#x2264; x < 0.1.0 | Zero major, zero minor, less than next minor            |
| `0`              | `[0,1)`         | 0.0.0 &#x2264; x < 1.0.0 | Zero major, less than next major                        |
| `*`              | `[0,)`          | 0.0.0 &#x2264; x         | All zero                                                |

### Precedence
Same as [Semantic Versioning](#semantic-versioning).

## Dart Pub
[Pub](https://github.com/dart-lang/pub) is the package manager for the [Dart](https://github.com/dart-lang) programming
language.

### Numbering
Pub requires [Semantic Versioning](#semantic-versioning) for all version numbers.

### Constraints
| Constraint       | Equivalent      | Definition               | Description              |
|------------------|-----------------|--------------------------|--------------------------|
| `1.2.3`          | `[1.2.3]`       | x = 1.2.3                | Fixed version            |
| `>=1.2.3`        | `[1.2.3,)`      | 1.2.3 &#x2264; x         | Greater than or equal to |
| `>1.2.3`         | `(1.2.3,)`      | 1.2.3 < x                | Greater than             |
| `<=1.2.3`        | `(,1.2.3]`      | x &#x2264; 1.2.3         | Less than or equal to    |
| `<1.2.3`         | `(,1.2.3)`      | x < 1.2.3                | Less than                |
| `>=1.2.3 <2.0.0` | `[1.2.3,2.0.0)` | 1.2.3 &#x2264; x < 2.0.0 | Multiple constraints     |
| `^1.2.3`         | `[1.2.3,2.0.0)` | 1.2.3 &#x2264; x < 2.0.0 | Less than next major     |
| `^0.1.2`         | `[0.1.2,0.2.0)` | 0.1.2 &#x2264; x < 0.2.0 | Less than next minor     |
| `any`            | `[0.0.0,)`      | 0.0.0 &#x2264; x         | Any version              |

### Precedence
Because the Pub versioning scheme requires the use of Semantic Versioning, the version ordering is provided by
that specification. See the [Semantic Versioning](#semantic-versioning) section above.
