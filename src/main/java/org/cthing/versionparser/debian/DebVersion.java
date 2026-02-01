/*
 * Copyright 2026 C Thing Software
 * SPDX-License-Identifier: Apache-2.0
 */
package org.cthing.versionparser.debian;

import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.cthing.versionparser.AbstractVersion;
import org.cthing.versionparser.Version;
import org.cthing.versionparser.VersionParsingException;


/**
 * Represents the version of a Debian package. Debian packages are described in the
 * <a href="https://www.debian.org/doc/debian-policy/">Debian Policy Manual</a>. The format of the package version
 * is defined in <a href="https://www.debian.org/doc/debian-policy/ch-controlfields.html#version">section 5.6.12</a>
 * of that manual. A Debian package version has the following format:
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
 */
public final class DebVersion extends AbstractVersion {

    // Validates [epoch:]upstream_version[-debian_revision]
    private static final Pattern VERSION_PATTERN = Pattern.compile("^(?:([0-9]+):)?([.+~0-9a-zA-Z-]+)$");

    private final int epoch;
    private final String upstream;
    private final String revision;

    /**
     * Constructs a Debian package version.
     *
     * @param version Original version string
     * @param epoch Epoch component
     * @param upstream Upstream version component
     * @param revision Revision component
     */
    private DebVersion(final String version, final int epoch, final String upstream, final String revision) {
        super(version);

        this.epoch = epoch;
        this.upstream = upstream;
        this.revision = revision;
    }

    /**
     * Parses the specified version string and returns a new instance of this class. To parse a version, call
     * {@link DebVersionScheme#parseVersion(String)}.
     *
     * @param version Version string to parse
     * @return Version object
     */
    static DebVersion parse(final String version) throws VersionParsingException {
        final String trimmedVersion = version.trim();
        final Matcher matcher = VERSION_PATTERN.matcher(trimmedVersion);

        if (!matcher.matches()) {
            throw new VersionParsingException("Invalid Debian version: " + trimmedVersion);
        }

        final String epochStr = matcher.group(1);
        final int epoch = (epochStr != null) ? Integer.parseInt(epochStr, 10) : 0;
        final String remainder = matcher.group(2);

        final String upstream;
        final String revision;

        final int lastHyphen = remainder.lastIndexOf('-');

        if (lastHyphen != -1) {
            upstream = remainder.substring(0, lastHyphen);
            revision = remainder.substring(lastHyphen + 1);
        } else {
            upstream = remainder;
            revision = "0";
        }

        if (upstream.isEmpty() || upstream.startsWith("-") || upstream.endsWith("-") || revision.isEmpty()) {
            throw new VersionParsingException("Invalid Debian version: " + trimmedVersion);
        }

        return new DebVersion(trimmedVersion, epoch, upstream, revision);
    }

    /**
     * Obtains the package version epoch, which is used to indicate changes to version numbering schemes.
     *
     * @return Package version scheme epoch
     */
    public int getEpoch() {
        return this.epoch;
    }

    /**
     * Obtains the version number of the original (“upstream”) package from which the Debian package has been made.
     *
     * @return Original package's version number
     */
    public String getUpstream() {
        return this.upstream;
    }

    /**
     * Obtains the version of the Debian package based on the upstream version.
     *
     * @return Debian version of the package
     */
    public String getRevision() {
        return this.revision;
    }

    @Override
    public boolean isPreRelease() {
        // A pre-release in the Debian world is defined by a tilde in the upstream version.
        // That tilde results in the version being sorted before a stable release. Tildes in
        // the revision (like ~22.04) indicate backports or distro-patches and are not
        // pre-release versions.
        final int tildeIdx = this.upstream.indexOf('~');
        if (tildeIdx <= 0) {
            return false;
        }

        // Compare upstream "1.2.3~rc1" against base "1.2.3"
        final String baseUpstream = this.upstream.substring(0, tildeIdx);
        return compareComponent(this.upstream, baseUpstream) < 0;
    }

    @Override
    public int compareTo(final Version obj) {
        if (getClass() != obj.getClass()) {
            throw new IllegalArgumentException("Expected instance of DebVersion but received "
                                                       + obj.getClass().getName());
        }

        final DebVersion other = (DebVersion)obj;

        int cmp = Integer.compare(this.epoch, other.epoch);
        if (cmp != 0) {
            return cmp;
        }

        cmp = compareComponent(this.upstream, other.upstream);
        if (cmp != 0) {
            return cmp;
        }

        return compareComponent(this.revision, other.revision);
    }

