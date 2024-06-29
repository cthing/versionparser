/*
 * Copyright 2023 C Thing Software
 * SPDX-License-Identifier: Apache-2.0
 */
package org.cthing.versionparser.java;

import java.util.List;
import java.util.stream.Stream;

import javax.annotation.Nullable;

import org.cthing.versionparser.VersionConstraint;
import org.cthing.versionparser.VersionParsingException;
import org.cthing.versionparser.VersionRange;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.junit.jupiter.params.provider.Arguments.arguments;


public class JavaVersionSchemeTest {

    static Stream<Arguments> versionProvider() {
        return Stream.of(
                arguments("17", "17", 17, 0, 0, 0, null, null, null, List.of(17), false),
                arguments(" 17.0.11 ", "17.0.11", 17, 0, 11, 0, null, null, null, List.of(17, 0, 11), false),
                arguments(" 17.1.4 ", "17.1.4", 17, 1, 4, 0, null, null, null, List.of(17, 1, 4), false),
                arguments(" 17.2.11.5 ", "17.2.11.5", 17, 2, 11, 5, null, null, null, List.of(17, 2, 11, 5), false),
                arguments("17.1", "17.1", 17, 1, 0, 0, null, null, null, List.of(17, 1), false),
                arguments("17.1.3+12-cthing", "17.1.3+12-cthing", 17, 1, 3, 0, 12, null, "cthing", List.of(17, 1, 3), false),
                arguments("17.1.3-beta+12", "17.1.3-beta+12", 17, 1, 3, 0, 12, "beta", null, List.of(17, 1, 3), true),
                arguments("17-alpha", "17-alpha", 17, 0, 0, 0, null, "alpha", null, List.of(17), true),
                arguments("17+3", "17+3", 17, 0, 0, 0, 3, null, null, List.of(17), false),
                arguments("1.0", "1.0", 1, 0, 0, 0, null, null, null, List.of(1), false),
                arguments("1.4", "1.4", 4, 0, 0, 0, null, null, null, List.of(4), false),
                arguments("1.4.2", "1.4.2", 4, 2, 0, 0, null, null, null, List.of(4, 2), false),
                arguments("1.4.2_151", "1.4.2_151", 4, 2, 151, 0, null, null, null, List.of(4, 2, 151), false),
                arguments("1.4.2-b034", "1.4.2-b034", 4, 2, 0, 0, 34, null, null, List.of(4, 2), false),
                arguments("1.4.2+b034", "1.4.2+b034", 4, 2, 0, 0, 34, null, null, List.of(4, 2), false),
                arguments("1.4.2+b0", "1.4.2+b0", 4, 2, 0, 0, 0, null, null, List.of(4, 2), false),
                arguments("1.4.2_151-b034", "1.4.2_151-b034", 4, 2, 151, 0, 34, null, null, List.of(4, 2, 151), false),
                arguments("1.4.2_151-foo-b034", "1.4.2_151-foo-b034", 4, 2, 151, 0, 34, null, "foo", List.of(4, 2, 151), false),
                arguments("1.4.2_151-b034-bar", "1.4.2_151-b034-bar", 4, 2, 151, 0, 34, null, "bar", List.of(4, 2, 151), false),
                arguments("8u17", "8u17", 8, 0, 17, 0, null, null, null, List.of(8, 0, 17), false),
                arguments("5.2u17", "5.2u17", 5, 2, 17, 0, null, null, null, List.of(5, 2, 17), false)
        );
    }

    @ParameterizedTest
    @MethodSource("versionProvider")
    public void testParseVerison(final String version, final String originalVersion, final int feature,
                                 final int interim, final int update, final int patch,
                                 @Nullable final Integer build, @Nullable final String pre,
                                 @Nullable final String opt, final List<Integer> components,
                                 final boolean preRelease)
            throws VersionParsingException {
        final JavaVersion javaVersion = JavaVersionScheme.parseVersion(version);
        assertThat(javaVersion).hasToString(originalVersion);
        assertThat(javaVersion.getOriginalVersion()).isEqualTo(originalVersion);
        assertThat(javaVersion.getFeature()).isEqualTo(feature);
        assertThat(javaVersion.getInterim()).isEqualTo(interim);
        assertThat(javaVersion.getUpdate()).isEqualTo(update);
        assertThat(javaVersion.getPatch()).isEqualTo(patch);
        assertThat(javaVersion.isPreRelease()).isEqualTo(preRelease);
        if (build == null) {
            assertThat(javaVersion.getBuild()).isNotPresent();
        } else {
            assertThat(javaVersion.getBuild()).contains(build);
        }
        if (pre == null) {
            assertThat(javaVersion.getPre()).isNotPresent();
        } else {
            assertThat(javaVersion.getPre()).contains(pre);
        }
        if (opt == null) {
            assertThat(javaVersion.getOptional()).isNotPresent();
        } else {
            assertThat(javaVersion.getOptional()).contains(opt);
        }
        assertThat(javaVersion.getComponents()).isEqualTo(components);
    }

