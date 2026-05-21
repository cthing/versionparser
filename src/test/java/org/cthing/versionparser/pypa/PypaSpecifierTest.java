/*
 * Copyright 2026 C Thing Software
 * SPDX-License-Identifier: Apache-2.0
 */
package org.cthing.versionparser.pypa;

import java.util.List;
import java.util.stream.Stream;

import org.cthing.versionparser.VersionParsingException;
import org.cthing.versionparser.VersionRange;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.params.provider.Arguments.arguments;
import static org.cthing.versionparser.pypa.PypaVersion.BoundaryType;


class PypaSpecifierTest {

    @ParameterizedTest
    @CsvSource({
            "~=2.0, '~=', 2.0",
            "==2.1.*, ==, 2.1.*",
            "==2.1.0.3, ==, 2.1.0.3",
            "!=2.2.*, '!=', 2.2.*",
            "!=2.2.0.5, '!=', 2.2.0.5",
            "<=5, <=, 5",
            ">=7.9a1, >=, 7.9a1",
            "<1.0.dev1, <, 1.0.dev1",
            ">2.0.post1, >, 2.0.post1"
    })
    void testParse(final String spec, final String expectedOperator, final String expectedVersion)
            throws VersionParsingException {
        final PypaSpecifier specifier = PypaSpecifier.parse(spec);
        assertThat(specifier.getOperator()).isEqualTo(expectedOperator);
        assertThat(specifier.getVersionId()).isEqualTo(expectedVersion);
        assertThat(specifier.toString()).isEqualTo(spec);
    }

    @ParameterizedTest
    @ValueSource(strings = {
            // Operator-less specifier
            "2.0",
            // Invalid operator
            "=>2.0",
            // Version-less specifier
            "==",
            // Local segment on operators which don't support them
            "~=1.0+5",
            ">=1.0+deadbeef",
            "<=1.0+abc123",
            ">1.0+watwat",
            "<1.0+1.0",
            // Prefix matching on operators which don't support them
            "~=1.0.*",
            ">=1.0.*",
            "<=1.0.*",
            ">1.0.*",
            "<1.0.*",
            // Combination of local and prefix matching on operators which do
            // support one or the other
            "==1.0.*+5",
            "!=1.0.*+deadbeef",
            // Prefix matching cannot be used with a pre-release, post-release,
            // dev or local version
            "==2.0a1.*",
            "!=2.0a1.*",
            "==2.0.post1.*",
            "!=2.0.post1.*",
            "==2.0.dev1.*",
            "!=2.0.dev1.*",
            "==1.0+5.*",
            "!=1.0+deadbeef.*",
            // Prefix matching must appear at the end
            "==1.0.*.5",
            // Compatible operator requires 2 digits in the release operator
            "~=1",
            // Cannot use a prefix matching after a .devN version
            "==1.0.dev1.*",
            "!=1.0.dev1.*"
    })
    void testInvalid(final String spec) {
        assertThatExceptionOfType(VersionParsingException.class).isThrownBy(() -> PypaSpecifier.parse(spec));
    }

    static Stream<Arguments> equalityProvider() {
        return Stream.of(
            arguments("~=2.0", "~=2.0", true),
            arguments("==2.1.*", "==2.1.*", true),
            arguments("==2.1.0.3", "==2.1.0.3", true),
            arguments("!=2.2.*", "!=2.2.*", true),
            arguments("!=2.2.0.5", "!=2.2.0.5", true),
            arguments("<=5", "<=5", true),
            arguments(">=7.9a1", ">=7.9a1", true),
            arguments("<1.0.dev1", "<1.0.dev1", true),
            arguments(">2.0.post1", ">2.0.post1", true),
            arguments("==2.8.0", "==2.8", true),
            arguments("==2.8.0", "==2.8.1", false),
            arguments("==2.8.1", "==2.8", false),
            arguments("==2.8.0", "~=2.8.0", false),
            arguments("==2.8.0", "===2.8.0", false)
        );
    }

    @ParameterizedTest
    @MethodSource("equalityProvider")
    void testEquality(final String spec1, final String spec2, final boolean expectedEqual)
            throws VersionParsingException {
        final PypaSpecifier specifier1 = PypaSpecifier.parse(spec1);
        final PypaSpecifier specifier2 = PypaSpecifier.parse(spec2);

        if (expectedEqual) {
            assertThat(specifier1).isEqualTo(specifier2);
            assertThat(specifier1).hasSameHashCodeAs(specifier2);
        } else {
            assertThat(specifier1).isNotEqualTo(specifier2);
            assertThat(specifier1).doesNotHaveSameHashCodeAs(specifier2);
        }
    }

