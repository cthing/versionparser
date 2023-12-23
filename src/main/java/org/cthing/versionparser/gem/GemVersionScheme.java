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

package org.cthing.versionparser.gem;

import java.util.Arrays;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.cthing.annotations.NoCoverageGenerated;
import org.cthing.versionparser.VersionConstraint;
import org.cthing.versionparser.VersionParsingException;


/**
 * Represents the version scheme used by the RubyGems packaging system. To parse a version string, call the
 * {@link #parseVersion(String)} method. To parse version constraint expressions, call the
 * {@link #parseConstraint(String...)} method.
 */
public final class GemVersionScheme {

    private static final String EQ = "=";
    private static final String NE = "!=";
    private static final String LT = "<";
    private static final String GT = ">";
    private static final String GE = ">=";
    private static final String LE = "<=";
    private static final String PE = "~>";
    private static final Pattern CONSTRAINT_PATTERN =
            Pattern.compile(String.format("\\A\\s*(%s|%s|%s|%s|%s|%s|%s)?\\s*(%s)\\s*\\z",
                                          EQ, NE, GT, LT, GE, LE, PE, GemVersion.VERSION_PATTERN));
    private static final String PESSIMISTIC_SUFFIX = ".ZZZ";

    @NoCoverageGenerated
    private GemVersionScheme() {
    }

    /**
     * Parses a RubyGems version.
     *
     * @param version Version to parse
     * @return Version object corresponding to the specified version string.
     * @throws VersionParsingException if there was a problem parsing the version string.
     */
    public static GemVersion parseVersion(final String version) throws VersionParsingException {
        return GemVersion.parse(version);
    }

    /**
     * Parses one or more RubyGem <a href="https://guides.rubygems.org/patterns/#declaring-dependencies">dependency
     * constraints</a>.
     *
     * @param constraints One or more dependency constraints. Each constraint is AND'd together.
     * @return Version constraint representing the specified constraints.
     * @throws VersionParsingException if there is a problem parsing the constraints.
     */
    public static VersionConstraint parseConstraint(final String... constraints) throws VersionParsingException {
        final Set<String> constraintSet = Arrays.stream(constraints)
                                                .filter(constraint -> !constraint.isBlank())
                                                .collect(Collectors.toSet());
        if (constraintSet.isEmpty()) {
            return new VersionConstraint(parseVersion("0"), null, true, false);
        }

        VersionConstraint constraint = VersionConstraint.ANY;
        for (final String constraintStr : constraintSet) {
            final Matcher matcher = CONSTRAINT_PATTERN.matcher(constraintStr);
            if (!matcher.matches()) {
                throw new VersionParsingException("Invalid version constraint '" + constraintStr + "'");
            }

            final String op = matcher.group(1);
            final GemVersion version = GemVersion.parse(matcher.group(2));

            constraint = switch (op == null ? EQ : op) {
                case EQ -> constraint.intersect(new VersionConstraint(version));
                case NE -> constraint.difference(new VersionConstraint(version));
                case GT -> constraint.intersect(new VersionConstraint(version, null, false, false));
                case LT -> constraint.intersect(new VersionConstraint(null, version, false, false));
                case GE -> constraint.intersect(new VersionConstraint(version, null, true, false));
                case LE -> constraint.intersect(new VersionConstraint(null, version, false, true));
                case PE -> {
                    // Pessimistic versioning handles prerelease versions differently than release versions.
                    // See https://velocitylabs.io/blog/2015/05/08/pessimistic-prerelease-peculiarities/ for
                    // an explanation of these differences and why a prerelease suffix must be added to the
                    // maximum version.
                    final GemVersion pessimisticMax = GemVersion.parse(version.toNextVersion() + PESSIMISTIC_SUFFIX);
                    yield constraint.intersect(new VersionConstraint(version, pessimisticMax, true, false));
                }
                default -> throw new IllegalStateException("Unexpected operator: " + op);
            };
        }

        return constraint;
    }
}
