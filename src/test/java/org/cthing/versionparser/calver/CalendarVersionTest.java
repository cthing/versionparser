/*
 * Copyright 2023 C Thing Software
 * SPDX-License-Identifier: Apache-2.0
 */
package org.cthing.versionparser.calver;

import java.util.stream.Stream;

import org.cthing.versionparser.VersionParsingException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.params.provider.Arguments.arguments;


public class CalendarVersionTest {

    private static final int GT = 1;
    private static final int LT = -1;
    private static final int EQ = 0;

    static Stream<Arguments> orderProvider() {
        return Stream.of(
                arguments("YYYY", "2020", "YYYY", "2010", GT),
                arguments("YYYY", "2010", "YYYY", "2020", LT),
                arguments("YYYY", "2020", "YYYY", "2020", EQ),
                arguments("YYYY", "2020", "YY", "10", GT),
                arguments("YYYY", "2010", "YY", "20", LT),
                arguments("YYYY", "2020", "YY", "20", EQ),
                arguments("YYYY", "2020", "0Y", "02", GT),
                arguments("YYYY", "2010", "0Y", "120", LT),
                arguments("YYYY", "2020", "0Y", "20", EQ),

                arguments("MM", "11", "MM", "5", GT),
                arguments("MM", "5", "MM", "11", LT),
                arguments("MM", "5", "MM", "5", EQ),
                arguments("MM", "11", "0M", "05", GT),
                arguments("MM", "5", "0M", "11", LT),
                arguments("MM", "5", "0M", "05", EQ),

                arguments("DD", "11", "DD", "5", GT),
                arguments("DD", "5", "DD", "11", LT),
                arguments("DD", "5", "DD", "5", EQ),
                arguments("DD", "11", "0D", "05", GT),
                arguments("DD", "5", "0D", "11", LT),
                arguments("DD", "5", "0D", "05", EQ),

                arguments("WW", "11", "WW", "5", GT),
                arguments("WW", "5", "WW", "11", LT),
                arguments("WW", "5", "WW", "5", EQ),
                arguments("WW", "11", "0W", "05", GT),
                arguments("WW", "5", "0W", "11", LT),
                arguments("WW", "5", "0W", "05", EQ),

                arguments("MAJOR", "11", "MAJOR", "5", GT),
                arguments("MAJOR", "5", "MAJOR", "11", LT),
                arguments("MAJOR", "5", "MAJOR", "5", EQ),

                arguments("MINOR", "11", "MINOR", "5", GT),
                arguments("MINOR", "5", "MINOR", "11", LT),
                arguments("MINOR", "5", "MINOR", "5", EQ),

                arguments("PATCH", "11", "PATCH", "5", GT),
                arguments("PATCH", "5", "PATCH", "11", LT),
                arguments("PATCH", "5", "PATCH", "5", EQ),

                arguments("MAJOR", "5-beta", "MAJOR", "5-alpha", GT),
                arguments("MAJOR", "5-alpha", "MAJOR", "5-beta", LT),
                arguments("MAJOR", "5-hello", "MAJOR", "5-hello", EQ),
                arguments("MAJOR", "5-hello.10", "MAJOR", "5-hello.1", GT),
                arguments("MAJOR", "5-hello.1", "MAJOR", "5-hello.10", LT),
                arguments("MAJOR", "5-hello.2", "MAJOR", "5-hello.10", LT),
                arguments("MAJOR", "5-hello1", "MAJOR", "5-hello10", LT),
                arguments("MAJOR", "5-hello2", "MAJOR", "5-hello10", LT),

                arguments("YYYY.MM.DD", "2020.10.15", "YYYY.MM.DD", "2010.10.15", GT),
                arguments("YYYY.MM.DD", "2020.10.15", "YYYY.MM.DD", "2020.09.15", GT),
                arguments("YYYY.MM.DD", "2020.10.15", "YYYY.MM.DD", "2020.10.07", GT),
                arguments("YYYY.MM.DD", "2010.10.15", "YYYY.MM.DD", "2020.10.15", LT),
                arguments("YYYY.MM.DD", "2020.09.15", "YYYY.MM.DD", "2020.10.15", LT),
                arguments("YYYY.MM.DD", "2020.10.07", "YYYY.MM.DD", "2020.10.15", LT),
                arguments("YYYY.MM.DD", "2020.1.6", "YY.0M.0D", "20.01.06", EQ),

                arguments("YYYY.MM.DD", "2020.10.15", "YYYY.MM.DD", "2020.10.15-alpha.1", GT),
                arguments("YYYY.MM.DD", "2020.10.15-alpha.1", "YYYY.MM.DD", "2020.10.15", LT),
                arguments("YYYY.MM.DD", "2020.10.15-alpha.1", "YYYY.MM.DD", "2020.10.15-alpha", GT),
                arguments("YYYY.MM.DD", "2020.10.15-alpha.1", "YYYY.MM.DD", "2020.10.15-beta", LT),

                arguments("YYYY.MM.DD", "2020.10.15", "YYYY.MM", "2020.10", GT),
                arguments("YYYY.MM", "2020.10", "YYYY.MM.DD", "2020.10.15", LT),
                arguments("YYYY.MM", "2020.10", "YYYY.DD", "2010.15", GT)
        );
    }