    static Stream<Arguments> allowsProvider() {
        return Stream.of(
                // Test the equality operator
                arguments("2.0", "==2", true),
                arguments("2.0", "==2.0", true),
                arguments("2.0", "==2.0.0", true),
                arguments("2.0+deadbeef", "==2", true),
                arguments("2.0+deadbeef", "==2.0", true),
                arguments("2.0+deadbeef", "==2.0.0", true),
                arguments("2.0+deadbeef", "==2+deadbeef", true),
                arguments("2.0+deadbeef", "==2.0+deadbeef", true),
                arguments("2.0+deadbeef", "==2.0.0+deadbeef", true),
                arguments("2.0+deadbeef.0", "==2.0.0+deadbeef.00", true),

                arguments("2.1", "==2", false),
                arguments("2.1", "==2.0", false),
                arguments("2.1", "==2.0.0", false),
                arguments("2.0", "==2.0+deadbeef", false),

                // Test the equality operator with a prefix
                arguments("2.dev1", "==2.*", true),
                arguments("2a1", "==2.*", true),
                arguments("2a1.post1", "==2.*", true),
                arguments("2b1", "==2.*", true),
                arguments("2b1.dev1", "==2.*", true),
                arguments("2c1", "==2.*", true),
                arguments("2c1.post1.dev1", "==2.*", true),
                arguments("2c1.post1.dev1", "==2.0.*", true),
                arguments("2rc1", "==2.*", true),
                arguments("2rc1", "==2.0.*", true),
                arguments("2", "==2.*", true),
                arguments("2", "==2.0.*", true),
                arguments("2", "==2.0.0.*", true),
                arguments("2", "==0!2.*", true),
                arguments("0!2", "==2.*", true),
                arguments("2.0", "==2.*", true),
                arguments("2.0.0", "==2.*", true),
                arguments("2.0.0.0", "==2.0.*", true),
                arguments("2.1+local.version", "==2.1.*", true),

                arguments("2.0", "==3.*", false),
                arguments("2.1", "==2.0.*", false),
                arguments("3", "==2.0.0.*", false),
                arguments("2.1.0.0", "==2.0.*", false),

                // Test the inequality operator
                arguments("2.1", "!=2", true),
                arguments("2.1", "!=2.0", true),
                arguments("2.0.1", "!=2", true),
                arguments("2.0.1", "!=2.0", true),
                arguments("2.0.1", "!=2.0.0", true),
                arguments("2.0", "!=2.0+deadbeef", true),

                arguments("2.0", "!=2", false),
                arguments("2.0", "!=2.0", false),
                arguments("2.0", "!=2.0.0", false),
                arguments("2.0+deadbeef", "!=2", false),
                arguments("2.0+deadbeef", "!=2.0", false),
                arguments("2.0+deadbeef", "!=2.0.0", false),
                arguments("2.0+deadbeef", "!=2+deadbeef", false),
                arguments("2.0+deadbeef", "!=2.0+deadbeef", false),
                arguments("2.0+deadbeef", "!=2.0.0+deadbeef", false),
                arguments("2.0+deadbeef.0", "!=2.0.0+deadbeef.00", false),

                // Test the inequality operator with a prefix
                arguments("2.0", "!=3.*", true),
                arguments("2.1", "!=2.0.*", true),
                arguments("3", "!=2.0.0.*", true),
                arguments("2.1.0.0", "!=2.0.*", true),

                arguments("2.dev1", "!=2.*", false),
                arguments("2a1", "!=2.*", false),
                arguments("2a1.post1", "!=2.*", false),
                arguments("2b1", "!=2.*", false),
                arguments("2b1.dev1", "!=2.*", false),
                arguments("2c1", "!=2.*", false),
                arguments("2c1.post1.dev1", "!=2.*", false),
                arguments("2c1.post1.dev1", "!=2.0.*", false),
                arguments("2rc1", "!=2.*", false),
                arguments("2rc1", "!=2.0.*", false),
                arguments("2", "!=2.*", false),
                arguments("2", "!=2.0.*", false),
                arguments("2", "!=2.0.0.*", false),
                arguments("2.0", "!=2.*", false),
                arguments("2.0.0", "!=2.*", false),
                arguments("2.0.0.0", "!=2.0.*", false),

                // Test the greater than or equal operator
                arguments("2.0", ">=2", true),
                arguments("2.0", ">=2.0", true),
                arguments("2.0", ">=2.0.0", true),
                arguments("2.0.post1", ">=2", true),
                arguments("2.0.post1.dev1", ">=2", true),
                arguments("3", ">=2", true),
                arguments("3.0.0a8", ">=3.0.0a7", true),

                arguments("2.0.dev1", ">=2", false),
                arguments("2.0a1", ">=2", false),
                arguments("2.0a1.dev1", ">=2", false),
                arguments("2.0b1", ">=2", false),
                arguments("2.0b1.post1", ">=2", false),
                arguments("2.0c1", ">=2", false),
                arguments("2.0c1.post1.dev1", ">=2", false),
                arguments("2.0rc1", ">=2", false),
                arguments("1", ">=2", false),

                // Test the less than or equal operator
                arguments("2.0", "<=2", true),
                arguments("2.0", "<=2.0", true),
                arguments("2.0", "<=2.0.0", true),
                arguments("2.0.dev1", "<=2", true),
                arguments("2.0a1", "<=2", true),
                arguments("2.0a1.dev1", "<=2", true),
                arguments("2.0b1", "<=2", true),
                arguments("2.0b1.post1", "<=2", true),
                arguments("2.0c1", "<=2", true),
                arguments("2.0c1.post1.dev1", "<=2", true),
                arguments("2.0rc1", "<=2", true),
                arguments("1", "<=2", true),
                arguments("3.0.0a7", "<=3.0.0a8", true),

                arguments("2.0.post1", "<=2", false),
                arguments("2.0.post1.dev1", "<=2", false),
                arguments("3", "<=2", false),

                // Test the greater than operator
                arguments("3", ">2", true),
                arguments("2.1", ">2.0", true),
                arguments("2.0.1", ">2", true),
                arguments("2.1.post1", ">2", true),
                arguments("2.1+local.version", ">2", true),
                arguments("3.0.0a8", ">3.0.0a7", true),

                arguments("1", ">2", false),
                arguments("2.0.dev1", ">2", false),
                arguments("2.0a1", ">2", false),
                arguments("2.0a1.post1", ">2", false),
                arguments("2.0b1", ">2", false),
                arguments("2.0b1.dev1", ">2", false),
                arguments("2.0c1", ">2", false),
                arguments("2.0c1.post1.dev1", ">2", false),
                arguments("2.0rc1", ">2", false),
                arguments("2.0", ">2", false),
                arguments("2.0.post1", ">2", false),
                arguments("2.0.post1.dev1", ">2", false),
                arguments("2.0+local.version", ">2", false),
                arguments("4.1.0a2.dev1234+local", ">4.1.0a2.dev1234", false),

                // Test the less than operator
                arguments("1", "<2", true),
                arguments("2.0", "<2.1", true),
                arguments("2.0.dev0", "<2.1", true),
                arguments("3.0.0a7", "<3.0.0a8", true),

                arguments("2.0.dev1", "<2", false),
                arguments("2.0a1", "<2", false),
                arguments("2.0a1.post1", "<2", false),
                arguments("2.0b1", "<2", false),
                arguments("2.0b2.dev1", "<2", false),
                arguments("2.0c1", "<2", false),
                arguments("2.0c1.post1.dev1", "<2", false),
                arguments("2.0rc1", "<2", false),
                arguments("2.0", "<2", false),
                arguments("2.post1", "<2", false),
                arguments("2.post1.dev1", "<2", false),
                arguments("3", "<2", false),

                // Test the compatibility operator
                arguments("1", "~=1.0", true),
                arguments("1.0.1", "~=1.0", true),
                arguments("1.1", "~=1.0", true),
                arguments("1.9999999", "~=1.0", true),
                arguments("1.1", "~=1.0a1", true),
                arguments("2022.01.01", "~=2022.01.01", true),

                arguments("2.0", "~=1.0", false),
                arguments("1.1.0", "~=1.0.0", false),
                arguments("1.1.post1", "~=1.0.0", false),

                // Test that epochs are handled correctly
                arguments("2!1.0", "~=2!1.0", true),
                arguments("2!1.0", "==2!1.*", true),
                arguments("2!1.0", "==2!1.0", true),
                arguments("2!1.0", "!=1.0", true),
                arguments("2!1.0.0", "==2!1.0.0.0.*", true),
                arguments("2!1.0.0", "==2!1.0.*", true),
                arguments("2!1.0.0", "==2!1.*", true),
                arguments("1.0", "!=2!1.0", true),
                arguments("1.0", "<=2!0.1", true),
                arguments("2!1.0", ">=2.0", true),
                arguments("1.0", "<2!0.1", true),
                arguments("2!1.0", ">2.0", true),

                arguments("1.0", "~=2!1.0", false),
                arguments("2!1.0", "~=1.0", false),
                arguments("2!1.0", "==1.0", false),
                arguments("1.0", "==2!1.0", false),
                arguments("2!1.0", "==1.0.0.*", false),
                arguments("1.0", "==2!1.0.0.*", false),
                arguments("2!1.0", "==1.*", false),
                arguments("1.0", "==2!1.*", false),
                arguments("2!1.0", "!=2!1.0", false),

                // Test various normalization rules
                arguments("2.0.5", ">2.0dev", true),

                // Test local versions with pre/dev/post segments and the greater than operator
                arguments("1.0+local", ">1.0.dev1", true),
                arguments("4.1.0a2.dev1235+local", ">4.1.0a2.dev1234", true),
                arguments("1.0a2+local", ">1.0a1", true),
                arguments("1.0b2+local", ">1.0b1", true),
                arguments("1.0rc2+local", ">1.0rc1", true),
                arguments("1.0.post2+local", ">1.0.post1", true),
                arguments("1.0.dev2+local", ">1.0.dev1", true),
                arguments("1.0a1.dev2+local", ">1.0a1.dev1", true),
                arguments("1.0.post1.dev2+local", ">1.0.post1.dev1", true),

                arguments("1.0a1+local", ">1.0a1", false),
                arguments("1.0b1+local", ">1.0b1", false),
                arguments("1.0rc1+local", ">1.0rc1", false),
                arguments("1.0.post1+local", ">1.0.post1", false),
                arguments("1.0.dev1+local", ">1.0.dev1", false),
                arguments("1.0a1.dev1+local", ">1.0a1.dev1", false),
                arguments("1.0.post1.dev1+local", ">1.0.post1.dev1", false),

                // Prerelease specifiers

                arguments("2.0.dev1", ">=1.0", true),
                arguments("2.0a1", ">=2.0.dev1", true),
                arguments("2.0a1.dev1", "==2.0.*", true),
                arguments("1.0.dev1", "<=2.0", true),
                arguments("1.0.dev1", "<=2.0a1", true),
                arguments("1.0a1", "<=2.0.dev1", true),
                arguments("2.0a1", "<2.0", false),
                arguments("2.0a1", "<2.0a2", true),
                // >V.devN: post-releases of V.devN itself are excluded
                // (V.devN can't have post-releases in PEP 440, so nothing
                // to exclude; these just confirm ordering still works)
                arguments("1.0.dev0", ">1.0.dev1", false),
                arguments("1.0.dev2", ">1.0.dev1", true),
                // >V.devN: post-releases of the base release are NOT
                // post-releases of V.devN, so they are accepted
                arguments("1.0.post0", ">1.0.dev1", true),
                arguments("1.0.post1", ">1.0.dev1", true),
                arguments("1.0.post0", ">1.0.dev0", true),
                // >V.preN: post-releases of the base release are NOT
                // post-releases of V.preN, so they are accepted
                arguments("1.0.post0", ">1.0a1", true),
                arguments("1.0.post0", ">1.0b1", true),
                arguments("1.0.post0", ">1.0rc1", true),
                // >V.preN: post-releases of the pre-release itself
                // ARE excluded
                arguments("1.0a1.post0", ">1.0a1", false),
                arguments("1.0b2.post0", ">1.0b2", false),
                arguments("1.0rc1.post0", ">1.0rc1", false),
                // >V.preN: post of a different pre is not a post-release
                // of V.preN either
                arguments("1.0a2.post0", ">1.0a1", true),
                arguments("1.0b2.post0", ">1.0b1", true),
                // >V.devN: non-post-release versions above V.devN
                arguments("1.0", ">1.0.dev1", true),
                arguments("1.0a1", ">1.0.dev1", true),
                arguments("1.1", ">1.0.dev1", true),
                // >V (final): post-releases of V are still excluded
                arguments("1.0.post0", ">1.0", false),
                arguments("1.0.post1", ">1.0", false),
                // >V (final): post-releases of a different base are fine
                arguments("2.0.post0", ">1.0", true),
                arguments("0.9.post0", ">1.0", false),
                // >V.devN: locals and different bases
                arguments("1.1.post0", ">1.0.dev1", true),
                arguments("0.9.post0", ">1.0.dev1", false),
                // <V.postN: pre-releases of V.postN itself are excluded
                arguments("1.0.post1.dev0", "<1.0.post1", false),
                arguments("1.0.post0.dev0", "<1.0.post0", false),
                // <V.postN: pre-releases of the base release are NOT
                // pre-releases of V.postN, so they are accepted
                arguments("1.0.dev0", "<1.0.post1", true),
                arguments("1.0a1", "<1.0.post1", true),
                arguments("1.0rc1", "<1.0.post1", true),
                arguments("1.0.dev0", "<1.0.post0", true),
                arguments("1.0a1", "<1.0.post0", true),
                arguments("1.0b1", "<1.0.post0", true),
                arguments("1.0rc2", "<1.0.post0", true),
                // <V.postN: dev of a different post is not a pre-release
                // of V.postN either
                arguments("1.0.post0.dev0", "<1.0.post1", true),
                arguments("1.0.post1.dev0", "<1.0.post2", true),
                // <V.postN: non-pre-release versions below V.postN
                arguments("1.0", "<1.0.post1", true),
                arguments("1.0.post0", "<1.0.post1", true),
                arguments("0.9", "<1.0.post1", true),
                arguments("1.0", "<1.0.post0", true),
                // <V.postN: higher post numbers
                arguments("1.0.dev0", "<1.0.post10", true),
                arguments("1.0.post9.dev0", "<1.0.post10", true),
                arguments("1.0.post9", "<1.0.post10", true),

                // Test arbitrary equality (valid version)

                arguments("1.0.0", "===1.0", false),
                arguments("1.0.dev0", "===1.0", false),
                // Test identity comparison by itself
                arguments("1.0", "===1.0", true),
                arguments("1.0.dev0", "===1.0.dev0", true),
                // Test that local versions don't match
                arguments("1.0+downstream1", "===1.0", false),
                arguments("1.0", "===1.0+downstream1", false),
                // Test with arbitrary (non-version) strings
                arguments("foobar", "===foobar", true),
                arguments("foobar", "===baz", false),
                // Test case insensitivity for pre-release versions
                arguments("1.0a1", "===1.0a1", true),
                arguments("1.0A1", "===1.0A1", true),
                arguments("1.0a1", "===1.0A1", true),
                arguments("1.0A1", "===1.0a1", true),
                // Test case insensitivity for beta versions
                arguments("1.0b1", "===1.0b1", true),
                arguments("1.0B1", "===1.0B1", true),
                arguments("1.0b1", "===1.0B1", true),
                arguments("1.0B1", "===1.0b1", true),
                // Test case insensitivity for release candidate versions
                arguments("1.0rc1", "===1.0rc1", true),
                arguments("1.0RC1", "===1.0RC1", true),
                arguments("1.0rc1", "===1.0RC1", true),
                arguments("1.0RC1", "===1.0rc1", true),
                // Test case insensitivity for post-release versions
                arguments("1.0.post1", "===1.0.post1", true),
                arguments("1.0.POST1", "===1.0.POST1", true),
                arguments("1.0.post1", "===1.0.POST1", true),
                arguments("1.0.POST1", "===1.0.post1", true),
                // Test case insensitivity for dev versions
                arguments("1.0.dev1", "===1.0.dev1", true),
                arguments("1.0.DEV1", "===1.0.DEV1", true),
                arguments("1.0.dev1", "===1.0.DEV1", true),
                arguments("1.0.DEV1", "===1.0.dev1", true),
                // Test case insensitivity with local versions
                arguments("1.0+local", "===1.0+local", true),
                arguments("1.0+LOCAL", "===1.0+LOCAL", true),
                arguments("1.0+local", "===1.0+LOCAL", true),
                arguments("1.0+LOCAL", "===1.0+local", true),
                arguments("1.0+abc.def", "===1.0+abc.def", true),
                arguments("1.0+ABC.DEF", "===1.0+ABC.DEF", true),
                arguments("1.0+abc.def", "===1.0+ABC.DEF", true),
                arguments("1.0+ABC.DEF", "===1.0+abc.def", true),
                // Test case insensitivity with mixed case letters in local
                arguments("1.0+AbC", "===1.0+AbC", true),
                arguments("1.0+AbC", "===1.0+abc", true),
                arguments("1.0+AbC", "===1.0+ABC", true),
                // Test complex cases with multiple segments
                arguments("1.0a1.post2.dev3", "===1.0a1.post2.dev3", true),
                arguments("1.0A1.POST2.DEV3", "===1.0A1.POST2.DEV3", true),
                arguments("1.0a1.post2.dev3", "===1.0A1.POST2.DEV3", true),
                arguments("1.0A1.POST2.DEV3", "===1.0a1.post2.dev3", true),
                // Test case insensitivity of non-PEP 440 versions
                arguments("lolwat", "===LOLWAT", true),
                arguments("lolwat", "===LoLWaT", true),
                arguments("LOLWAT", "===lolwat", true),
                arguments("LoLWaT", "===lOlwAt", true)
        );
    }

