/*
 * Copyright 2023 C Thing Software
 * SPDX-License-Identifier: Apache-2.0
 *
 * This file is derived from org.eclipse.aether.util.version.GenericVersionTest.java
 * which is covered by the following copyright and permission notices:
 *
 *   Licensed to the Apache Software Foundation (ASF) under one
 *   or more contributor license agreements.  See the NOTICE file
 *   distributed with this work for additional information
 *   regarding copyright ownership.  The ASF licenses this file
 *   to you under the Apache License, Version 2.0 (the
 *   "License"); you may not use this file except in compliance
 *   with the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing,
 *   software distributed under the License is distributed on an
 *   "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *   KIND, either express or implied.  See the License for the
 *   specific language governing permissions and limitations
 *   under the License.
 */

package org.cthing.versionparser.maven;

import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import org.cthing.versionparser.Version;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.junit.jupiter.params.provider.Arguments.arguments;


public class MvnVersionTest {

    enum Order {
        LT,
        EQ,
        GT,
    }

    static Stream<Arguments> parsingProvider() {
        return Stream.of(
                arguments("", "", List.of("0")),
                arguments("   ", "", List.of("0")),
                arguments("0", "0", List.of("0")),
                arguments("0.0", "0.0", List.of("0")),
                arguments("one", "one", List.of("one")),
                arguments("a.b.c", "a.b.c", List.of("a", "b", "c")),
                arguments("LATEST", "LATEST", List.of("latest")),
                arguments("1", "1", List.of("1")),
                arguments("1.2.3", "1.2.3", List.of("1", "2", "3")),
                arguments("1.2.3.0", "1.2.3.0", List.of("1", "2", "3")),
                arguments("1.2.3.0.0", "1.2.3.0.0", List.of("1", "2", "3")),
                arguments("1.2.3-alpha", "1.2.3-alpha", List.of("1", "2", "3", "alpha")),
                arguments("1.2.3-release", "1.2.3-release", List.of("1", "2", "3")),
                arguments("1.2.3-foo", "1.2.3-foo", List.of("1", "2", "3", "foo")),
                arguments("1.2.3fooBar", "1.2.3fooBar", List.of("1", "2", "3", "foobar")),
                arguments("1.2.3foo4Bar", "1.2.3foo4Bar", List.of("1", "2", "3", "foo", "4", "bar"))
        );
    }

    @ParameterizedTest
    @MethodSource("parsingProvider")
    public void testParsing(final String versionStr, final String rep, final List<String> components) {
        final MvnVersion version = MvnVersion.parse(versionStr);
        assertThat(version).hasToString(rep).isInstanceOf(Version.class);
        assertThat(version.getOriginalVersion()).isEqualTo(rep);
        assertThat(version.getComponents()).isEqualTo(components);
    }

    @Test
    public void testMax() {
        final Version v21 = MvnVersion.parse("2.1");
        final Version v22 = MvnVersion.parse("2.2");
        final Version v23 = MvnVersion.parse("2.3");
        final Version v24 = MvnVersion.parse("2.4");
        final Version v241 = MvnVersion.parse("2.4.1");

        final List<Version> versions = List.of(v22, v21, v241, v24, v23);
        final Optional<Version> max = versions.stream().max(Version::compareTo);
        assertThat(max).contains(v241);
    }

