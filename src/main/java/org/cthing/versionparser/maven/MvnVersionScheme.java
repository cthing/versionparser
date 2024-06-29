/*
 * Copyright 2023 C Thing Software
 * SPDX-License-Identifier: Apache-2.0
 *
 * This file is derived from org.eclipse.aether.util.version.GenericVersionScheme.java
 * which is covered by the following copyright and permission notices:
 *
 *   Licensed to the Apache Software Foundation (ASF) under one
 *   or more contributor license agreements.  See the NOTICE file
 *   distributed with this work for additional information
 *   regarding copyright ownership.  The ASF licenses this file
 *   to you under the Apache License, Version 2.0 (the
 *   "License"); you may not use this file except in compliance
 *   with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing,
 *   software distributed under the License is distributed on an
 *   "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *   KIND, either express or implied.  See the License for the
 *   specific language governing permissions and limitations
 *   under the License.
 */

package org.cthing.versionparser.maven;

import java.util.ArrayList;
import java.util.List;

import org.cthing.annotations.NoCoverageGenerated;
import org.cthing.versionparser.Version;
import org.cthing.versionparser.VersionConstraint;
import org.cthing.versionparser.VersionParsingException;
import org.cthing.versionparser.VersionRange;


/**
 * Represents the version scheme used by the Maven build tool. To parse a version string, call the
 * {@link #parseVersion(String)} method. To parse a version constraint expression, call the
 * {@link #parseConstraint(String)} method.
 */
public final class MvnVersionScheme {

    @NoCoverageGenerated
    private MvnVersionScheme() {
    }

    /**
     * Parses a Maven artifact version.
     *
     * @param version Version to parse
     * @return Version object corresponding to the specified version string.
     */
    public static MvnVersion parseVersion(final String version) {
        return MvnVersion.parse(version);
    }

    /**
     * Parses a Maven dependency <a href="https://maven.apache.org/enforcer/enforcer-rules/versionRanges.html">version
     * constraint</a>. While the terms version constraint and version range are often used interchangeably, for the
     * purposes of this library, a version constraint is composed of one or more version ranges.
     *
     * @param constraint Version constraint to parse
     * @return Version constraint object corresponding to the specified constraint.
     * @throws VersionParsingException if there is a problem parsing the constraint
     */
    public static VersionConstraint parseConstraint(final String constraint) throws VersionParsingException {
        final List<VersionRange> ranges = new ArrayList<>();

        String constraintBuffer = constraint.trim();
        if (constraintBuffer.isEmpty()) {
            throw new VersionParsingException("Constraint cannot be empty or blank");
        }

        while (constraintBuffer.startsWith("[") || constraintBuffer.startsWith("(")) {
            final int index1 = constraintBuffer.indexOf(')');
            final int index2 = constraintBuffer.indexOf(']');

            int index = index2;
            if (index2 < 0 || (index1 >= 0 && index1 < index2)) {
                index = index1;
            }

            if (index < 0) {
                throw new VersionParsingException("Unbounded version range " + constraint);
            }

            final VersionRange range = parseRange(constraintBuffer.substring(0, index + 1));
            ranges.add(range);

            constraintBuffer = constraintBuffer.substring(index + 1).trim();

            if (constraintBuffer.startsWith(",")) {
                constraintBuffer = constraintBuffer.substring(1).trim();
            }
        }

        if (!constraintBuffer.isEmpty() && !ranges.isEmpty()) {
            throw new VersionParsingException("Invalid version range " + constraint + ", expected [ or ( but got "
                                                      + constraintBuffer);
        }

        // Maven considers this a so called "soft" constraint, which is a weak constraint in this library. A weak
        // constraint can be replaced with a different version by a dependency resolution algorithm. The version
        // range corresponding to an undecorated version is not well-defined (e.g. is it a fully open range). This
        // library considers an undecorated version as a range with an inclusive lower bound and open upper bound.
        if (ranges.isEmpty()) {
            final Version version = parseVersion(constraint);
            return new VersionConstraint(version, null, true, false, true);
        }

        return new VersionConstraint(ranges);
    }

    /**
     * Parses a single version range.
     *
     * @param range Version range to parse
     * @return Newly created version range.
     * @throws VersionParsingException if there is a problem parsing the range
     */
    private static VersionRange parseRange(final String range) throws VersionParsingException {
        String rangeBuffer = range;

        final boolean minIncluded;
        final boolean maxIncluded;

        if (range.startsWith("[")) {
            minIncluded = true;
        } else if (range.startsWith("(")) {
            minIncluded = false;
        } else {
            throw new VersionParsingException("Invalid version range '" + range
                                                      + "', a range must start with either [ or (");
        }

        if (range.endsWith("]")) {
            maxIncluded = true;
        } else if (range.endsWith(")")) {
            maxIncluded = false;
        } else {
            throw new VersionParsingException("Invalid version range '" + range
                                                      + "', a range must end with either ] or )");
        }

        final MvnVersion minVersion;
        final MvnVersion maxVersion;

        rangeBuffer = rangeBuffer.substring(1, rangeBuffer.length() - 1);

        final int index = rangeBuffer.indexOf(',');
        if (index < 0) {
            if (!minIncluded || !maxIncluded) {
                throw new VersionParsingException("Invalid version range '" + range
                                                          + "', single version must be surrounded by []");
            }

            final String version = rangeBuffer.trim();
            if (version.endsWith(".*")) {
                final String prefix = version.substring(0, version.length() - 1);
                minVersion = MvnVersion.parse(prefix + "min");
                maxVersion = MvnVersion.parse(prefix + "max");
            } else {
                minVersion = MvnVersion.parse(version);
                maxVersion = minVersion;
            }
        } else {
            final String minVersionStr = rangeBuffer.substring(0, index).trim();
            final String maxVersionStr = rangeBuffer.substring(index + 1).trim();

            // More than two bounds, e.g. (1,2,3)
            if (maxVersionStr.contains(",")) {
                throw new VersionParsingException("Invalid version range '" + range
                                                          + "', bounds may not contain additional ','");
            }

            minVersion = minVersionStr.isEmpty() ? null : MvnVersion.parse(minVersionStr);
            maxVersion = maxVersionStr.isEmpty() ? null : MvnVersion.parse(maxVersionStr);

            if (maxVersion != null && minVersion != null && maxVersion.compareTo(minVersion) < 0) {
                throw new VersionParsingException("Invalid version range '" + range
                                                          + "', lower bound must not be greater than upper bound");
            }
        }

        return new VersionRange(minVersion, maxVersion, minIncluded, maxIncluded);
    }
}
