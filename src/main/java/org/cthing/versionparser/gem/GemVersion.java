/*
 * Copyright 2023 C Thing Software
 * SPDX-License-Identifier: Apache-2.0
 *
 * This file is derived from https://github.com/rubygems/rubygems/blob/master/lib/rubygems/version.rb
 * which is covered by the following copyright and permission notices:
 *
 *  Copyright (c) Chad Fowler, Rich Kilmer, Jim Weirich and others.
 *  Portions copyright (c) Engine Yard and Andre Arko
 *
 *  Permission is hereby granted, free of charge, to any person obtaining
 *  a copy of this software and associated documentation files (the
 *  'Software'), to deal in the Software without restriction, including
 *  without limitation the rights to use, copy, modify, merge, publish,
 *  distribute, sublicense, and/or sell copies of the Software, and to
 *  permit persons to whom the Software is furnished to do so, subject to
 *  the following conditions:
 *
 *  The above copyright notice and this permission notice shall be
 *  included in all copies or substantial portions of the Software.
 *
 *  THE SOFTWARE IS PROVIDED 'AS IS', WITHOUT WARRANTY OF ANY KIND,
 *  EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
 *  MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT.
 *  IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY
 *  CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT,
 *  TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE
 *  SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package org.cthing.versionparser.gem;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.cthing.versionparser.AbstractVersion;
import org.cthing.versionparser.Version;
import org.cthing.versionparser.VersionParsingException;
import org.jspecify.annotations.Nullable;


/**
 * Represents the version of an artifact in the <a href="https://rubygems.org/">RubyGems</a> ecosystem. To obtain
 * an instance of this class, call the  {@link GemVersionScheme#parseVersion(String)} method.
 *
 * <p>
 * This class processes string versions into comparable values. A version string should normally be a series of
 * numbers separated by periods. Each part (digits separated by periods) is considered its own number, and these
 * are used for sorting. For instance, 3.10 sorts higher than 3.2 because ten is greater than two.
 * </p>
 *
 * <p>
 * If any part of the version string contains letters (currently only a-z and A-Z are supported) then that version
 * is considered pre-release. Versions with a pre-release part in the Nth part sort less than versions with N-1
 * parts. Pre-release parts are sorted alphabetically using the normal string sorting rules. If a pre-release part
 * contains both letters and numbers, it will be broken into multiple parts to provide the expected sort behavior
 * (e.g. 1.0.a10 becomes 1.0.a.10, and is greater than 1.0.a9).
 * </p>
 *
 * <p>
 * Pre-release versions sort between real releases (newest to oldest):
 * </p>
 * <ol>
 *   <li>1.0</li>
 *   <li>1.0.b1</li>
 *   <li>1.0.a.2</li>
 *   <li>0.9</li>
 * </ol>
 */
public final class GemVersion extends AbstractVersion {

    static final String VERSION_PATTERN = "[0-9]+(?>\\.[0-9a-zA-Z]+)*(-[0-9A-Za-z-]+(\\.[0-9A-Za-z-]+)*)?";

    private static final Pattern ANCHORED_VERSION_PATTERN = Pattern.compile("\\A\\s*(" + VERSION_PATTERN + ")?\\s*\\z");
    private static final Pattern COMPONENT_PATTERN = Pattern.compile("(\\d+|[a-z]+)", Pattern.CASE_INSENSITIVE);
    private static final Pattern TRAILING_ZERO_PATTERN = Pattern.compile("(?<=[a-zA-Z.])[.0]+\\z");
    private static final Pattern PRERELEASE_ZERO_PATTERN = Pattern.compile("(?<=\\.|\\A)[0.]+(?=[a-zA-Z])");
    private static final Pattern PRERELEASE_PATTERN = Pattern.compile("[a-zA-Z]");
    private static final Pattern STARTS_NUMERIC = Pattern.compile("\\A\\d");
    private static final Component ZERO_COMPONENT = new Component(0);

    private final String version;
    private final boolean preRelease;
    private final List<Component> components;
    private final List<Component> canonicalComponents;

    @Nullable
    private GemVersion nextVersion;

