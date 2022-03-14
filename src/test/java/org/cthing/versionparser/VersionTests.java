/*
 * Copyright 2022 C Thing Software
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.cthing.versionparser;

import java.util.stream.Stream;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import static org.assertj.core.api.Assertions.assertThat;


public class VersionTests {

    @ParameterizedTest
    @MethodSource("versionParsingProvider")
    @DisplayName("Version parsing")
    void testParsing(final String version, final String trailing, final boolean released,
                     final boolean trailingRecognized, final int trailingValue,
                     final Long... expectedComponents) {
        final Version parsedVersion = new Version(version);

        assertThat(parsedVersion.getComponents()).containsExactly(expectedComponents);
        assertThat(parsedVersion.getTrailing()).isEqualTo(trailing);
        assertThat(parsedVersion.isTrailingRecognized()).isEqualTo(trailingRecognized);
        assertThat(parsedVersion.getTrailingValue()).isEqualTo(trailingValue);
        assertThat(parsedVersion.isReleased()).isEqualTo(released);
        assertThat(Version.isReleased(version)).isEqualTo(released);
    }

    private static Stream<Arguments> versionParsingProvider() {
        return Stream.of(
                Arguments.of("", "", true, true, 0, new Long[0]),
                Arguments.of("1", "", true, true, 0, new Long[]{ 1L }),
                Arguments.of("1.20", "", true, true, 0, new Long[]{ 1L, 20L }),
                Arguments.of("1.20.32", "", true, true, 0, new Long[]{ 1L, 20L, 32L }),
                Arguments.of("1.20.32.6", "", true, true, 0, new Long[]{ 1L, 20L, 32L, 6L }),
                Arguments.of("1.20.32.7.201606060606", "", true, true, 0,
                             new Long[]{ 1L, 20L, 32L, 7L, 201606060606L }),
                Arguments.of("1.20.32.8.10.21", "", true, true, 0, new Long[]{ 1L, 20L, 32L, 8L, 10L, 21L }),
                Arguments.of("20030203.000129", "", true, true, 0, new Long[]{ 20030203L, 129L }),
                Arguments.of("1.2.+", "", true, true, 0, new Long[]{ 1L, 2L, Long.MAX_VALUE }),
                Arguments.of("1.2.3Beta", "Beta", false, true, -500, new Long[]{ 1L, 2L, 3L }),
                Arguments.of("1.2.3Beta2", "Beta2", false, true, -498, new Long[]{ 1L, 2L, 3L }),
                Arguments.of("1.2.3.RELEASE", "RELEASE", true, true, 0, new Long[]{ 1L, 2L, 3L }),
                Arguments.of("1.2.3-Final", "Final", true, true, 0, new Long[]{ 1L, 2L, 3L }),
                Arguments.of("1.2.3-GA", "GA", true, true, 0, new Long[]{ 1L, 2L, 3L }),
                Arguments.of("1.2.3-alpha", "alpha", false, true, -2000, new Long[]{ 1L, 2L, 3L }),
                Arguments.of("1.2.3-alpha-1", "alpha-1", false, true, -1999, new Long[]{ 1L, 2L, 3L }),
                Arguments.of("1.2-rc", "rc", false, true, -100, new Long[]{ 1L, 2L }),
                Arguments.of("1.2-rc1", "rc1", false, true, -99, new Long[]{ 1L, 2L }),
                Arguments.of("1.2-cr", "cr", false, true, -100, new Long[]{ 1L, 2L }),
                Arguments.of("1.2-M2", "M2", false, true, -998, new Long[]{ 1L, 2L }),
                Arguments.of("1.2-Milestone3", "Milestone3", false, true, -997, new Long[]{ 1L, 2L }),
                Arguments.of("0.98f", "f", true, true, 102, new Long[]{ 0L, 98L }),
                Arguments.of("1.2-M", "M", true, true, 109, new Long[]{ 1L, 2L }),
                Arguments.of("beta", "beta", false, true, -500, new Long[]{}),
                Arguments.of("1.2.3Foo", "Foo", true, false, 0, new Long[]{ 1L, 2L, 3L }),
                Arguments.of("1.2.3-4.5.6-Foo", "Foo", true, false, 0, new Long[]{ 1L, 2L, 3L, 4L, 5L, 6L }),
                Arguments.of("1.2.3-1552171596805", "", true, true, 0, new Long[]{ 1L, 2L, 3L, 1552171596805L })
        );
    }

    @ParameterizedTest
    @MethodSource("versionComparisonProvider")
    @DisplayName("Version comparison")
    void testComparison(final String version1, final String version2, final int result) {
        assertThat(Version.compareTo(version1, version2)).isEqualTo(result);
    }

    private static Stream<Arguments> versionComparisonProvider() {
        //@formatter:off
        return Stream.of(
                Arguments.of("1",                   "1",                    0),
                Arguments.of("1.2",                 "1.2",                  0),
                Arguments.of("1.2.3",               "1.2.3",                0),
                Arguments.of("  1.2.3",             "1.2.3  ",              0),
                Arguments.of("1.2.3.4",             "1.2.3.4",              0),
                Arguments.of("1.2.3.4-beta1",       "1.2.3.4-beta1",        0),
                Arguments.of("1.2.+",               "1.2.3",                0),
                Arguments.of("1.2.+",               "1.2.+",                0),
                Arguments.of("1",                   "1.0",                  0),
                Arguments.of("1.2.3-Beta1",         "1.2.3BETA1",           0),
                Arguments.of("1.2.3",               "1.2.3.RELEASE",        0),
                Arguments.of("1",                   "2",                   -1),
                Arguments.of("2",                   "1",                    1),
                Arguments.of("1.1",                 "1.2",                 -1),
                Arguments.of("1.2",                 "1.1",                  1),
                Arguments.of("1",                   "1.2",                 -1),
                Arguments.of("1.2",                 "1",                    1),
                Arguments.of("1.1",                 "1.1-alpha1",           1),
                Arguments.of("1.1",                 "1.1-beta1",            1),
                Arguments.of("1.1",                 "1.1-rc1",              1),
                Arguments.of("1.1",                 "1.1-m1",               1),
                Arguments.of("1.1",                 "1.1-SNAPSHOT",         1),
                Arguments.of("1.1-alpha2",          "1.1-alpha1",           1),
                Arguments.of("1.1-beta1",           "1.1-alpha1",           1),
                Arguments.of("1.1-rc1",             "1.1-beta1",            1),
                Arguments.of("1.1-rc1",             "1.1-m1",               1),
                Arguments.of("0.98g",               "0.98b",                1),
                Arguments.of("1.1-FOO",             "1.1-BOO",              1),
                Arguments.of("1.2.3-1652171596805", "1.2.3-1552171596805",  1),
                Arguments.of("2022.1",              "2022.1.1",            -1)
        );
        //@formatter:on
    }

    @ParameterizedTest
    @MethodSource("versionEqualityProvider")
    @DisplayName("Version equality")
    void testEquality(final String version1, final String version2, final boolean isEqual) {
        final Version v1 = new Version(version1);
        final Version v2 = new Version(version2);

        assertThat(v1.equals(v2)).isEqualTo(isEqual);
        assertThat(v1.hashCode() == v2.hashCode()).isEqualTo(isEqual);
    }

    private static Stream<Arguments> versionEqualityProvider() {
        //@formatter:off
        return Stream.of(
                Arguments.of("",            "",             true),
                Arguments.of("1",           "1",            true),
                Arguments.of("1",           "1.0.0",        true),
                Arguments.of("1-Beta1",     "1.0.0.beta-1", true),
                Arguments.of("1.2.3",       "1.2.3",        true),
                Arguments.of("1.2.3",       "1.2-3",        true),
                Arguments.of("1",           "2",            false),
                Arguments.of("1.2.3-beta1", "1.2.3-beta2",  false),
                Arguments.of("1.2.3",       "1.2.3-beta2",  false),
                Arguments.of("1.2.3",       "2.45.6",       false)
        );
        //@formatter:on
    }
}
