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

import org.cthing.versionparser.VersionParsingException;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;


public class ComponentTest {

    @Test
    public void testConstruction() throws VersionParsingException {
        Component component = new Component(ComponentFormat.MM, "10");
        assertThat(component.getCategory()).isEqualTo(ComponentCategory.MONTH);
        assertThat(component.getFormat()).isEqualTo(ComponentFormat.MM);
        assertThat(component.getValueStr()).isEqualTo("10");
        assertThat(component.getValue()).isEqualTo(10);
        assertThat(component).hasToString("10");

        component = new Component(ComponentFormat.YY, "10");
        assertThat(component.getCategory()).isEqualTo(ComponentCategory.YEAR);
        assertThat(component.getFormat()).isEqualTo(ComponentFormat.YY);
        assertThat(component.getValueStr()).isEqualTo("10");
        assertThat(component.getValue()).isEqualTo(2010);
        assertThat(component).hasToString("10");

        component = new Component(ComponentFormat.ZERO_Y, "20");
        assertThat(component.getCategory()).isEqualTo(ComponentCategory.YEAR);
        assertThat(component.getFormat()).isEqualTo(ComponentFormat.ZERO_Y);
        assertThat(component.getValueStr()).isEqualTo("20");
        assertThat(component.getValue()).isEqualTo(2020);
        assertThat(component).hasToString("20");

        component = new Component(ComponentFormat.MODIFIER, "hello");
        assertThat(component.getCategory()).isEqualTo(ComponentCategory.MODIFIER);
        assertThat(component.getFormat()).isEqualTo(ComponentFormat.MODIFIER);
        assertThat(component.getValueStr()).isEqualTo("hello");
        assertThat(component.getValue()).isEqualTo(0);
        assertThat(component).hasToString("hello");
    }

    @Test
    @SuppressWarnings("EqualsWithItself")
    public void testEquality() throws VersionParsingException {
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
