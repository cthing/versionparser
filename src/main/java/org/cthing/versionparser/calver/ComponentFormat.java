/*
 * Copyright 2023 C Thing Software
 * SPDX-License-Identifier: Apache-2.0
 */
package org.cthing.versionparser.calver;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;


/**
 * Represents the format of a component of a calendar version. These formats correspond to those defined in the
 * <a href="https://calver.org/">Calendar Version</a> specification.
 */
enum ComponentFormat {
    MAJOR(ComponentCategory.MAJOR, "MAJOR", "([\\d]+)"),
    MINOR(ComponentCategory.MINOR, "MINOR", "([\\d]+)"),
    PATCH(ComponentCategory.PATCH, "PATCH", "([\\d]+)"),
    YYYY(ComponentCategory.YEAR, "YYYY", "([\\d]{4})"),
    YY(ComponentCategory.YEAR, "YY", "([\\d]{1,3})"),
    ZERO_Y(ComponentCategory.YEAR, "0Y", "([\\d]{2,3})"),
    MM(ComponentCategory.MONTH, "MM", "([\\d]{1,2})"),
    ZERO_M(ComponentCategory.MONTH, "0M", "([\\d]{2})"),
    WW(ComponentCategory.WEEK, "WW", "([\\d]{1,2})"),
    ZERO_W(ComponentCategory.WEEK, "0W", "([\\d]{2})"),
    DD(ComponentCategory.DAY, "DD", "([\\d]{1,2})"),
    ZERO_D(ComponentCategory.DAY, "0D", "([\\d]{2})"),
    MODIFIER(ComponentCategory.MODIFIER, "modifier", "(?:[\\-._](.+))?");

    private static final Map<String, ComponentFormat> FORMATS = new HashMap<>();

    private final ComponentCategory category;
    private final String format;
    private final String regex;

    static {
        for (final ComponentFormat compFormat : values()) {
            FORMATS.put(compFormat.format, compFormat);
        }
    }

    /**
     * Constructs a format enum instance.
     *
     * @param category Whereas the instance represents the notation for the format, the category indicates what the
     *      instance represents (e.g. a year, a month).
     * @param format Format identifier for the instance (e.g. YYYY, 0D)
     * @param regex Regular expression for parsing a version component in this format
     */
    ComponentFormat(final ComponentCategory category, final String format, final String regex) {
        this.category = category;
        this.format = format;
        this.regex = regex;
    }

    /**
     * Obtains the category for the instance. Whereas the instance represents the notation for the
     * format, the category indicates what the instance represents (e.g. a year, a month).
     *
     * @return Category for the instance.
     */
    ComponentCategory getCategory() {
        return this.category;
    }

    /**
     * Obtains the format identifier for the instance (e.g. YYYY, 0D).
     *
     * @return Format identifier.
     */
    String getFormat() {
        return this.format;
    }

    /**
     * Obtains the regular expression for parsing a version component in this format.
     *
     * @return Regular expression for parsing a version component.
     */
    String getRegex() {
        return this.regex;
    }

    /**
     * Finds the component format instance corresponding to the specified format identifier.
     *
     * @param formatStr Format identifier for the instance (e.g. {@code YYYY}, {@code 0D}). The identifier search
     *      is case-insensitive and any whitespace surrounding the identifier is ignored.
     * @return Component format instance corresponding to the specified format identifier. An empty optional is
     *      returned if the specified format identifier is not valid.
     */
    static Optional<ComponentFormat> from(final String formatStr) {
        final String normalizedFormatStr = formatStr.trim().toUpperCase(Locale.ROOT);
        return Optional.ofNullable(FORMATS.get(normalizedFormatStr));
    }
}
