/*
 * Copyright 2026 C Thing Software
 * SPDX-License-Identifier: Apache-2.0
 */
package org.cthing.versionparser.debian;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.cthing.annotations.NoCoverageGenerated;
import org.cthing.versionparser.Version;
import org.cthing.versionparser.VersionConstraint;
import org.cthing.versionparser.VersionParsingException;


/**
 * Represents the version scheme used by a Debian package. To parse a version string, call the
 * {@link #parseVersion(String)} method. To parse a version constraint expression, call the
 * {@link #parseConstraint(String)} method.
 */
public final class DebVersionScheme {

    private static final Pattern RANGE_PATTERN = Pattern.compile("^([<>=]*)(.+)$");
    private static final Pattern SPLIT_PATTERN = Pattern.compile("\\s+");

    @NoCoverageGenerated
    private DebVersionScheme() {
    }

    /**
     * Parse a Debian package version string. Debian packages are described in the
     * <a href="https://www.debian.org/doc/debian-policy/">Debian Policy Manual</a>. The format of the package version
     * is defined in
     * <a href="https://www.debian.org/doc/debian-policy/ch-controlfields.html#version">section 5.6.12</a> of that
     * manual. A Debian package version has the following format:
     * <pre>
     *     [epoch:]upstream_version[-debian_revision]
     * </pre>
     * Refer to the policy manual for details on each field. The following are examples of valid Debian package versions:
     * <pre>
     * 22.07.5-2ubuntu1.5
     * 2.3.1-1
     * 3.2.2
     * 0.24
     * 1.0.126+nmu1ubuntu0.7
     * 5:27.3.1-1~ubuntu.22.04~jammy
     * 9.55.0~dfsg1-0ubuntu5.13
     * </pre>
     *
     * @param version Debian package version to parse
     * @return Version object corresponding to the specified version string.
     * @throws VersionParsingException if there was a problem parsing the version
     */
    public static DebVersion parseVersion(final String version) throws VersionParsingException {
        return DebVersion.parse(version);
    }

    /**
     * Parses a Debian package version constraint. The Debian package manager does not define or handle version
     * ranges. However, tools such as Puppet allow version ranges to be specified for package selection. The
     * {@code <}, {@code >}, {@code <=}, {@code >=}, and {@code =} operators are supported. The following are
     * examples of valid version constraints:
     * <pre>
     * 3
     * =3
     * &lt;3
     * &gt;3 &lt;=5
     * &gt;=1.0-1 &lt;2.0-1
     * </pre>
     * Note that {@code 3} and {@code =3} are equivalent.
     *
     * @param rangeString Version constraint to parse
     * @return Version constraint object corresponding to the specified constraint.
     * @throws VersionParsingException if there is a problem parsing the constraint
     */
    public static VersionConstraint parseConstraint(final String rangeString) throws VersionParsingException {
        if (rangeString.isBlank()) {
            return VersionConstraint.ANY;
        }

        final String[] parts = SPLIT_PATTERN.split(rangeString.trim());
        VersionConstraint constraint = null;

        for (final String part : parts) {
            final Matcher rangeMatcher = RANGE_PATTERN.matcher(part);
            if (!rangeMatcher.matches()) {
                throw new VersionParsingException("Unable to parse '" + part + "' as a version range");
            }

            final String operator = rangeMatcher.group(1);
            final Version version = DebVersion.parse(rangeMatcher.group(2));

            final VersionConstraint simpleConstraint = switch (operator) {
                case ">"  -> new VersionConstraint(version, null, false, false);
                case ">=" -> new VersionConstraint(version, null, true, false);
                case "<"  -> new VersionConstraint(null, version, false, false);
                case "<=" -> new VersionConstraint(null, version, false, true);
                case "=", "" -> new VersionConstraint(version);
                default -> throw new VersionParsingException("Operator '" + operator + "' is not supported");
            };

            // If we have multiple parts, we intersect them (Puppet's MinMax logic)
            constraint = (constraint == null) ? simpleConstraint : constraint.intersect(simpleConstraint);
        }

        assert constraint != null;
        return constraint;
    }
}