    /**
     * Constructs a RubyGems version from the specified version string.
     *
     * @param version Version string from which to construct the version
     */
    private GemVersion(final String version) {
        super(version);

        // Remove leading and trailing whitespace. If the version string is empty, set the version to 0.
        // Versions containing a dash are considered pre-release. Replace the dashes with a component that
        // will indicate this is a pre-release version.
        String v = version.strip();
        if (v.isEmpty()) {
            v = "0";
        }
        this.version = v.replace("-", ".pre.");

        this.preRelease = PRERELEASE_PATTERN.matcher(this.version).find();
        this.components = partitionComponents(this.version);
        this.canonicalComponents = canonicalizeComponents(this.version, this.preRelease);
    }

    /**
     * Parses the specified version string to create a Gem version object representing that version.
     *
     * @param ver Version string to parse
     * @return Newly created Gem version representing the specified version string.
     * @throws VersionParsingException if there was a problem parsing the specified version.
     */
    static GemVersion parse(final String ver) throws VersionParsingException {
        if (!isCorrect(ver)) {
            throw new VersionParsingException("Malformed version number string " + ver);
        }

        return new GemVersion(ver);
    }

    /**
     * Provides the parsed components of the version.
     *
     * @return Parsed components of the version. Note that due to removal of padding and other substitutions,
     *      reconstructing the version from the components may not match the original specified version.
     */
    public List<String> getComponents() {
        return this.canonicalComponents.stream().map(Component::toString).toList();
    }

    @Override
    public boolean isPreRelease() {
        // A version is considered pre-release if it contains one or more letters.
        return this.preRelease;
    }

    @Override
    @SuppressWarnings("ObjectEquality")
    public int compareTo(final Version obj) {
        if (getClass() != obj.getClass()) {
            throw new IllegalArgumentException("Expected instance of MvnVersion but received "
                                                       + obj.getClass().getName());
        }

        final GemVersion other = (GemVersion)obj;
        if (this == other) {
            return 0;
        }
        if (this.version.equals(other.version)) {
            return 0;
        }

        if (this.canonicalComponents.equals(other.canonicalComponents)) {
            return 0;
        }

        final int thisSize = this.canonicalComponents.size();
        final int otherSize = other.canonicalComponents.size();
        final int limit = Math.max(thisSize, otherSize);

        for (int i = 0; i < limit; i++) {
            final Component thisComponent = (i < thisSize) ? this.canonicalComponents.get(i) : ZERO_COMPONENT;
            final Component otherComponent = (i < otherSize) ? other.canonicalComponents.get(i) : ZERO_COMPONENT;

            final int result = thisComponent.compareTo(otherComponent);
            if (result != 0) {
                return result;
            }
        }

        return 0;
    }

    /**
     * Obtains a new version where the next to the last component has been incremented. Prerelease versions are
     * converted to release versions before they are incremented. For example, the next version after "5.3.1" is
     * "5.4" and the next version after "5.3.1.b.2" is "5.4".
     *
     * @return Newly created version representing the next version.
     */
    GemVersion toNextVersion() {
        if (this.nextVersion == null) {
            final List<Long> comps = this.components.stream()
                                                    .takeWhile(Component::isNumber)
                                                    .map(comp -> comp.number)
                                                    .collect(Collectors.toCollection(ArrayList::new));
            int lastIdx = comps.size() - 1;
            if (lastIdx > 0) {
                comps.remove(lastIdx--);
            }
            comps.set(lastIdx, comps.get(lastIdx) + 1);
            final String nextVer = comps.stream().map(Object::toString).collect(Collectors.joining("."));
            this.nextVersion = new GemVersion(nextVer);
        }

        return this.nextVersion;
    }

    /**
     * Indicates whether the specified version string matches the RubyGems requirements.
     *
     * @param ver Version to test
     * @return {@code true} if the specified version is valid according to the RubyGems requirements.
     */
    static boolean isCorrect(final String ver) {
        return ANCHORED_VERSION_PATTERN.matcher(ver).matches();
    }

