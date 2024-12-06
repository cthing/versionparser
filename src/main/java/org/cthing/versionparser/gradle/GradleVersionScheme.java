/*
 * Copyright 2023 C Thing Software
 * SPDX-License-Identifier: Apache-2.0
 *
 * This file is derived from
 * org.gradle.api.internal.artifacts.ivyservice.ivyresolve.strategy.VersionRangeSelector
 * which us covered by the following copyright and permission notices:
 *
 *   Copyright 2013 the original author or authors.
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 */

package org.cthing.versionparser.gradle;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.cthing.annotations.NoCoverageGenerated;
import org.cthing.versionparser.VersionConstraint;
import org.cthing.versionparser.VersionParsingException;


/**
 * Represents the version scheme used by the Gradle build tool. To parse a version string, call the
 * {@link #parseVersion(String)} method. To parse a version constraint expression, call the
 * {@link #parseConstraint(String)} method.
 *
 * <p>
 * The following are examples of version constraint expressions supported by Gradle:
 * </p>
 * <ul>
 * <li>[1.0,2.0] matches all versions greater or equal to 1.0 and lower or equal to 2.0</li>
 * <li>[1.0,2.0[ matches all versions greater or equal to 1.0 and lower than 2.0</li>
 * <li>]1.0,2.0] matches all versions greater than 1.0 and lower or equal to 2.0</li>
 * <li>]1.0,2.0[ matches all versions greater than 1.0 and lower than 2.0</li>
 * <li>[1.0,) matches all versions greater or equal to 1.0</li>
 * <li>]1.0,) matches all versions greater than 1.0</li>
 * <li>(,2.0] matches all versions lower or equal to 2.0</li>
 * <li>(,2.0[ matches all versions lower than 2.0</li>
 * </ul>
 */
@SuppressWarnings("RegExpRedundantEscape")
public final class GradleVersionScheme {

    private static final String LOWER_INFINITE = "(";
    private static final String UPPER_INFINITE = ")";
    private static final String SEPARATOR = ",";
    private static final String LI_PATTERN = "\\" + LOWER_INFINITE;
    private static final String UI_PATTERN = "\\" + UPPER_INFINITE;
    private static final String SEP_PATTERN = "\\s*\\" + SEPARATOR + "\\s*";
    private static final String OPEN_INC = "[";
    private static final String OPEN_EXC = "]";
    private static final String OPEN_EXC_MAVEN = "(";
    private static final String OPEN_INC_PATTERN = "\\" + OPEN_INC;
    private static final String OPEN_EXC_PATTERN = "\\" + OPEN_EXC + "\\" + OPEN_EXC_MAVEN;
    private static final String OPEN_PATTERN = "[" + OPEN_INC_PATTERN + OPEN_EXC_PATTERN + "]";
    private static final String CLOSE_INC = "]";
    private static final String CLOSE_EXC = "[";
    private static final String CLOSE_EXC_MAVEN = ")";
    private static final String CLOSE_INC_PATTERN = "\\" + CLOSE_INC;
    private static final String CLOSE_EXC_PATTERN = "\\" + CLOSE_EXC + "\\" + CLOSE_EXC_MAVEN;
    private static final String CLOSE_PATTERN = "[" + CLOSE_INC_PATTERN + CLOSE_EXC_PATTERN + "]";
    private static final String ANY_NON_SPECIAL_PATTERN = "[^\\s" + SEPARATOR + OPEN_INC_PATTERN
            + OPEN_EXC_PATTERN + CLOSE_INC_PATTERN + CLOSE_EXC_PATTERN + LI_PATTERN + UI_PATTERN
            + "]";
    private static final String LOWER_INFINITE_PATTERN = LI_PATTERN + SEP_PATTERN + "("
            + ANY_NON_SPECIAL_PATTERN + "+)\\s*" + CLOSE_PATTERN;
    private static final String UPPER_INFINITE_PATTERN = OPEN_PATTERN + "\\s*("
            + ANY_NON_SPECIAL_PATTERN + "+)" + SEP_PATTERN + UI_PATTERN;
    private static final String FINITE_PATTERN = OPEN_PATTERN
            + "\\s*(" + ANY_NON_SPECIAL_PATTERN + "+)"
            + SEP_PATTERN
            + "(" + ANY_NON_SPECIAL_PATTERN + "+)\\s*"
            + CLOSE_PATTERN;
    private static final String INFINITE_PATTERN = "[" + OPEN_EXC_PATTERN + "]"
            + SEP_PATTERN
            + "[" + CLOSE_EXC_PATTERN + "]";
    private static final String INFINITE_DYNAMIC_PATTERN = "\\s*\\+\\s*";
    private static final String SINGLE_VALUE_PATTERN = OPEN_INC_PATTERN
            + "\\s*(" + ANY_NON_SPECIAL_PATTERN + "+)"
            + CLOSE_INC_PATTERN;

