/*
 * Copyright 2023 C Thing Software
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

package org.cthing.versionparser.calver;

import java.util.List;
import java.util.stream.Stream;

import org.cthing.versionparser.VersionParsingException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.cthing.versionparser.calver.ComponentFormat.DD;
import static org.cthing.versionparser.calver.ComponentFormat.MAJOR;
import static org.cthing.versionparser.calver.ComponentFormat.MINOR;
import static org.cthing.versionparser.calver.ComponentFormat.MM;
import static org.cthing.versionparser.calver.ComponentFormat.MODIFIER;
import static org.cthing.versionparser.calver.ComponentFormat.PATCH;
import static org.cthing.versionparser.calver.ComponentFormat.WW;
import static org.cthing.versionparser.calver.ComponentFormat.YY;
import static org.cthing.versionparser.calver.ComponentFormat.YYYY;
import static org.cthing.versionparser.calver.ComponentFormat.ZERO_D;
import static org.cthing.versionparser.calver.ComponentFormat.ZERO_M;
import static org.cthing.versionparser.calver.ComponentFormat.ZERO_W;
import static org.cthing.versionparser.calver.ComponentFormat.ZERO_Y;
import static org.junit.jupiter.params.provider.Arguments.arguments;


public class CalendarVersionSchemeTest {

    static Stream<Arguments> versionProvider() throws VersionParsingException {
        return Stream.of(
                arguments("yyyy", "2021", false, List.of(comp(YYYY, "2021"))),
                arguments("yy", "21", false, List.of(comp(YY, "21"))),
                arguments("0y", "01", false, List.of(comp(ZERO_Y, "01"))),
                arguments("0y", "101", false, List.of(comp(ZERO_Y, "101"))),
                arguments("WW", "11", false, List.of(comp(WW, "11"))),
                arguments("0W", "01", false, List.of(comp(ZERO_W, "01"))),
                arguments("MM", "11", false, List.of(comp(MM, "11"))),
                arguments("0M", "01", false, List.of(comp(ZERO_M, "01"))),
                arguments("DD", "11", false, List.of(comp(DD, "11"))),
                arguments("0D", "01", false, List.of(comp(ZERO_D, "01"))),
                arguments("MAJOR", "1", false, List.of(comp(MAJOR, "1"))),
                arguments("MINOR", "1", false, List.of(comp(MINOR, "1"))),
                arguments("PATCH", "1", false, List.of(comp(PATCH, "1"))),

                arguments("YYYY.mm.dd", "2016.6.1", false, List.of(comp(YYYY, "2016"),
                                                                   comp(MM, "6"),
                                                                   comp(DD, "1"))),
                arguments("YYYY.mm.0d_MAJOR-MINOR-PATCH", "2016.6.01_5-4-3", false, List.of(comp(YYYY, "2016"),
                                                                                            comp(MM, "6"),
                                                                                            comp(DD, "01"),
                                                                                            comp(MAJOR, "5"),
                                                                                            comp(MINOR, "4"),
                                                                                            comp(PATCH, "3"))),
                arguments("yy.ww", "20.16-hello.world", false, List.of(comp(YY, "20"),
                                                                       comp(WW, "16"),
                                                                       comp(MODIFIER, "hello.world"))),
                arguments("yy.ww", "20.16-alpha", true, List.of(comp(YY, "20"),
                                                                       comp(WW, "16"),
                                                                       comp(MODIFIER, "alpha"))),
                arguments("yy.ww", "20.16-alpha.1", true, List.of(comp(YY, "20"),
                                                                       comp(WW, "16"),
                                                                       comp(MODIFIER, "alpha.1"))),
                arguments("yy.ww", "20.16-beta.1", true, List.of(comp(YY, "20"),
                                                                       comp(WW, "16"),
                                                                       comp(MODIFIER, "beta.1"))),
                arguments("yy.ww", "20.16-cr", true, List.of(comp(YY, "20"),
                                                                       comp(WW, "16"),
                                                                       comp(MODIFIER, "cr"))),
                arguments("yy.ww", "20.16-dev", true, List.of(comp(YY, "20"),
                                                                       comp(WW, "16"),
                                                                       comp(MODIFIER, "dev"))),
                arguments("yy.ww", "20.16-milestone-1", true, List.of(comp(YY, "20"),
                                                                       comp(WW, "16"),
                                                                       comp(MODIFIER, "milestone-1"))),
                arguments("yy.ww", "20.16-rc_1", true, List.of(comp(YY, "20"),
                                                                       comp(WW, "16"),
                                                                       comp(MODIFIER, "rc_1"))),
                arguments("yy.ww", "20.16-snapshot", true, List.of(comp(YY, "20"),
                                                                       comp(WW, "16"),
                                                                       comp(MODIFIER, "snapshot")))
        );
    }

    @ParameterizedTest
    @MethodSource("versionProvider")
    public void testParseValid(final String format, final String versionStr, final boolean prerelease,
                               final List<Component> expectedComponents) throws VersionParsingException {
        final CalendarVersion version = CalendarVersionScheme.parse(format, versionStr);
        assertThat(version.isPreRelease()).isEqualTo(prerelease);
        assertThat(version.getComponents()).isEqualTo(expectedComponents);
    }

    @Test
    public void testParseInvalid() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> CalendarVersionScheme.parse("YY-ZZ", "21-17"))
                .withMessage("Unrecognized format specifier 'ZZ'");
        assertThatExceptionOfType(VersionParsingException.class)
                .isThrownBy(() -> CalendarVersionScheme.parse("YY.DD", "21-17"))
                .withMessage("Version '21-17' does not match format 'YY.DD'");
        assertThatExceptionOfType(VersionParsingException.class)
                .isThrownBy(() -> CalendarVersionScheme.parse("YY.DD", "2023.17"))
                .withMessage("Version '2023.17' does not match format 'YY.DD'");
        assertThatExceptionOfType(VersionParsingException.class)
                .isThrownBy(() -> CalendarVersionScheme.parse("YYYY.DD", "23.17"))
                .withMessage("Version '23.17' does not match format 'YYYY.DD'");
        assertThatExceptionOfType(VersionParsingException.class)
                .isThrownBy(() -> CalendarVersionScheme.parse("YYYY.MM.DD", "2023.11"))
                .withMessage("Version '2023.11' does not match format 'YYYY.MM.DD'");
        assertThatExceptionOfType(VersionParsingException.class)
                .isThrownBy(() -> CalendarVersionScheme.parse("YYYY.MM.DD", "2023.11.17."))
                .withMessage("Version '2023.11.17.' does not match format 'YYYY.MM.DD'");
        assertThatIllegalArgumentException()
                .isThrownBy(() -> CalendarVersionScheme.parse("", "2023.11.17"))
                .withMessage("Format must not be empty");
        assertThatExceptionOfType(VersionParsingException.class)
                .isThrownBy(() -> CalendarVersionScheme.parse("YYYY.MM.DD", ""))
                .withMessage("Version '' does not match format 'YYYY.MM.DD'");
        assertThatExceptionOfType(VersionParsingException.class)
                .isThrownBy(() -> CalendarVersionScheme.parse("YYYY.MM.DD", "2023.13.1"))
                .withMessage("Invalid month '13' (1 <= month <= 12)");
    }

    private static Component comp(final ComponentFormat format, final String valueStr) throws VersionParsingException {
        return new Component(format, valueStr);
    }
}
