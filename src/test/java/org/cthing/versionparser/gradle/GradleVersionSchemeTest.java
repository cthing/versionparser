/*
 * Copyright 2023 C Thing Software
 * SPDX-License-Identifier: Apache-2.0
 */

package org.cthing.versionparser.gradle;

import java.util.stream.Stream;

import org.assertj.core.api.Assertions;
import org.cthing.versionparser.Version;
import org.cthing.versionparser.VersionConstraint;
import org.cthing.versionparser.VersionParsingException;
import org.cthing.versionparser.VersionRange;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.junit.jupiter.params.provider.Arguments.arguments;


public class GradleVersionSchemeTest {

    @Test
    public void testParseVersion() {
        assertThat(GradleVersionScheme.parseVersion("1.2.3")).isInstanceOf(GradleVersion.class).hasToString("1.2.3");
        assertThat(GradleVersionScheme.parseVersion("")).isInstanceOf(GradleVersion.class).hasToString("");
    }

    static Stream<Arguments> versionProvider() {
        return Stream.of(
                arguments("1.2.3", "[1.2.3,)", "1.2.3"),
                arguments("1", "[1,)", "1"),
                arguments("1.2-SNAPSHOT", "[1.2-SNAPSHOT,)", "1.2-SNAPSHOT"),
                arguments("1.2+SNAPSHOT", "[1.2+SNAPSHOT,)", "1.2+SNAPSHOT")
        );
    }

    @ParameterizedTest
    @MethodSource("versionProvider")
    public void testParseConstraintVersion(final String version, final String rangeRep,
                                           @Nullable final String versionRep) throws VersionParsingException {
        final VersionConstraint constraint = GradleVersionScheme.parseConstraint(version);
        assertThat(constraint).hasToString(rangeRep);
        assertThat(constraint.isWeak()).isFalse();
        final VersionRange versionRange = constraint.getRanges().get(0);
        Assertions.<@Nullable Version>assertThat(versionRange.getMinVersion()).hasToString(versionRep);
        Assertions.<@Nullable Version>assertThat(versionRange.getMaxVersion()).isNull();
        assertThat(versionRange.isMinIncluded()).isTrue();
        assertThat(versionRange.isMaxIncluded()).isFalse();
    }

    @Test
    public void testParseConstaintDynamicVersion() throws VersionParsingException {
        assertThat(GradleVersionScheme.parseConstraint("1.0.10.+")).hasToString("[1.0.10,1.0.11)");
        assertThat(GradleVersionScheme.parseConstraint("1.a.10.+")).hasToString("[1.a.10,1.a.11)");
        assertThat(GradleVersionScheme.parseConstraint("1.0.+")).hasToString("[1.0,1.1)");
        assertThat(GradleVersionScheme.parseConstraint("1.+")).hasToString("[1,2)");
        assertThat(GradleVersionScheme.parseConstraint("+")).hasToString("(,)");
    }

    static Stream<Arguments> rangeProvider() {
        return Stream.of(
                arguments("[1.2.3]", "[1.2.3]", "1.2.3", "1.2.3", true, true),
                arguments("[1.2.3,)", "[1.2.3,)", "1.2.3", null, true, false),
                arguments("(,1.2.3]", "(,1.2.3]", null, "1.2.3", false, true),
                arguments("(,1.2.3[", "(,1.2.3)", null, "1.2.3", false, false),
                arguments("(1.0.0,1.2.3]", "(1.0.0,1.2.3]", "1.0.0", "1.2.3", false, true),
                arguments("]1.0.0,1.2.3]", "(1.0.0,1.2.3]", "1.0.0", "1.2.3", false, true),
                arguments("(1.0.0,1.2.3)", "(1.0.0,1.2.3)", "1.0.0", "1.2.3", false, false),
                arguments("(,)", "(,)", null, null, false, false)
        );
    }

    @ParameterizedTest
    @MethodSource("rangeProvider")
    public void testParseConstraintRange(final String range, final String rangeRep, @Nullable final String minRep,
                                         @Nullable final String maxRep, final boolean minIncluded,
                                         final boolean maxIncluded) throws VersionParsingException {
        final VersionConstraint constraint = GradleVersionScheme.parseConstraint(range);
        assertThat(constraint).hasToString(rangeRep);
        assertThat(constraint.isWeak()).isFalse();
        final VersionRange versionRange = constraint.getRanges().get(0);
        if (minRep == null) {
            Assertions.<@Nullable Version>assertThat(versionRange.getMinVersion()).isNull();
        } else {
            Assertions.<@Nullable Version>assertThat(versionRange.getMinVersion()).hasToString(minRep);
        }
        if (maxRep == null) {
            Assertions.<@Nullable Version>assertThat(versionRange.getMaxVersion()).isNull();
        } else {
            Assertions.<@Nullable Version>assertThat(versionRange.getMaxVersion()).hasToString(maxRep);
        }
        assertThat(versionRange.isMinIncluded()).isEqualTo(minIncluded);
        assertThat(versionRange.isMaxIncluded()).isEqualTo(maxIncluded);
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "1.a.+",
            "a.+"
    })
    public void testParseConstraintBad(final String constraint) {
        assertThatExceptionOfType(VersionParsingException.class)
                .isThrownBy(() -> GradleVersionScheme.parseConstraint(constraint));
    }
}