    @ParameterizedTest
    @MethodSource("allowsProvider")
    void testAllows(final String version, final String spec, final boolean allow) throws VersionParsingException {
        final PypaSpecifier specifier = PypaSpecifier.parse(spec);
        assertThat(specifier.allows(version)).isEqualTo(allow);
    }

    static Stream<Arguments> releaseCompsProvider() {
        return Stream.of(
                // Empty list
                arguments(List.of(), 0),

                // No numeric prefix
                arguments(List.of("a", "1", "2"), 0),
                arguments(List.of("1a1", "2"), 0),

                // Mixed content
                arguments(List.of("0", "1", "2", "a", "3"), 3),
                arguments(List.of("1", "rc1", "0"), 1),

                // All numeric
                arguments(List.of("1", "2", "3", "4"), 4),
                arguments(List.of("0", "00"), 2)
        );
    }

    @ParameterizedTest(name = "[{index}] input={0}, expected={1}")
    @MethodSource("releaseCompsProvider")
    void testCountReleaseComps(final List<String> split, final int expectedLen) {
        assertThat(PypaSpecifier.AbstractSpec.countReleaseComps(split)).isEqualTo(expectedLen);
    }

    @Nested
    @DisplayName("padRelease")
    class PadReleaseTest {

        @Test
        void testNoPaddingNeeded() {
            final List<String> split = List.of("1", "2", "a");
            assertThat(PypaSpecifier.AbstractSpec.padRelease(split, 2)).containsExactly("1", "2", "a");
            assertThat(PypaSpecifier.AbstractSpec.padRelease(split, 1)).containsExactly("1", "2", "a");
        }

