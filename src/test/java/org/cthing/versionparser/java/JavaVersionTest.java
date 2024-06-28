/*
 * Copyright 2023 C Thing Software
 * SPDX-License-Identifier: Apache-2.0
 */
package org.cthing.versionparser.java;

import java.util.List;
import java.util.stream.Stream;

import javax.annotation.Nullable;

import org.cthing.versionparser.Version;
import org.cthing.versionparser.VersionParsingException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.junit.jupiter.params.provider.Arguments.arguments;


public class JavaVersionTest {

    enum Order {
        LT,
        EQ,
        GT,
    }

    static Stream<Arguments> canonicalProvider() {
        return Stream.of(
                arguments("1.4", "4"),
                arguments("1.4.2", "4.2"),
                arguments("1.4.2_151", "4.2.151"),
                arguments("1.4.2-b034", "4.2+34"),
                arguments("1.4.2-b3", "4.2+3"),
                arguments("1.4.2_151-b034", "4.2.151+34"),
                arguments("1.4.2_151-foo-b034", "4.2.151+34-foo"),
                arguments("1.4.2_151-b034-bar", "4.2.151+34-bar"),
                arguments("8u17", "8.0.17"),
                arguments("5.2u17", "5.2.17"),
                arguments("", ""),
                arguments("abc", "abc"),
                arguments("1", "1"),
                arguments("1.0", "1")
        );
    }

