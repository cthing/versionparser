/*
 * Copyright 2026 C Thing Software
 * SPDX-License-Identifier: Apache-2.0
 */
package org.cthing.versionparser.debian;

import java.util.stream.Stream;

import org.cthing.versionparser.VersionParsingException;
import org.cthing.versionparser.semver.SemanticVersion;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.junit.jupiter.params.provider.Arguments.arguments;


public class DebVersionTest {

    public static Stream<Arguments> goodVersionProvider() {
        return Stream.of(
                arguments("1.2.3", 0, "1.2.3", "0", false),
                arguments("1.2.3-1ubuntu1", 0, "1.2.3", "1ubuntu1", false),
                arguments("2:1.2.3-4", 2, "1.2.3", "4", false),
                arguments("1.2.3+git2023-1", 0, "1.2.3+git2023", "1", false),
                arguments("5:1.0.0+git-20190109.133f4c4-0ubuntu2", 5, "1.0.0+git-20190109.133f4c4", "0ubuntu2", false),
                arguments("3.32.2+git20190711-2ubuntu1~19.04.1", 0, "3.32.2+git20190711", "2ubuntu1~19.04.1", false),
                arguments("11.4.0-1ubuntu1~22.04.2", 0, "11.4.0", "1ubuntu1~22.04.2", false),
                arguments("2.42.1+19.04", 0, "2.42.1+19.04", "0", false),
                arguments("1.0~alpha1-1", 0, "1.0~alpha1", "1", true),
                arguments("2.4.0~rc2-0ubuntu1", 0, "2.4.0~rc2", "0ubuntu1", true),
                arguments("5.0~exp20260101", 0, "5.0~exp20260101", "0", true),
                arguments("1.2.3~~pre1-1", 0, "1.2.3~~pre1", "1", true)
        );
    }

