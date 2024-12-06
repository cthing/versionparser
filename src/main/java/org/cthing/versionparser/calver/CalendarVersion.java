/*
 * Copyright 2023 C Thing Software
 * SPDX-License-Identifier: Apache-2.0
 */
package org.cthing.versionparser.calver;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Pattern;

import org.cthing.versionparser.AbstractVersion;
import org.cthing.versionparser.Version;
import org.jspecify.annotations.Nullable;


/**
 * Represents an artifact version adhering to the <a href="https://calver.org">Calendar Versioning</a>
 * specification. Calendar versions are ordered according to the following precedence:
 * <ol>
 *     <li>{@code YYYY}/{@code YY}/{@code 0Y}</li>
 *     <li>{@code WW}/{@code 0W} <strong>or</strong> {@code MM}/{@code 0M} and {@code DD}/{@code 0D}</li>
 *     <li>{@code MAJOR}</li>
 *     <li>{@code MINOR}</li>
 *     <li>{@code PATCH}</li>
 *     <li>Modifier according to the <a href="https://semver.org/">Semantic Version</a> specification for pre-release
 *         versions (specification item 9)</li>
 * </ol>
 *
 * <p>
 * Except for modifier components, a missing component is always less than a present component of the same
 * category.
 * </p>
 *
 * <p>
 * The version is considered pre-release if a modifier is present and begins with one of the following identifiers
 * (case-insensitive):
 * </p>
 * <ul>
 *     <li>{@code alpha}</li>
 *     <li>{@code beta}</li>
 *     <li>{@code cr}</li>
 *     <li>{@code dev}</li>
 *     <li>{@code milestone}</li>
 *     <li>{@code rc}</li>
 *     <li>{@code snapshot}</li>
 * </ul>
 */
public class CalendarVersion extends AbstractVersion {

    private static final Set<String> PRERELEASE_QUALIFIERS = Set.of("alpha", "beta", "cr", "dev", "milestone",
                                                                    "rc", "snapshot");
    private static final Pattern SEPARATOR_PATTERN = Pattern.compile("\\.");
    private static final String UNDEFINED_MARKER = "uNdeF";
    private static final Pattern HAS_DIGITS_PATTERN = Pattern.compile(".*\\d.*");
    private static final String EXTRACT_DIGITS = "(?<=\\D)(?=\\d)";


    @Nullable
    private Component year;

    @Nullable
    private Component month;

    @Nullable
    private Component day;

    @Nullable
    private Component week;

    @Nullable
    private Component major;

    @Nullable
    private Component minor;

    @Nullable
    private Component patch;

    private final List<Component> components;
    private final boolean preRelease;
    private List<String> modifierComponents;

    /**
     * Constructs a calendar version with the specified format and components.
     *
     * @param originalFormat Calendar version layout
     * @param components Parsed components comprising the version
     */
    CalendarVersion(final String originalFormat, final List<Component> components) {
        super(originalFormat);

        this.components = Collections.unmodifiableList(components);
        this.modifierComponents = new ArrayList<>();

        // CHECKSTYLE:OFF
        for (final Component component : components) {
            switch (component.getCategory()) {
                case MAJOR -> this.major = component;
                case MINOR -> this.minor = component;
                case PATCH -> this.patch = component;
                case YEAR -> this.year = component;
                case MONTH -> this.month = component;
                case WEEK -> this.week = component;
                case DAY -> this.day = component;
                case MODIFIER -> this.modifierComponents = List.of(SEPARATOR_PATTERN.split(component.getValueStr()));
                default -> throw new IllegalStateException("Unexpected value: " + component.getFormat());
            }
        }
        // CHECKSTYLE:ON

        // To be pre-release, a modifier must be present, and it must start with one of the pre-release keywords.
        // The comparison is case-insensitive.
        if (this.modifierComponents.isEmpty()) {
            this.preRelease = false;
        } else {
            final String modifier = this.modifierComponents.get(0).toLowerCase(Locale.ROOT);
            this.preRelease = PRERELEASE_QUALIFIERS.stream().anyMatch(modifier::startsWith);
        }
    }

    /**
     * Obtains the components that comprise the version.
     *
     * @return Components comprising the version.
     */
    public List<Component> getComponents() {
        return this.components;
    }

    @Override
    public boolean isPreRelease() {
        return this.preRelease;
    }

    @Override
    @SuppressWarnings("IfStatementWithIdenticalBranches")
    public int compareTo(final Version obj) {
        if (getClass() != obj.getClass()) {
            throw new IllegalArgumentException("Expected instance of CalendarVersion but received "
                                                       + obj.getClass().getName());
        }

        final CalendarVersion otherVersion = (CalendarVersion)obj;

        int result = compareComponent(this.year, otherVersion.year);
        if (result != 0) {
            return result;
        }

        if (this.week != null) {
            result = compareComponent(this.week, otherVersion.week);
            if (result != 0) {
                return result;
            }
        } else {
            result = compareComponent(this.month, otherVersion.month);
            if (result != 0) {
                return result;
            }

            result = compareComponent(this.day, otherVersion.day);
            if (result != 0) {
                return result;
            }
        }

        result = compareComponent(this.major, otherVersion.major);
        if (result != 0) {
            return result;
        }

        result = compareComponent(this.minor, otherVersion.minor);
        if (result != 0) {
            return result;
        }

        result = compareComponent(this.patch, otherVersion.patch);
        if (result != 0) {
            return result;
        }

        return compareModifiers(this.modifierComponents, otherVersion.modifierComponents);
    }