        @Test
        void testPaddingNeededMixed() {
            final List<String> split = List.of("1", "2", "a", "b");
            assertThat(PypaSpecifier.AbstractSpec.padRelease(split, 4))
                    .containsExactly("1", "2", "0", "0", "a", "b");
        }

        @Test
        void testPaddingNeededNoPrefix() {
            final List<String> split = List.of("a", "b");
            assertThat(PypaSpecifier.AbstractSpec.padRelease(split, 2)).containsExactly("0", "0", "a", "b");
        }

        @Test
        void testPaddingNeededNoSuffix() {
            final List<String> split = List.of("1", "2");
            assertThat(PypaSpecifier.AbstractSpec.padRelease(split, 4)).containsExactly("1", "2", "0", "0");
        }

        @Test
        void testEmptyList() {
            final List<String> split = List.of();
            assertThat(PypaSpecifier.AbstractSpec.padRelease(split, 3)).containsExactly("0", "0", "0");
            assertThat(PypaSpecifier.AbstractSpec.padRelease(split, 0)).isEmpty();
        }

        @Test
        @SuppressWarnings("DataFlowIssue")
        void testReturnsImmutableList() {
            final List<String> split = List.of("1", "2");

            final List<String> padded = PypaSpecifier.AbstractSpec.padRelease(split, 4);
            assertThatThrownBy(() -> padded.add("3")).isInstanceOf(UnsupportedOperationException.class);

            final List<String> unpadded = PypaSpecifier.AbstractSpec.padRelease(split, 2);
            assertThatThrownBy(() -> unpadded.add("3")).isInstanceOf(UnsupportedOperationException.class);
        }
    }