    @ParameterizedTest
    @MethodSource("goodVersionProvider")
    public void testValidParsing(final String versionStr, final int expectedEpoch, final String expectedUpstream,
                                 final String expectedRevision, final boolean isPrerelease)
            throws VersionParsingException {
        final DebVersion version = DebVersion.parse(versionStr);
        assertThat(version.getEpoch()).isEqualTo(expectedEpoch);
        assertThat(version.getUpstream()).isEqualTo(expectedUpstream);
        assertThat(version.getRevision()).isEqualTo(expectedRevision);
        assertThat(version.isPreRelease()).isEqualTo(isPrerelease);
        assertThat(version).hasToString(versionStr);
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "1.2.3-",           // Trailing hyphen results in empty revision
            "",                 // Empty string not allowed
            "---",              // Starts with hyphen (Forbidden)
            ":1.2",             // Missing epoch number
            "1.2.3-rev!",       // '!' is not in [.+~0-9a-zA-Z-]
            "1.2.3_4",          // Underscore not allowed
            "-1.2.3",           // Upstream starts with hyphen
            "1.2.3-re vision",  // Spaces not allowed
            "1:2:3",            // Multiple colons not allowed
            "a:1.2.3"           // Epoch must be numeric [0-9]+
    })
    public void testInvalidParsing(final String version) {
        assertThatExceptionOfType(VersionParsingException.class)
                .isThrownBy(() -> DebVersion.parse(version))
                .withMessage("Invalid Debian version: " + version);
    }

    public static Stream<Arguments> equalityProvider() {
        return Stream.of(
                arguments("1:2.3.4-5", "1:2.3.4-5", true),
                arguments(" 1:2.3.4-5 ", "1:2.3.4-5", true),
                arguments("1:2.3.4-5", "1:2.3.4-6", false),
                arguments("1:2.3.4-5", "1:2.3.5-5", false),
                arguments("1:1.0", "2:1.0", false),
                arguments("0:1.0", "1.0", true),
                arguments("1.2.3", "1.2.3-0", true),
                arguments("abd-def", "abd-def", true),
                arguments("1.0.6972245602301010000", "1.0.6972245602301010000", true)
        );
    }

    @ParameterizedTest
    @MethodSource("equalityProvider")
    public void testEquality(final String versionStr1, final String versionStr2, final boolean isEqual)
            throws VersionParsingException {
        final DebVersion version1 = DebVersion.parse(versionStr1);
        final DebVersion version2 = DebVersion.parse(versionStr2);

        if (isEqual) {
            assertThat(version1).isEqualTo(version2);
            assertThat(version1).hasSameHashCodeAs(version2);
            assertThat(version1).isEqualByComparingTo(version2);
        } else {
            assertThat(version1).isNotEqualTo(version2);
            assertThat(version1).doesNotHaveSameHashCodeAs(version2);
            assertThat(version1).isNotEqualByComparingTo(version2);
        }
    }

    @Test
    @SuppressWarnings("AssertBetweenInconvertibleTypes")
    public void testTypeInequality() throws VersionParsingException {
        final DebVersion deb = DebVersion.parse("1.2.3");
        final SemanticVersion other = SemanticVersion.parse("1.2.3");
        assertThat(deb).isNotEqualTo(other);
    }

    @ParameterizedTest(name = "{0} < {1}")
    @CsvSource({
            // Basic Numeric
            "1.0, 1.1",
            "1.0, 2.0",
            "1.0, 1.0.1",

            // Epoch Handling
            "1.0, 1:1.0",
            "1:1.0, 2:0.1", // Higher epoch wins regardless of version
            "1.2.3, 1:1.2.3",

            // Tilde Sorting (The "Magic" of Debian versions)
            // Tilde sorts before the end of the string (empty)
            "1.0~, 1.0",
            "1.0~rc1, 1.0",
            "1.0~beta, 1.0~rc",
            "1.0-1~, 1.0-1",
            "1.0~rc2, 1.0",           // Tilde beats everything
            "1.0~rc2, 1.0~rc10",      // Numeric comparison after tilde
            "1.0~rc1, 1.0a",          // Tilde (smaller than empty) vs Letter
            "1.0~rc1, 1.0.1",         // Tilde vs Punctuation
            "1.0~alpha1, 1.0~beta1",  // Letters after tildes

            // Tilde sorting before other characters?
            // "~~" comes before "~"
            "1.0~~, 1.0~",

            // Letters vs Non-Letters
            // Letters sort *before* non-letters (like . or +)
            "1.0a, 1.0.1",
            "1.0a, 1.0+",
            "1.0-1, 1.0a-1",

            // Letters vs Letters
            "1.0a, 1.0b",
            "1.0A, 1.0b",
            "1.0-A, 1.0-B",

            // Revisions
            "1.0-1, 1.0-2",
            "1.0, 1.0-1", // No revision (0) vs revision 1

            // Lexical Digits
            "1.0-1, 1.0-10",
            "1.0-9, 1.0-10",
            "1.9, 1.10",

            // Upstream vs Revision boundaries
            "1.2-3, 1.2.1-1", // 1.2 < 1.2.1
            "1.2-1, 1.2.1-1",      // Length divergence in upstream
            "1.2.3-1, 1.2.3.1",    // Revision separator '-' vs Upstream '.'

            // Complex Real World Examples
            "1.0.4+git2023, 1.0.5",
            "2.2.0-0ubuntu1, 2.2.0-0ubuntu2",

            // Large integers
            "1.0.202301010000, 1.0.202301010001",
            "1.0.1769893975703, 1.0.1769893975707",
            "1.0.62245602301010000, 1.0.62245602301010001",
            "1.0.672245602301010000, 1.0.672245602301010001",
            "1.0.6972245602301010000, 1.0.6972245602301010001",

            "9:99-99, 10:01-01",      // Epoch precedence (9 vs 10)
            "abd-de, abd-def",        // Shorter lexical string is smaller
            "a1b2d-d3e, a1b2d-d3ef",  // Shorter string smaller even with digits
            "a1b2d-d9, a1b2d-d13",    // Embedded numeric comparison
            "a1b2d-d10~, a1b2d-d10"   // Tilde is smaller than End-of-String
    })
    public void testCompare(final String smaller, final String larger) throws VersionParsingException {
        final DebVersion version1 = DebVersion.parse(smaller);
        final DebVersion version2 = DebVersion.parse(larger);

        assertThat(version1).isLessThan(version2);
        assertThat(version2).isGreaterThan(version1);
    }

    @Test
    @SuppressWarnings("ResultOfMethodCallIgnored")
    public void testBadCompare() throws VersionParsingException {
        final DebVersion deb = DebVersion.parse("1.2.3");
        final SemanticVersion other = SemanticVersion.parse("1.2.4");
        assertThatIllegalArgumentException().isThrownBy(() -> deb.compareTo(other));
    }
}
