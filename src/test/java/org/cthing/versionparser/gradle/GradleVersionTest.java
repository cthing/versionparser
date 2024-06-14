/*
 * Copyright 2023 C Thing Software
 * SPDX-License-Identifier: Apache-2.0
 *
 * This file is derived from org.gradle.api.internal.artifacts.ivyservice.ivyresolve.strategy.VersionParserTest.java
 * which is covered by the following copyright and permission notices:
 *
 *   Copyright 2014 the original author or authors.
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 */
package org.cthing.versionparser.gradle;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Stream;

import org.cthing.versionparser.Version;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.junit.jupiter.params.provider.Arguments.arguments;


public class GradleVersionTest {

    enum Order {
        LT,
        EQ,
        NE,
        GT,
    }

    @SuppressWarnings("UnnecessaryUnicodeEscape")
    static Stream<Arguments> parsingProvider() {
        return Stream.of(
            // Splits version on punctuation
            arguments("a.b.c", "a.b.c", false, "a.b.c", List.of("a", "b", "c"), Arrays.asList(null, null, null)),
            arguments("a-b-c", "a-b-c", true, "a", List.of("a", "b", "c"), Arrays.asList(null, null, null)),
            arguments("a_b_c", "a_b_c", true, "a", List.of("a", "b", "c"), Arrays.asList(null, null, null)),
            arguments("a+b+c", "a+b+c", true, "a", List.of("a", "b", "c"), Arrays.asList(null, null, null)),
            arguments("a.b-c+d_e", "a.b-c+d_e", true, "a.b", List.of("a", "b", "c", "d", "e"),
                      Arrays.asList(null, null, null, null, null)),
            arguments("\u03b1-\u03b2", "\u03b1-\u03b2", true, "\u03b1", List.of("\u03b1", "\u03b2"),
                      Arrays.asList(null, null)),

            // Base version includes the first . separated parts
            arguments("1.2.3", "1.2.3", false, "1.2.3", List.of("1", "2", "3"), Arrays.asList(1L, 2L, 3L)),
            arguments("1.2-3", "1.2-3", true, "1.2", List.of("1", "2", "3"), Arrays.asList(1L, 2L, 3L)),
            arguments("1.2-beta_3+0000", "1.2-beta_3+0000", true, "1.2", List.of("1", "2", "beta", "3", "0000"),
                      Arrays.asList(1L, 2L, null, 3L, 0L)),
            arguments("1.2b3", "1.2b3", true, "1.2", List.of("1", "2", "b", "3"), Arrays.asList(1L, 2L, null, 3L)),
            arguments("1-alpha", "1-alpha", true, "1", List.of("1", "alpha"), Arrays.asList(1L, null)),
            arguments("abc.1-3", "abc.1-3", true, "abc.1", List.of("abc", "1", "3"), Arrays.asList(null, 1L, 3L)),
            arguments("123", "123", false, "123", List.of("123"), Arrays.asList(123L)),
            arguments("abc", "abc", false, "abc", List.of("abc"), Collections.singletonList(null)),
            arguments("a.b.c.1.2", "a.b.c.1.2", false, "a.b.c.1.2", List.of("a", "b", "c", "1", "2"),
                      Arrays.asList(null, null, null, 1L, 2L)),
            arguments("1b2.1.2.3", "1b2.1.2.3", true, "1", List.of("1", "b", "2", "1", "2", "3"),
                      Arrays.asList(1L, null, 2L, 1L, 2L, 3L)),
            arguments("b1-2-3.3", "b1-2-3.3", true, "b", List.of("b", "1", "2", "3", "3"),
                      Arrays.asList(null, 1L, 2L, 3L, 3L)),

            // Handles empty parts and retains whitespace
            arguments("", "", false, "", List.of(), Arrays.asList()),
            arguments("a b c", "a b c", false, "a b c", List.of("a b c"), Collections.singletonList(null)),
            arguments("...", "...", false, "...", List.of("", "", ""), Arrays.asList(null, null, null)),
            arguments("-a b c-  ", "-a b c-  ", true, "-a b c", List.of("", "a b c", "  "),
                      Arrays.asList(null, null, null)),

            // Dynamic versions
            arguments("1.2.+", "1.2.+", true, "1.2", List.of("1", "2", "+"), Arrays.asList(1L, 2L, null)),
            arguments("1.2-+", "1.2-+", true, "1.2", List.of("1", "2", "+"), Arrays.asList(1L, 2L, null)),
            arguments("1.2++", "1.2++", true, "1.2", List.of("1", "2", "+"), Arrays.asList(1L, 2L, null))
        );
    }

