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

import java.util.Objects;

import org.cthing.versionparser.VersionParsingException;


/**
 * Represents a single component in a calendar version. For example, a component represents the month value parsed
 * according to the {@code YYYY.MM.DD} version format.
 */
public class Component {

    private static final int BASE_YEAR = 2000;

    private final ComponentFormat format;
    private final String valueStr;
    private final int value;

    /**
     * Constructs the component from the specified value in the specified format.
     *
     * @param format Format of the component (e.g. YYYY)
     * @param valueStr Value for the component parsed from a calendar version
     * @throws VersionParsingException if the component value is not valid (e.g. month greater than 12)
     */
    Component(final ComponentFormat format, final String valueStr) throws VersionParsingException {
        this.format = format;
        this.valueStr = valueStr;

        if (format == ComponentFormat.MODIFIER) {
            // Modifier strings have no numerical value.
            this.value = 0;
        } else if (format == ComponentFormat.YY || format == ComponentFormat.ZERO_Y) {
            // Relative years are resolved against the base year.
            this.value = BASE_YEAR + Integer.parseInt(valueStr, 10);

            // Validate the year value.
            format.getCategory().validate(this.value);
        } else {
            // All other formats are parsed to numerical values
            this.value = Integer.parseInt(valueStr, 10);

            // Validate the value based on its category (e.g. months must be in the range 1 through 12).
            format.getCategory().validate(this.value);
        }
    }

    /**
     * Obtains the category of the component (e.g. YEAR).
     *
     * @return Component category
     */
    public ComponentCategory getCategory() {
        return this.format.getCategory();
    }

    /**
     * Obtains the format for the component (e.g. YYYY).
     *
     * @return Component format
     */
    ComponentFormat getFormat() {
        return this.format;
    }

    /**
     * Obtains the parsed string value of the component (e.g. "06", "alpha").
     *
     * @return String representation of the component value.
     */
    public String getValueStr() {
        return this.valueStr;
    }

    /**
     * Obtains the numeric value of the component (e.g. 2016, 6).
     *
     * @return Numeric value of the component. Returns zero for a component whose category is
     *      {@link ComponentCategory#MODIFIER}.
     */
    public int getValue() {
        return this.value;
    }

    @Override
    public String toString() {
        return this.valueStr;
    }

    @Override
    public boolean equals(final Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }

        final Component component = (Component)obj;
        if (getCategory() != component.getCategory()) {
            return false;
        }

        // Modifiers are compared lexicographically.
        if (getCategory() == ComponentCategory.MODIFIER) {
            return this.valueStr.equals(component.valueStr);
        }

        // All other components are compared numerically.
        return this.value == component.value;
    }

    @Override
    public int hashCode() {
        if (getCategory() == ComponentCategory.MODIFIER) {
            return Objects.hash(getCategory(), this.valueStr);
        }
        return Objects.hash(getCategory(), this.value);
    }
}
