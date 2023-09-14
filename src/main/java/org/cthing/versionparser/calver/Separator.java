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

import java.util.Arrays;
import java.util.Optional;


/**
 * Indicates the separator used between two calendar version components. The specification allows for a different
 * separator to be used between each component. The separators used by a version must match the separators used in
 * the format. For example, if the format is {@code YYYY.MM.DD-MAJOR}, the version must use dots to separate the
 * year, month and day components, and a dash to separate the major component from the day component.
 */
public enum Separator {
    /** Represent a separator using the '.' character as a delimiter. */
    PERIOD(".", "\\."),

    /** Represent a separator using the '-' character as a delimiter. */
    DASH("-", "\\-"),

    /** Represent a separator using the '_' character as a delimiter. */
    UNDERSCORE("_", "_");

    private final String delimiter;
    private final String regex;

    /**
     * Constructs a separator enum instance.
     *
     * @param delimiter Character representing the separator
     * @param regex Regular expression for parsing the delimiter
     */
    Separator(final String delimiter, final String regex) {
        this.delimiter = delimiter;
        this.regex = regex;
    }

    /**
     * Obtains the character representing the separator.
     *
     * @return Separator character.
     */
    String getDelimiter() {
        return this.delimiter;
    }

    /**
     * Obtains the regular expression for parsing the separator.
     *
     * @return Regular expression for parsing the separator.
     */
    String getRegex() {
        return this.regex;
    }

    /**
     * Finds the separator instance corresponding to the specified delimiter.
     *
     * @param delimiterStr Character representing the separator. The search ignores any whitespace around the
     *      delimiter.
     * @return Separator instance corresponding to the specified delimiter. An empty optional is returned if the
     *      specified delimiter is not valid.
     */
    static Optional<Separator> from(final String delimiterStr) {
        final String normalizedDelimiterStr = delimiterStr.trim();
        return Arrays.stream(values())
                     .filter(separator -> separator.delimiter.equals(normalizedDelimiterStr))
                     .findFirst();
    }
}
