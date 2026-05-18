/*
 * Copyright 2026 C Thing Software
 * SPDX-License-Identifier: Apache-2.0
 */
package org.cthing.versionparser.pypa;

import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import org.cthing.versionparser.VersionParsingException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.junit.jupiter.params.provider.Arguments.arguments;
import static org.cthing.versionparser.pypa.PypaVersion.PrePhase;


class PypaVersionTest {

    static Stream<Arguments> goodVersionProvider() {
        return Stream.of(
                arguments("1.0.dev0",               "1.0.dev0",               0, List.of(1, 0),    null,        null, null, 0,    null,     true,  false, true),
                arguments("1.0.dev456",             "1.0.dev456",             0, List.of(1, 0),    null,        null, null, 456,  null,     true,  false, true),
                arguments("1.0.dev456+local",       "1.0.dev456+local",       0, List.of(1, 0),    null,        null, null, 456,  "local",  true,  false, true),
                arguments("1.0a0",                  "1.0a0",                  0, List.of(1, 0),    PrePhase.a,  0,    null, null, null,     true,  false, false),
                arguments("1.0a0.post0.dev0",       "1.0a0.post0.dev0",       0, List.of(1, 0),    PrePhase.a,  0,    0,    0,    null,     true,  true,  true),
                arguments("1.0a0.post0",            "1.0a0.post0",            0, List.of(1, 0),    PrePhase.a,  0,    0,    null, null,     true,  true,  false),
                arguments("1.0a1.dev1",             "1.0a1.dev1",             0, List.of(1, 0),    PrePhase.a,  1,    null, 1,    null,     true,  false, true),
                arguments("1.0a1.dev1+local",       "1.0a1.dev1+local",       0, List.of(1, 0),    PrePhase.a,  1,    null, 1,    "local",  true,  false, true),
                arguments("1.0a1",                  "1.0a1",                  0, List.of(1, 0),    PrePhase.a,  1,    null, null, null,     true,  false, false),
                arguments("1.0ALPHA1+LOCAL",        "1.0a1+local",            0, List.of(1, 0),    PrePhase.a,  1,    null, null, "local",  true,  false, false),
                arguments("1.0b0",                  "1.0b0",                  0, List.of(1, 0),    PrePhase.b,  0,    null, null, null,     true,  false, false),
                arguments("1.0b1.dev456",           "1.0b1.dev456",           0, List.of(1, 0),    PrePhase.b,  1,    null, 456,  null,     true,  false, true),
                arguments("1.0b2",                  "1.0b2",                  0, List.of(1, 0),    PrePhase.b,  2,    null, null, null,     true,  false, false),
                arguments("1.0b2.post345.dev456",   "1.0b2.post345.dev456",   0, List.of(1, 0),    PrePhase.b,  2,    345,  456,  null,     true,  true,  true),
                arguments("1.0b2.post345",          "1.0b2.post345",          0, List.of(1, 0),    PrePhase.b,  2,    345,  null, null,     true,  true,  false),
                arguments("1.0b2-346",              "1.0b2.post346",          0, List.of(1, 0),    PrePhase.b,  2,    346,  null, null,     true,  true,  false),
                arguments("1.0rc0",                 "1.0rc0",                 0, List.of(1, 0),    PrePhase.rc, 0,    null, null, null,     true,  false, false),
                arguments("1.0rc1.dev1",            "1.0rc1.dev1",            0, List.of(1, 0),    PrePhase.rc, 1,    null, 1,    null,     true,  false, true),
                arguments("1.0c1",                  "1.0rc1",                 0, List.of(1, 0),    PrePhase.rc, 1,    null, null, null,     true,  false, false),
                arguments("1.0c2",                  "1.0rc2",                 0, List.of(1, 0),    PrePhase.rc, 2,    null, null, null,     true,  false, false),
                arguments("1.0",                    "1.0",                    0, List.of(1, 0),    null,        null, null, null, null,     false, false, false),
                arguments("1.0.post0.dev0",         "1.0.post0.dev0",         0, List.of(1, 0),    null,        null, 0,    0,    null,     true,  true,  true),
                arguments("1.0.post0",              "1.0.post0",              0, List.of(1, 0),    null,        null, 0,    null, null,     false, true,  false),
                arguments("1.0.post456.dev34",      "1.0.post456.dev34",      0, List.of(1, 0),    null,        null, 456,  34,   null,     true,  true,  true),
                arguments("1.0.post456",            "1.0.post456",            0, List.of(1, 0),    null,        null, 456,  null, null,     false, true,  false),
                arguments("1.0.post456+local",      "1.0.post456+local",      0, List.of(1, 0),    null,        null, 456,  null, "local",  false, true,  false),
                arguments("1.0.1.dev1",             "1.0.1.dev1",             0, List.of(1, 0, 1), null,        null, null, 1,    null,     true,  false, true),
                arguments("1.0.1a1",                "1.0.1a1",                0, List.of(1, 0, 1), PrePhase.a,  1,    null, null, null,     true,  false, false),
                arguments("1.0.1",                  "1.0.1",                  0, List.of(1, 0, 1), null,        null, null, null, null,     false, false, false),
                arguments("1.0.1+local",            "1.0.1+local",            0, List.of(1, 0, 1), null,        null, null, null, "local",  false, false, false),
                arguments("1.0.1.post1",            "1.0.1.post1",            0, List.of(1, 0, 1), null,        null, 1,    null, null,     false, true,  false),
                arguments("1.1.dev1",               "1.1.dev1",               0, List.of(1, 1),    null,        null, null, 1,    null,     true,  false, true),
                arguments("1.2+a",                  "1.2+a",                  0, List.of(1, 2),    null,        null, null, null, "a",      false, false, false),
                arguments("1.2+abc",                "1.2+abc",                0, List.of(1, 2),    null,        null, null, null, "abc",    false, false, false),
                arguments("1.2+abcdef",             "1.2+abcdef",             0, List.of(1, 2),    null,        null, null, null, "abcdef", false, false, false),
                arguments("1.2+def",                "1.2+def",                0, List.of(1, 2),    null,        null, null, null, "def",    false, false, false),
                arguments("1.2+0",                  "1.2+0",                  0, List.of(1, 2),    null,        null, null, null, "0",      false, false, false),
                arguments("1.2+1",                  "1.2+1",                  0, List.of(1, 2),    null,        null, null, null, "1",      false, false, false),
                arguments("1.2+1.abc",              "1.2+1.abc",              0, List.of(1, 2),    null,        null, null, null, "1.abc",  false, false, false),
                arguments("1.2+1.1",                "1.2+1.1",                0, List.of(1, 2),    null,        null, null, null, "1.1",    false, false, false),
                arguments("1.2+1.1.0",              "1.2+1.1.0",              0, List.of(1, 2),    null,        null, null, null, "1.1.0",  false, false, false),
                arguments("1.2+2",                  "1.2+2",                  0, List.of(1, 2),    null,        null, null, null, "2",      false, false, false),
                arguments("1.2+123",                "1.2+123",                0, List.of(1, 2),    null,        null, null, null, "123",    false, false, false),
                arguments("1.2+123456",             "1.2+123456",             0, List.of(1, 2),    null,        null, null, null, "123456", false, false, false),
                arguments("1.2.r32+123456",         "1.2.post32+123456",      0, List.of(1, 2),    null,        null, 32,   null, "123456", false, true,  false),
                arguments("1.2.rev33+123456",       "1.2.post33+123456",      0, List.of(1, 2),    null,        null, 33,   null, "123456", false, true,  false),
                arguments("1!1.0.DEV0",             "1!1.0.dev0",             1, List.of(1, 0),    null,        null, null, 0,    null,     true,  false, true),
                arguments("1!1.0.DEV456",           "1!1.0.dev456",           1, List.of(1, 0),    null,        null, null, 456,  null,     true,  false, true),
                arguments("1!1.0.dev456+local",     "1!1.0.dev456+local",     1, List.of(1, 0),    null,        null, null, 456,  "local",  true,  false, true),
                arguments("1!1.0a0",                "1!1.0a0",                1, List.of(1, 0),    PrePhase.a,  0,    null, null, null,     true,  false, false),
                arguments("1!1.0a0.post0.dev0",     "1!1.0a0.post0.dev0",     1, List.of(1, 0),    PrePhase.a,  0,    0,    0,    null,     true,  true,  true),
                arguments("1!1.0a0.post0",          "1!1.0a0.post0",          1, List.of(1, 0),    PrePhase.a,  0,    0,    null, null,     true,  true,  false),
                arguments("1!1.0a1.dev1",           "1!1.0a1.dev1",           1, List.of(1, 0),    PrePhase.a,  1,    null, 1,    null,     true,  false, true),
                arguments("1!1.0alpha1.dev1+local", "1!1.0a1.dev1+local",     1, List.of(1, 0),    PrePhase.a,  1,    null, 1,    "local",  true,  false, true),
                arguments("1!1.0a1",                "1!1.0a1",                1, List.of(1, 0),    PrePhase.a,  1,    null, null, null,     true,  false, false),
                arguments("1!1.0a1+local",          "1!1.0a1+local",          1, List.of(1, 0),    PrePhase.a,  1,    null, null, "local",  true,  false, false),
                arguments("1!1.0b0",                "1!1.0b0",                1, List.of(1, 0),    PrePhase.b,  0,    null, null, null,     true,  false, false),
                arguments("1!1.0b1.dev456",         "1!1.0b1.dev456",         1, List.of(1, 0),    PrePhase.b,  1,    null, 456,  null,     true,  false, true),
                arguments("1!1.0b2",                "1!1.0b2",                1, List.of(1, 0),    PrePhase.b,  2,    null, null, null,     true,  false, false),
                arguments("1!1.0b2.post345.dev456", "1!1.0b2.post345.dev456", 1, List.of(1, 0),    PrePhase.b,  2,    345,  456,  null,     true,  true,  true),
                arguments("1!1.0beta2.post345",     "1!1.0b2.post345",        1, List.of(1, 0),    PrePhase.b,  2,    345,  null, null,     true,  true,  false),
                arguments("1!1.0BETA2-346",         "1!1.0b2.post346",        1, List.of(1, 0),    PrePhase.b,  2,    346,  null, null,     true,  true,  false),
                arguments("1!1.0rc0",               "1!1.0rc0",               1, List.of(1, 0),    PrePhase.rc, 0,    null, null, null,     true,  false, false),
                arguments("1!1.0RC1.dev1",          "1!1.0rc1.dev1",          1, List.of(1, 0),    PrePhase.rc, 1,    null, 1,    null,     true,  false, true),
                arguments("1!1.0c1",                "1!1.0rc1",               1, List.of(1, 0),    PrePhase.rc, 1,    null, null, null,     true,  false, false),
                arguments("1!1.0rc2",               "1!1.0rc2",               1, List.of(1, 0),    PrePhase.rc, 2,    null, null, null,     true,  false, false),
                arguments("1!1.0",                  "1!1.0",                  1, List.of(1, 0),    null,        null, null, null, null,     false, false, false),
                arguments("1!1.0.post0.dev0",       "1!1.0.post0.dev0",       1, List.of(1, 0),    null,        null, 0,    0,    null,     true,  true,  true),
                arguments("1!1.0.post0",            "1!1.0.post0",            1, List.of(1, 0),    null,        null, 0,    null, null,     false, true,  false),
                arguments("1!1.0.post456.dev34",    "1!1.0.post456.dev34",    1, List.of(1, 0),    null,        null, 456,  34,   null,     true,  true,  true),
                arguments("1!1.0.POST456",          "1!1.0.post456",          1, List.of(1, 0),    null,        null, 456,  null, null,     false, true,  false),
                arguments("1!1.0.post456+local",    "1!1.0.post456+local",    1, List.of(1, 0),    null,        null, 456,  null, "local",  false, true,  false),
                arguments("1!1.0.1.dev1",           "1!1.0.1.dev1",           1, List.of(1, 0, 1), null,        null, null, 1,    null,     true,  false, true),
                arguments("1!1.0.1a1",              "1!1.0.1a1",              1, List.of(1, 0, 1), PrePhase.a,  1,    null, null, null,     true,  false, false),
                arguments("1!1.0.1",                "1!1.0.1",                1, List.of(1, 0, 1), null,        null, null, null, null,     false, false, false),
                arguments("1!1.0.1+local",          "1!1.0.1+local",          1, List.of(1, 0, 1), null,        null, null, null, "local",  false, false, false),
                arguments("1!1.0.1.post1",          "1!1.0.1.post1",          1, List.of(1, 0, 1), null,        null, 1,    null, null,     false, true,  false),
                arguments("1!1.1.dev1",             "1!1.1.dev1",             1, List.of(1, 1),    null,        null, null, 1,    null,     true,  false, true),
                arguments("1!1.2+a",                "1!1.2+a",                1, List.of(1, 2),    null,        null, null, null, "a",      false, false, false),
                arguments("1!1.2+abc",              "1!1.2+abc",              1, List.of(1, 2),    null,        null, null, null, "abc",    false, false, false),
                arguments("1!1.2+abcdef",           "1!1.2+abcdef",           1, List.of(1, 2),    null,        null, null, null, "abcdef", false, false, false),
                arguments("1!1.2+def",              "1!1.2+def",              1, List.of(1, 2),    null,        null, null, null, "def",    false, false, false),
                arguments("1!1.2+0",                "1!1.2+0",                1, List.of(1, 2),    null,        null, null, null, "0",      false, false, false),
                arguments("1!1.2+1",                "1!1.2+1",                1, List.of(1, 2),    null,        null, null, null, "1",      false, false, false),
                arguments("1!1.2+1.abc",            "1!1.2+1.abc",            1, List.of(1, 2),    null,        null, null, null, "1.abc",  false, false, false),
                arguments("1!1.2+1.1",              "1!1.2+1.1",              1, List.of(1, 2),    null,        null, null, null, "1.1",    false, false, false),
                arguments("1!1.2+1.1.0",            "1!1.2+1.1.0",            1, List.of(1, 2),    null,        null, null, null, "1.1.0",  false, false, false),
                arguments("1!1.2+2",                "1!1.2+2",                1, List.of(1, 2),    null,        null, null, null, "2",      false, false, false),
                arguments("1!1.2+123",              "1!1.2+123",              1, List.of(1, 2),    null,        null, null, null, "123",    false, false, false),
                arguments("1!1.2+123456",           "1!1.2+123456",           1, List.of(1, 2),    null,        null, null, null, "123456", false, false, false),
                arguments("1!1.2.r32+123456",       "1!1.2.post32+123456",    1, List.of(1, 2),    null,        null, 32,   null, "123456", false, true,  false),
                arguments("1!1.2.rev33+123456",     "1!1.2.post33+123456",    1, List.of(1, 2),    null,        null, 33,   null, "123456", false, true,  false)
        );
    }

