/*
 * Copyright 2023 C Thing Software
 * SPDX-License-Identifier: Apache-2.0
 */
package org.cthing.versionparser.calver;

import java.util.HashMap;
import java.util.Map;
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

    private static final Map<String, Separator> DELIMITERS = new HashMap<>();

    private final String delimiter;
    private final String regex;

    static {
        for (final Separator separator : values()) {
            DELIMITERS.put(separator.delimiter, separator);
        }
    }

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
        return Optional.ofNullable(DELIMITERS.get(delimiterStr.trim()));
    }
}