    @ParameterizedTest
    @MethodSource("parsingProvider")
    public void testParsing(final String versionStr, final String rep, final boolean qualified,
                            final String baseVersionStr, final List<String> parts,
                            final List<Long> numericalParts) {
        final GradleVersion version = GradleVersion.parse(versionStr);
        assertThat(version).hasToString(rep).isInstanceOf(Version.class);
        assertThat(version.getOriginalVersion()).isEqualTo(rep);
        assertThat(version.isQualified()).isEqualTo(qualified);
        assertThat(version.getBaseVersion()).isEqualTo(GradleVersion.parse(baseVersionStr));
        assertThat(version.getComponents()).isEqualTo(parts);
        assertThat(version.getNumericParts()).isEqualTo(numericalParts);
    }
    static Stream<Arguments> orderingProvider() {
        return Stream.of(
                // Consider snapshot, ga or sp special
                arguments("1.0-rc", Order.LT, "1.0-snapshot"),
                arguments("1.0-snapshot", Order.LT, "1.0-release"),
                arguments("1.0-release-1", Order.LT, "1.0-sp1"),
                arguments("1.0-snapshot", Order.LT, "1.0-final"),
                arguments("1.0-snapshot", Order.LT, "1.0-ga"),
                arguments("1.0-final", Order.LT, "1.0-release"),

                // Compares versions numerically when parts are digits
                arguments("1.0", Order.LT, "2.0"),
                arguments("1.0", Order.LT, "1.1"),
                arguments("1.2", Order.LT, "1.10"),
                arguments("1.0.1", Order.LT, "1.1.0"),
                arguments("1.2", Order.LT, "1.2.3"),
                arguments("12", Order.LT, "12.2.3"),
                arguments("12", Order.LT, "13"),
                arguments("1.0-1", Order.LT, "1.0-2"),
                arguments("1.0-1", Order.LT, "1.0.2"),
                arguments("1.0-1", Order.LT, "1+0_2"),

                // Compares versions lexicographically when parts are not digits
                arguments("1.0.a", Order.LT, "1.0.b"),
                arguments("1.0.A", Order.LT, "1.0.b"),
                arguments("1.0-alpha", Order.LT, "1.0-beta"),
                arguments("1.0-ALPHA", Order.LT, "1.0-BETA"),
                arguments("1.0-ALPHA", Order.LT, "1.0-alpha"),
                arguments("1.0.alpha", Order.LT, "1.0.b"),
                arguments("alpha", Order.LT, "beta"),
                arguments("1.0-a", Order.LT, "1.0-alpha"),
                arguments("1.0-a", Order.LT, "1.0-a1"),

                // Considers parts that are digits as larger than parts that are not
                arguments("1.0-alpha", Order.LT, "1.0.1"),
                arguments("a.b.c", Order.LT, "a.b.123"),
                arguments("a", Order.LT, "123"),
                arguments("1.0.0-alpha.beta", Order.LT, "1.0.0-alpha.1"),

                // Considers a trailing part that contains no digits as smaller
                arguments("1.0-alpha", Order.LT, "1.0"),
                arguments("1.0.a", Order.LT, "1.0"),
                arguments("1.beta.a", Order.LT, "1.beta"),
                arguments("a-b-c", Order.LT, "a.b"),

                // Gives some special treatment to 'dev', 'rc', 'release', and 'final' qualifiers
                arguments("1.0-dev-1", Order.LT, "1.0"),
                arguments("1.0-dev-1", Order.LT, "1.0-dev-2"),
                arguments("1.0-rc-1", Order.LT, "1.0"),
                arguments("1.0-rc-1", Order.LT, "1.0-rc-2"),
                arguments("1.0-rc-1", Order.LT, "1.0-release"),
                arguments("1.0-dev-1", Order.LT, "1.0-xx-1"),
                arguments("1.0-xx-1", Order.LT, "1.0-rc-1"),
                arguments("1.0-release", Order.LT, "1.0"),
                arguments("1.0-final", Order.LT, "1.0"),
                arguments("1.0-dev-1", Order.LT, "1.0-rc-1"),
                arguments("1.0-rc-1", Order.LT, "1.0-final"),
                arguments("1.0-dev-1", Order.LT, "1.0-final"),
                arguments("1.0.0.RC1", Order.LT, "1.0.0.RC2"),
                arguments("1.0.0.RC2", Order.LT, "1.0.0.RELEASE"),

                // Compares special qualifiers against non-special strings
                arguments("1.1.a", Order.LT, "1.1"),
                arguments("1.0-dev", Order.LT, "1.0-a"),
                arguments("1.0-a", Order.LT, "1.0-rc"),
                arguments("1.0-a", Order.LT, "1.0-release"),
                arguments("1.0-a", Order.LT, "1.0-final"),
                arguments("1.0-dev", Order.LT, "1.0-snapshot"),
                arguments("1.0-patch", Order.LT, "1.0-release"),

                // Compares identical versions equal
                arguments("", Order.EQ, ""),
                arguments("1", Order.EQ, "1"),
                arguments("1.0.0", Order.EQ, "1.0.0"),
                arguments("!@#%", Order.EQ, "!@#%"),
                arguments("hey joe", Order.EQ, "hey joe"),

                // Compares versions that differ only in separators equal
                arguments("1.0", Order.EQ, "1_0"),
                arguments("1_0", Order.EQ, "1-0"),
                arguments("1-0", Order.EQ, "1+0"),
                arguments("1.a.2", Order.EQ, "1a2"), // number-word and word-number boundaries are considered separators

                // Compares versions that differ only in leading zeros equal
                arguments("01.0", Order.EQ, "1.0"),
                arguments("1.0", Order.EQ, "01.0"),
                arguments("001.2.003", Order.EQ, "0001.02.3"),

                // Compares different versions that also differ in leading zeros
                arguments("1.01", Order.LT, "1.2"),
                arguments("1.1", Order.LT, "1.02"),
                arguments("01.0", Order.LT, "2.0"),
                arguments("1.0", Order.LT, "02.0"),

                // Compares versions where earlier version parts differ only in leading zeros
                arguments("01.1", Order.LT, "1.2"),
                arguments("1.1", Order.LT, "01.2"),
                arguments("1.01.1", Order.LT, "1.1.2"),
                arguments("1.1.1", Order.LT, "1.01.2"),

                // Compares unrelated versions unequal
                arguments("1.0", Order.NE, ""),
                arguments("1.0", Order.NE, "!@#%"),
                arguments("1.0", Order.NE, "hey joe"),

                // Does not compare versions with different number of trailing .0's equal
                arguments("1.0.0", Order.GT, "1.0"),
                arguments("1.0.0", Order.GT, "1"),

                // Does not compare versions with different capitalization equal
                arguments("1.0-alpha", Order.GT, "1.0-ALPHA"),

                // Incorrectly compares Maven snapshot-like versions (current behaviour not necessarily
                // desired behaviour
                arguments("1.0-SNAPSHOT", Order.LT, "1.0"),
                arguments("1.0", Order.LT, "1.0-20150201.121010-123"),
                arguments("1.0-20150201.121010-123", Order.LT, "1.0-20150201.121010-124"),
                arguments("1.0-20150201.121010-123", Order.LT, "1.0-20150201.131010-1"),
                arguments("1.0-SNAPSHOT", Order.LT, "1.0-20150201.131010-1"),
                arguments("1.0", Order.LT, "1.1-SNAPSHOT"),
                arguments("1.0", Order.LT, "1.1-20150201.121010-12")
        );
    }