    @Nested
    @DisplayName("canonicalizeVersion")
    class CanonicalizeVersionTest {

        @ParameterizedTest(name = "[{index}] input={0} (strip=true) -> expected={1}")
        @CsvSource({
                "1.2.0, 1.2",
                "1.0.0, 1",
                "0.0.0, 0",
                "2!1.2.3.0, 2!1.2.3",
                "1.2.0a1, 1.2a1"
        })
        void testWithStripping(final String input, final String expected) throws VersionParsingException {
            final PypaVersion version = PypaVersion.parse(input);
            final String result = PypaSpecifier.AbstractSpec.canonicalizeVersion(version, true);

            assertThat(result).isEqualTo(expected);
        }

        @ParameterizedTest(name = "[{index}] input={0} (strip=false) -> expected={0}")
        @CsvSource({
                "1.2.0",
                "1.0.0",
                "0.0.0",
                "2!1.2.3.0",
                "1.2.0a1"
        })
        void testWithoutStripping(final String input) throws VersionParsingException {
            final PypaVersion version = PypaVersion.parse(input);
            final String result = PypaSpecifier.AbstractSpec.canonicalizeVersion(version, false);

            assertThat(result).isEqualTo(version.toCanonicalString());
        }

        @Test
        void testNoZerosToStrip() throws VersionParsingException {
            final PypaVersion version = PypaVersion.parse("1.2.3");
            final String stripped = PypaSpecifier.AbstractSpec.canonicalizeVersion(version, true);
            final String unstripped = PypaSpecifier.AbstractSpec.canonicalizeVersion(version, false);

            assertThat(stripped).isEqualTo("1.2.3");
            assertThat(unstripped).isEqualTo(stripped);
        }
    }