    /**
     * Remove trailing zero components before the first letter or at the end of the version string.
     *
     * @return Version string separated into components with zero components trimmed as appropriate.
     */
    private static List<Component> canonicalizeComponents(final String ver, final boolean preRel) {
        final Matcher trailingMatcher = TRAILING_ZERO_PATTERN.matcher(ver);
        String canonicalVersion = trailingMatcher.replaceAll("");
        if (preRel) {
            final Matcher zeroMatcher = PRERELEASE_ZERO_PATTERN.matcher(canonicalVersion);
            canonicalVersion = zeroMatcher.replaceAll("");
        }

        return partitionComponents(canonicalVersion);
    }

    /**
     * Splits the specified version string into individual components. A component is a number or a group of one
     * or more letters.
     *
     * @param ver Version string to split
     * @return Version string separated into components.
     */
    static List<Component> partitionComponents(final String ver) {
        final List<Component> components = new ArrayList<>();
        final Matcher matcher = COMPONENT_PATTERN.matcher(ver);
        while (matcher.find()) {
            final String match = matcher.group(1);
            final Component component = STARTS_NUMERIC.matcher(match).find()
                                        ? new Component(Long.parseLong(match, 10))
                                        : new Component(match);
            components.add(component);
        }
        return components;
    }

    /**
     * For RubyGems a Version is only equal to another version if it is specified to the same precision.
     * For example, version "1.0" is not equal to version "1".
     *
     * @param obj Object to compare with this
     * @return {@code true} if the specified object is equal to this.
     */
    @Override
    public boolean equals(final Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }

        final GemVersion that = (GemVersion)obj;
        return Objects.equals(this.version, that.version);
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.version);
    }


    /**
     * Represents a component in a version string. A component is either an integer value or a string.
     */
    static class Component implements Comparable<Component> {

        private final String string;
        private final long number;
        private final boolean valueIsString;

        /**
         * Constructs a component consisting of the specified numerical value.
         *
         * @param number Numerical value for the component
         */
        Component(final long number) {
            this("", number, false);
        }

        /**
         * Constructs a component consisting of the specified string value.
         *
         * @param string String value for the component
         */
        Component(final String string) {
            this(string, 0, true);
        }

        private Component(final String string, final long number, final boolean valueIsString) {
            this.string = string;
            this.number = number;
            this.valueIsString = valueIsString;
        }

        /**
         * Indicates if this component represents a string value.
         *
         * @return {@code true} if this component represents a string value.
         */
        boolean isString() {
            return this.valueIsString;
        }

        /**
         * Indicates if this component represents a numerical value.
         *
         * @return {@code true} if this component represents a numerical value.
         */
        boolean isNumber() {
            return !this.valueIsString;
        }

        /**
         * Obtains the string value of this component.
         *
         * @return String value oif this component
         * @throws IllegalStateException if this component holds a number rather than a string.
         */
        String getString() {
            if (!this.valueIsString) {
                throw new IllegalStateException("Component contains an integer not a string");
            }
            return this.string;
        }

        /**
         * Obtains the numerical value of this component.
         *
         * @return Numerical value oif this component
         * @throws IllegalStateException if this component holds a string rather than a number.
         */
        long getNumber() {
            if (this.valueIsString) {
                throw new IllegalStateException("Component contains a string not an integer");
            }
            return this.number;
        }

        @Override
        public String toString() {
            return this.valueIsString ? this.string : Long.toString(this.number);
        }

        @Override
        @SuppressWarnings("ObjectEquality")
        public int compareTo(final Component other) {
            if (this == other) {
                return 0;
            }
            if (this.valueIsString && !other.valueIsString) {
                return -1;
            }
            if (!this.valueIsString && other.valueIsString) {
                return 1;
            }
            return this.valueIsString
                   ? this.string.compareTo(other.string)
                   : Long.compare(this.number, other.number);
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
            return this.number == component.number
                    && this.valueIsString == component.valueIsString
                    && Objects.equals(this.string, component.string);
        }

        @Override
        public int hashCode() {
            return Objects.hash(this.string, this.number, this.valueIsString);
        }
    }
}