    @ParameterizedTest(name = "[{index}] \"{0}\"")
    @MethodSource("goodVersionProvider")
    void testParse(final String versionStr, final String expectedCanonicalStr, final int expectedEpoch,
                   final List<Integer> expectedRelease, final PrePhase expectedPrePhase,
                   final Integer expectedPre, final Integer expectedPost, final Integer expectedDev,
                   final String expectedLocal, final boolean expectedIsPreRelease,
                   final boolean expectedIsPostRelease, final boolean expectedIsDevRelease)
            throws VersionParsingException {
        final PypaVersion version = PypaVersion.parse(versionStr);
        assertThat(version.getEpoch()).as("epoch").isEqualTo(expectedEpoch);
        assertThat(version.getRelease()).as("release").isEqualTo(expectedRelease);
        assertThat(version.getPrePhase()).as("prePhase").isEqualTo(Optional.ofNullable(expectedPrePhase));
        assertThat(version.getPre()).as("pre").isEqualTo(Optional.ofNullable(expectedPre));
        assertThat(version.getPost()).as("post").isEqualTo(Optional.ofNullable(expectedPost));
        assertThat(version.getDev()).as("dev").isEqualTo(Optional.ofNullable(expectedDev));
        assertThat(version.getLocal()).as("local").isEqualTo(Optional.ofNullable(expectedLocal));
        assertThat(version.isPreRelease()).as("isPreRelease").isEqualTo(expectedIsPreRelease);
        assertThat(version.isPostRelease()).as("isPostRelease").isEqualTo(expectedIsPostRelease);
        assertThat(version.isDevRelease()).as("isDevRelease").isEqualTo(expectedIsDevRelease);
        assertThat(version.toCanonicalString()).isEqualTo(expectedCanonicalStr);
        assertThat(version).hasToString(versionStr);
    }

