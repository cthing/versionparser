/*
 * Copyright 2023 C Thing Software
 * SPDX-License-Identifier: Apache-2.0
 *
 * Portions of this file are derived from the semver4j (https://github.com/semver4j/semver4j)
 * project, which is covered by the following copyright and permission notices:
 *
 *   The MIT License (MIT)
 *
 *   Copyright (c) 2022-present Semver4j contributors
 *
 *   Permission is hereby granted, free of charge, to any person obtaining a copy
 *   of this software and associated documentation files (the "Software"), to deal
 *   in the Software without restriction, including without limitation the rights
 *   to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 *   copies of the Software, and to permit persons to whom the Software is
 *   furnished to do so, subject to the following conditions:
 *
 *   The above copyright notice and this permission notice shall be included in all
 *   copies or substantial portions of the Software.
 *
 *   THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 *   IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 *   FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 *   AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 *   LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 *   OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 *   SOFTWARE.
 */

package org.cthing.versionparser.npm;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.cthing.annotations.NoCoverageGenerated;
import org.cthing.versionparser.Version;
import org.cthing.versionparser.VersionConstraint;
import org.cthing.versionparser.VersionParsingException;
import org.cthing.versionparser.VersionRange;
import org.cthing.versionparser.semver.SemanticVersion;


/**
 * Represents the version scheme used by the NPM ecosystem. To parse a version string, call the
 * {@link #parseVersion(String)} method. To parse a version constraint expression, call the
 * {@link #parseConstraint(String)} method.
 * <p>
 * Artifacts in the NPM ecosystem are versioned using the <a href="https://semver.org/">Semantic Version</a>
 * specification. Therefore the {@link #parseVersion(String)} method returns an instance of a {@link SemanticVersion}
 * and there is no NPM-specific version class.
 * </p>
 */
public final class NpmVersionScheme {

    private static final Pattern OR_PATTERN = Pattern.compile("\\|\\|");
    private static final Pattern SPLITTER_PATTERN = Pattern.compile("(\\s*)([<>]?=?)\\s*");
    private static final Pattern COMPARATOR_PATTERN =
            Pattern.compile(String.format(Locale.ROOT, "^%s\\s*(%s)$|^$", ConstraintTranslator.GLTL,
                                          SemanticVersion.SEMVER));
    private static final Pattern SPACE_PATTERN = Pattern.compile("\\s+");

    @NoCoverageGenerated
    private NpmVersionScheme() {
    }

    /**
     * Parses an NPM artifact version.
     *
     * @param version Version to parse
     * @return Version object corresponding to the specified version string.
     * @throws VersionParsingException if there was a problem parsing the specified version.
     */
    public static SemanticVersion parseVersion(final String version) throws VersionParsingException {
        return SemanticVersion.parse(version);
    }

