# ![C Thing Software](https://www.cthing.com/branding/CThingSoftware-57x60.png "C Thing Software") versionparser
Parses a version string expressed in a wide range of formats and provides a canonical, comparable version object.

### Usage
```
final Version version1 = new Version("1.2.3");
final Version version2 = new Version("1.2.3-SNAPSHOT");

assertThat(Version.compareTo(version1, version2)).isEqualTo(1);
assertThat(Version.compareTo(version2, version1)).isEqualTo(-1);
assertThat(Version.compareTo(version1, version1)).isEqualTo(0);
```

### Building
The libray is compiled for Java 17. If a Java 17 toolchain is not available, one will be downloaded.

Gradle is used to build the library:
```
./gradlew build
```