    static Stream<Arguments> orderingProvider() {
        return Stream.of(
                // Basic
                arguments("0", Order.EQ, ""),
                arguments("0", Order.EQ, "   "),
                arguments("a.b.c", Order.GT, "a.b"),
                arguments("two", Order.GT, "one"),

                // Numeric
                arguments("2", Order.LT, "10"),
                arguments("1.2", Order.LT, "1.10"),
                arguments("1.0.2", Order.LT, "1.0.10"),
                arguments("1.0.0.2", Order.LT, "1.0.0.10"),
                arguments("1.0.20101206.111434.1", Order.LT, "1.0.20101206.111435.1"),
                arguments("1.0.20101206.111434.2", Order.LT, "1.0.20101206.111434.10"),
                arguments("1.0.20101206.111434.2", Order.LT, "1.0.20101206789.111434.10"),
                arguments("1.0.20101206789.111434.2", Order.LT, "1.0.20101206789.111434.10"),

                // Delimiters
                arguments("1.0", Order.EQ, "1-0"),
                arguments("1.0", Order.EQ, "1_0"),
                arguments("1.a", Order.EQ, "1a"),

                // Leading zeros are semantically irrelevant
                arguments("0", Order.EQ, "00"),
                arguments("1", Order.EQ, "01"),
                arguments("1", Order.EQ, "001"),
                arguments("1.2", Order.EQ, "1.002"),
                arguments("1.2.3", Order.EQ, "1.2.0003"),
                arguments("1.2.3.4", Order.EQ, "1.2.3.00004"),

                // Trailing zeros are semantically irrelevant
                arguments("1", Order.EQ, "1.0.0.0.0.0.0.0.0.0.0.0.0.0"),
                arguments("1", Order.EQ, "1-0-0-0-0-0-0-0-0-0-0-0-0-0"),
                arguments("1", Order.EQ, "1_0_0_0_0_0_0_0_0_0_0_0_0_0"),
                arguments("1", Order.EQ, "1.0-0.0-0.0-0.0-0.0-0.0-0.0"),
                arguments("1", Order.EQ, "1.0000000000000"),
                arguments("1.0", Order.EQ, "1.0.0"),

                // Trailing zeros before qualifier are semantically irrelevant
                arguments("1.0-ga", Order.EQ, "1.0.0-ga"),
                arguments("1.0.ga", Order.EQ, "1.0.0.ga"),
                arguments("1.0ga", Order.EQ, "1.0.0ga"),
                arguments("1.0-alpha", Order.EQ, "1.0.0-alpha"),
                arguments("1.0.alpha", Order.EQ, "1.0.0.alpha"),
                arguments("1.0alpha", Order.EQ, "1.0.0alpha"),
                arguments("1.0-alpha-snapshot", Order.EQ, "1.0.0-alpha-snapshot"),
                arguments("1.0.alpha.snapshot", Order.EQ, "1.0.0.alpha.snapshot"),
                arguments("1.x.0-alpha", Order.EQ, "1.x.0.0-alpha"),
                arguments("1.x.0.alpha", Order.EQ, "1.x.0.0.alpha"),
                arguments("1.x.0-alpha-snapshot", Order.EQ, "1.x.0.0-alpha-snapshot"),
                arguments("1.x.0.alpha.snapshot", Order.EQ, "1.x.0.0.alpha.snapshot"),
                arguments("1.1.0.0-alpha-1", Order.LT, "1.1.0-beta"),
                arguments("1.1.0.0-alpha.1", Order.LT, "1.1.0-beta"),
                arguments("1.3.0.Beta1", Order.LT, "1.3.0.Final"),
                arguments("4.1.0-173", Order.LT, "4.1.1-178"),

                // Trailing delimiters are semantically irrelevant
                arguments("1", Order.EQ, "1............."),
                arguments("1", Order.EQ, "1-------------"),
                arguments("1", Order.EQ, "1_____________"),
                arguments("1.0", Order.EQ, "1............."),
                arguments("1.0", Order.EQ, "1-------------"),
                arguments("1.0", Order.EQ, "1_____________"),

                // Initialize delimiters
                arguments("0.1", Order.EQ, ".1"),
                arguments("0.0.1", Order.EQ, "..1"),
                arguments("0.1", Order.EQ, "-1"),
                arguments("0.0.1", Order.EQ, "--1"),

                // Consecutive delimiters
                arguments("1.0.1", Order.EQ, "1..1"),
                arguments("1.0.0.1", Order.EQ, "1...1"),
                arguments("1.0.1", Order.EQ, "1--1"),
                arguments("1.0.0.1", Order.EQ, "1---1"),

                // Unlimited number of components
                arguments("1.0.1.2.3.4.5.6.7.8.9.0.1.2.10", Order.GT, "1.0.1.2.3.4.5.6.7.8.9.0.1.2.3"),

                // Unlimited number of digits in numeric component
                arguments("1.1234567890123456789012345678901", Order.GT, "1.123456789012345678901234567891"),

                // Transition from digit to letter and vice versa is equivalent to delimiter
                arguments("1alpha10", Order.EQ, "1.alpha.10"),
                arguments("1alpha10", Order.EQ, "1-alpha-10"),
                arguments("1.alpha10", Order.GT, "1.alpha2"),
                arguments("10alpha", Order.GT, "1alpha"),

                // Well known qualifier ordering
                arguments("1-alpha1", Order.EQ, "1-a1"),
                arguments("1-alpha", Order.LT, "1-beta"),
                arguments("1-beta1", Order.EQ, "1-b1"),
                arguments("1-beta", Order.LT, "1-milestone"),
                arguments("1-milestone1", Order.EQ, "1-m1"),
                arguments("1-milestone", Order.LT, "1-rc"),
                arguments("1-rc", Order.EQ, "1-cr"),
                arguments("1-rc", Order.LT, "1-snapshot"),
                arguments("1-snapshot", Order.LT, "1"),
                arguments("1", Order.EQ, "1-ga"),
                arguments("1", Order.EQ, "1.ga.0.ga"),
                arguments("1.0", Order.EQ, "1-ga"),
                arguments("1", Order.EQ, "1-ga.ga"),
                arguments("1", Order.EQ, "1-ga-ga"),
                arguments("A", Order.EQ, "A.ga.ga"),
                arguments("A", Order.EQ, "A-ga-ga"),
                arguments("1", Order.EQ, "1-final"),
                arguments("1", Order.EQ, "1-release"),
                arguments("1", Order.LT, "1-sp"),
                arguments("A.rc.1", Order.LT, "A.ga.1"),
                arguments("A.sp.1", Order.GT, "A.ga.1"),
                arguments("A.rc.x", Order.LT, "A.ga.x"),
                arguments("A.sp.x", Order.GT, "A.ga.x"),
                arguments("2.12.4-bin-typelevel-4", Order.GT, "2.12.4"),

                // Well known qualifier versus unknown qualifier ordering
                arguments("1-milestone", Order.LT, "1-rc"),
                arguments("1-milestone", Order.GT, "1-beta"),
                arguments("1-M1", Order.LT, "1-rc1"),
                arguments("1-abc", Order.GT, "1-alpha"),
                arguments("1-abc", Order.GT, "1-beta"),
                arguments("1-abc", Order.GT, "1-milestone"),
                arguments("1-abc", Order.GT, "1-rc"),
                arguments("1-abc", Order.GT, "1-snapshot"),
                arguments("1-abc", Order.GT, "1"),
                arguments("1-abc", Order.GT, "1-sp"),
                arguments("1.0m", Order.GT, "1.0"),
                arguments("1.0-m", Order.GT, "1.0"),
                arguments("1.0.m", Order.GT, "1.0"),
                arguments("1.0m1", Order.LT, "1.0"),
                arguments("1.0-m1", Order.LT, "1.0"),
                arguments("1.0.m1", Order.LT, "1.0"),
                arguments("1.0m.1", Order.GT, "1.0"),
                arguments("1.0m-1", Order.GT, "1.0"),
                arguments("1.0.1-MF", Order.GT, "1.0.0"),
                arguments("1.0.1-MF", Order.GT, "1.0.1"),
                arguments("1.0.1-MF", Order.LT, "1.0.2"),
                arguments("1.0.1-X20", Order.GT, "1.0.0"),
                arguments("1.0.1-X20", Order.GT, "1.0.1"),
                arguments("1.0.1-X20", Order.LT, "1.0.2"),
                arguments("1.0.1-SNAP12", Order.GT, "1.0.0"),
                arguments("1.0.1-SNAP12", Order.GT, "1.0.1"),
                arguments("1.0.1-SNAP12", Order.LT, "1.0.2"),

                // Well known single char qualifiers only recognized if immediately followed by number
                arguments("1.0a", Order.GT, "1.0"),
                arguments("1.0-a", Order.GT, "1.0"),
                arguments("1.0.a", Order.GT, "1.0"),
                arguments("1.0b", Order.GT, "1.0"),
                arguments("1.0-b", Order.GT, "1.0"),
                arguments("1.0.b", Order.GT, "1.0"),
                arguments("1.0m", Order.GT, "1.0"),
                arguments("1.0-m", Order.GT, "1.0"),
                arguments("1.0.m", Order.GT, "1.0"),
                arguments("1.0a1", Order.LT, "1.0"),
                arguments("1.0A1", Order.LT, "1.0"),
                arguments("1.0-a1", Order.LT, "1.0"),
                arguments("1.0.a1", Order.LT, "1.0"),
                arguments("1.0b1", Order.LT, "1.0"),
                arguments("1.0B1", Order.LT, "1.0"),
                arguments("1.0-b1", Order.LT, "1.0"),
                arguments("1.0.b1", Order.LT, "1.0"),
                arguments("1.0m1", Order.LT, "1.0"),
                arguments("1.0M1", Order.LT, "1.0"),
                arguments("1.0-m1", Order.LT, "1.0"),
                arguments("1.0.m1", Order.LT, "1.0"),
                arguments("1.0a.1", Order.GT, "1.0"),
                arguments("1.0a-1", Order.GT, "1.0"),
                arguments("1.0b.1", Order.GT, "1.0"),
                arguments("1.0b-1", Order.GT, "1.0"),
                arguments("1.0m.1", Order.GT, "1.0"),
                arguments("1.0m-1", Order.GT, "1.0"),
                arguments("1.0Z1", Order.GT, "1.0"),

                // Unknown qualifier ordering
                arguments("1-abc", Order.LT, "1-abcd"),
                arguments("1-abc", Order.LT, "1-bcd"),
                arguments("1-abc", Order.GT, "1-aac"),

                // Case-insensitive ordering of qualifiers
                arguments("1.alpha", Order.EQ, "1.ALPHA"),
                arguments("1.alpha", Order.EQ, "1.Alpha"),
                arguments("1.beta", Order.EQ, "1.BETA"),
                arguments("1.beta", Order.EQ, "1.Beta"),
                arguments("1.milestone", Order.EQ, "1.MILESTONE"),
                arguments("1.milestone", Order.EQ, "1.Milestone"),
                arguments("1.rc", Order.EQ, "1.RC"),
                arguments("1.rc", Order.EQ, "1.Rc"),
                arguments("1.cr", Order.EQ, "1.CR"),
                arguments("1.cr", Order.EQ, "1.Cr"),
                arguments("1.snapshot", Order.EQ, "1.SNAPSHOT"),
                arguments("1.snapshot", Order.EQ, "1.Snapshot"),
                arguments("1.ga", Order.EQ, "1.GA"),
                arguments("1.ga", Order.EQ, "1.Ga"),
                arguments("1.final", Order.EQ, "1.FINAL"),
                arguments("1.final", Order.EQ, "1.Final"),
                arguments("1.release", Order.EQ, "1.RELEASE"),
                arguments("1.release", Order.EQ, "1.Release"),
                arguments("1.sp", Order.EQ, "1.SP"),
                arguments("1.sp", Order.EQ, "1.Sp"),
                arguments("1.unknown", Order.EQ, "1.UNKNOWN"),
                arguments("1.unknown", Order.EQ, "1.Unknown"),

                // Qualifier versus number ordering
                arguments("1-ga", Order.LT, "1-1"),
                arguments("1.ga", Order.LT, "1.1"),
                arguments("1-ga", Order.EQ, "1.0"),
                arguments("1.ga", Order.EQ, "1.0"),
                arguments("1-ga-1", Order.LT, "1-0-1"),
                arguments("1.ga.1", Order.LT, "1.0.1"),
                arguments("1.sp", Order.GT, "1.0"),
                arguments("1.sp", Order.LT, "1.1"),
                arguments("1-abc", Order.LT, "1-1"),
                arguments("1.abc", Order.LT, "1.1"),
                arguments("1-xyz", Order.LT, "1-1"),
                arguments("1.xyz", Order.LT, "1.1"),

                // Minimum segment
                arguments("1.min", Order.LT, "1.0-alpha-1"),
                arguments("1.min", Order.LT, "1.0-SNAPSHOT"),
                arguments("1.min", Order.LT, "1.0"),
                arguments("1.min", Order.LT, "1.9999999999"),
                arguments("1.min", Order.EQ, "1.MIN"),
                arguments("1.min", Order.GT, "0.99999"),
                arguments("1.min", Order.GT, "0.max"),

                // Maximum segment
                arguments("1.max", Order.GT, "1.0-alpha-1"),
                arguments("1.max", Order.GT, "1.0-SNAPSHOT"),
                arguments("1.max", Order.GT, "1.0"),
                arguments("1.max", Order.GT, "1.9999999999"),
                arguments("1.max", Order.EQ, "1.MAX"),
                arguments("1.max", Order.LT, "2.0-alpha-1"),
                arguments("1.max", Order.LT, "2.min")
        );
    }

