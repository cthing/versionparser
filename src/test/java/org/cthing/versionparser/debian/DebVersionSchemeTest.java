/*
 * Copyright 2026 C Thing Software
 * SPDX-License-Identifier: Apache-2.0
 */
package org.cthing.versionparser.debian;

import org.cthing.versionparser.VersionConstraint;
import org.cthing.versionparser.VersionParsingException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DebVersionSchemeTest {

    @Test
    void testParseVersion() throws VersionParsingException {
        assertThat(DebVersionScheme.parseVersion("1.2.3-1ubuntu1")).isInstanceOf(DebVersion.class)
                                                                   .hasToString("1.2.3-1ubuntu1");
    }

    @Test
    void testBlank() throws VersionParsingException {
        assertThat(DebVersionScheme.parseConstraint(" ").isAny()).isTrue();
    }

    @Test
    void testConflict() throws VersionParsingException {
        assertThat(DebVersionScheme.parseConstraint("<1.0 >2.0").isEmpty()).isTrue();
    }

    @ParameterizedTest
    @ValueSource(strings = {"~=1", "1.2.3-", "==1.0", "^2.0 a"})
    void testInvalid(final String invalidRange) {
        assertThatThrownBy(() -> DebVersionScheme.parseConstraint(invalidRange))
                .isInstanceOf(VersionParsingException.class);
    }

    @Test
    void testExactVersion() throws VersionParsingException {
        final VersionConstraint constraint = DebVersionScheme.parseConstraint("3");

        assertThat(constraint.isSingleVersion()).isTrue();
        assertThat(constraint.getRanges()).satisfiesExactly(range -> {
            assertThat(range.getMinVersion()).isNotNull().hasToString("3");
            assertThat(range.getMaxVersion()).isNotNull();
            assertThat(range.isMinIncluded()).isTrue();
            assertThat(range.isMaxIncluded()).isTrue();
        });

        assertThat(constraint.allows(DebVersion.parse("3"))).isTrue();
        assertThat(constraint.allows(DebVersion.parse("4"))).isFalse();
        assertThat(constraint.allows(DebVersion.parse("2"))).isFalse();
    }

    @Test
    void testEqualTo() throws VersionParsingException {
        final VersionConstraint constraint = DebVersionScheme.parseConstraint("=3");

        assertThat(constraint.isSingleVersion()).isTrue();
        assertThat(constraint.getRanges()).satisfiesExactly(range -> {
            assertThat(range.getMinVersion()).isNotNull().hasToString("3");
            assertThat(range.getMaxVersion()).isNotNull();
            assertThat(range.isMinIncluded()).isTrue();
            assertThat(range.isMaxIncluded()).isTrue();
        });

        assertThat(constraint.allows(DebVersion.parse("3"))).isTrue();
        assertThat(constraint.allows(DebVersion.parse("4"))).isFalse();
        assertThat(constraint.allows(DebVersion.parse("2"))).isFalse();
    }

    @Test
    void testGreaterThanOrEqual() throws VersionParsingException {
        final VersionConstraint constraint = DebVersionScheme.parseConstraint(">=3");

        assertThat(constraint.isSingleVersion()).isFalse();
        assertThat(constraint.getRanges()).satisfiesExactly(range -> {
            assertThat(range.getMinVersion()).isNotNull().hasToString("3");
            assertThat(range.getMaxVersion()).isNull();
            assertThat(range.isMinIncluded()).isTrue();
            assertThat(range.isMaxIncluded()).isFalse();
        });

        assertThat(constraint.allows(DebVersion.parse("4"))).isTrue();
        assertThat(constraint.allows(DebVersion.parse("3"))).isTrue();
        assertThat(constraint.allows(DebVersion.parse("2"))).isFalse();
    }

    @Test
    void testGreaterThan() throws VersionParsingException {
        final VersionConstraint constraint = DebVersionScheme.parseConstraint(">3");

        assertThat(constraint.isSingleVersion()).isFalse();
        assertThat(constraint.getRanges()).satisfiesExactly(range -> {
            assertThat(range.getMinVersion()).isNotNull().hasToString("3");
            assertThat(range.getMaxVersion()).isNull();
            assertThat(range.isMinIncluded()).isFalse();
            assertThat(range.isMaxIncluded()).isFalse();
        });

        assertThat(constraint.allows(DebVersion.parse("10"))).isTrue();
        assertThat(constraint.allows(DebVersion.parse("3"))).isFalse();
        assertThat(constraint.allows(DebVersion.parse("1"))).isFalse();
    }

    @Test
    void testLessThanOrEqual() throws VersionParsingException {
        final VersionConstraint constraint = DebVersionScheme.parseConstraint("<=3");

        assertThat(constraint.isSingleVersion()).isFalse();
        assertThat(constraint.getRanges()).satisfiesExactly(range -> {
            assertThat(range.getMinVersion()).isNull();
            assertThat(range.getMaxVersion()).isNotNull().hasToString("3");
            assertThat(range.isMinIncluded()).isFalse();
            assertThat(range.isMaxIncluded()).isTrue();
        });

        assertThat(constraint.allows(DebVersion.parse("5"))).isFalse();
        assertThat(constraint.allows(DebVersion.parse("3"))).isTrue();
        assertThat(constraint.allows(DebVersion.parse("1"))).isTrue();
    }

    @Test
    void testLessThan() throws VersionParsingException {
        final VersionConstraint constraint = DebVersionScheme.parseConstraint("<3");

        assertThat(constraint.isSingleVersion()).isFalse();
        assertThat(constraint.getRanges()).satisfiesExactly(range -> {
            assertThat(range.getMinVersion()).isNull();
            assertThat(range.getMaxVersion()).isNotNull().hasToString("3");
            assertThat(range.isMinIncluded()).isFalse();
            assertThat(range.isMaxIncluded()).isFalse();
        });

        assertThat(constraint.allows(DebVersion.parse("8"))).isFalse();
        assertThat(constraint.allows(DebVersion.parse("3"))).isFalse();
        assertThat(constraint.allows(DebVersion.parse("2"))).isTrue();
    }

    @Test
    void testSimpleRange() throws VersionParsingException {
        final VersionConstraint constraint = DebVersionScheme.parseConstraint(">3 <=5");

        assertThat(constraint.isSingleVersion()).isFalse();
        assertThat(constraint.getRanges()).satisfiesExactly(range -> {
            assertThat(range.getMinVersion()).isNotNull().hasToString("3");
            assertThat(range.getMaxVersion()).isNotNull().hasToString("5");
            assertThat(range.isMinIncluded()).isFalse();
            assertThat(range.isMaxIncluded()).isTrue();
        });

        assertThat(constraint.allows(DebVersion.parse("7"))).isFalse();
        assertThat(constraint.allows(DebVersion.parse("5"))).isTrue();
        assertThat(constraint.allows(DebVersion.parse("4"))).isTrue();
        assertThat(constraint.allows(DebVersion.parse("3"))).isFalse();
        assertThat(constraint.allows(DebVersion.parse("2"))).isFalse();
    }

    @Test
    void testComplexRange() throws VersionParsingException {
        final VersionConstraint constraint = DebVersionScheme.parseConstraint(">=1.0-1 <2.0-1");

        assertThat(constraint.isSingleVersion()).isFalse();
        assertThat(constraint.getRanges()).satisfiesExactly(range -> {
            assertThat(range.getMinVersion()).isNotNull().hasToString("1.0-1");
            assertThat(range.getMaxVersion()).isNotNull().hasToString("2.0-1");
            assertThat(range.isMinIncluded()).isTrue();
            assertThat(range.isMaxIncluded()).isFalse();
        });

        assertThat(constraint.allows(DebVersion.parse("1.0-1"))).isTrue();
        assertThat(constraint.allows(DebVersion.parse("1.5-1"))).isTrue();
        assertThat(constraint.allows(DebVersion.parse("2.0-1"))).isFalse();
    }
}