    private static final Pattern FINITE_RANGE = Pattern.compile(FINITE_PATTERN);
    private static final Pattern INFINITE_RANGE = Pattern.compile(INFINITE_PATTERN);
    private static final Pattern INFINITE_DYNAMIC_RANGE = Pattern.compile(INFINITE_DYNAMIC_PATTERN);
    private static final Pattern LOWER_INFINITE_RANGE = Pattern.compile(LOWER_INFINITE_PATTERN);
    private static final Pattern UPPER_INFINITE_RANGE = Pattern.compile(UPPER_INFINITE_PATTERN);
    private static final Pattern SINGLE_VALUE_RANGE = Pattern.compile(SINGLE_VALUE_PATTERN);
    private static final Pattern ALL_RANGE = Pattern.compile(FINITE_PATTERN + "|"
                                                                    + LOWER_INFINITE_PATTERN + "|"
                                                                    + UPPER_INFINITE_PATTERN + "|"
                                                                    + INFINITE_RANGE + "|"
                                                                    + INFINITE_DYNAMIC_RANGE + "|"
                                                                    + SINGLE_VALUE_RANGE);

    @NoCoverageGenerated
    private GradleVersionScheme() {
    }

    /**
     * Parses a Gradle artifact version.
     *
     * @param version Version to parse
     * @return Version object corresponding to the specified version string.
     */
    public static GradleVersion parseVersion(final String version) {
        return GradleVersion.parse(version);
    }

    /**
     * Parses a Gradle dependency version constraint. While the terms version constraint and version range are often
     * used interchangeably, for the purposes of this library, a version constraint is composed of one or more version
     * ranges.
     *
     * @param constraint Version constraint to parse
     * @return Version constraint object corresponding to the specified constraint.
     * @throws VersionParsingException if there is a problem parsing the specified constraint.
     */
    public static VersionConstraint parseConstraint(final String constraint) throws VersionParsingException {
        if (ALL_RANGE.matcher(constraint).matches()) {
            return parseRange(constraint);
        }

        if (constraint.endsWith("+")) {
            return parseDynamicVersion(constraint);
        }

        return parseSingleVersion(constraint);
    }

