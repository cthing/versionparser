/*
 * Copyright 2023 C Thing Software
 * SPDX-License-Identifier: Apache-2.0
 */

package org.cthing.versionparser.npm;

import org.cthing.versionparser.VersionParsingException;
import org.cthing.versionparser.semver.SemanticVersion;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.cthing.versionparser.npm.ConstraintComponent.Operator.EQ;
import static org.cthing.versionparser.npm.ConstraintComponent.Operator.GT;
import static org.cthing.versionparser.npm.ConstraintComponent.Operator.GTE;
import static org.cthing.versionparser.npm.ConstraintComponent.Operator.LT;
import static org.cthing.versionparser.npm.ConstraintComponent.Operator.LTE;


public class ConstraintComponentTest {

    @Test
    public void testOperators() {
        assertThat(EQ.asString()).isEqualTo("=");
        assertThat(LT.asString()).isEqualTo("<");
        assertThat(LTE.asString()).isEqualTo("<=");
        assertThat(GT.asString()).isEqualTo(">");
        assertThat(GTE.asString()).isEqualTo(">=");
    }

    @Test
    public void testOperatorLookup() {
        assertThat(ConstraintComponent.Operator.value("=")).isEqualTo(EQ);
        assertThat(ConstraintComponent.Operator.value("")).isEqualTo(EQ);
        assertThat(ConstraintComponent.Operator.value("<")).isEqualTo(LT);
        assertThat(ConstraintComponent.Operator.value("<=")).isEqualTo(LTE);
        assertThat(ConstraintComponent.Operator.value(">")).isEqualTo(GT);
        assertThat(ConstraintComponent.Operator.value(">=")).isEqualTo(GTE);
        assertThatIllegalArgumentException().isThrownBy(() -> ConstraintComponent.Operator.value("abc"));
    }

    @Test
    public void testConstruction() throws VersionParsingException {
        final SemanticVersion version = SemanticVersion.parse("1.2.3");

        ConstraintComponent component = new ConstraintComponent(version, LT);
        assertThat(component.getVersion()).isEqualTo(version);
        assertThat(component.getOperator()).isEqualTo(LT);
        assertThat(component).hasToString("<1.2.3");

        component = new ConstraintComponent("1.2.3", ">=");
        assertThat(component.getVersion()).isEqualTo(version);
        assertThat(component.getOperator()).isEqualTo(GTE);
        assertThat(component).hasToString(">=1.2.3");
    }

    @Test
    @SuppressWarnings({ "EqualsWithItself", "AssertBetweenInconvertibleTypes" })
    public void testEquality() throws VersionParsingException {
        final ConstraintComponent comp1 = new ConstraintComponent("1.2.3", "=");
        final ConstraintComponent comp2 = new ConstraintComponent("1.2.3", "=");
        final ConstraintComponent comp3 = new ConstraintComponent("1.2.3", "<=");
        final ConstraintComponent comp4 = new ConstraintComponent("1.0.0", "=");

        assertThat(comp1).isEqualTo(comp1);

        assertThat(comp1).isEqualTo(comp2);
        assertThat(comp1).hasSameHashCodeAs(comp2);

        assertThat(comp1).isNotEqualTo(comp3);
        assertThat(comp1).doesNotHaveSameHashCodeAs(comp3);

        assertThat(comp1).isNotEqualTo(comp4);
        assertThat(comp1).doesNotHaveSameHashCodeAs(comp4);

        assertThat(comp1).isNotEqualTo(null);
        assertThat(comp1).isNotEqualTo("");
    }
}