    /**
     * Parses an NPM dependency
     * <a href="https://docs.npmjs.com/cli/v10/configuring-npm/package-json#dependencies">version constraint</a>.
     * Note that Git URLs, tags and local paths are not supported.
     *
     * @param constraint Version constraint to parse
     * @return Version constraint object corresponding to the specified constraint.
     * @throws VersionParsingException if there is a problem parsing the constraint
     */
    @SuppressWarnings("DataFlowIssue")
    public static VersionConstraint parseConstraint(final String constraint) throws VersionParsingException {
        final List<VersionRange> versionRanges = new ArrayList<>();

        final String[] constraintSections = OR_PATTERN.split(constraint.trim());
        for (final String constraintSection : constraintSections) {
            // Prepare the constraint components for parsing.
            String translatedConstraintSection = stripWhitespacesBetweenRangeOperator(constraintSection);

            // Translate high level dependency notation (e.g. "~", "^") to low level notation (e.g. "<", ">=").
            translatedConstraintSection = ConstraintTranslator.translate(translatedConstraintSection);

            // Create version range objects corresponding to the low level range components.
            final List<ConstraintComponent> components = parseConstraintComponents(translatedConstraintSection);
            switch (components.size()) {
                case 0 -> { }
                case 1 -> {
                    final ConstraintComponent component = components.get(0);
                    final VersionRange versionConstraint = switch (component.getOperator()) {
                        case EQ -> new VersionRange(component.getVersion());
                        case LT -> new VersionRange(null, component.getVersion(), false, false);
                        case LTE -> new VersionRange(null, component.getVersion(), false, true);
                        case GT -> new VersionRange(component.getVersion(), null, false, false);
                        case GTE -> new VersionRange(component.getVersion(), null, true, false);
                    };
                    versionRanges.add(versionConstraint);
                }
                case 2 -> {
                    final ConstraintComponent component1 = components.get(0);
                    final ConstraintComponent component2 = components.get(1);
                    Version minVersion = null;
                    Version maxVersion = null;
                    boolean minIncluded = false;
                    boolean maxIncluded = false;

                    switch (component1.getOperator()) {
                        case LT -> {
                            maxVersion = component1.getVersion();
                            maxIncluded = false;
                        }
                        case LTE -> {
                            maxVersion = component1.getVersion();
                            maxIncluded = true;
                        }
                        case GT -> {
                            minVersion = component1.getVersion();
                            minIncluded = false;
                        }
                        case GTE -> {
                            minVersion = component1.getVersion();
                            minIncluded = true;
                        }
                        case EQ -> throw new VersionParsingException("Cannot have '=' in an 'AND' constraint");
                        default -> throw new IllegalStateException("Unknown operator: " + component1.getOperator());
                    }

                    switch (component2.getOperator()) {
                        case LT -> {
                            maxVersion = component2.getVersion();
                            maxIncluded = false;
                        }
                        case LTE -> {
                            maxVersion = component2.getVersion();
                            maxIncluded = true;
                        }
                        case GT -> {
                            minVersion = component2.getVersion();
                            minIncluded = false;
                        }
                        case GTE -> {
                            minVersion = component2.getVersion();
                            minIncluded = true;
                        }
                        case EQ -> throw new VersionParsingException("Cannot have '=' in an 'AND' constraint");
                        default -> throw new IllegalStateException("Unknown operator: " + component1.getOperator());
                    }

                    final VersionRange versionRange = new VersionRange(minVersion, maxVersion, minIncluded, maxIncluded);
                    versionRanges.add(versionRange);
                }
                default ->
                        throw new VersionParsingException("Cannot handle more than two ranges within a single clause");
            }
        }

        if (versionRanges.isEmpty()) {
            return VersionConstraint.ANY;
        }
        return new VersionConstraint(versionRanges);
    }


    /**
     * Remove any whitespace between the operator and the version.
     *
     * @param constraintSection Range to be stripped
     * @return Range stripped of any whitespace between the operator and version.
     */
    private static String stripWhitespacesBetweenRangeOperator(final String constraintSection) {
        final Matcher matcher = SPLITTER_PATTERN.matcher(constraintSection);
        return matcher.replaceAll("$1$2").trim();
    }

    /**
     * Parses constraint components (i.e. one or more [operator]version separated by whitespace).
     *
     * @param constraintSection Constraints to be parsed
     * @return Constraint components.
     * @throws VersionParsingException if there was a problem parsing the constraint component.
     */
    private static List<ConstraintComponent> parseConstraintComponents(final String constraintSection)
            throws VersionParsingException {
        final List<ConstraintComponent> components = new ArrayList<>();

        final String[] constraintItems = SPACE_PATTERN.split(constraintSection);
        for (final String constraintItem : constraintItems) {
            final Matcher matcher = COMPARATOR_PATTERN.matcher(constraintItem);
            if (matcher.matches()) {
                final String operator = matcher.group(1);
                final String version = matcher.group(2);
                components.add(new ConstraintComponent(version, operator));
            }
        }

        return components;
    }
}