    static Stream<Arguments> splitVersionProvider() {
        return Stream.of(
                // No explicit epoch (default to "0")
                arguments("1.2.3", List.of("0", "1", "2", "3")),
                arguments("0", List.of("0", "0")),

                // Explicit epoch present
                arguments("1!2.3.4", List.of("1", "2", "3", "4")),
                arguments("12!0", List.of("12", "0")),

                // Items matching PREFIX_PATTERN (e.g., "([0-9]+)((?:a|b|c|rc)[0-9]+)")
                arguments("1.2a1", List.of("0", "1", "2", "a1")),
                arguments("1!2.3rc4.5", List.of("1", "2", "3", "rc4", "5")),
                arguments("1b2", List.of("0", "1", "b2")),

                // Period tracking with trailing/empty elements (-1 limit flag preservation)
                arguments("1.2.", List.of("0", "1", "2", ""))
        );
    }

    @ParameterizedTest(name = "[{index}] input={0} -> expected={1}")
    @MethodSource("splitVersionProvider")
    @SuppressWarnings("DataFlowIssue")
    void testSplitVersion(final String version, final List<String> expected) {
        final List<String> result = PypaSpecifier.AbstractSpec.splitVersion(version);

        assertThat(result).containsExactlyElementsOf(expected);
        assertThatThrownBy(() -> result.add("mutation-test")).isInstanceOf(UnsupportedOperationException.class);
    }

    static Stream<Arguments> joinVersionProvider() {
        return Stream.of(
                // Empty list branch
                arguments(List.of(), "0!"),

                // Empty epoch string handling
                arguments(List.of("", "1", "2"), "0!1.2"),

                // Single-element list (loop is bypassed entirely)
                arguments(List.of("1"), "1!"),
                arguments(List.of("0"), "0!"),

                // Multi-element list / normal execution (loop completes and manages delimiters)
                arguments(List.of("0", "1", "2", "3"), "0!1.2.3"),
                arguments(List.of("2", "4", "0", "a1"), "2!4.0.a1")
        );
    }

    @ParameterizedTest(name = "[{index}] components={0} -> expected={1}")
    @MethodSource("joinVersionProvider")
    void testJoinVersion(final List<String> components, final String expected) {
        assertThat(PypaSpecifier.AbstractSpec.joinVersion(components)).isEqualTo(expected);
    }

    @Nested
    @DisplayName("toRanges")
    class ToRangesTest {

        @Test
        @DisplayName("=== operator should throw UnsupportedOperationException")
        void testArbitraryEqualityThrowsException() throws VersionParsingException {
            final PypaSpecifier specifier = PypaSpecifier.parse("===1.2.0");

            assertThatThrownBy(specifier::toRanges)
                    .isInstanceOf(UnsupportedOperationException.class)
                    .hasMessageContaining("Cannot represent the '===V' specifier as a version range");
        }

        @Nested
        @DisplayName("Wildcard Operators (.*)")
        class WildcardTests {

            @Test
            @DisplayName("==1.2.* should map to [1.2.dev0, 1.3.dev0)")
            void testWildcardEquals() throws VersionParsingException {
                final List<VersionRange> ranges = PypaSpecifier.parse("==1.2.*").toRanges();
                assertThat(ranges).hasSize(1);

                final VersionRange range = ranges.get(0);

                // Lower Bound: [1.2.dev0
                assertThat(range.isMinIncluded()).isTrue();
                assertThat(range.getMinVersion()).isNotNull().hasToString("1.2.dev0");
                assertThat(((PypaVersion)range.getMinVersion()).getBoundaryType()).isEqualTo(BoundaryType.None);

                // Upper Bound: 1.3.dev0)
                assertThat(range.isMaxIncluded()).isFalse();
                assertThat(range.getMaxVersion()).isNotNull().hasToString("1.3.dev0");
                assertThat(((PypaVersion)range.getMinVersion()).getBoundaryType()).isEqualTo(BoundaryType.None);
            }

            @Test
            @DisplayName("!=1.2.* should map to (-INF, 1.2.dev0) and [1.3.dev0, +INF)")
            void testWildcardNotEquals() throws VersionParsingException {
                final List<VersionRange> ranges = PypaSpecifier.parse("!=1.2.*").toRanges();
                assertThat(ranges).hasSize(2);

                // Range 1: (-INF, 1.2.dev0)
                final VersionRange r1 = ranges.get(0);
                assertThat(r1.isMinIncluded()).isFalse();
                assertThat(r1.getMinVersion()).isNull();
                assertThat(r1.isMaxIncluded()).isFalse();
                assertThat(r1.getMaxVersion()).hasToString("1.2.dev0");

                // Range 2: [1.3.dev0, +INF)
                final VersionRange r2 = ranges.get(1);
                assertThat(r2.isMinIncluded()).isTrue();
                assertThat(r2.getMinVersion()).hasToString("1.3.dev0");
                assertThat(r2.isMaxIncluded()).isFalse();
                assertThat(r2.getMaxVersion()).isNull();
            }
        }

