/*
 * Copyright 2023 C Thing Software
 * SPDX-License-Identifier: Apache-2.0
 */

package org.cthing.versionparser.npm;

import java.util.List;
import java.util.stream.Stream;

import javax.annotation.Nullable;

import org.cthing.versionparser.VersionConstraint;
import org.cthing.versionparser.VersionParsingException;
import org.cthing.versionparser.VersionRange;
import org.cthing.versionparser.semver.SemanticVersion;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.junit.jupiter.params.provider.Arguments.arguments;


public class NpmVersionSchemeTest {

    @Test
    public void testParseVersion() throws VersionParsingException {
        assertThat(NpmVersionScheme.parseVersion("1.2.3")).isInstanceOf(SemanticVersion.class).hasToString("1.2.3");
        assertThat(NpmVersionScheme.parseVersion("1.2.3-abc1+20230405")).isInstanceOf(SemanticVersion.class)
                                                                   .hasToString("1.2.3-abc1+20230405");
    }

    static Stream<Arguments> versionProvider() {
        return Stream.of(
                arguments("1.2.3", "[1.2.3]", "1.2.3"),
                arguments("=1.2.3", "[1.2.3]", "1.2.3"),
                arguments("1.2.3-abc.1", "[1.2.3-abc.1]", "1.2.3-abc.1")
        );
    }

    @ParameterizedTest
    @MethodSource("versionProvider")
    public void testParseConstraintVersion(final String version, final String rangeRep, final String versionRep)
            throws VersionParsingException {
        final VersionConstraint constraint = NpmVersionScheme.parseConstraint(version);
        assertThat(constraint).hasToString(rangeRep);
        assertThat(constraint.isWeak()).isFalse();
        final VersionRange versionRange = constraint.getRanges().get(0);
        assertThat(versionRange.getMinVersion()).hasToString(versionRep);
        assertThat(versionRange.getMaxVersion()).hasToString(versionRep);
        assertThat(versionRange.isMinIncluded()).isTrue();
        assertThat(versionRange.isMaxIncluded()).isTrue();
    }

    static Stream<Arguments> rangeProvider() {
        return Stream.of(
                arguments("", "[0.0.0,)", "0.0.0", null, true, false),
                arguments("<", "(,)", null, null, false, false),
                arguments(">", "(,)", null, null, false, false),
                arguments("||", "(,)", null, null, false, false),
                arguments(">=1.2.3", "[1.2.3,)", "1.2.3", null, true, false),
                arguments(">1.2.3", "(1.2.3,)", "1.2.3", null, false, false),
                arguments("<=2.0.0", "(,2.0.0]", null, "2.0.0", false, true),
                arguments("<2.0.0", "(,2.0.0)", null, "2.0.0", false, false),
                arguments("<=2.0.0", "(,2.0.0]", null, "2.0.0", false, true),
                arguments(">=1.2.3 <2.0.0", "[1.2.3,2.0.0)", "1.2.3", "2.0.0", true, false),
                arguments("<2.0.0 >=1.2.3 ", "[1.2.3,2.0.0)", "1.2.3", "2.0.0", true, false),
                arguments(">1.0.0 <=1.5.0 ", "(1.0.0,1.5.0]", "1.0.0", "1.5.0", false, true),
                arguments("<2.0.0 >1.2.3 ", "(1.2.3,2.0.0)", "1.2.3", "2.0.0", false, false),
                arguments("<=2.0.0 >1.2.3 ", "(1.2.3,2.0.0]", "1.2.3", "2.0.0", false, true)
        );
    }

    @ParameterizedTest
    @MethodSource("rangeProvider")
    public void testParseConstraintRange(final String range, final String rangeRep, @Nullable final String minRep,
                                         @Nullable final String maxRep, final boolean minIncluded,
                                         final boolean maxIncluded) throws VersionParsingException {
        final VersionConstraint constraint = NpmVersionScheme.parseConstraint(range);
        assertThat(constraint).hasToString(rangeRep);
        assertThat(constraint.isWeak()).isFalse();
        final VersionRange versionRange = constraint.getRanges().get(0);
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
    public void testParseConstraintUnion() throws VersionParsingException {
        final VersionConstraint constraint = NpmVersionScheme.parseConstraint(">=1.2.3 <=1.3.0 || =2.0.0");
        assertThat(constraint).hasToString("[1.2.3,1.3.0],[2.0.0]");
        assertThat(constraint.isWeak()).isFalse();
        final List<VersionRange> ranges1 = constraint.getRanges();
        assertThat(ranges1).hasSize(2);
        final VersionRange vr11 = ranges1.get(0);
        assertThat(vr11.getMinVersion()).hasToString("1.2.3");
        assertThat(vr11.getMaxVersion()).hasToString("1.3.0");
        assertThat(vr11.isMinIncluded()).isTrue();
        assertThat(vr11.isMaxIncluded()).isTrue();
        final VersionRange vr12 = ranges1.get(1);
        assertThat(vr12.getMinVersion()).hasToString("2.0.0");
        assertThat(vr12.getMaxVersion()).hasToString("2.0.0");
        assertThat(vr12.isMinIncluded()).isTrue();
        assertThat(vr12.isMaxIncluded()).isTrue();
    }

    @Test
    public void testParseConstraintBad() {
        assertThatExceptionOfType(VersionParsingException.class)
                .isThrownBy(() -> NpmVersionScheme.parseConstraint(">2.0.0 =1.2.3"));
        assertThatExceptionOfType(VersionParsingException.class)
                .isThrownBy(() -> NpmVersionScheme.parseConstraint("=2.0.0 <1.2.3"));
        assertThatExceptionOfType(VersionParsingException.class)
                .isThrownBy(() -> NpmVersionScheme.parseConstraint("=2.0.0 =1.2.3"));
    }
}