    @ParameterizedTest
    @SuppressWarnings("UnnecessaryUnicodeEscape")
    @ValueSource(strings = {
            "French Toast",
            // Versions with invalid local versions
            "1.0+a+",
            "1.0++",
            "1.0+_foobar",
            "1.0+foo&asd",
            "1.0+1+1",
            // Spaces in versions are also invalid
            "1. 0",
            "1 .0",
            "1. 0a1",
            "1 .0a1",
            "1.0 a1",
            "1.0a 1",
            // Invalid versions that trigger the fast path (digits/dots only)
            ".",
            "..",
            "1..0",
            "1.0.",
            ".1.0",
            "1..2.3",
            // Local version which includes a non-ASCII letter that matches
            // regex '[a-z]' when re.IGNORECASE is in force and re.ASCII is not
            "1.0+\u0130"
    })
    void testInvalidVersion(final String version) {
        assertThatExceptionOfType(VersionParsingException.class)
                .isThrownBy(() -> PypaVersion.parse(version))
                .withMessage("Invalid PyPA version: " + version);
    }

    @Test
    void testOrdering() {
        // Extract the version strings from the provider and parse them into a list
        final List<PypaVersion> versions = goodVersionProvider()
                .map(args -> (String)args.get()[0])
                .map(versionStr -> {
                    try {
                        return PypaVersion.parse(versionStr);
                    } catch (final VersionParsingException ex) {
                        throw new RuntimeException("Failed to parse valid version: " + versionStr, ex);
                    }
                })
                .toList();

        // Compare every version against itself and all subsequent versions
        for (int i = 0; i < versions.size(); i++) {
            final PypaVersion v1 = versions.get(i);

            // A version should always equal itself
            assertThat(v1).isEqualByComparingTo(v1);

            for (int j = i + 1; j < versions.size(); j++) {
                final PypaVersion v2 = versions.get(j);

                // v1 comes before v2 in the provider, so v1 < v2
                assertThat(v1)
                        .as("Expected %s to be less than %s", v1, v2)
                        .isLessThan(v2);

                // Inversely, v2 > v1
                assertThat(v2)
                        .as("Expected %s to be greater than %s", v2, v1)
                        .isGreaterThan(v1);
            }
        }
    }

