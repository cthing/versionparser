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

package org.cthing.versionparser.gem;

import java.util.List;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import javax.annotation.Nullable;

import org.cthing.versionparser.Version;
import org.cthing.versionparser.VersionConstraint;
import org.cthing.versionparser.VersionParsingException;
import org.cthing.versionparser.VersionRange;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.aggregator.ArgumentsAccessor;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.junit.jupiter.params.provider.Arguments.arguments;


public class GemVersionSchemeTest {

    @Test
    public void testParseVersion() throws VersionParsingException {
        assertThat(GemVersionScheme.parseVersion("1.2.3")).isInstanceOf(GemVersion.class).hasToString("1.2.3");
        assertThat(GemVersionScheme.parseVersion("")).isInstanceOf(GemVersion.class).hasToString("");
    }

    static Stream<Arguments> constraintProvider() {
        return Stream.of(
                arguments("=1", "[1]", "1", "1", true, true),
                arguments(">1", "(1,)", "1", null, false, false),
                arguments(" < 1 ", "(,1)", null, "1", false, false),
                arguments(">=1", "[1,)", "1", null, true, false),
                arguments("<=1", "(,1]", null, "1", false, true),
                arguments("~>1.1", "[1.1,2.ZZZ)", "1.1", "2.ZZZ", true, false)
        );
    }

    @ParameterizedTest
    @MethodSource("constraintProvider")
    public void testParseConstraint(final String constraint, final String constraintRep, @Nullable final String minRep,
                                    @Nullable final String maxRep, final boolean minIncluded,
                                    final boolean maxIncluded)
            throws VersionParsingException {
        final VersionConstraint versionConstraint = GemVersionScheme.parseConstraint(constraint);
        assertThat(versionConstraint).hasToString(constraintRep);
        assertThat(versionConstraint.isWeak()).isFalse();
        final VersionRange versionRange = versionConstraint.getRanges().get(0);
        if (minRep == null) {
            assertThat(versionRange.getMinVersion()).isNull();
        } else {
            assertThat(versionRange.getMinVersion()).hasToString(minRep);
        }
        if (maxRep == null) {
            assertThat(versionRange.getMaxVersion()).isNull();
        } else {
            assertThat(versionRange.getMaxVersion()).hasToString(maxRep);
        }
        assertThat(versionRange.isMinIncluded()).isEqualTo(minIncluded);
        assertThat(versionRange.isMaxIncluded()).isEqualTo(maxIncluded);
    }

    @Test
    public void testParseEmpty() throws VersionParsingException {
        final VersionConstraint versionConstraint = GemVersionScheme.parseConstraint();
        assertThat(versionConstraint).hasToString("[0,)");
        assertThat(versionConstraint.isWeak()).isFalse();
        final VersionRange versionRange = versionConstraint.getRanges().get(0);
        assertThat(versionRange.getMinVersion()).hasToString("0");
        assertThat(versionRange.getMaxVersion()).isNull();
        assertThat(versionRange.isMinIncluded()).isTrue();
        assertThat(versionRange.isMaxIncluded()).isFalse();
    }

    @Test
    public void testParseNotEqual() throws VersionParsingException {
        final VersionConstraint versionConstraint = GemVersionScheme.parseConstraint("!=1");
        assertThat(versionConstraint).hasToString("(,1),(1,)");
        assertThat(versionConstraint.isWeak()).isFalse();
        final List<VersionRange> ranges = versionConstraint.getRanges();
        assertThat(ranges).hasSize(2);
        final VersionRange versionRange1 = ranges.get(0);
        assertThat(versionRange1.getMinVersion()).isNull();
        assertThat(versionRange1.getMaxVersion()).hasToString("1");
        assertThat(versionRange1.isMinIncluded()).isFalse();
        assertThat(versionRange1.isMaxIncluded()).isFalse();
        final VersionRange versionRange2 = ranges.get(1);
        assertThat(versionRange2.getMinVersion()).hasToString("1");
        assertThat(versionRange2.getMaxVersion()).isNull();
        assertThat(versionRange2.isMinIncluded()).isFalse();
        assertThat(versionRange2.isMaxIncluded()).isFalse();
    }

    @Test
    public void testParseBad() {
        assertThatExceptionOfType(VersionParsingException.class)
                .isThrownBy(() -> GemVersionScheme.parseConstraint("! 1"));
        assertThatExceptionOfType(VersionParsingException.class)
                .isThrownBy(() -> GemVersionScheme.parseConstraint("= junk"));
        assertThatExceptionOfType(VersionParsingException.class)
                .isThrownBy(() -> GemVersionScheme.parseConstraint("1..2"));
    }