    @ParameterizedTest
    @MethodSource("orderingProvider")
    public void testOrdering(final String version1, final Order order, final String version2) {
        final Version v1 = GradleVersion.parse(version1);
        final Version v2 = GradleVersion.parse(version2);

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
            case NE -> {
                assertThat(v1).isNotEqualByComparingTo(v2);
                assertThat(v2).isNotEqualByComparingTo(v1);
                assertThat(v2).isNotEqualTo(v1);
                assertThat(v1).isNotEqualTo(v2);
                assertThat(v1).doesNotHaveSameHashCodeAs(v2);
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
    @SuppressWarnings("ResultOfMethodCallIgnored")
    public void testBadCompare() {
        final Version v1 = GradleVersion.parse("1.2.3");
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
                Arguments.of("", false),
                Arguments.of("  ", false),
                Arguments.of("1", false),
                Arguments.of("1.2", false),
                Arguments.of("1.2.3", false),
                Arguments.of("3.5.1-dev", true),
                Arguments.of("3.5.1-rc", true),
                Arguments.of("3.5.1-snapshot", true),
                Arguments.of("3.5.1-final", false),
                Arguments.of("3.5.1-FINAL", false),
                Arguments.of("3.5.1-ga", false),
                Arguments.of("3.5.1-release", false),
                Arguments.of("3.5.1-sp", false),
                Arguments.of("3.5.1-foobar", false),
                Arguments.of("3.5.1-1689977053", false)
        );
    }

    @ParameterizedTest
    @MethodSource("prereleaseProvider")
    public void testIsPreRelease(final String versionStr, final boolean prerelease) {
        final Version version = GradleVersion.parse(versionStr);
        assertThat(version.isPreRelease()).isEqualTo(prerelease);
    }

    @Test
    public void testToString() {
        assertThat(GradleVersion.parse("1.0.1+abc")).hasToString("1.0.1+abc");
        assertThat(GradleVersion.parse("   1.0.1+abc  ")).hasToString("   1.0.1+abc  ");
        assertThat(GradleVersion.parse("0")).hasToString("0");
        assertThat(GradleVersion.parse("")).hasToString("");
        assertThat(GradleVersion.parse("    ")).hasToString("    ");
    }

    @Test
    public void testEquality() {
        final Version version1 = GradleVersion.parse("1.2.3");
        final Version version2 = GradleVersion.parse("1.2.3");
        final Version version3 = GradleVersion.parse("2.3.4");

        //noinspection EqualsWithItself
        assertThat(version1).isEqualTo(version1);
        assertThat(version1).isEqualTo(version2);
        assertThat(version1).hasSameHashCodeAs(version2);
        assertThat(version1).isNotEqualTo(version3);
        assertThat(version1.hashCode()).isNotEqualTo(version3.hashCode());
        assertThat(version1).isNotEqualTo(null);
    }
}