    @Test
    @SuppressWarnings({ "ConstantValue", "EqualsBetweenInconvertibleTypes", "EqualsWithItself" })
    void testEqualsAndHashCode() throws VersionParsingException {
        // Extract version strings from the provider
        final List<String> versionStrings = goodVersionProvider()
                .map(args -> (String)args.get()[0])
                .toList();

        for (int i = 0; i < versionStrings.size(); i++) {
            final String currentStr = versionStrings.get(i);
            final PypaVersion v1 = PypaVersion.parse(currentStr);
            final PypaVersion v1Copy = PypaVersion.parse(currentStr);

            // Reflexivity: x.equals(x)
            assertThat(v1).isEqualTo(v1);

            // Symmetry and logical equality
            assertThat(v1).isEqualTo(v1Copy);
            assertThat(v1Copy).isEqualTo(v1);

            // HashCode contract: if x.equals(y), then x.hashCode() == y.hashCode()
            assertThat(v1).hasSameHashCodeAs(v1Copy);

            // Not equal to null or different types
            assertThat(v1.equals(null)).isFalse();
            assertThat(v1.equals("not a version")).isFalse();

            // Inequality: current version should not equal any other version in the list
            for (int j = i + 1; j < versionStrings.size(); j++) {
                final PypaVersion v2 = PypaVersion.parse(versionStrings.get(j));

                assertThat(v1).isNotEqualTo(v2);
                assertThat(v1.hashCode()).as("HashCodes for %s and %s should ideally differ", v1, v2)
                                         .isNotEqualTo(v2.hashCode());
            }
        }
    }