    /**
     * Compares two components of the version string.
     *
     * @param component1 First component to compare
     * @param component2 Second component to compare
     * @return Negative if first component evaluates to less than the second. Positive if the first component
     *      evaluates to greater than the second. Zero if both components are equal.
     */
    private int compareComponent(final String component1, final String component2) {
        int idx1 = 0;
        int idx2 = 0;
        final int len1 = component1.length();
        final int len2 = component2.length();

        while (idx1 < len1 || idx2 < len2) {
            // Compare non-digit parts character-by-character
            while ((idx1 < len1 && !isAsciiDigit(component1.charAt(idx1)))
                    || (idx2 < len2 && !isAsciiDigit(component2.charAt(idx2)))) {

                final int ch1 = idx1 < len1 ? component1.charAt(idx1) : 0;
                final int ch2 = idx2 < len2 ? component2.charAt(idx2) : 0;

                if (ch1 != ch2) {
                    // Tilde Rule: ~ is the absolute lowest character
                    if (ch1 == '~') {
                        return -1;
                    }
                    if (ch2 == '~') {
                        return 1;
                    }

                    // End of String Rule: If a segment ends, it is smaller than non-tilde
                    if (ch1 == 0) {
                        return -1;
                    }
                    if (ch2 == 0) {
                        return 1;
                    }

                    // Character Priority: Letters (a-z) < Punctuation (+ . -)
                    return Integer.compare(getWeight(ch1), getWeight(ch2));
                }

                idx1++;
                idx2++;
            }

            // Compare digit parts numerically
            final int start1 = idx1;
            while (idx1 < len1 && isAsciiDigit(component1.charAt(idx1))) {
                idx1++;
            }

            final int start2 = idx2;
            while (idx2 < len2 && isAsciiDigit(component2.charAt(idx2))) {
                idx2++;
            }

            final int digitCmp = compareNumericSegments(component1, start1, idx1, component2, start2, idx2);
            if (digitCmp != 0) {
                return digitCmp;
            }
        }

        return 0;
    }

    /**
     * Indicates if the specified character is an ASCII digit. The {@link Character#isDigit(char)} is not used
     * because it considers higher Unicode digit characters.
     *
     * @param ch Character to test
     * @return {@code true} if the specified character is an ASCII digit.
     */
    private boolean isAsciiDigit(final int ch) {
        return ch >= '0' && ch <= '9';
    }

    /**
     * Determines a sorting weight for the specified character. Ensures that punctation characters sort after
     * letters.
     *
     * @param ch Character to test
     * @return Weighting for sorting the specified character.
     */
    private int getWeight(final int ch) {
        // Punctuation must sort after letters.
        return ((ch >= 'a' && ch <= 'z') || (ch >= 'A' && ch <= 'Z')) ? ch : ch + 256;
    }

    /**
     * Compares numeric portions of the version.
     *
     * @param string1 First string containing digits
     * @param start1 Starting offset into the string (inclusive)
     * @param end1 Ending offset into the string (exclusive)
     * @param string2 Second string containing digits
     * @param start2 Starting offset into the string (inclusive)
     * @param end2 Ending offset into the string (exclusive)
     * @return Negative if first string is numerically less than the second. Positive if the first string
     *      is numerically greater than the second. Zero if both strings are numerically equal.
     */
    private int compareNumericSegments(final String string1, final int start1, final int end1,
                                       final String string2, final int start2, final int end2) {
        // Strip leading zeros for length checking
        int effectiveStart1 = start1;
        while (effectiveStart1 < end1 && string1.charAt(effectiveStart1) == '0') {
            effectiveStart1++;
        }
        final int len1 = end1 - effectiveStart1;

        int effectiveStart2 = start2;
        while (effectiveStart2 < end2 && string2.charAt(effectiveStart2) == '0') {
            effectiveStart2++;
        }
        final int len2 = end2 - effectiveStart2;

        // If both are empty (meaning both were 0 or empty), they are equal
        if (len1 == 0 && len2 == 0) {
            return 0;
        }
        if (len1 == 0) {
            return -1;      // string1 was 0, string2 was > 0
        }
        if (len2 == 0) {
            return 1;       // string1 was > 0, string2 was 0
        }

        // Longer string (without leading zeros) is always larger
        if (len1 > len2) {
            return 1;
        }
        if (len1 < len2) {
            return -1;
        }

        // At this point the string lengths are equal. If lengths < 18, they fit in a Long.
        if (len1 < 18) {
            final long n1 = Long.parseLong(string1, effectiveStart1, end1, 10);
            final long n2 = Long.parseLong(string2, effectiveStart2, end2, 10);
            return Long.compare(n1, n2);
        }

        // If the strings would overflow a Long, handle them using lexicographical comparison. Because the
        // strings are the same length, lexicographic comparison matches numeric comparison (e.g. "500" > "100"
        // and "5" > "1").
        for (int i = 0; i < len1; i++) {
            final int c1 = string1.charAt(effectiveStart1 + i);
            final int c2 = string2.charAt(effectiveStart2 + i);
            if (c1 != c2) {
                return c1 - c2;
            }
        }

        return 0;
    }

    @Override
    public boolean equals(final Object obj) {
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }

        final DebVersion other = (DebVersion)obj;
        return this.epoch == other.epoch
                && Objects.equals(this.upstream, other.upstream)
                && Objects.equals(this.revision, other.revision);
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.epoch, this.upstream, this.revision);
    }
}