    static Stream<Arguments> rangeProvider() {
        return Stream.of(
                arguments("1", "[1,2)", "1", "2", true, false),
                arguments("1.0", "[1.0,1.1)", "1.0", "1.1", true, false),
                arguments("1.3", "[1.3,1.4)", "1.3", "1.4", true, false),
                arguments("17.0.11", "[17.0.11,18)", "17.0.11", "18", true, false),
                arguments("[17.0.11]", "[17.0.11]", "17.0.11", "17.0.11", true, true),
                arguments("[17,)", "[17,)", "17", null, true, false),
                arguments("(,17]", "(,17]", null, "17", false, true),
                arguments("(17, 21]", "(17,21]", "17", "21", false, true),
                arguments("(,)", "(,)", null, null, false, false)
        );
    }

    @ParameterizedTest
    @MethodSource("rangeProvider")
    public void testParseRange(final String range, final String rangeRep, @Nullable final String minRep,
                               @Nullable final String maxRep, final boolean minIncluded,
                               final boolean maxIncluded) throws VersionParsingException {
        final VersionConstraint constraint = JavaVersionScheme.parseRange(range);
        assertThat(constraint).hasToString(rangeRep);
        assertThat(constraint.isWeak()).isFalse();
        final VersionRange versionRange = constraint.getRanges().get(0);
        if (minRep == null) {
            assertThat(versionRange.getMinVersion()).isNull();
        } else {
            assertThat(versionRange.getMinVersion()).hasToString(minRep);
        }
        if (maxRep == null) {
            assertThat(versionRange.getMaxVersion()).isNull();
        } else {
            assertThat(versionRange.getMaxVersion()).hasToString(maxRep);
        }
        assertThat(versionRange.isMinIncluded()).isEqualTo(minIncluded);
        assertThat(versionRange.isMaxIncluded()).isEqualTo(maxIncluded);
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "",
            "[)",
            "(]",
            "()",
            "[,",
            "(,",
            "[",
            "(",
            "[11",
            "[11.0.7,",
            "(11",
            "(11,",
            "(17)",
            "[1.4,1.5,1.6)",
            "[17,11]",
            "[17,11)",
            "(17,11]",
            "(17,11)"
    })
    public void testParseRangeBad(final String range) {
        assertThatExceptionOfType(VersionParsingException.class)
                .isThrownBy(() -> JavaVersionScheme.parseRange(range));
    }

    static Stream<Arguments> allowsProvider() {
        return Stream.of(
                arguments("[1.4, 9)", "1.4", true),
                arguments("[1.4, 9)", "9", false),
                arguments("[1.4, 9)", "1.5", true),
                arguments("[1.4, 9)", "8.0.2", true),
                arguments("(1.4, 9]", "8.0.2", true),
                arguments("(1.4, 9]", "1.4", false),
                arguments("(1.4, )", "1.4", false),
                arguments("(1.4, )", "17", true)
        );
    }

    @ParameterizedTest
    @MethodSource("allowsProvider")
    public void testAllows(final String range, final String version, final boolean result)
            throws VersionParsingException {
        assertThat(JavaVersionScheme.parseRange(range).allows(JavaVersionScheme.parseVersion(version))).isEqualTo(result);
    }

    static Stream<Arguments> releasesProvider() {
        return Stream.of(
                arguments(JavaVersionScheme.JAVA_1_0, "1.0", "1.1"),
                arguments(JavaVersionScheme.JAVA_1_1, "1.1", "1.2"),
                arguments(JavaVersionScheme.JAVA_1_2, "1.2", "1.3"),
                arguments(JavaVersionScheme.JAVA_1_3, "1.3", "1.4"),
                arguments(JavaVersionScheme.JAVA_1_4, "1.4", "1.5"),
                arguments(JavaVersionScheme.JAVA_1_5, "1.5", "1.6"),
                arguments(JavaVersionScheme.JAVA_1_6, "1.6", "1.7"),
                arguments(JavaVersionScheme.JAVA_1_7, "1.7", "1.8"),
                arguments(JavaVersionScheme.JAVA_1_8, "1.8", "1.9"),
                arguments(JavaVersionScheme.JAVA_9,   "9", "10"),
                arguments(JavaVersionScheme.JAVA_10,  "10", "11"),
                arguments(JavaVersionScheme.JAVA_11,  "11", "12"),
                arguments(JavaVersionScheme.JAVA_12,  "12", "13"),
                arguments(JavaVersionScheme.JAVA_13,  "13", "14"),
                arguments(JavaVersionScheme.JAVA_14,  "14", "15"),
                arguments(JavaVersionScheme.JAVA_15,  "15", "16"),
                arguments(JavaVersionScheme.JAVA_16,  "16", "17"),
                arguments(JavaVersionScheme.JAVA_17,  "17", "18"),
                arguments(JavaVersionScheme.JAVA_18,  "18", "19"),
                arguments(JavaVersionScheme.JAVA_19,  "19", "20"),
                arguments(JavaVersionScheme.JAVA_20,  "20", "21"),
                arguments(JavaVersionScheme.JAVA_21,  "21", "22"),
                arguments(JavaVersionScheme.JAVA_22,  "22", "23"),
                arguments(JavaVersionScheme.JAVA_23,  "23", "24"),
                arguments(JavaVersionScheme.JAVA_24,  "24", "25"),
                arguments(JavaVersionScheme.JAVA_25,  "25", "26"),
                arguments(JavaVersionScheme.JAVA_26,  "26", "27"),
                arguments(JavaVersionScheme.JAVA_27,  "27", "28")
        );
    }

    @ParameterizedTest
    @MethodSource("releasesProvider")
    public void testReleases(final VersionConstraint javaRelease, final String minRep, final String maxRep) {
        assertThat(javaRelease).hasToString("[" + minRep + "," + maxRep + ")");
        assertThat(javaRelease.isWeak()).isFalse();
        final VersionRange versionRange = javaRelease.getRanges().get(0);
        assertThat(versionRange.getMinVersion()).hasToString(minRep);
        assertThat(versionRange.getMaxVersion()).hasToString(maxRep);
        assertThat(versionRange.isMinIncluded()).isTrue();
        assertThat(versionRange.isMaxIncluded()).isFalse();
    }

    static Stream<Arguments> releasesPlusProvider() {
        return Stream.of(
                arguments(JavaVersionScheme.JAVA_1_2_PLUS, "1.2"),
                arguments(JavaVersionScheme.JAVA_1_3_PLUS, "1.3"),
                arguments(JavaVersionScheme.JAVA_1_4_PLUS, "1.4"),
                arguments(JavaVersionScheme.JAVA_1_5_PLUS, "1.5"),
                arguments(JavaVersionScheme.JAVA_1_6_PLUS, "1.6"),
                arguments(JavaVersionScheme.JAVA_1_7_PLUS, "1.7"),
                arguments(JavaVersionScheme.JAVA_1_8_PLUS, "1.8"),
                arguments(JavaVersionScheme.JAVA_9_PLUS,   "9"),
                arguments(JavaVersionScheme.JAVA_10_PLUS,  "10"),
                arguments(JavaVersionScheme.JAVA_11_PLUS,  "11"),
                arguments(JavaVersionScheme.JAVA_12_PLUS,  "12"),
                arguments(JavaVersionScheme.JAVA_13_PLUS,  "13"),
                arguments(JavaVersionScheme.JAVA_14_PLUS,  "14"),
                arguments(JavaVersionScheme.JAVA_15_PLUS,  "15"),
                arguments(JavaVersionScheme.JAVA_16_PLUS,  "16"),
                arguments(JavaVersionScheme.JAVA_17_PLUS,  "17"),
                arguments(JavaVersionScheme.JAVA_18_PLUS,  "18"),
                arguments(JavaVersionScheme.JAVA_19_PLUS,  "19"),
                arguments(JavaVersionScheme.JAVA_20_PLUS,  "20"),
                arguments(JavaVersionScheme.JAVA_21_PLUS,  "21"),
                arguments(JavaVersionScheme.JAVA_22_PLUS,  "22"),
                arguments(JavaVersionScheme.JAVA_23_PLUS,  "23"),
                arguments(JavaVersionScheme.JAVA_24_PLUS,  "24"),
                arguments(JavaVersionScheme.JAVA_25_PLUS,  "25"),
                arguments(JavaVersionScheme.JAVA_26_PLUS,  "26"),
                arguments(JavaVersionScheme.JAVA_27_PLUS,  "27")
        );
    }

    @ParameterizedTest
    @MethodSource("releasesPlusProvider")
    public void testReleasesPlus(final VersionConstraint javaRelease, final String minRep) {
        assertThat(javaRelease).hasToString("[" + minRep + ",)");
        assertThat(javaRelease.isWeak()).isFalse();
        final VersionRange versionRange = javaRelease.getRanges().get(0);
        assertThat(versionRange.getMinVersion()).hasToString(minRep);
        assertThat(versionRange.getMaxVersion()).isNull();
        assertThat(versionRange.isMinIncluded()).isTrue();
        assertThat(versionRange.isMaxIncluded()).isFalse();
    }

    static Stream<Arguments> javaProvider() {
        return Stream.of(
                arguments(JavaVersionScheme.JAVA_1_4, "1.4", true),
                arguments(JavaVersionScheme.JAVA_1_4, "1.4.2", true),
                arguments(JavaVersionScheme.JAVA_1_4, "1.3", false),
                arguments(JavaVersionScheme.JAVA_1_4, "1.5", false),
                arguments(JavaVersionScheme.JAVA_17, "17.0.11", true),
                arguments(JavaVersionScheme.JAVA_17, "21", false)
        );
    }

    @ParameterizedTest
    @MethodSource("javaProvider")
    public void testIsVersion(final VersionConstraint expectedVersion, final String version, final boolean result)
            throws VersionParsingException {
        assertThat(JavaVersionScheme.isVersion(expectedVersion, version)).isEqualTo(result);
    }
}