    /**
     * Parses the specified version range constraint.
     *
     * @param constraint Version range constraint
     * @return Version range constraint
     * @throws VersionParsingException if there is a problem parsing the range
     */
    private static VersionConstraint parseRange(final String constraint) throws VersionParsingException {
        // A single '+'
        Matcher matcher = INFINITE_DYNAMIC_RANGE.matcher(constraint);
        if (matcher.matches()) {
            return VersionConstraint.ANY;
        }

        // A version range with both sides included
        matcher = FINITE_RANGE.matcher(constraint);
        if (matcher.matches()) {
            final GradleVersion minVersion = GradleVersion.parse(matcher.group(1));
            final GradleVersion maxVersion = GradleVersion.parse(matcher.group(2));
            final boolean minIncluded = constraint.startsWith(OPEN_INC);
            final boolean maxIncluded = constraint.endsWith(CLOSE_INC);
            return new VersionConstraint(minVersion, maxVersion, minIncluded, maxIncluded);
        }

        // A version range with an infinite lower bound. The upper bound can be finite or infinite.
        matcher = LOWER_INFINITE_RANGE.matcher(constraint);
        if (matcher.matches()) {
            final GradleVersion maxVersion = GradleVersion.parse(matcher.group(1));
            final boolean maxIncluded = constraint.endsWith(CLOSE_INC);
            return new VersionConstraint(null, maxVersion, false, maxIncluded);
        }

        // A version range with an infinite upper bound. The lower bound can be finite or infinite.
        matcher = UPPER_INFINITE_RANGE.matcher(constraint);
        if (matcher.matches()) {
            final GradleVersion minVersion = GradleVersion.parse(matcher.group(1));
            final boolean minIncluded = constraint.startsWith(OPEN_INC);
            return new VersionConstraint(minVersion, null, minIncluded, false);
        }

        // A version range of the form '(,)'.
        matcher = INFINITE_RANGE.matcher(constraint);
        if (matcher.matches()) {
            return VersionConstraint.ANY;
        }

        // A version range consisting of a single version (e.g. [1.5]).
        matcher = SINGLE_VALUE_RANGE.matcher(constraint);
        if (matcher.matches()) {
            final GradleVersion version = GradleVersion.parse(matcher.group(1));
            return new VersionConstraint(version);
        }

        throw new VersionParsingException("Not a version range selector: " + constraint);
    }

    /**
     * Parses a version ending with a '+'. Ideally, this type of version treats the portion before the
     * '+' as a prefix and tests whether a given version beings with that prefix. However, it is impractical
     * to work with string matching when set operations are involved. Therefore, the dynamic version is converted
     * to a version range with the lower bound included as the prefix and the upper bound excluded as one version
     * higher. The following examples illustrate the conversion:
     * <ul>
     *     <li>1.5.0.+ &#x2248; [1.5.0,1.5.1)</li>
     *     <li>1.5.+ &#x2248; [1.5,1.6)</li>
     *     <li>1.+ &#x2248; [1,2)</li>
     * </ul>
     *
     * @param constraint Dynamic version constraint
     * @return Version range approximating the dynamic version.
     * @throws VersionParsingException if there is a problem parsing the dynamic version
     */
    private static VersionConstraint parseDynamicVersion(final String constraint) throws VersionParsingException {
        final GradleVersion version = GradleVersion.parse(constraint);
        final List<String> parts = new ArrayList<>(version.getComponents());
        assert "+".equals(parts.get(parts.size() - 1));

        if (parts.size() < 2) {         // SUPPRESS CHECKSTYLE skip + and delimiter
            throw new VersionParsingException("Invalid dynamic version " + constraint);
        }

        final String minVersionStr = IntStream.range(0, parts.size() - 1)
                                              .mapToObj(parts::get)
                                              .collect(Collectors.joining("."));

        final int pos = parts.size() - 2;
        final Long numericPart = version.getNumericParts().get(pos);
        if (numericPart == null) {
            throw new VersionParsingException("Dynamic versions must have a numeric component before the '+'");
        }

        parts.set(pos, String.valueOf(numericPart + 1));

        final String maxVersionStr = IntStream.range(0, parts.size() - 1)
                                              .mapToObj(parts::get)
                                              .collect(Collectors.joining("."));

        final GradleVersion minVersion = GradleVersion.parse(minVersionStr);
        final GradleVersion maxVersion = GradleVersion.parse(maxVersionStr);
        return new VersionConstraint(minVersion, maxVersion, true, false);
    }

    /**
     * Creates a version range based on the specified single version. This type of constraint is treated as a
     * range with the specified version as the included lower bound, and an unlimited upper bound. For example,
     * if 1.5.0 is specified, the resulting range is [1.5.0,).
     *
     * @param constraint Single version from which to create a constraint
     * @return Version range based on the specified version.
     */
    private static VersionConstraint parseSingleVersion(final String constraint) {
        final GradleVersion version = GradleVersion.parse(constraint);
        return new VersionConstraint(version, null, true, false);
    }
}