        @Nested
        @DisplayName("Greater Than or Equal (>=) and Less Than or Equal (<=)")
        class InequalityWithEqualsTests {

            @Test
            @DisplayName(">=1.2.3 should map to [1.2.3, +INF)")
            void testGreaterThanOrEqual() throws VersionParsingException {
                final List<VersionRange> ranges = PypaSpecifier.parse(">=1.2.3").toRanges();
                assertThat(ranges).hasSize(1);

                final VersionRange range = ranges.get(0);
                assertThat(range.isMinIncluded()).isTrue();
                assertThat(range.getMinVersion()).hasToString("1.2.3");
                assertThat(range.isMaxIncluded()).isFalse();
                assertThat(range.getMaxVersion()).isNull();
            }

            @Test
            @DisplayName("<=1.2.3 should map to (-INF, AfterLocals(1.2.3)]")
            void testLessThanOrEqual() throws VersionParsingException {
                final List<VersionRange> ranges = PypaSpecifier.parse("<=1.2.3").toRanges();
                assertThat(ranges).hasSize(1);

                final VersionRange range = ranges.get(0);
                assertThat(range.isMinIncluded()).isFalse();
                assertThat(range.getMinVersion()).isNull();
                assertThat(range.isMaxIncluded()).isTrue();
                assertThat(range.getMaxVersion()).isNotNull().hasToString("1.2.3");
                assertThat(((PypaVersion)range.getMaxVersion()).getBoundaryType()).isEqualTo(BoundaryType.AfterLocals);
            }
        }

        @Nested
        @DisplayName("Greater Than (>)")
        class GreaterThanTests {

            @Test
            @DisplayName(">1.2.3 (Final release) should map to (AfterPosts(1.2.3), +INF)")
            void testGreaterThanFinal() throws VersionParsingException {
                final List<VersionRange> ranges = PypaSpecifier.parse(">1.2.3").toRanges();
                assertThat(ranges).hasSize(1);

                final VersionRange range = ranges.get(0);
                assertThat(range.isMinIncluded()).isFalse();
                assertThat(range.getMinVersion()).isNotNull().hasToString("1.2.3");
                assertThat(((PypaVersion)range.getMinVersion()).getBoundaryType()).isEqualTo(BoundaryType.AfterPosts);
                assertThat(range.isMaxIncluded()).isFalse();
                assertThat(range.getMaxVersion()).isNull();
            }

            @Test
            @DisplayName(">1.2.3.dev1 (Dev release) should map to [1.2.3.dev2, +INF)")
            void testGreaterThanDev() throws VersionParsingException {
                final List<VersionRange> ranges = PypaSpecifier.parse(">1.2.3.dev1").toRanges();
                assertThat(ranges).hasSize(1);

                final VersionRange range = ranges.get(0);
                assertThat(range.isMinIncluded()).isTrue();
                assertThat(range.getMinVersion()).isNotNull().hasToString("1.2.3.dev2");
                assertThat(((PypaVersion)range.getMinVersion()).getBoundaryType()).isEqualTo(BoundaryType.None);
                assertThat(range.isMaxIncluded()).isFalse();
                assertThat(range.getMaxVersion()).isNull();
            }

            @Test
            @DisplayName(">1.2.3.post1 (Post release) should map to [1.2.3.post2.dev0, +INF)")
            void testGreaterThanPost() throws VersionParsingException {
                final List<VersionRange> ranges = PypaSpecifier.parse(">1.2.3.post1").toRanges();
                assertThat(ranges).hasSize(1);

                final VersionRange range = ranges.get(0);
                assertThat(range.isMinIncluded()).isTrue();
                assertThat(range.getMinVersion()).isNotNull().hasToString("1.2.3.post2.dev0");
                assertThat(((PypaVersion)range.getMinVersion()).getBoundaryType()).isEqualTo(BoundaryType.None);
                assertThat(range.isMaxIncluded()).isFalse();
                assertThat(range.getMaxVersion()).isNull();
            }
        }

        @Nested
        @DisplayName("Less Than (<)")
        class LessThanTests {

            @Test
            @DisplayName("<1.2.3 (Final release) should map to (-INF, 1.2.3.dev0)")
            void testLessThanFinal() throws VersionParsingException {
                final List<VersionRange> ranges = PypaSpecifier.parse("<1.2.3").toRanges();
                assertThat(ranges).hasSize(1);

                final VersionRange range = ranges.get(0);
                assertThat(range.isMinIncluded()).isFalse();
                assertThat(range.getMinVersion()).isNull();
                assertThat(range.isMaxIncluded()).isFalse();
                assertThat(range.getMaxVersion()).hasToString("1.2.3.dev0");
            }

            @Test
            @DisplayName("<1.2.3a1 (Pre-release) should map to (-INF, 1.2.3a1)")
            void testLessThanPrerelease() throws VersionParsingException {
                final List<VersionRange> ranges = PypaSpecifier.parse("<1.2.3a1").toRanges();
                assertThat(ranges).hasSize(1);

                final VersionRange range = ranges.get(0);
                assertThat(range.isMinIncluded()).isFalse();
                assertThat(range.getMinVersion()).isNull();
                assertThat(range.isMaxIncluded()).isFalse();
                assertThat(range.getMaxVersion()).hasToString("1.2.3a1");
            }

