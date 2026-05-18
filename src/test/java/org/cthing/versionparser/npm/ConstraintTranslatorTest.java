/*
 * Copyright 2023 C Thing Software
 * SPDX-License-Identifier: Apache-2.0
 */

package org.cthing.versionparser.npm;

import java.util.stream.Stream;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.cthing.versionparser.npm.ConstraintTranslator.translate;
import static org.junit.jupiter.params.provider.Arguments.arguments;


class ConstraintTranslatorTest {

    static Stream<Arguments> noChangeProvider() {
        return Stream.of(
                arguments("<1.0.0"),
                arguments(">=1.0.0 <2.0.0")
        );
    }

    @ParameterizedTest
    @MethodSource("noChangeProvider")
    void testNoChange(final String constraint) {
        assertThat(translate(constraint)).isEqualTo(constraint);
    }

    static Stream<Arguments> greaterThanOrEqualZeroProvider() {
        return Stream.of(
                arguments("latest"),
                arguments("latest.integration"),
                arguments("*")
        );
    }

    @ParameterizedTest
    @MethodSource("greaterThanOrEqualZeroProvider")
    void testGreaterThanOrEqualZero(final String constraint) {
        assertThat(translate(constraint)).isEqualTo(">=0.0.0");
    }

    static Stream<Arguments> hyphenProvider() {
        return Stream.of(
                arguments("1.2.0 - 1.5.0", ">=1.2.0 <=1.5.0"),
                arguments("1.0.0 - 3.0.0", ">=1.0.0 <=3.0.0"),
                arguments("1.0.0 - 3.0",   ">=1.0.0 <3.1.0-0"),
                arguments("1.0.0 - 3",     ">=1.0.0 <4.0.0-0")
        );
    }

    @ParameterizedTest
    @MethodSource("hyphenProvider")
    void testHyphen(final String input, final String expected) {
        assertThat(translate(input)).isEqualTo(expected);
    }

    static Stream<Arguments> caretProvider() {
        return Stream.of(
                arguments("^1.2.3",       ">=1.2.3 <2.0.0-0"),
                arguments("^0.2.3",       ">=0.2.3 <0.3.0-0"),
                arguments("^0.0.3",       ">=0.0.3 <0.0.4-0"),
                arguments("^1.0.x",       ">=1.0.0 <2.0.0-0"),
                arguments("^1.0",         ">=1.0.0 <2.0.0-0"),
                arguments("^1.x",         ">=1.0.0 <2.0.0-0"),
                arguments("^1",           ">=1.0.0 <2.0.0-0"),
                arguments("^0.0.3-abc",   ">=0.0.3-abc <0.0.4-0"),
                arguments("^1.0.0-abc",   ">=1.0.0-abc <2.0.0-0"),
                arguments("^0.3.0-abc",   ">=0.3.0-abc <0.4.0-0"),
                arguments("^0.1.x",       ">=0.1.0 <0.2.0-0")
        );
    }

    @ParameterizedTest
    @MethodSource("caretProvider")
    void testCaret(final String input, final String expected) {
        assertThat(translate(input)).isEqualTo(expected);
    }

    static Stream<Arguments> tildeProvider() {
        return Stream.of(
                arguments("~1.2.3",     ">=1.2.3 <1.3.0-0"),
                arguments("~1.2.3-abc", ">=1.2.3-abc <1.3.0-0"),
                arguments("~0.2.3",     ">=0.2.3 <0.3.0-0"),
                arguments("~1.2",       ">=1.2.0 <1.3.0-0"),
                arguments("~1",         ">=1.0.0 <2.0.0-0")
        );
    }

    @ParameterizedTest
    @MethodSource("tildeProvider")
    void testTilde(final String input, final String expected) {
        assertThat(translate(input)).isEqualTo(expected);
    }

    static Stream<Arguments> xRangeProvider() {
        return Stream.of(
                arguments("1.2.x",         ">=1.2.0 <1.3.0-0"),
                arguments("1.2.X",         ">=1.2.0 <1.3.0-0"),
                arguments("1.x",           ">=1.0.0 <2.0.0-0"),
                arguments("1.2.x-abc",     ">=1.2.0 <1.3.0-0"),
                arguments("=1.2.x",        ">=1.2.0 <1.3.0-0"),
                arguments(">1.0.0 <1.2.x",  ">1.0.0 <1.2.0"),
                arguments(">1.0.0 <2.x",    ">1.0.0 <2.0.0"),
                arguments(">1.0.0 <2.0.x",  ">1.0.0 <2.0.0"),
                arguments(">1.0.0 <=2.x",   ">1.0.0 <3.0.0"),
                arguments(">1.0.0 <=2.0.x", ">1.0.0 <2.1.0"),
                arguments("<4.0.0 >2.0.x",  "<4.0.0 >=2.1.0"),
                arguments("<4.0.0 >=2.0.x", "<4.0.0 >=2.0.0"),
                arguments("<4.0.0 >=2.x",   "<4.0.0 >=2.0.0"),
                arguments("<4.0.0 >2.x",    "<4.0.0 >=3.0.0")
        );
    }

    @ParameterizedTest
    @MethodSource("xRangeProvider")
    void testXRange(final String input, final String expected) {
        assertThat(translate(input)).isEqualTo(expected);
    }
}
