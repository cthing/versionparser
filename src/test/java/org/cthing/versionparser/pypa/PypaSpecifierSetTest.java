/*
 * Copyright 2026 C Thing Software
 * SPDX-License-Identifier: Apache-2.0
 */
package org.cthing.versionparser.pypa;

import java.util.List;
import java.util.stream.Stream;

import org.cthing.versionparser.VersionParsingException;
import org.cthing.versionparser.VersionRange;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.junit.jupiter.params.provider.Arguments.arguments;


class PypaSpecifierSetTest {

    static Stream<Arguments> specifierProvider() {
        return Stream.of(
                arguments("~=2.0", List.of("~=2.0")),
                arguments(">=1.0,<2.0", List.of(">=1.0", "<2.0")),
                arguments(">=1.0,<=2.0dev", List.of(">=1.0", "<=2.0dev"))
        );
    }

    @ParameterizedTest
    @MethodSource("specifierProvider")
    void testParse(final String spec, final List<String> expectedSpecifiers) throws VersionParsingException {
        final PypaSpecifierSet specifier = PypaSpecifierSet.parse(spec);
        final List<String> specifiers = specifier.getSpecifiers().stream().map(PypaSpecifier::toString).toList();
        assertThat(specifiers).isEqualTo(expectedSpecifiers);
        assertThat(specifier.toString()).isEqualTo(spec);
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "2.0",
            "=>2.0",
            ">2.0,foobar",
            ">2.0 <3.0",
            ",<3.0",
            "!=1.0.dev1.*"
    })
    void testInvalid(final String spec) {
        assertThatExceptionOfType(VersionParsingException.class).isThrownBy(() -> PypaSpecifierSet.parse(spec));
    }

    static Stream<Arguments> equalityProvider() {
        return Stream.of(
                arguments("~=2.0", "~=2.0", true),
                arguments("==2.1.*", "==2.1.*", true),
                arguments("==2.1.0.3,<1.2", "==2.1.0.3, <1.2", true),
                arguments("!=2.2.*,>1", "!=2.2.*,>1", true),
                arguments("==2.8.0", "==2.8.1", false),
                arguments("<2.8,>1.0", "<2.8,>1.1", false)
        );
    }

    @ParameterizedTest
    @MethodSource("equalityProvider")
    void testEquality(final String spec1, final String spec2, final boolean expectedEqual)
            throws VersionParsingException {
        final PypaSpecifierSet specifier1 = PypaSpecifierSet.parse(spec1);
        final PypaSpecifierSet specifier2 = PypaSpecifierSet.parse(spec2);

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
                // !=1.*, !=2.*, !=3.0 leaves gap at 3.0 prereleases
                arguments(">=1,!=1.*,!=2.*,!=3.0,<=3.0", "3.0.dev0", true),
                arguments(">=1,!=1.*,!=2.*,!=3.0,<=3.0", "3.0a1", true),
                // Versions outside the gap should not match
                arguments(">=1,!=1.*,!=2.*,!=3.0,<=3.0", "0.9", false),
                arguments(">=1,!=1.*,!=2.*,!=3.0,<=3.0", "1.0", false),
                arguments(">=1,!=1.*,!=2.*,!=3.0,<=3.0", "2.0", false),
                arguments(">=1,!=1.*,!=2.*,!=3.0,<=3.0", "3.0", false),
                arguments(">=1,!=1.*,!=2.*,!=3.0,<=3.0", "4.0", false),
                // >=1.0a1,!=1.*,!=2.*,<3.0 has no matching versions
                // because <3.0 excludes 3.0 prereleases
                arguments(">=1.0a1,!=1.*,!=2.*,<3.0", "1.0a1", false),
                arguments(">=1.0a1,!=1.*,!=2.*,<3.0", "2.0a1", false),
                arguments(">=1.0a1,!=1.*,!=2.*,<3.0", "3.0a1", false),
                // >=1.0.dev0,!=1.*,!=2.*,<3.0.dev0 has no matching versions
                arguments(">=1.0.dev0,!=1.*,!=2.*,<3.0.dev0", "1.0.dev0", false),
                arguments(">=1.0.dev0,!=1.*,!=2.*,<3.0.dev0", "2.0.dev0", false),
                arguments(">=1.0.dev0,!=1.*,!=2.*,<3.0.dev0", "3.0.dev0", false),
                // Gaps with post-releases
                arguments(">=1.0,!=1.0,!=1.1,<2.0", "1.0.post1", true),
                arguments(">=1.0,!=1.0,!=1.1,<2.0", "1.1.post1", true),
                arguments(">=1.0,!=1.0,!=1.1,<2.0", "1.0", false),
                arguments(">=1.0,!=1.0,!=1.1,<2.0", "1.1", false),
                arguments(">=1.0,!=1.0,!=1.1,<2.0", "2.0", false),
                // Dev version gaps
                arguments(">=1,!=1.*,!=2.*,!=3.0,!=3.1,<4", "3.0.dev0", true),
                arguments(">=1,!=1.*,!=2.*,!=3.0,!=3.1,<4", "3.1.dev0", true),
                arguments(">=1,!=1.*,!=2.*,!=3.0,!=3.1,<4", "0.5", false),
                arguments(">=1,!=1.*,!=2.*,!=3.0,!=3.1,<4", "3.0", false),
                arguments(">=1,!=1.*,!=2.*,!=3.0,!=3.1,<4", "3.1", false),
                arguments(">=1,!=1.*,!=2.*,!=3.0,!=3.1,<4", "5.0", false),
                // Test that < (exclusive) excludes prereleases of the specified version
                // but allows prereleases of earlier versions
                arguments(">=1.0a1,!=1.0,<1.1", "1.0a1", true),
                arguments(">=1.0a1,!=1.0,<1.1", "1.0b1", true),
                arguments(">=1.0a1,!=1.0,<1.1", "0.9", false),
                arguments(">=1.0a1,!=1.0,<1.1", "1.0", false),
                arguments(">=1.0a1,!=1.0,<1.1", "1.1", false),
                arguments(">=1.0a1,!=1.0,<1.1", "1.1.dev0", false),
                arguments(">=1.0a1,!=1.0,<1.1", "1.1a1", false),
                // Test that <= (inclusive) allows prereleases of the specified version
                // when explicitly requested, but follows default prerelease filtering
                arguments(">=0.9,!=0.9,<=1.0", "0.9.post1", true),
                arguments(">=0.9,!=0.9,<=1.0", "1.0", true),
                arguments(">=0.9,!=0.9,<=1.0", "1.0.dev0", true),
                arguments(">=0.9,!=0.9,<=1.0", "1.0a1", true),
                arguments(">=0.9,!=0.9,<=1.0", "1.0.post1", false),
                // Epoch-based gaps
                arguments(">=1!0,!=1!1.*,!=1!2.*,<1!3", "1!0.5", true),
                arguments(">=1!0,!=1!1.*,!=1!2.*,<1!3", "1!2.5", false),
                arguments(">=1!0,!=1!1.*,!=1!2.*,<1!3", "0!5.0", false),
                arguments(">=1!0,!=1!1.*,!=1!2.*,<1!3", "2!0.0", false),
                // >V.devN combined with other specifiers: post-releases of
                // the base release are accepted (they are not post-releases
                // of V.devN).
                arguments(">1.0.dev1,==1.0.post0", "1.0.post0", true),
                arguments(">1.0.dev1,==1.0.post1", "1.0.post1", true),
                arguments(">1.0a1,==1.0.post0", "1.0.post0", true),
                arguments(">1.0.dev1,<=2.0", "1.0.post0", true),
                arguments(">1.0.dev1,<=2.0", "1.0", true),
                // >V.preN: post of the pre-release itself is still excluded
                arguments(">1.0a1,<=2.0", "1.0a1.post0", false),
                // With an upper bound that includes post-releases
                arguments(">1.0.dev1,<=1.0.post1", "1.0.post0", true),
                arguments(">1.0.dev1,<=1.0.post1", "1.0.post1", true),
                arguments(">1.0.dev1,<=1.0.post1", "1.0", true),
                // != can remove some versions but post-releases still match
                arguments(">1.0.dev1,!=1.0,<=2.0", "1.0.post0", true),
                arguments(">1.0.dev1,!=1.0,!=1.0.post0,<=2.0", "1.0.post1", true),
                // <V.postN combined with other specifiers: pre-releases of
                // the base release are accepted (they are not pre-releases
                // of V.postN).
                arguments("==1.0.dev0,<1.0.post1", "1.0.dev0", true),
                arguments("==1.0a1,<1.0.post0", "1.0a1", true),
                arguments("==1.0.post0.dev0,<1.0.post1", "1.0.post0.dev0", true),
                arguments(">=1.0,<1.0.post1", "1.0", true),
                arguments(">=1.0,<1.0.post1", "1.0.post0", true),
                // 1.0.dev0 < 1.0, so it fails >=1.0 regardless of <
                arguments(">=1.0,<1.0.post1", "1.0.dev0", false),
                // With a lower bound that includes pre-releases
                arguments(">=1.0.dev0,<1.0.post1", "1.0.dev0", true),
                arguments(">=1.0.dev0,<1.0.post1", "1.0.a1", true),
                arguments(">=1.0.dev0,<1.0.post1", "1.0.post0.dev0", true),
                // != can remove non-pre-releases but pre-releases still match
                arguments(">=1.0.dev0,<1.0.post1,!=1.0,!=1.0.post0", "1.0.dev0", true),
                arguments(">=1.0.dev0,<1.0.post1,!=1.0,!=1.0.post0", "1.0.post0.dev0", true),
                // Post-release survivors still match
                arguments(">=1.0.dev0,<1.0.post2,!=1.0,!=1.0.post0", "1.0.post1", true),
                arguments(">=1.0.dev0,<1.0.post2,!=1.0", "1.0.post0", true)
        );
    }

    @ParameterizedTest
    @MethodSource("allowsProvider")
    void testAllows(final String spec, final String version, final boolean allow) throws VersionParsingException {
        final PypaSpecifierSet specifier = PypaSpecifierSet.parse(spec);
        assertThat(specifier.allows(version)).isEqualTo(allow);
        assertThat(specifier.allows(PypaVersion.parse(version))).isEqualTo(allow);
        assertThat(PypaSpecifierSet.ANY.allows(version)).isTrue();
        assertThat(PypaSpecifierSet.EMPTY.allows(version)).isFalse();
    }

    @Test
    void testToRanges() throws VersionParsingException {
        final PypaSpecifierSet specifierSet = PypaSpecifierSet.parse(">=1.2.0, <2.0.0, !=1.5.0");

        final List<VersionRange> ranges = specifierSet.toRanges();
        assertThat(ranges).hasSize(4);

        // Assert: Range 1 -> >=1.2.0
        final VersionRange greaterThanOrEqual = ranges.get(0);
        assertThat(greaterThanOrEqual.isMinIncluded()).isTrue();
        assertThat(greaterThanOrEqual.getMinVersion()).hasToString("1.2.0");
        assertThat(greaterThanOrEqual.isMaxIncluded()).isFalse();
        assertThat(greaterThanOrEqual.getMaxVersion()).isNull();

        // Assert: Range 2 -> <2.0.0
        final VersionRange lessThan = ranges.get(1);
        assertThat(lessThan.isMinIncluded()).isFalse();
        assertThat(lessThan.getMinVersion()).isNull();
        assertThat(lessThan.isMaxIncluded()).isFalse();
        assertThat(lessThan.getMaxVersion()).hasToString("2.0.0.dev0");

        // Assert: Range 3 -> !=1.5.0 (Lower Split: <1.5.0)
        final VersionRange notEqualLower = ranges.get(2);
        assertThat(notEqualLower.isMinIncluded()).isFalse();
        assertThat(notEqualLower.getMinVersion()).isNull();
        assertThat(notEqualLower.isMaxIncluded()).isFalse();
        assertThat(notEqualLower.getMaxVersion()).hasToString("1.5.0");

        // Assert: Range 4 -> !=1.5.0 (Upper Split: >1.5.0)
        final VersionRange notEqualUpper = ranges.get(3);
        assertThat(notEqualUpper.isMinIncluded()).isFalse();
        assertThat(notEqualUpper.getMinVersion()).hasToString("1.5");
        assertThat(notEqualUpper.isMaxIncluded()).isFalse();
        assertThat(notEqualUpper.getMaxVersion()).isNull();
    }

    @Test
    void testAnyToRanges() {
        final List<VersionRange> ranges = PypaSpecifierSet.ANY.toRanges();
        assertThat(ranges).hasSize(1);

        final VersionRange greaterThanOrEqual = ranges.get(0);
        assertThat(greaterThanOrEqual.isMinIncluded()).isTrue();
        assertThat(greaterThanOrEqual.getMinVersion()).hasToString("0.dev0");
        assertThat(greaterThanOrEqual.isMaxIncluded()).isFalse();
        assertThat(greaterThanOrEqual.getMaxVersion()).isNull();
    }

    @Test
    void testEmptyToRanges() {
        final List<VersionRange> ranges = PypaSpecifierSet.EMPTY.toRanges();
        assertThat(ranges).isEmpty();
    }
}