    @Nested
    @DisplayName("Replace")
    class ReplaceTest {

        @Test
        void testReplaceAllSegments() throws VersionParsingException {
            final PypaVersion original = PypaVersion.parse("1!1.2.3a1.post1.dev1+ubuntu");

            final PypaVersion modified = original.replace(modifier -> modifier.withEpoch(2)
                                                                              .withRelease(List.of(4, 5, 6))
                                                                              .withPrePhase(PrePhase.b)
                                                                              .withPre(2)
                                                                              .withPost(2)
                                                                              .withDev(2)
                                                                              .withLocal("debian"));

            assertThat(modified.getEpoch()).isEqualTo(2);
            assertThat(modified.getRelease()).containsExactly(4, 5, 6);
            assertThat(modified.getPrePhase()).hasValue(PrePhase.b);
            assertThat(modified.getPre()).hasValue(2);
            assertThat(modified.getPost()).hasValue(2);
            assertThat(modified.getDev()).hasValue(2);
            assertThat(modified.getLocal()).hasValue("debian");
            assertThat(modified.toString()).isEqualTo("2!4.5.6b2.post2.dev2+debian");
        }

        @Test
        void testRemoveAllOptionalSegments() throws VersionParsingException {
            final PypaVersion original = PypaVersion.parse("1!1.2.3a1.post1.dev1+ubuntu");

            final PypaVersion modified = original.replace(modifier -> modifier.withoutEpoch()
                                                                              .withoutPrePhase()
                                                                              .withoutPre()
                                                                              .withoutPost()
                                                                              .withoutDev()
                                                                              .withoutLocal());

            assertThat(modified.getEpoch()).isZero();
            assertThat(modified.getPrePhase()).isEmpty();
            assertThat(modified.getPre()).isEmpty();
            assertThat(modified.getPost()).isEmpty();
            assertThat(modified.getDev()).isEmpty();
            assertThat(modified.getLocal()).isEmpty();
            assertThat(modified.toString()).isEqualTo("1.2.3");
        }