    static Stream<Arguments> allowsProvider() {
        return Stream.of(
                arguments("1.0",         true,  "1.0"),
                arguments("1.2",         true,  "1.2"),
                arguments("1.1",         false, "1.2"),

                arguments("",            true,  "= 0"),
                arguments("1.0",         true,  "= 1.0"),
                arguments("1.0",         true,  "= 1.0.0"),
                arguments("1.0.0",       true,  "= 1.0"),
                arguments("1.0.0.0",     true,  "= 1.0"),
                arguments("1.2",         true,  "= 1.2"),
                arguments("0.2.33",      true,  "= 0.2.33"),
                arguments("",            false, "= 0.1"),
                arguments("1.0.0.1",     false, "= 1.0"),
                arguments("1.1",         false, "= 1.2"),
                arguments("1.2",         false, "= 1.1"),
                arguments("1.3",         false, "= 1.2"),
                arguments("1.3",         false, "= 1.40"),
                arguments("1.40",        false, "= 1.1"),

                arguments("1.1",         true,  "!= 1.2"),
                arguments("1.3",         true,  "!= 1.2"),
                arguments("10.3.2",      true,  "!= 9.3.4"),
                arguments("1.2",         false, "!= 1.2"),
                arguments("1.2.3",       false, "!= 1.2.3"),
                arguments("1.2.003.0.0", false, "!= 1.02.3"),

                arguments("1.3",         true,  "> 1.2"),
                arguments("0.2.34",      true,  "> 0.2.33"),
                arguments("1.8.2",       true,  "> 1.8.0"),
                arguments("1.112",       true,  "> 1.111"),
                arguments("0.2",         true,  "> 0.0.0"),
                arguments("0.0.0.0.0.2", true,  "> 0.0.0"),
                arguments("0.0.1.0",     true,  "> 0.0.0.1"),
                arguments("10.3.2",      true,  "> 9.3.2"),
                arguments(" 10.3.2 ",    true,  "> 9.3.2"),
                arguments("1.1",         false, "> 1.2"),
                arguments("1.2",         false, "> 1.2"),
                arguments("",            false, "> 0.1"),
                arguments("1.1.1",       false, "> 1.1.1"),

                arguments("1.2",         true,  ">= 1.2"),
                arguments("1.3",         true,  ">= 1.2"),
                arguments(" 9.3.2",      true,  ">= 9.3.2"),
                arguments("9.3.2 ",      true,  ">= 9.3.2"),
                arguments("1.4",         true,  ">= 1.4", "<= 1.6", "!= 1.5"),
                arguments("1.6",         true,  ">= 1.4", "<= 1.6", "!= 1.5"),
                arguments("1.4.5",       true,  ">= 1.4.4", "< 1.5"),
                arguments("1.5.0.rc1",   true,  ">= 1.4.4", "< 1.5"),
                arguments("1.4.5",       true,  ">= 1.4.4", "< 1.5.a"),
                arguments("1.1",         false, ">= 1.2"),
                arguments("1.3",         false, ">= 1.4", "<= 1.6", "!= 1.5"),
                arguments("1.5",         false, ">= 1.4", "<= 1.6", "!= 1.5"),
                arguments("1.7",         false, ">= 1.4", "<= 1.6", "!= 1.5"),
                arguments("2.0",         false, ">= 1.4", "<= 1.6", "!= 1.5"),
                arguments("1.5.0",       false, ">= 1.4.4", "< 1.5"),
                arguments("1.5.0.rc1",   false, ">= 1.4.4", "< 1.5.a"),
                arguments("1.5.0",       false, ">= 1.4.4", "< 1.5.a"),
                arguments("9.3.1",       false, ">= 9.3.2"),

                arguments("1.1",         true,  "< 1.2"),
                arguments("",            true,  "< 0.1"),
                arguments("  ",          true,  "< 0.1 "),
                arguments("",            true,  " <  0.1"),
                arguments("3.1",         true,  "< 3.2.rc1"),
                arguments("3.0.rc2",     true,  "< 3.0"),
                arguments("3.0.rc2",     true,  "< 3.0.0"),
                arguments("3.0.rc2",     true,  "< 3.0.1"),
                arguments("1.2",         false, "< 1.2"),
                arguments("1.3",         false, "< 1.2"),
                arguments("4.5.6",       false, "< 1.2.3"),

                arguments("1.1",         true,  "<= 1.2"),
                arguments("1.2",         true,  "<= 1.2"),
                arguments("1.3",         false, "<= 1.2"),
                arguments("9.3.3",       false, "<= 9.3.2"),
                arguments("9.3.03",      false, "<= 9.3.2"),

                arguments("1.2",         true,  "> 1.1", "< 1.3"),
                arguments("  ",          true,  "> 0.a "),
                arguments("",            true,  " >  0.a"),
                arguments("3.2.0",       true,  "> 3.2.0.rc1"),
                arguments("3.2.0.rc2",   true,  "> 3.2.0.rc1"),
                arguments("3.0.rc2",     true,  "> 0"),
                arguments("1.1",         false, "> 1.1", "< 1.3"),
                arguments("1.3",         false, "> 1.1", "< 1.3"),
                arguments("1.0",         false, "> 1.1"),

                arguments("1.2",         true,  "~> 1.2"),
                arguments("1.3",         true,  "~> 1.2"),
                arguments("0.0.2",       true,  "~> 0.0.1"),
                arguments("0.0.1",       true,  "~> 0.0.1"),
                arguments("5.0.0.rc2",   true,  "~> 5.a"),
                arguments("5.0.0",       true,  "~> 5.a"),
                arguments("5.0.0",       true,  "~> 5.x"),
                arguments("1.4",         true,  "~> 1.4"),
                arguments("1.5",         true,  "~> 1.4"),
                arguments("1.4.4",       true,  "~> 1.4.4"),
                arguments("1.4.5",       true,  "~> 1.4.4"),
                arguments("1.0",         true,  "~> 1"),
                arguments("1.1",         true,  "~> 1"),
                arguments("1.4",         true,  "~> 1.4"),
                arguments("1.4.0",       true,  "~> 1.4"),
                arguments("1.5",         true,  "~> 1.4"),
                arguments("1.4.4",       true,  "~> 1.4.4"),
                arguments("1.4.5",       true,  "~> 1.4.4"),
                arguments("1.1",         false, "~> 1.2"),
                arguments("0.1.1",       false, "~> 0.0.1"),
                arguments("5.0.0.rc2",   false, "~> 5.x"),
                arguments("1.3",         false, "~> 1.4"),
                arguments("2.0",         false, "~> 1.4"),
                arguments("1.3",         false, "~> 1.4.4"),
                arguments("1.4",         false, "~> 1.4.4"),
                arguments("1.5",         false, "~> 1.4.4"),
                arguments("2.0",         false, "~> 1.4.4"),
                arguments("1.1.pre",     false, "~> 1.0.0"),
                arguments("1.1.beta.1",  false, "~> 1.0.0"),
                arguments("1.1.pre",     false, "~> 1.1"),
                arguments("2.0.a",       false, "~> 1.0"),
                arguments("2.0.a",       false, "~> 2.0"),
                arguments("2.0.beta1.2", false, "~> 2.0"),
                arguments("0.9",         false, "~> 1"),
                arguments("2.0",         false, "~> 1"),
                arguments("1.3",         false, "~> 1.4"),
                arguments("2.0",         false, "~> 1.4"),
                arguments("1.3",         false, "~> 1.4.4"),
                arguments("1.4",         false, "~> 1.4.4"),
                arguments("1.5",         false, "~> 1.4.4"),
                arguments("2.0",         false, "~> 1.4.4")
        );
    }

    @ParameterizedTest
    @MethodSource("allowsProvider")
    public void testAllows(final ArgumentsAccessor accessor)
            throws VersionParsingException {
        final boolean allows = accessor.getBoolean(1);
        final VersionConstraint versionConstraint =
                GemVersionScheme.parseConstraint(IntStream.range(2, accessor.size())
                                                          .mapToObj(accessor::getString)
                                                          .toArray(String[]::new));
        final Version ver = GemVersionScheme.parseVersion(accessor.getString(0));
        assertThat(versionConstraint.allows(ver)).isEqualTo(allows);
        final VersionConstraint verConstraint = new VersionConstraint(ver);
        assertThat(versionConstraint.allowsAll(verConstraint)).isEqualTo(allows);
        assertThat(versionConstraint.allowsAny(verConstraint)).isEqualTo(allows);
        assertThat(versionConstraint.intersect(verConstraint).isEmpty()).isEqualTo(!allows);
    }
}
