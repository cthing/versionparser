/*
 * Copyright 2023 C Thing Software
 * SPDX-License-Identifier: Apache-2.0
 */

package org.cthing.versionparser.semver;

import java.util.List;
import java.util.stream.Stream;

import org.cthing.versionparser.Version;
import org.cthing.versionparser.VersionParsingException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.junit.jupiter.params.provider.Arguments.arguments;


public class SemanticVersionTest {

    private static final List<String> EMPTY = List.of();

    enum Order {
        LT,
        EQ,
        GT,
    }

    static Stream<Arguments> parsingSingleProvider() {
        return Stream.of(
                arguments("1.2.3", "1.2.3", "1.2.3", "1.2.3", 1, 2, 3, EMPTY, EMPTY),
                arguments("  1.2.3  ", "1.2.3", "1.2.3", "1.2.3", 1, 2, 3, EMPTY, EMPTY),
                arguments("1.2.3", "1.2.3", "1.2.3", "1.2.3", 1, 2, 3, EMPTY, EMPTY),
                arguments("v1.2.3", "v1.2.3", "1.2.3", "1.2.3", 1, 2, 3, EMPTY, EMPTY),
                arguments("1.2.3-abc", "1.2.3-abc", "1.2.3-abc", "1.2.3", 1, 2, 3, List.of("abc"), EMPTY),
                arguments("1.2.3-abc.2.foo", "1.2.3-abc.2.foo", "1.2.3-abc.2.foo", "1.2.3", 1, 2, 3,
                          List.of("abc", "2", "foo"), EMPTY),
                arguments("1.2.3+1234", "1.2.3+1234", "1.2.3+1234", "1.2.3", 1, 2, 3, EMPTY, List.of("1234")),
                arguments("1.2.3-abc+1234", "1.2.3-abc+1234", "1.2.3-abc+1234", "1.2.3", 1, 2, 3, List.of("abc"),
                          List.of("1234"))
        );
    }

    @ParameterizedTest
    @MethodSource("parsingSingleProvider")
    public void testSingleParsing(final String versionStr, final String rep, final String value, final String core,
                                  final int major, final int minor, final int patch, final List<String> preRelease,
                                  final List<String> build) throws VersionParsingException {
        final SemanticVersion version = SemanticVersion.parse(versionStr);
        assertThat(version).hasToString(rep).isInstanceOf(Version.class);
        assertThat(version.getOriginalVersion()).isEqualTo(rep);
        assertThat(version.getCoreVersion()).isEqualTo(core);
        assertThat(version.getNormalizedVersion()).isEqualTo(value);
        assertThat(version.getMajor()).isEqualTo(major);
        assertThat(version.getMinor()).isEqualTo(minor);
        assertThat(version.getPatch()).isEqualTo(patch);
        assertThat(version.getPreReleaseIdentifiers()).isEqualTo(preRelease);
        assertThat(version.getBuild()).isEqualTo(build);
    }

    static Stream<Arguments> parsingPreReleaseProvider() {
        return Stream.of(
                arguments("1.2.3", "", "1.2.3", "1.2.3", "1.2.3", 1, 2, 3, EMPTY),
                arguments("1.2.3", "  ", "1.2.3", "1.2.3", "1.2.3", 1, 2, 3, EMPTY),
                arguments("  1.2.3  ", "", "1.2.3", "1.2.3", "1.2.3", 1, 2, 3, EMPTY),
                arguments("1.2.3", "", "1.2.3", "1.2.3", "1.2.3", 1, 2, 3, EMPTY),
                arguments("v1.2.3", "", "v1.2.3", "1.2.3", "1.2.3", 1, 2, 3, EMPTY),
                arguments("1.2.3", "abc", "1.2.3-abc", "1.2.3-abc", "1.2.3", 1, 2, 3, List.of("abc")),
                arguments("1.2.3", "abc.2.foo", "1.2.3-abc.2.foo", "1.2.3-abc.2.foo", "1.2.3", 1, 2, 3,
                          List.of("abc", "2", "foo"))
        );
    }

    @ParameterizedTest
    @MethodSource("parsingPreReleaseProvider")
    public void testPreReleaseParsing(final String baseVersion, final String preReleaseIdentifier, final String rep,
                                      final String value, final String core, final int major, final int minor,
                                      final int patch, final List<String> preRelease)
            throws VersionParsingException {
        final SemanticVersion version = SemanticVersion.parse(baseVersion, preReleaseIdentifier);
        assertThat(version).hasToString(rep).isInstanceOf(Version.class);
        assertThat(version.getOriginalVersion()).isEqualTo(rep);
        assertThat(version.getCoreVersion()).isEqualTo(core);
        assertThat(version.getNormalizedVersion()).isEqualTo(value);
        assertThat(version.getMajor()).isEqualTo(major);
        assertThat(version.getMinor()).isEqualTo(minor);
        assertThat(version.getPatch()).isEqualTo(patch);
        assertThat(version.getPreReleaseIdentifiers()).isEqualTo(preRelease);
        assertThat(version.getBuild()).isEmpty();
    }

