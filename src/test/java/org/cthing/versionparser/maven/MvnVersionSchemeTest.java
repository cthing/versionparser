/*
 * Copyright 2023 C Thing Software
 * SPDX-License-Identifier: Apache-2.0
 */

package org.cthing.versionparser.maven;

import java.util.List;
import java.util.stream.Stream;

import javax.annotation.Nullable;

import org.cthing.versionparser.VersionConstraint;
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


public class MvnVersionSchemeTest {

    @Test
    public void testParseVersion() {
        assertThat(MvnVersionScheme.parseVersion("1.2.3")).isInstanceOf(MvnVersion.class).hasToString("1.2.3");
        assertThat(MvnVersionScheme.parseVersion("")).isInstanceOf(MvnVersion.class).hasToString("");
    }

    static Stream<Arguments> versionProvider() {
        return Stream.of(
            arguments("1.2.3", "[1.2.3,)", "1.2.3"),
            arguments("1", "[1,)", "1"),
            arguments("1.2-SNAPSHOT", "[1.2-SNAPSHOT,)", "1.2-SNAPSHOT")
        );
    }

    @ParameterizedTest
    @MethodSource("versionProvider")
    public void testParseConstraintVersion(final String version, final String rangeRep,
                                           @Nullable final String versionRep) throws VersionParsingException {
        final VersionConstraint constraint = MvnVersionScheme.parseConstraint(version);
        assertThat(constraint).hasToString(rangeRep);
        assertThat(constraint.isWeak()).isTrue();
        final VersionRange versionRange = constraint.getRanges().get(0);
        assertThat(versionRange.getMinVersion()).hasToString(versionRep);
        assertThat(versionRange.getMaxVersion()).isNull();
        assertThat(versionRange.isMinIncluded()).isTrue();
        assertThat(versionRange.isMaxIncluded()).isFalse();
    }

    static Stream<Arguments> rangeProvider() {
        return Stream.of(
                arguments("[1.2.3]", "[1.2.3]", "1.2.3", "1.2.3", true, true),
                arguments("[1.2.3,)", "[1.2.3,)", "1.2.3", null, true, false),
                arguments("(,1.2.3]", "(,1.2.3]", null, "1.2.3", false, true),
                arguments("(1.0.0,1.2.3]", "(1.0.0,1.2.3]", "1.0.0", "1.2.3", false, true),
                arguments("(,)", "(,)", null, null, false, false)
        );
    }

    @ParameterizedTest
    @MethodSource("rangeProvider")
    public void testParseConstraintRange(final String range, final String rangeRep, @Nullable final String minRep,
                                         @Nullable final String maxRep, final boolean minIncluded,
                                         final boolean maxIncluded) throws VersionParsingException {
        final VersionConstraint constraint = MvnVersionScheme.parseConstraint(range);
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
        final VersionConstraint constraint = MvnVersionScheme.parseConstraint("[1.2.3, 1.3.0  ], [2.0.0,)");
        assertThat(constraint).hasToString("[1.2.3,1.3.0],[2.0.0,)");
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
        assertThat(vr12.getMaxVersion()).isNull();
        assertThat(vr12.isMinIncluded()).isTrue();
        assertThat(vr12.isMaxIncluded()).isFalse();
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "",
            "[)",
            "(]",
            "()",
            "[,",
            "(,",
            "[",
            "(",
            "[1.2.3",
            "[1.2.3,",
            "(1.2.3",
            "(1.2.3,",
            "(1.2.3)",
            "[1.2.3,2.0.0,3.0.0)",
            "[2.0.0,1.2.3]"
    })
    public void testParseConstraintBad(final String constraint) {
        assertThatExceptionOfType(VersionParsingException.class)
                .isThrownBy(() -> MvnVersionScheme.parseConstraint(constraint));
    }
}