    /**
     * Compares two components. The method assumes each component is of the same category and is not a
     * modifier component.
     *
     * @param comp1 First component to compare
     * @param comp2 Second component to compare
     * @return If both components have equal values or are both {@code null}, 0 is returned. If the first component
     *      is greater than the second, 1 is returned. If the second component is greater than the first, -1 is
     *      returned.
     */
    private static int compareComponent(@Nullable final Component comp1, @Nullable final Component comp2) {
        if (comp1 == null && comp2 == null) {
            return 0;
        }
        if (comp1 == null) {
            return -1;
        }
        if (comp2 == null) {
            return 1;
        }

        assert comp1.getCategory() == comp2.getCategory();
        assert comp1.getCategory() != ComponentCategory.MODIFIER;
        return Integer.compare(comp1.getValue(), comp2.getValue());
    }

    /**
     * Compares the two lists of modifiers according to the pre-release rule of the Semantic Version specification.
     *
     * @param modifiers1 First version modifiers
     * @param modifiers2 Second version modifiers
     * @return If both collections of modifiers are equal, 0 is returned. If the first is greater than the second,
     *      1 is returned. If the second is greater than the first, -1 is returned. Note that the presence of a
     *      modifier is considered less than the absence of a modifier.
     */
    private static int compareModifiers(final List<String> modifiers1, final List<String> modifiers2) {
        if (!modifiers1.isEmpty() && modifiers2.isEmpty()) {
            return -1;
        }
        if (modifiers1.isEmpty() && !modifiers2.isEmpty()) {
            return 1;
        }
        if (modifiers1.isEmpty()) {
            return 0;
        }

        final int maxElements = Math.max(modifiers1.size(), modifiers2.size());

        int i = 0;
        do {
            final String a = safeGet(modifiers1, i);
            final String b = safeGet(modifiers2, i);

            i++;

            if (UNDEFINED_MARKER.equals(a) && UNDEFINED_MARKER.equals(b)) {
                return 0;
            }
            if (UNDEFINED_MARKER.equals(b)) {
                return 1;
            }
            if (UNDEFINED_MARKER.equals(a)) {
                return -1;
            }
            if (a.equals(b)) {
                continue;
            }

            return compareModifier(a, b);
        } while (maxElements > i);

        return 0;
    }

    /**
     * Compares two modifiers according to the pre-release rule of the Semantic Version specification.
     *
     * @param modifier1 First modifier to compare
     * @param modifier2 Second modifier to compare
     * @return If both modifiers are equal, 0 is returned. If the first modifier is greater than the second, 1
     *      is returned. If the second is greater than the first, -1 is returned.
     */
    private static int compareModifier(final String modifier1, final String modifier2) {
        try {
            final int aInt = Integer.parseInt(modifier1);
            final int bInt = Integer.parseInt(modifier2);
            return Integer.compare(aInt, bInt);
        } catch (final NumberFormatException ignore) {
            //ignore
        }

        if (hasDigits(modifier1, modifier2)) {
            final String[] tokenArr1 = modifier1.split(EXTRACT_DIGITS);
            final String[] tokenArr2 = modifier2.split(EXTRACT_DIGITS);
            if (tokenArr1[0].equals(tokenArr2[0])) {
                final int aInt = Integer.parseInt(tokenArr1[1]);
                final int bInt = Integer.parseInt(tokenArr2[1]);
                return Integer.compare(aInt, bInt);
            }
        }

        final int result = modifier1.compareTo(modifier2);
        if (result > 0) {
            return 1;
        }
        return (result < 0) ? -1 : 0;
    }

    /**
     * Indicates whether the two specified strings both contain digits.
     *
     * @param a First string to test
     * @param b Second string to test
     * @return {@code true} if both strings contain digits.
     */
    private static boolean hasDigits(final String a, final String b) {
        return HAS_DIGITS_PATTERN.matcher(a).matches() && HAS_DIGITS_PATTERN.matcher(b).matches();
    }

    /**
     * Obtains the item at the specified index in the list or returns {@link #UNDEFINED_MARKER} if the index
     * exceeds the bounds of the list.
     *
     * @param list List whose item is desired
     * @param idx Zero-based index of the item to retrieve from the list
     * @return Item at the specified index in the list or {@link #UNDEFINED_MARKER} if the index is greater than
     *      the highest index for the list.
     */
    private static String safeGet(final List<String> list, final int idx) {
        assert idx >= 0;
        return idx < list.size() ? list.get(idx) : UNDEFINED_MARKER;
    }

    @Override
    public boolean equals(final Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        return compareTo((Version)obj) == 0;
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.year, this.month, this.day, this.week, this.major, this.minor, this.patch,
                            this.modifierComponents);
    }
}
