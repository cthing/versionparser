/*
 * Copyright 2026 C Thing Software
 * SPDX-License-Identifier: Apache-2.0
 */
package org.cthing.versionparser.pypa;

import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import org.cthing.versionparser.VersionParsingException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.cthing.versionparser.pypa.PypaVersion.PrePhase;
import static org.junit.jupiter.params.provider.Arguments.arguments;


class PypaVersionTest {

    @SuppressWarnings("checkstyle:LineLength")
    static Stream<Arguments> goodVersionProvider() {
        return Stream.of(
                arguments("0.0",                    "0.0",                    "0.0",                    "0.0",       "0.0.dev0",               "0",                    0, List.of(0, 0),    null,        null, null, null, null,     false, false, false),
                arguments("1.0.dev0",               "1.0.dev0",               "1.0.dev0",               "1.0",       "1.0.dev0",               "1.dev0",               0, List.of(1, 0),    null,        null, null, 0,    null,     true,  false, true),
                arguments("1.0.dev456",             "1.0.dev456",             "1.0.dev456",             "1.0",       "1.0.dev0",               "1.dev456",             0, List.of(1, 0),    null,        null, null, 456,  null,     true,  false, true),
                arguments("1.0.dev456+local",       "1.0.dev456+local",       "1.0.dev456",             "1.0",       "1.0.dev0",               "1.dev456+local",       0, List.of(1, 0),    null,        null, null, 456,  "local",  true,  false, true),
                arguments("1.0a0",                  "1.0a0",                  "1.0a0",                  "1.0a0",     "1.0a0.dev0",             "1a0",                  0, List.of(1, 0),    PrePhase.a,  0,    null, null, null,     true,  false, false),
                arguments("1.0a0.post0.dev0",       "1.0a0.post0.dev0",       "1.0a0.post0.dev0",       "1.0a0",     "1.0a0.post0.dev0",       "1a0.post0.dev0",       0, List.of(1, 0),    PrePhase.a,  0,    0,    0,    null,     true,  true,  true),
                arguments("1.0a0.post0",            "1.0a0.post0",            "1.0a0.post0",            "1.0a0",     "1.0a0.post0.dev0",       "1a0.post0",            0, List.of(1, 0),    PrePhase.a,  0,    0,    null, null,     true,  true,  false),
                arguments("1.0a1.dev1",             "1.0a1.dev1",             "1.0a1.dev1",             "1.0a1",     "1.0a1.dev0",             "1a1.dev1",             0, List.of(1, 0),    PrePhase.a,  1,    null, 1,    null,     true,  false, true),
                arguments("1.0a1.dev1+local",       "1.0a1.dev1+local",       "1.0a1.dev1",             "1.0a1",     "1.0a1.dev0",             "1a1.dev1+local",       0, List.of(1, 0),    PrePhase.a,  1,    null, 1,    "local",  true,  false, true),
                arguments("1.0a1",                  "1.0a1",                  "1.0a1",                  "1.0a1",     "1.0a1.dev0",             "1a1",                  0, List.of(1, 0),    PrePhase.a,  1,    null, null, null,     true,  false, false),
                arguments("1.0ALPHA1+LOCAL",        "1.0a1+local",            "1.0a1",                  "1.0a1",     "1.0a1.dev0",             "1a1+local",            0, List.of(1, 0),    PrePhase.a,  1,    null, null, "local",  true,  false, false),
                arguments("1.0b0",                  "1.0b0",                  "1.0b0",                  "1.0b0",     "1.0b0.dev0",             "1b0",                  0, List.of(1, 0),    PrePhase.b,  0,    null, null, null,     true,  false, false),
                arguments("1.0b1.dev456",           "1.0b1.dev456",           "1.0b1.dev456",           "1.0b1",     "1.0b1.dev0",             "1b1.dev456",           0, List.of(1, 0),    PrePhase.b,  1,    null, 456,  null,     true,  false, true),
                arguments("1.0b2",                  "1.0b2",                  "1.0b2",                  "1.0b2",     "1.0b2.dev0",             "1b2",                  0, List.of(1, 0),    PrePhase.b,  2,    null, null, null,     true,  false, false),
                arguments("1.0b2.post345.dev456",   "1.0b2.post345.dev456",   "1.0b2.post345.dev456",   "1.0b2",     "1.0b2.post345.dev0",     "1b2.post345.dev456",   0, List.of(1, 0),    PrePhase.b,  2,    345,  456,  null,     true,  true,  true),
                arguments("1.0b2.post345",          "1.0b2.post345",          "1.0b2.post345",          "1.0b2",     "1.0b2.post345.dev0",     "1b2.post345",          0, List.of(1, 0),    PrePhase.b,  2,    345,  null, null,     true,  true,  false),
                arguments("1.0b2-346",              "1.0b2.post346",          "1.0b2.post346",          "1.0b2",     "1.0b2.post346.dev0",     "1b2.post346",          0, List.of(1, 0),    PrePhase.b,  2,    346,  null, null,     true,  true,  false),
                arguments("1.0rc0",                 "1.0rc0",                 "1.0rc0",                 "1.0rc0",    "1.0rc0.dev0",            "1rc0",                 0, List.of(1, 0),    PrePhase.rc, 0,    null, null, null,     true,  false, false),
                arguments("1.0rc1.dev1",            "1.0rc1.dev1",            "1.0rc1.dev1",            "1.0rc1",    "1.0rc1.dev0",            "1rc1.dev1",            0, List.of(1, 0),    PrePhase.rc, 1,    null, 1,    null,     true,  false, true),
                arguments("1.0c1",                  "1.0rc1",                 "1.0rc1",                 "1.0rc1",    "1.0rc1.dev0",            "1rc1",                 0, List.of(1, 0),    PrePhase.rc, 1,    null, null, null,     true,  false, false),
                arguments("1.0c2",                  "1.0rc2",                 "1.0rc2",                 "1.0rc2",    "1.0rc2.dev0",            "1rc2",                 0, List.of(1, 0),    PrePhase.rc, 2,    null, null, null,     true,  false, false),
                arguments("1.0",                    "1.0",                    "1.0",                    "1.0",       "1.0.dev0",               "1",                    0, List.of(1, 0),    null,        null, null, null, null,     false, false, false),
                arguments("1.0.post0.dev0",         "1.0.post0.dev0",         "1.0.post0.dev0",         "1.0",       "1.0.post0.dev0",         "1.post0.dev0",         0, List.of(1, 0),    null,        null, 0,    0,    null,     true,  true,  true),
                arguments("1.0.post0",              "1.0.post0",              "1.0.post0",              "1.0",       "1.0.post0.dev0",         "1.post0",              0, List.of(1, 0),    null,        null, 0,    null, null,     false, true,  false),
                arguments("1.0.post456.dev34",      "1.0.post456.dev34",      "1.0.post456.dev34",      "1.0",       "1.0.post456.dev0",       "1.post456.dev34",      0, List.of(1, 0),    null,        null, 456,  34,   null,     true,  true,  true),
                arguments("1.0.post456",            "1.0.post456",            "1.0.post456",            "1.0",       "1.0.post456.dev0",       "1.post456",            0, List.of(1, 0),    null,        null, 456,  null, null,     false, true,  false),
                arguments("1.0.post456+local",      "1.0.post456+local",      "1.0.post456",            "1.0",       "1.0.post456.dev0",       "1.post456+local",      0, List.of(1, 0),    null,        null, 456,  null, "local",  false, true,  false),
                arguments("1.0.1.dev1",             "1.0.1.dev1",             "1.0.1.dev1",             "1.0.1",     "1.0.1.dev0",             "1.0.1.dev1",           0, List.of(1, 0, 1), null,        null, null, 1,    null,     true,  false, true),
                arguments("1.0.1a1",                "1.0.1a1",                "1.0.1a1",                "1.0.1a1",   "1.0.1a1.dev0",           "1.0.1a1",              0, List.of(1, 0, 1), PrePhase.a,  1,    null, null, null,     true,  false, false),
                arguments("1.0.1",                  "1.0.1",                  "1.0.1",                  "1.0.1",     "1.0.1.dev0",             "1.0.1",                0, List.of(1, 0, 1), null,        null, null, null, null,     false, false, false),
                arguments("1.0.1+local",            "1.0.1+local",            "1.0.1",                  "1.0.1",     "1.0.1.dev0",             "1.0.1+local",          0, List.of(1, 0, 1), null,        null, null, null, "local",  false, false, false),
                arguments("1.0.1.post1",            "1.0.1.post1",            "1.0.1.post1",            "1.0.1",     "1.0.1.post1.dev0",       "1.0.1.post1",          0, List.of(1, 0, 1), null,        null, 1,    null, null,     false, true,  false),
                arguments("1.1.dev1",               "1.1.dev1",               "1.1.dev1",               "1.1",       "1.1.dev0",               "1.1.dev1",             0, List.of(1, 1),    null,        null, null, 1,    null,     true,  false, true),
                arguments("1.2+a",                  "1.2+a",                  "1.2",                    "1.2",       "1.2.dev0",               "1.2+a",                0, List.of(1, 2),    null,        null, null, null, "a",      false, false, false),
                arguments("1.2+abc",                "1.2+abc",                "1.2",                    "1.2",       "1.2.dev0",               "1.2+abc",              0, List.of(1, 2),    null,        null, null, null, "abc",    false, false, false),
                arguments("1.2+abcdef",             "1.2+abcdef",             "1.2",                    "1.2",       "1.2.dev0",               "1.2+abcdef",           0, List.of(1, 2),    null,        null, null, null, "abcdef", false, false, false),
                arguments("1.2+def",                "1.2+def",                "1.2",                    "1.2",       "1.2.dev0",               "1.2+def",              0, List.of(1, 2),    null,        null, null, null, "def",    false, false, false),
                arguments("1.2+0",                  "1.2+0",                  "1.2",                    "1.2",       "1.2.dev0",               "1.2+0",                0, List.of(1, 2),    null,        null, null, null, "0",      false, false, false),
                arguments("1.2+1",                  "1.2+1",                  "1.2",                    "1.2",       "1.2.dev0",               "1.2+1",                0, List.of(1, 2),    null,        null, null, null, "1",      false, false, false),
                arguments("1.2+1.abc",              "1.2+1.abc",              "1.2",                    "1.2",       "1.2.dev0",               "1.2+1.abc",            0, List.of(1, 2),    null,        null, null, null, "1.abc",  false, false, false),
                arguments("1.2+1.1",                "1.2+1.1",                "1.2",                    "1.2",       "1.2.dev0",               "1.2+1.1",              0, List.of(1, 2),    null,        null, null, null, "1.1",    false, false, false),
                arguments("1.2+1.1.0",              "1.2+1.1.0",              "1.2",                    "1.2",       "1.2.dev0",               "1.2+1.1.0",            0, List.of(1, 2),    null,        null, null, null, "1.1.0",  false, false, false),
                arguments("1.2+2",                  "1.2+2",                  "1.2",                    "1.2",       "1.2.dev0",               "1.2+2",                0, List.of(1, 2),    null,        null, null, null, "2",      false, false, false),
                arguments("1.2+123",                "1.2+123",                "1.2",                    "1.2",       "1.2.dev0",               "1.2+123",              0, List.of(1, 2),    null,        null, null, null, "123",    false, false, false),
                arguments("1.2+123456",             "1.2+123456",             "1.2",                    "1.2",       "1.2.dev0",               "1.2+123456",           0, List.of(1, 2),    null,        null, null, null, "123456", false, false, false),
                arguments("1.2.r32+123456",         "1.2.post32+123456",      "1.2.post32",             "1.2",       "1.2.post32.dev0",        "1.2.post32+123456",    0, List.of(1, 2),    null,        null, 32,   null, "123456", false, true,  false),
                arguments("1.2.rev33+123456",       "1.2.post33+123456",      "1.2.post33",             "1.2",       "1.2.post33.dev0",        "1.2.post33+123456",    0, List.of(1, 2),    null,        null, 33,   null, "123456", false, true,  false),
                arguments("1!1.0.DEV0",             "1!1.0.dev0",             "1!1.0.dev0",             "1!1.0",     "1!1.0.dev0",             "1!1.dev0",             1, List.of(1, 0),    null,        null, null, 0,    null,     true,  false, true),
                arguments("1!1.0.DEV456",           "1!1.0.dev456",           "1!1.0.dev456",           "1!1.0",     "1!1.0.dev0",             "1!1.dev456",           1, List.of(1, 0),    null,        null, null, 456,  null,     true,  false, true),
                arguments("1!1.0.dev456+local",     "1!1.0.dev456+local",     "1!1.0.dev456",           "1!1.0",     "1!1.0.dev0",             "1!1.dev456+local",     1, List.of(1, 0),    null,        null, null, 456,  "local",  true,  false, true),
                arguments("1!1.0a0",                "1!1.0a0",                "1!1.0a0",                "1!1.0a0",   "1!1.0a0.dev0",           "1!1a0",                1, List.of(1, 0),    PrePhase.a,  0,    null, null, null,     true,  false, false),
                arguments("1!1.0a0.post0.dev0",     "1!1.0a0.post0.dev0",     "1!1.0a0.post0.dev0",     "1!1.0a0",   "1!1.0a0.post0.dev0",     "1!1a0.post0.dev0",     1, List.of(1, 0),    PrePhase.a,  0,    0,    0,    null,     true,  true,  true),
                arguments("1!1.0a0.post0",          "1!1.0a0.post0",          "1!1.0a0.post0",          "1!1.0a0",   "1!1.0a0.post0.dev0",     "1!1a0.post0",          1, List.of(1, 0),    PrePhase.a,  0,    0,    null, null,     true,  true,  false),
                arguments("1!1.0a1.dev1",           "1!1.0a1.dev1",           "1!1.0a1.dev1",           "1!1.0a1",   "1!1.0a1.dev0",           "1!1a1.dev1",           1, List.of(1, 0),    PrePhase.a,  1,    null, 1,    null,     true,  false, true),
                arguments("1!1.0alpha1.dev1+local", "1!1.0a1.dev1+local",     "1!1.0a1.dev1",           "1!1.0a1",   "1!1.0a1.dev0",           "1!1a1.dev1+local",     1, List.of(1, 0),    PrePhase.a,  1,    null, 1,    "local",  true,  false, true),
                arguments("1!1.0a1",                "1!1.0a1",                "1!1.0a1",                "1!1.0a1",   "1!1.0a1.dev0",           "1!1a1",                1, List.of(1, 0),    PrePhase.a,  1,    null, null, null,     true,  false, false),
                arguments("1!1.0a1+local",          "1!1.0a1+local",          "1!1.0a1",                "1!1.0a1",   "1!1.0a1.dev0",           "1!1a1+local",          1, List.of(1, 0),    PrePhase.a,  1,    null, null, "local",  true,  false, false),
                arguments("1!1.0b0",                "1!1.0b0",                "1!1.0b0",                "1!1.0b0",   "1!1.0b0.dev0",           "1!1b0",                1, List.of(1, 0),    PrePhase.b,  0,    null, null, null,     true,  false, false),
                arguments("1!1.0b1.dev456",         "1!1.0b1.dev456",         "1!1.0b1.dev456",         "1!1.0b1",   "1!1.0b1.dev0",           "1!1b1.dev456",         1, List.of(1, 0),    PrePhase.b,  1,    null, 456,  null,     true,  false, true),
                arguments("1!1.0b2",                "1!1.0b2",                "1!1.0b2",                "1!1.0b2",   "1!1.0b2.dev0",           "1!1b2",                1, List.of(1, 0),    PrePhase.b,  2,    null, null, null,     true,  false, false),
                arguments("1!1.0b2.post345.dev456", "1!1.0b2.post345.dev456", "1!1.0b2.post345.dev456", "1!1.0b2",   "1!1.0b2.post345.dev0",   "1!1b2.post345.dev456", 1, List.of(1, 0),    PrePhase.b,  2,    345,  456,  null,     true,  true,  true),
                arguments("1!1.0beta2.post345",     "1!1.0b2.post345",        "1!1.0b2.post345",        "1!1.0b2",   "1!1.0b2.post345.dev0",   "1!1b2.post345",        1, List.of(1, 0),    PrePhase.b,  2,    345,  null, null,     true,  true,  false),
                arguments("1!1.0BETA2-346",         "1!1.0b2.post346",        "1!1.0b2.post346",        "1!1.0b2",   "1!1.0b2.post346.dev0",   "1!1b2.post346",        1, List.of(1, 0),    PrePhase.b,  2,    346,  null, null,     true,  true,  false),
                arguments("1!1.0rc0",               "1!1.0rc0",               "1!1.0rc0",               "1!1.0rc0",  "1!1.0rc0.dev0",          "1!1rc0",               1, List.of(1, 0),    PrePhase.rc, 0,    null, null, null,     true,  false, false),
                arguments("1!1.0RC1.dev1",          "1!1.0rc1.dev1",          "1!1.0rc1.dev1",          "1!1.0rc1",  "1!1.0rc1.dev0",          "1!1rc1.dev1",          1, List.of(1, 0),    PrePhase.rc, 1,    null, 1,    null,     true,  false, true),
                arguments("1!1.0c1",                "1!1.0rc1",               "1!1.0rc1",               "1!1.0rc1",  "1!1.0rc1.dev0",          "1!1rc1",               1, List.of(1, 0),    PrePhase.rc, 1,    null, null, null,     true,  false, false),
                arguments("1!1.0rc2",               "1!1.0rc2",               "1!1.0rc2",               "1!1.0rc2",  "1!1.0rc2.dev0",          "1!1rc2",               1, List.of(1, 0),    PrePhase.rc, 2,    null, null, null,     true,  false, false),
                arguments("1!1.0",                  "1!1.0",                  "1!1.0",                  "1!1.0",     "1!1.0.dev0",             "1!1",                  1, List.of(1, 0),    null,        null, null, null, null,     false, false, false),
                arguments("1!1.0.post0.dev0",       "1!1.0.post0.dev0",       "1!1.0.post0.dev0",       "1!1.0",     "1!1.0.post0.dev0",       "1!1.post0.dev0",       1, List.of(1, 0),    null,        null, 0,    0,    null,     true,  true,  true),
                arguments("1!1.0.post0",            "1!1.0.post0",            "1!1.0.post0",            "1!1.0",     "1!1.0.post0.dev0",       "1!1.post0",            1, List.of(1, 0),    null,        null, 0,    null, null,     false, true,  false),
                arguments("1!1.0.post456.dev34",    "1!1.0.post456.dev34",    "1!1.0.post456.dev34",    "1!1.0",     "1!1.0.post456.dev0",     "1!1.post456.dev34",    1, List.of(1, 0),    null,        null, 456,  34,   null,     true,  true,  true),
                arguments("1!1.0.POST456",          "1!1.0.post456",          "1!1.0.post456",          "1!1.0",     "1!1.0.post456.dev0",     "1!1.post456",          1, List.of(1, 0),    null,        null, 456,  null, null,     false, true,  false),
                arguments("1!1.0.post456+local",    "1!1.0.post456+local",    "1!1.0.post456",          "1!1.0",     "1!1.0.post456.dev0",     "1!1.post456+local",    1, List.of(1, 0),    null,        null, 456,  null, "local",  false, true,  false),
                arguments("1!1.0.1.dev1",           "1!1.0.1.dev1",           "1!1.0.1.dev1",           "1!1.0.1",   "1!1.0.1.dev0",           "1!1.0.1.dev1",         1, List.of(1, 0, 1), null,        null, null, 1,    null,     true,  false, true),
                arguments("1!1.0.1a1",              "1!1.0.1a1",              "1!1.0.1a1",              "1!1.0.1a1", "1!1.0.1a1.dev0",         "1!1.0.1a1",            1, List.of(1, 0, 1), PrePhase.a,  1,    null, null, null,     true,  false, false),
                arguments("1!1.0.1",                "1!1.0.1",                "1!1.0.1",                "1!1.0.1",   "1!1.0.1.dev0",           "1!1.0.1",              1, List.of(1, 0, 1), null,        null, null, null, null,     false, false, false),
                arguments("1!1.0.1+local",          "1!1.0.1+local",          "1!1.0.1",                "1!1.0.1",   "1!1.0.1.dev0",           "1!1.0.1+local",        1, List.of(1, 0, 1), null,        null, null, null, "local",  false, false, false),
                arguments("1!1.0.1.post1",          "1!1.0.1.post1",          "1!1.0.1.post1",          "1!1.0.1",   "1!1.0.1.post1.dev0",     "1!1.0.1.post1",        1, List.of(1, 0, 1), null,        null, 1,    null, null,     false, true,  false),
                arguments("1!1.1.dev1",             "1!1.1.dev1",             "1!1.1.dev1",             "1!1.1",     "1!1.1.dev0",             "1!1.1.dev1",           1, List.of(1, 1),    null,        null, null, 1,    null,     true,  false, true),
                arguments("1!1.2+a",                "1!1.2+a",                "1!1.2",                  "1!1.2",     "1!1.2.dev0",             "1!1.2+a",              1, List.of(1, 2),    null,        null, null, null, "a",      false, false, false),
                arguments("1!1.2+abc",              "1!1.2+abc",              "1!1.2",                  "1!1.2",     "1!1.2.dev0",             "1!1.2+abc",            1, List.of(1, 2),    null,        null, null, null, "abc",    false, false, false),
                arguments("1!1.2+abcdef",           "1!1.2+abcdef",           "1!1.2",                  "1!1.2",     "1!1.2.dev0",             "1!1.2+abcdef",         1, List.of(1, 2),    null,        null, null, null, "abcdef", false, false, false),
                arguments("1!1.2+def",              "1!1.2+def",              "1!1.2",                  "1!1.2",     "1!1.2.dev0",             "1!1.2+def",            1, List.of(1, 2),    null,        null, null, null, "def",    false, false, false),
                arguments("1!1.2+0",                "1!1.2+0",                "1!1.2",                  "1!1.2",     "1!1.2.dev0",             "1!1.2+0",              1, List.of(1, 2),    null,        null, null, null, "0",      false, false, false),
                arguments("1!1.2+1",                "1!1.2+1",                "1!1.2",                  "1!1.2",     "1!1.2.dev0",             "1!1.2+1",              1, List.of(1, 2),    null,        null, null, null, "1",      false, false, false),
                arguments("1!1.2+1.abc",            "1!1.2+1.abc",            "1!1.2",                  "1!1.2",     "1!1.2.dev0",             "1!1.2+1.abc",          1, List.of(1, 2),    null,        null, null, null, "1.abc",  false, false, false),
                arguments("1!1.2+1.1",              "1!1.2+1.1",              "1!1.2",                  "1!1.2",     "1!1.2.dev0",             "1!1.2+1.1",            1, List.of(1, 2),    null,        null, null, null, "1.1",    false, false, false),
                arguments("1!1.2+1.1.0",            "1!1.2+1.1.0",            "1!1.2",                  "1!1.2",     "1!1.2.dev0",             "1!1.2+1.1.0",          1, List.of(1, 2),    null,        null, null, null, "1.1.0",  false, false, false),
                arguments("1!1.2+2",                "1!1.2+2",                "1!1.2",                  "1!1.2",     "1!1.2.dev0",             "1!1.2+2",              1, List.of(1, 2),    null,        null, null, null, "2",      false, false, false),
                arguments("1!1.2+123",              "1!1.2+123",              "1!1.2",                  "1!1.2",     "1!1.2.dev0",             "1!1.2+123",            1, List.of(1, 2),    null,        null, null, null, "123",    false, false, false),
                arguments("1!1.2+123456",           "1!1.2+123456",           "1!1.2",                  "1!1.2",     "1!1.2.dev0",             "1!1.2+123456",         1, List.of(1, 2),    null,        null, null, null, "123456", false, false, false),
                arguments("1!1.2.r32+123456",       "1!1.2.post32+123456",    "1!1.2.post32",           "1!1.2",     "1!1.2.post32.dev0",      "1!1.2.post32+123456",  1, List.of(1, 2),    null,        null, 32,   null, "123456", false, true,  false),
                arguments("1!1.2.rev33+123456",     "1!1.2.post33+123456",    "1!1.2.post33",           "1!1.2",     "1!1.2.post33.dev0",      "1!1.2.post33+123456",  1, List.of(1, 2),    null,        null, 33,   null, "123456", false, true,  false)
        );
    }

    @ParameterizedTest(name = "[{index}] \"{0}\"")
    @MethodSource("goodVersionProvider")
    void testParse(final String versionStr, final String expectedCanonicalStr, final String expectedPublicVersion,
                   final String expectedPostBaseVersion, final String expectedEarliestPrerelease,
                   final String expectedTrimmedVersion, final int expectedEpoch, final List<Integer> expectedRelease,
                   final PrePhase expectedPrePhase, final Integer expectedPre, final Integer expectedPost,
                   final Integer expectedDev, final String expectedLocal, final boolean expectedIsPreRelease,
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
        assertThat(version.toPublicVersion().toCanonicalString()).as("public").isEqualTo(expectedPublicVersion);
        assertThat(version.toPostBaseVersion().toCanonicalString()).as("postBase").isEqualTo(expectedPostBaseVersion);
        assertThat(version.toEarliestPrereleaseVersion().toCanonicalString()).as("earliestPrerelease")
                                                                             .isEqualTo(expectedEarliestPrerelease);
        assertThat(version.toTrimmedVersion().toCanonicalString()).as("trimmed").isEqualTo(expectedTrimmedVersion);
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
}