        @Test
        void testWithoutRelease() throws VersionParsingException {
            final PypaVersion original = PypaVersion.parse("1.2.3");

            final PypaVersion modified = original.replace(PypaVersion.Modifier::withoutRelease);

            assertThat(modified.getRelease()).containsExactly(0);
            assertThat(modified.toString()).isEqualTo("0");
        }

        @Test
        void testNoChangesReturnsSameInstance() throws VersionParsingException {
            final PypaVersion original = PypaVersion.parse("1.2.3a1.post2");

            final PypaVersion modified = original.replace(modifier -> { });

            assertThat(modified).isSameAs(original);
        }

        @Test
        void testRedundantChangesReturnsSameInstance() throws VersionParsingException {
            final PypaVersion original = PypaVersion.parse("1!2.3.4");

            final PypaVersion modified = original.replace(modifier -> modifier.withEpoch(1)
                                                                              .withRelease(List.of(2, 3, 4)));

            assertThat(modified).isSameAs(original);
        }

        @Test
        void testValidationRelease() throws VersionParsingException {
            final PypaVersion version = PypaVersion.parse("1.0.0");

            // Test empty list
            assertThatIllegalArgumentException()
                    .isThrownBy(() -> version.replace(m -> m.withRelease(List.of())))
                    .withMessage("Release cannot be empty");

            // Test negative values in list
            assertThatIllegalArgumentException()
                    .isThrownBy(() -> version.replace(m -> m.withRelease(List.of(1, -1, 0))))
                    .withMessage("Release values cannot be negative");
        }

        @Test
        void testValidationNegativeValues() throws VersionParsingException {
            final PypaVersion version = PypaVersion.parse("1.0.0");

            assertThatIllegalArgumentException()
                    .isThrownBy(() -> version.replace(m -> m.withEpoch(-1)))
                    .withMessage("Epoch cannot be negative");

            assertThatIllegalArgumentException()
                    .isThrownBy(() -> version.replace(m -> m.withPre(-1)))
                    .withMessage("Pre cannot be negative");

            assertThatIllegalArgumentException()
                    .isThrownBy(() -> version.replace(m -> m.withPost(-1)))
                    .withMessage("Post cannot be negative");

            assertThatIllegalArgumentException()
                    .isThrownBy(() -> version.replace(m -> m.withDev(-1)))
                    .withMessage("Dev cannot be negative");
        }

        @ParameterizedTest
        @ValueSource(strings = {"", "inv@lid", "no spaces", "+label"})
        void testValidationInvalidLocal(final String invalidLocal) throws VersionParsingException {
            final PypaVersion version = PypaVersion.parse("1.0.0");

            assertThatIllegalArgumentException()
                    .isThrownBy(() -> version.replace(m -> m.withLocal(invalidLocal)))
                    .withMessageContaining("Invalid local name");
        }
    }
}