    @ParameterizedTest
    @MethodSource("orderingProvider")
    public void testOrdering(final String version1, final Order order, final String version2) {
        final Version v1 = MvnVersion.parse(version1);
        final Version v2 = MvnVersion.parse(version2);

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
    @SuppressWarnings("ResultOfMethodCallIgnored")
    public void testBadCompare() {
        final Version v1 = MvnVersion.parse("1.2.3");
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

    static Stream<Arguments> sequenceProvider() {
        return Stream.of(
                arguments(List.of("0.9.9-SNAPSHOT",
                                  "0.9.9",
                                  "0.9.10-SNAPSHOT",
                                  "0.9.10",
                                  "1.0-alpha-2-SNAPSHOT",
                                  "1.0-alpha-2",
                                  "1.0-alpha-10-SNAPSHOT",
                                  "1.0-alpha-10",
                                  "1.0-beta-1-SNAPSHOT",
                                  "1.0-beta-1",
                                  "1.0-rc-1-SNAPSHOT",
                                  "1.0-rc-1", "1.0-SNAPSHOT",
                                  "1.0", "1.0-sp-1-SNAPSHOT",
                                  "1.0-sp-1",
                                  "1.0.1-alpha-1-SNAPSHOT",
                                  "1.0.1-alpha-1",
                                  "1.0.1-beta-1-SNAPSHOT",
                                  "1.0.1-beta-1",
                                  "1.0.1-rc-1-SNAPSHOT",
                                  "1.0.1-rc-1",
                                  "1.0.1-SNAPSHOT",
                                  "1.0.1",
                                  "1.1-SNAPSHOT",
                                  "1.1")),
                arguments(List.of("1.0-alpha", "1.0", "1.0-1")),
                arguments(List.of("1.0.alpha", "1.0", "1.0-1")),
                arguments(List.of("1.0-alpha", "1.0", "1.0.1")),
                arguments(List.of("1.0.alpha", "1.0", "1.0.1")),
                arguments(List.of("1.0-alpha1", "1.0-M1", "1.0-RC1", "1.0", "1.0-MF", "1.0-X1", "2.0", "2.0.2")),
                arguments(List.of("1.0-RC1", "1.0", "1.0a", "1.0-MF", "1.0-X1", "2.0", "2.0.2"))
        );
    }

    @ParameterizedTest
    @MethodSource("sequenceProvider")
    public void testVersionEvolution(final List<String> sequence) {
        for (int i = 0; i < sequence.size() - 1; i++) {
            for (int j = i + 1; j < sequence.size(); j++) {
                assertThat(MvnVersion.parse(sequence.get(i))).isLessThan(MvnVersion.parse(sequence.get(j)));
            }
        }
    }

    static Stream<Arguments> prereleaseProvider() {
        return Stream.of(
                Arguments.of("", false),
                Arguments.of("  ", false),
                Arguments.of("1", false),
                Arguments.of("1.2", false),
                Arguments.of("1.2.3", false),
                Arguments.of("3.5.1-alpha", true),
                Arguments.of("3.5.1.alpha0", true),
                Arguments.of("12.0.0.alpha3", true),
                Arguments.of("9.4.0.M1", true),
                Arguments.of("3.5.1-beta", true),
                Arguments.of("3.5.1-milestone", true),
                Arguments.of("3.5.1-cr", true),
                Arguments.of("3.5.1-CR", true),
                Arguments.of("3.5.1-rc", true),
                Arguments.of("3.5.1-snapshot", true),
                Arguments.of("3.5.1-ga", false),
                Arguments.of("3.5.1-final", false),
                Arguments.of("3.5.1-release", false),
                Arguments.of("3.5.1-sp", false),
                Arguments.of("3.5.1-foobar", false),
                Arguments.of("3.5.1-1689977053", false),
                Arguments.of("9.4.51.v20230217", false)
        );
    }

    @ParameterizedTest
    @MethodSource("prereleaseProvider")
    public void testIsPreRelease(final String versionStr, final boolean prerelease) {
        final Version version = MvnVersion.parse(versionStr);
        assertThat(version.isPreRelease()).isEqualTo(prerelease);
    }

    @Test
    public void testToString() {
        assertThat(MvnVersion.parse("1.0.1+abc")).hasToString("1.0.1+abc");
        assertThat(MvnVersion.parse("   1.0.1+abc  ")).hasToString("1.0.1+abc");
        assertThat(MvnVersion.parse("0")).hasToString("0");
        assertThat(MvnVersion.parse("")).hasToString("");
        assertThat(MvnVersion.parse("    ")).hasToString("");
    }

    @Test
    public void testEquality() {
        final Version version1 = MvnVersion.parse("1.2.3");
        final Version version2 = MvnVersion.parse("1.2.3");
        final Version version3 = MvnVersion.parse("2.3.4");

        //noinspection EqualsWithItself
        assertThat(version1).isEqualTo(version1);
        assertThat(version1).isEqualTo(version2);
        assertThat(version1).hasSameHashCodeAs(version2);
        assertThat(version1).isNotEqualTo(version3);
        assertThat(version1.hashCode()).isNotEqualTo(version3.hashCode());
        assertThat(version1).isNotEqualTo(null);
    }
}