    @ParameterizedTest
    @MethodSource("orderProvider")
    public void testOrdering(final String format1, final String version1, final String format2, final String version2,
                             final int result) throws VersionParsingException {
        final CalendarVersion v1 = CalendarVersionScheme.parse(format1, version1);
        final CalendarVersion v2 = CalendarVersionScheme.parse(format2, version2);
        assertThat(v1.compareTo(v2)).isEqualTo(result);
    }

    static Stream<Arguments> equalityProvider() {
        return Stream.of(
                arguments("YYYY", "2020", "YYYY", "2020", true),
                arguments("YYYY", "2020", "YY", "20", true),
                arguments("YYYY", "2002", "0Y", "02", true),
                arguments("WW", "3", "WW", "3", true),
                arguments("WW", "3", "0W", "03", true),
                arguments("MM", "3", "MM", "3", true),
                arguments("MM", "3", "0M", "03", true),
                arguments("DD", "3", "DD", "3", true),
                arguments("DD", "3", "0D", "03", true),

                arguments("YYYY", "2020", "YYYY", "2021", false),
                arguments("YYYY", "2020", "YY", "21", false),
                arguments("YYYY", "2002", "0Y", "01", false),
                arguments("WW", "3", "WW", "2", false),
                arguments("WW", "3", "0W", "02", false),
                arguments("MM", "3", "MM", "2", false),
                arguments("MM", "3", "0M", "02", false),
                arguments("DD", "3", "DD", "2", false),
                arguments("DD", "3", "0D", "02", false),

                arguments("YYYY.MM.DD.MAJOR.MINOR", "2023.11.2.1.0", "YYYY.MM.DD.MAJOR.MINOR", "2023.11.2.1.0", true),
                arguments("YYYY.MM.DD.MAJOR.MINOR", "2023.11.2.1.0-abc", "YYYY.MM.DD.MAJOR.MINOR", "2023.11.2.1.0-abc",
                          true),
                arguments("YYYY.MM.DD.MAJOR.MINOR", "2023.11.2.1.0", "YYYY.MM.DD.MAJOR.MINOR", "2023.11.2.1.1", false),
                arguments("YYYY.MM.DD.MAJOR.MINOR", "2023.11.2.1.0", "YYYY.MM.DD.MAJOR.MINOR", "2023.11.2.0.0", false),
                arguments("YYYY.MM.DD.MAJOR.MINOR", "2023.11.2.1.0", "YYYY.MM.DD.MAJOR.MINOR", "2023.11.1.1.0", false),
                arguments("YYYY.MM.DD.MAJOR.MINOR", "2023.11.2.1.0", "YYYY.MM.DD.MAJOR.MINOR", "2023.10.2.1.0", false),
                arguments("YYYY.MM.DD.MAJOR.MINOR", "2023.11.2.1.0", "YYYY.MM.DD.MAJOR.MINOR", "2022.11.2.1.0", false),
                arguments("YYYY.MAJOR", "2023.2", "YYYY.MINOR", "2023.2", false)
        );
    }

    @ParameterizedTest
    @MethodSource("equalityProvider")
    public void testEquality(final String format1, final String version1, final String format2, final String version2,
                             final boolean equal) throws VersionParsingException {
        final CalendarVersion v1 = CalendarVersionScheme.parse(format1, version1);
        final CalendarVersion v2 = CalendarVersionScheme.parse(format2, version2);

        if (equal) {
            assertThat(v1).isEqualTo(v2);
            assertThat(v1).hasSameHashCodeAs(v2);
        } else {
            assertThat(v1).isNotEqualTo(v2);
            assertThat(v1).doesNotHaveSameHashCodeAs(v2);
        }
    }

    @Test
    @SuppressWarnings("EqualsWithItself")
    public void testEquality2() throws VersionParsingException {
        final CalendarVersion version = CalendarVersionScheme.parse("YYYY.WW", "2023.50");
        assertThat(version).isEqualTo(version);
        assertThat(version).isNotEqualTo(null);
        assertThat(version).isNotEqualTo("hello");
    }
}