    @ParameterizedTest
    @MethodSource("canonicalProvider")
    public void testCanonicalize(final String version, final String canonicalVersion) {
        assertThat(JavaVersion.canonicalize(version)).isEqualTo(canonicalVersion);
    }

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
    public void testParse(final String version, final String originalVersion, final int feature, final int interim,
                          final int update, final int patch, @Nullable final Integer build, @Nullable final String pre,
                          @Nullable final String opt, final List<Integer> components, final boolean preRelease)
            throws VersionParsingException {
        final JavaVersion javaVersion = JavaVersion.parse(version);
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

    @Test
    public void testBadParse() {
        assertThatExceptionOfType(VersionParsingException.class).isThrownBy(() -> JavaVersion.parse(""));
        assertThatExceptionOfType(VersionParsingException.class).isThrownBy(() -> JavaVersion.parse("  "));
        assertThatExceptionOfType(VersionParsingException.class).isThrownBy(() -> JavaVersion.parse("abc"));
        assertThatExceptionOfType(VersionParsingException.class).isThrownBy(() -> JavaVersion.parse("17.0.035"));
        assertThatExceptionOfType(VersionParsingException.class).isThrownBy(() -> JavaVersion.parse("17A"));
        assertThatExceptionOfType(VersionParsingException.class).isThrownBy(() -> JavaVersion.parse("1.4.0"));
        assertThatExceptionOfType(VersionParsingException.class).isThrownBy(() -> JavaVersion.parse("17.0"));
        assertThatExceptionOfType(VersionParsingException.class).isThrownBy(() -> JavaVersion.parse("17.0.0"));
        assertThatExceptionOfType(VersionParsingException.class).isThrownBy(() -> JavaVersion.parse("0"));
        assertThatExceptionOfType(VersionParsingException.class).isThrownBy(() -> JavaVersion.parse("0.0.0"));
        assertThatExceptionOfType(VersionParsingException.class).isThrownBy(() -> JavaVersion.parse("1.4-"));
        assertThatExceptionOfType(VersionParsingException.class).isThrownBy(() -> JavaVersion.parse("8u"));
    }

    static Stream<Arguments> equalityProvider() {
        return Stream.of(
                arguments("17", "17", true),
                arguments("17", "17.1.10", false),
                arguments("17", "17.0.10", false),
                arguments("17.0.10", "17.0.10", true),
                arguments("17.0.10", "17.0.10+11", false),
                arguments("17.0.10+11", "17.0.10+11", true),
                arguments("17.0.10-alpha+11", "17.0.10-alpha+11", true),
                arguments("17.0.10+11-foo", "17.0.10+11-bar", false),
                arguments("17.0.10-foo+11", "17.0.10-bar+11", false),
                arguments("17.0.10-foo", "17.0.10-bar", false),
                arguments("21", "17", false),
                arguments("1.4_10", "1.4.0_10", false)
        );
    }

    @ParameterizedTest
    @MethodSource("equalityProvider")
    public void testEquality(final String version1, final String version2, final boolean eq)
            throws VersionParsingException {
        final JavaVersion javaVersion1 = JavaVersion.parse(version1);
        final JavaVersion javaVersion2 = JavaVersion.parse(version2);
        if (eq) {
            assertThat(javaVersion1).isEqualTo(javaVersion2);
            assertThat(javaVersion1).hasSameHashCodeAs(javaVersion2);
        } else {
            assertThat(javaVersion1).isNotEqualTo(javaVersion2);
            assertThat(javaVersion1).doesNotHaveSameHashCodeAs(javaVersion2);
        }
    }

    static Stream<Arguments> orderingProvider() {
        return Stream.of(
                arguments("17", Order.EQ, "17"),
                arguments("17.1.10", Order.EQ, "17.1.10"),
                arguments("17", Order.LT, "17.1.10"),
                arguments("17.1", Order.LT, "17.1.10"),
                arguments("17.1.10", Order.GT, "17"),
                arguments("17.1.10", Order.GT, "17.0.5"),
                arguments("21", Order.GT, "17"),
                arguments("1.5", Order.GT, "1.4"),
                arguments("17", Order.GT, "1.4"),
                arguments("17.0.11+34", Order.GT, "17.0.11"),
                arguments("17.0.11+34", Order.GT, "17.0.11+14"),
                arguments("17.0.11-def", Order.GT, "17.0.11-abc"),
                arguments("8u17", Order.GT, "7u17"),
                arguments("1.4.2", Order.GT, "1.4")
        );
    }

    @ParameterizedTest
    @MethodSource("orderingProvider")
    public void testOrdering(final String version1, final Order order, final String version2)
            throws VersionParsingException {
        final Version v1 = JavaVersion.parse(version1);
        final Version v2 = JavaVersion.parse(version2);

        switch (order) {
            case LT -> {
                assertThat(v1).isLessThan(v2);
                assertThat(v2).isGreaterThan(v1);
                assertThat(v2).isNotEqualTo(v1);
                assertThat(v1).isNotEqualTo(v2);
                assertThat(v1).doesNotHaveSameHashCodeAs(v2);
            }
            case EQ -> {
                assertThat(v1).isEqualByComparingTo(v2);
                assertThat(v2).isEqualByComparingTo(v1);
                assertThat(v2).isEqualTo(v1);
                assertThat(v1).isEqualTo(v2);
                assertThat(v1).hasSameHashCodeAs(v2);
            }
            case GT -> {
                assertThat(v1).isGreaterThan(v2);
                assertThat(v2).isLessThan(v1);
                assertThat(v2).isNotEqualTo(v1);
                assertThat(v1).isNotEqualTo(v2);
                assertThat(v1).doesNotHaveSameHashCodeAs(v2);
            }
            default -> throw new IllegalStateException("Unexpected value: " + order);
        }
    }

    @Test
    public void testRuntimeVersion() {
        final Runtime.Version expectedVersion = Runtime.version();
        final JavaVersion actualVersion = JavaVersion.RUNTIME_VERSION;

        assertThat(actualVersion.getFeature()).isEqualTo(expectedVersion.feature());
        assertThat(actualVersion.getInterim()).isEqualTo(expectedVersion.interim());
        assertThat(actualVersion.getUpdate()).isEqualTo(expectedVersion.update());
        assertThat(actualVersion.getPatch()).isEqualTo(expectedVersion.patch());
        assertThat(actualVersion.getBuild()).isEqualTo(expectedVersion.build());
        assertThat(actualVersion.getPre()).isEqualTo(expectedVersion.pre());
        assertThat(actualVersion.getOptional()).isEqualTo(expectedVersion.optional());
        assertThat(actualVersion.getComponents()).isEqualTo(expectedVersion.version());
    }
}