            @Test
            @DisplayName("<0.0.dev0 should produce an empty range list if it hits absolute minimum")
            void testLessThanAbsoluteMinimum() throws VersionParsingException {
                // Assuming 0.0.dev0 or your system's _MIN_VERSION evaluates bound <= _MIN_VERSION
                final List<VersionRange> ranges = PypaSpecifier.parse("<0.0.dev0").toRanges();
                assertThat(ranges).isEmpty();
            }
        }

        @Nested
        @DisplayName("Exact Match (==) and Exclusion (!=)")
        class EqualityTests {

            @Test
            @DisplayName("==1.2.3 (No Local) should map to [1.2.3, AfterLocals(1.2.3)]")
            void testEqualsNoLocal() throws VersionParsingException {
                final List<VersionRange> ranges = PypaSpecifier.parse("==1.2.3").toRanges();
                assertThat(ranges).hasSize(1);

                final VersionRange range = ranges.get(0);
                assertThat(range.isMinIncluded()).isTrue();
                assertThat(range.getMinVersion()).hasToString("1.2.3");
                assertThat(range.isMaxIncluded()).isTrue();
                assertThat(range.getMaxVersion()).isNotNull().hasToString("1.2.3");
                assertThat(((PypaVersion)range.getMaxVersion()).getBoundaryType()).isEqualTo(BoundaryType.AfterLocals);
            }

            @Test
            @DisplayName("==1.2.3+local (With Local) should map to [1.2.3+local, 1.2.3+local]")
            void testEqualsWithLocal() throws VersionParsingException {
                final List<VersionRange> ranges = PypaSpecifier.parse("==1.2.3+local").toRanges();
                assertThat(ranges).hasSize(1);

                final VersionRange range = ranges.get(0);
                assertThat(range.isMinIncluded()).isTrue();
                assertThat(range.getMinVersion()).hasToString("1.2.3+local");
                assertThat(range.isMaxIncluded()).isTrue();
                assertThat(range.getMaxVersion()).isNotNull().hasToString("1.2.3+local");
                assertThat(((PypaVersion)range.getMaxVersion()).getBoundaryType()).isEqualTo(BoundaryType.None);
            }

            @Test
            @DisplayName("!=1.2.3 (No Local) should map to (-INF, 1.2.3) and (AfterLocals(1.2.3), +INF)")
            void testNotEqualsNoLocal() throws VersionParsingException {
                final List<VersionRange> ranges = PypaSpecifier.parse("!=1.2.3").toRanges();
                assertThat(ranges).hasSize(2);

                final VersionRange r1 = ranges.get(0);
                assertThat(r1.isMaxIncluded()).isFalse();
                assertThat(r1.getMinVersion()).isNull();
                assertThat(r1.isMaxIncluded()).isFalse();
                assertThat(r1.getMaxVersion()).hasToString("1.2.3");

                final VersionRange r2 = ranges.get(1);
                assertThat(r2.isMinIncluded()).isFalse();
                assertThat(r2.getMinVersion()).isNotNull();
                assertThat(((PypaVersion)r2.getMinVersion()).getBoundaryType()).isEqualTo(BoundaryType.AfterLocals);
                assertThat(r2.isMaxIncluded()).isFalse();
                assertThat(r2.getMaxVersion()).isNull();
            }

            @Test
            @DisplayName("!=1.2.3+local (With Local) should map to (-INF, 1.2.3+local) and (1.2.3+local, +INF)")
            void testNotEqualsWithLocal() throws VersionParsingException {
                final List<VersionRange> ranges = PypaSpecifier.parse("!=1.2.3+local").toRanges();
                assertThat(ranges).hasSize(2);

                final VersionRange r1 = ranges.get(0);
                assertThat(r1.isMinIncluded()).isFalse();
                assertThat(r1.getMinVersion()).isNull();
                assertThat(r1.isMaxIncluded()).isFalse();
                assertThat(r1.getMaxVersion()).hasToString("1.2.3+local");

                final VersionRange r2 = ranges.get(1);
                assertThat(r2.isMinIncluded()).isFalse();
                assertThat(r2.getMinVersion()).hasToString("1.2.3+local");
                assertThat(r2.isMaxIncluded()).isFalse();
                assertThat(r2.getMaxVersion()).isNull();
            }
        }

        @Nested
        @DisplayName("Compatible Match (~=)")
        class CompatibleTests {

            @Test
            @DisplayName("~=1.2.3 should map to [1.2.3, 1.3.dev0)")
            void testCompatibleThreeSegments() throws VersionParsingException {
                final List<VersionRange> ranges = PypaSpecifier.parse("~=1.2.3").toRanges();
                assertThat(ranges).hasSize(1);

                final VersionRange range = ranges.get(0);
                assertThat(range.isMinIncluded()).isTrue();
                assertThat(range.getMinVersion()).hasToString("1.2.3");
                assertThat(range.isMaxIncluded()).isFalse();
                assertThat(range.getMaxVersion()).hasToString("1.3.dev0");
            }

            @Test
            @DisplayName("~=2.2 should map to [2.2, 3.dev0)")
            void testCompatibleTwoSegments() throws VersionParsingException {
                final List<VersionRange> ranges = PypaSpecifier.parse("~=2.2").toRanges();
                assertThat(ranges).hasSize(1);

                final VersionRange range = ranges.get(0);
                assertThat(range.isMinIncluded()).isTrue();
                assertThat(range.getMinVersion()).hasToString("2.2");
                assertThat(range.isMaxIncluded()).isFalse();
                assertThat(range.getMaxVersion()).hasToString("3.dev0");
            }
        }
    }
}
