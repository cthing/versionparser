/*
 * Copyright 2023 C Thing Software
 * SPDX-License-Identifier: Apache-2.0
 */
package org.cthing.versionparser.calver;

import java.util.function.Predicate;

import org.cthing.versionparser.VersionParsingException;


/**
 * Indicates the category for a calendar version component. Whereas the calendar version specification provides
 * identifiers for specifying version components in various ways (e.g. {@code YYYY}, {@code YY}, {@code 0Y}), they
 * can all be grouped into the categories represented by this enum.
 */
public enum ComponentCategory {
    /** Represents a year. Values in this category must be greater than or equal to 1900. */
    YEAR(integer -> integer < 1900, "Invalid year '%d' (year >= 1900)"),

    /** Represents a month of the year. Values in this category must be between 1 and 12 inclusive. */
    MONTH(integer -> integer < 1 || integer > 12, "Invalid month '%d' (1 <= month <= 12)"),

    /** Represents a day of the month. Values in this category must be between 1 and 31 inclusive. */
    DAY(integer -> integer < 1 || integer > 31, "Invalid day '%d' (1 <= day <= 31)"),

    /** Represents a week of the year. Values in this category must be between 1 and 52 inclusive. */
    WEEK(integer -> integer < 1 || integer > 52, "Invalid week '%d' (1 <= week <= 52)"),

    /** Represents a major version number per the Semantic Version specification. Values must be positive. */
    MAJOR(integer -> integer < 0, "Invalid major version '%d' (major >= 0)"),

    /** Represents a minor version number per the Semantic Version specification. Values must be positive. */
    MINOR(integer -> integer < 0, "Invalid minor version '%d' (minor >= 0)"),

    /** Represents a patch version number per the Semantic Version specification. Values must be positive. */
    PATCH(integer -> integer < 0, "Invalid patch version '%d' (patch >= 0)"),

    /** Represents an optional modifier on the version (e.g. alpha). */
    MODIFIER(integer -> false, "");

    private final Predicate<Integer> validator;
    private final String validationMessage;

    /**
     * Constructs a calendar version component category.
     *
     * @param validator Predicate to validate the category's value
     * @param message Message to emit if validation fails
     */
    ComponentCategory(final Predicate<Integer> validator, final String message) {
        this.validator = validator;
        this.validationMessage = message;
    }

    /**
     * Performs validation on the specified value based on the category's validator predicate.
     *
     * @param value Value to validate
     * @throws VersionParsingException if the specified value violates the category's validator predicate.
     */
    void validate(final int value) throws VersionParsingException {
        if (this.validator.test(value)) {
            throw new VersionParsingException(String.format(this.validationMessage, value));
        }
    }
}
