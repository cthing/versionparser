/*
 * Copyright 2023 C Thing Software
 * SPDX-License-Identifier: Apache-2.0
 */
package org.cthing.versionparser.calver;

import org.cthing.versionparser.VersionParsingException;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;


class ComponentTest {

    @Test
    void testConstruction() throws VersionParsingException {
        final Component monthComponent = new Component(ComponentFormat.MM, "10");
        assertThat(monthComponent.getCategory()).isEqualTo(ComponentCategory.MONTH);
        assertThat(monthComponent.getFormat()).isEqualTo(ComponentFormat.MM);
        assertThat(monthComponent.getValueStr()).isEqualTo("10");
        assertThat(monthComponent.getValue()).isEqualTo(10);
        assertThat(monthComponent).hasToString("10");

        final Component year = new Component(ComponentFormat.YY, "10");
        assertThat(year.getCategory()).isEqualTo(ComponentCategory.YEAR);
        assertThat(year.getFormat()).isEqualTo(ComponentFormat.YY);
        assertThat(year.getValueStr()).isEqualTo("10");
        assertThat(year.getValue()).isEqualTo(2010);
        assertThat(year).hasToString("10");

        final Component zeroYearComponent = new Component(ComponentFormat.ZERO_Y, "20");
        assertThat(zeroYearComponent.getCategory()).isEqualTo(ComponentCategory.YEAR);
        assertThat(zeroYearComponent.getFormat()).isEqualTo(ComponentFormat.ZERO_Y);
        assertThat(zeroYearComponent.getValueStr()).isEqualTo("20");
        assertThat(zeroYearComponent.getValue()).isEqualTo(2020);
        assertThat(zeroYearComponent).hasToString("20");

        final Component modifierComponent = new Component(ComponentFormat.MODIFIER, "hello");
        assertThat(modifierComponent.getCategory()).isEqualTo(ComponentCategory.MODIFIER);
        assertThat(modifierComponent.getFormat()).isEqualTo(ComponentFormat.MODIFIER);
        assertThat(modifierComponent.getValueStr()).isEqualTo("hello");
        assertThat(modifierComponent.getValue()).isEqualTo(0);
        assertThat(modifierComponent).hasToString("hello");
    }

    @Test
    @SuppressWarnings("EqualsWithItself")
    void testEquality() throws VersionParsingException {
        final Component component1 = new Component(ComponentFormat.YYYY, "2023");
        final Component component2 = new Component(ComponentFormat.YYYY, "2023");
        final Component component3 = new Component(ComponentFormat.YY, "23");
        final Component component4 = new Component(ComponentFormat.MM, "12");
        final Component component5 = new Component(ComponentFormat.ZERO_M, "12");
        final Component component6 = new Component(ComponentFormat.ZERO_M, "10");
        final Component component7 = new Component(ComponentFormat.MODIFIER, "hello");
        final Component component8 = new Component(ComponentFormat.MODIFIER, "hello");
        final Component component9 = new Component(ComponentFormat.MODIFIER, "world");

        assertThat(component1).isEqualTo(component1);
        assertThat(component1).isEqualTo(component2);
        assertThat(component1).hasSameHashCodeAs(component2);
        assertThat(component1).isEqualTo(component3);
        assertThat(component1).hasSameHashCodeAs(component3);
        assertThat(component1).isNotEqualTo(component4);
        assertThat(component1).isNotEqualTo(null);

        assertThat(component4).isEqualTo(component5);
        assertThat(component4).hasSameHashCodeAs(component5);
        assertThat(component4).isNotEqualTo(component6);

        assertThat(component7).isEqualTo(component8);
        assertThat(component7).hasSameHashCodeAs(component8);
        assertThat(component7).isNotEqualTo(component9);
    }
}