    static Stream<Arguments> parsingSnapshotProvider() {
        return Stream.of(
                arguments("1.2.3", false, "1\\.2\\.3", "1\\.2\\.3", "1.2.3", 1, 2, 3),
                arguments("  1.2.3  ", false, "1\\.2\\.3", "1\\.2\\.3", "1.2.3", 1, 2, 3),
                arguments("v1.2.3", false, "v1\\.2\\.3", "1\\.2\\.3", "1.2.3", 1, 2, 3),
                arguments("1.2.3", true, "1\\.2\\.3-\\d+", "1\\.2\\.3-\\d+", "1.2.3", 1, 2, 3),
                arguments(" v1.2.3 ", true, "v1\\.2\\.3-\\d+", "1\\.2\\.3-\\d+", "1.2.3", 1, 2, 3)
        );
    }

    @ParameterizedTest
    @MethodSource("parsingSnapshotProvider")
    public void testSnapshotParsing(final String baseVersion, final boolean snapshot, final String rep,
                                    final String value, final String core, final int major, final int minor,
                                    final int patch)
            throws VersionParsingException {
        final SemanticVersion version = SemanticVersion.parse(baseVersion, snapshot);
        assertThat(version).isInstanceOf(Version.class);
        assertThat(version.toString()).matches(rep);
        assertThat(version.getOriginalVersion()).matches(rep);
        assertThat(version.getCoreVersion()).isEqualTo(core);
        assertThat(version.getNormalizedVersion()).matches(value);
        assertThat(version.getMajor()).isEqualTo(major);
        assertThat(version.getMinor()).isEqualTo(minor);
        assertThat(version.getPatch()).isEqualTo(patch);
        assertThat(version.getBuild()).isEmpty();
        if (snapshot) {
            assertThat(version.getPreReleaseIdentifiers()).hasSize(1);
            assertThat(version.getPreReleaseIdentifiers().get(0)).matches("\\d+");
        } else {
            assertThat(version.getPreReleaseIdentifiers()).isEmpty();
        }
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "1",
            "1.0",
            "1.2.3.",
            "1.0.0+",
            "1.0.0-",
            "1.1.1.1",
            "1.Y.3",
            "1.2.Y",
            "1.0.0-alpha..1",
            "1.0.0-001",
            "1.0.0-äöü",
            "1.2.30000000000000000000000000000000000000"
    })
    public void testParsingBad(final String versionStr) {
        assertThatExceptionOfType(VersionParsingException.class).isThrownBy(() -> SemanticVersion.parse(versionStr));
    }

    static Stream<Arguments> orderingProvider() {
        return Stream.of(
                arguments("1.0.0-alpha.1", Order.GT, "1.0.0-alpha"),
                arguments("1.0.0-alpha.beta", Order.GT, "1.0.0-alpha.1"),
                arguments("1.0.0-beta", Order.GT, "1.0.0-alpha.beta"),
                arguments("1.0.0-beta.2", Order.GT, "1.0.0-beta"),
                arguments("1.0.0-beta.11", Order.GT, "1.0.0-beta.2"),
                arguments("1.0.0-rc.1", Order.GT, "1.0.0-beta.11"),
                arguments("1.0.0-abc5", Order.GT, "1.0.0-abc3"),
                arguments("1.0.0", Order.GT, "1.0.0-rc.1"),
                arguments("1.2.0", Order.GT, "1.1.0"),
                arguments("1.2.3", Order.GT, "1.2.0"),

                arguments("1.0.0-alpha", Order.LT, "1.0.0-alpha.1"),
                arguments("1.0.0-alpha.1", Order.LT, "1.0.0-alpha.beta"),
                arguments("1.0.0-alpha.beta", Order.LT, "1.0.0-beta"),
                arguments("1.0.0-beta", Order.LT, "1.0.0-beta.2"),
                arguments("1.0.0-beta.2", Order.LT, "1.0.0-beta.11"),
                arguments("1.0.0-beta.11", Order.LT, "1.0.0-rc.1"),
                arguments("1.0.0-rc.1", Order.LT, "1.0.0"),
                arguments("0.0.1", Order.LT, "5.0.0"),
                arguments("0.1.0", Order.LT, "5.0.0"),
                arguments("1.0.0", Order.LT, "5.0.0"),

                arguments("1.0.0", Order.EQ, "1.0.0"),
                arguments("1.0.0-alpha.12", Order.EQ, "1.0.0-alpha.12"),
                arguments("1.0.0-alpha.12.ab-c", Order.EQ, "1.0.0-alpha.12.ab-c"),
                arguments("1.0.0-alpha.12.ab-c+123", Order.EQ, "1.0.0-alpha.12.ab-c+123"),
                arguments("1.0.0-alpha.12.ab-c+123", Order.EQ, "1.0.0-alpha.12.ab-c"),
                arguments("1.0.0-alpha.12.ab-c+123", Order.EQ, "1.0.0-alpha.12.ab-c+xyz1")
        );
    }

    @ParameterizedTest
    @MethodSource("orderingProvider")
    public void testOrdering(final String version1, final Order order, final String version2)
            throws VersionParsingException {
        final Version v1 = SemanticVersion.parse(version1);
        final Version v2 = SemanticVersion.parse(version2);

        switch (order) {
            case LT -> {
                assertThat(v1).isLessThan(v2);
                assertThat(v2).isGreaterThan(v1);
            }
            case EQ -> {
                assertThat(v1).isEqualByComparingTo(v2);
                assertThat(v2).isEqualByComparingTo(v1);
            }
            case GT -> {
                assertThat(v1).isGreaterThan(v2);
                assertThat(v2).isLessThan(v1);
            }
            default -> throw new IllegalStateException("Unexpected value: " + order);
        }
    }

    @Test
    @SuppressWarnings("ResultOfMethodCallIgnored")
    public void testBadCompare() throws VersionParsingException {
        final Version v1 = SemanticVersion.parse("1.2.3");
        final Version v2 = new Version() {
            @Override
            public int hashCode() {
                return super.hashCode();
            }

            @Override
            public boolean equals(final Object obj) {
                return obj instanceof Version && compareTo((Version)obj) == 0;
            }

            @Override
            public String getOriginalVersion() {
                return "";
            }

            @Override
            public boolean isPreRelease() {
                return false;
            }

            @Override
            public int compareTo(final Version o) {
                return 0;
            }
        };

        assertThatIllegalArgumentException().isThrownBy(() -> v1.compareTo(v2));
    }

    static Stream<Arguments> prereleaseProvider() {
        return Stream.of(
                Arguments.of("0.0.3", false),
                Arguments.of("1.2.3", false),
                Arguments.of("3.5.1-alpha", true),
                Arguments.of("3.5.1-beta", true),
                Arguments.of("3.5.1-milestone", true),
                Arguments.of("3.5.1-cr", true),
                Arguments.of("3.5.1-CR", true),
                Arguments.of("3.5.1-rc", true),
                Arguments.of("3.5.1-snapshot", true),
                Arguments.of("3.5.1-foobar", true),
                Arguments.of("3.5.1-1689977053", true),
                Arguments.of("3.5.1+build.1", false),
                Arguments.of("3.5.1-alpha+build.1", true)
        );
    }

    @ParameterizedTest
    @MethodSource("prereleaseProvider")
    public void testIsPreRelease(final String versionStr, final boolean prerelease) throws VersionParsingException {
        final Version version = SemanticVersion.parse(versionStr);
        assertThat(version.isPreRelease()).isEqualTo(prerelease);
    }

    static Stream<Arguments> equalityProvider() {
        return Stream.of(
                arguments("1.2.3", "1.2.3", true),
                arguments("v1.2.3", "v1.2.3", true),
                arguments("   1.2.3  ", "1.2.3", true),
                arguments("1.2.3-abc.1", "1.2.3-abc.1", true),
                arguments("1.2.3", "3.2.1", false),
                arguments("1.2.3", "1.2.3-abc", false),
                arguments("1.2.3-abc.1", "1.2.3-abc.2", false)
        );
    }

    @ParameterizedTest
    @MethodSource("equalityProvider")
    public void testEquality(final String versionStr1, final String versionStr2, final boolean equal)
            throws VersionParsingException {
        final Version version1 = SemanticVersion.parse(versionStr1);
        final Version version2 = SemanticVersion.parse(versionStr2);
        if (equal) {
            assertThat(version1).isEqualTo(version2);
            assertThat(version2).isEqualTo(version1);
            assertThat(version1).hasSameHashCodeAs(version2);
        } else {
            assertThat(version1).isNotEqualTo(version2);
            assertThat(version2).isNotEqualTo(version1);
            assertThat(version1).doesNotHaveSameHashCodeAs(version2);
        }
    }

    @Test
    @SuppressWarnings("EqualsWithItself")
    public void testEqualityCorners() throws VersionParsingException {
        final Version version1 = SemanticVersion.parse("1.2.3");
        assertThat(version1).isEqualTo(version1);
        assertThat(version1).isNotEqualTo(null);
    }
}
