/*
 * Copyright 2024 C Thing Software
 * SPDX-License-Identifier: Apache-2.0
 */

package org.cthing.versionparser.java;

import org.cthing.annotations.NoCoverageGenerated;
import org.cthing.versionparser.VersionConstraint;
import org.cthing.versionparser.VersionParsingException;


/**
 * Represents the version scheme used by the Java language. To parse a version string, call the
 * {@link #parseVersion(String)} method. To create a version constraint representing a contiguous
 * range of Java versions, call the {@link #parseRange(String)} method.
 */
public final class JavaVersionScheme {

    /** Any release of Java 1.0. */
    public static final VersionConstraint JAVA_1_0 = safeParseRange("1.0");

    /** Any release of Java 1.1. */
    public static final VersionConstraint JAVA_1_1 = safeParseRange("1.1");

    /** Any release of Java 1.2. */
    public static final VersionConstraint JAVA_1_2 = safeParseRange("1.2");

    /** Any release of Java 1.3. */
    public static final VersionConstraint JAVA_1_3 = safeParseRange("1.3");

    /** Any release of Java 1.4. */
    public static final VersionConstraint JAVA_1_4 = safeParseRange("1.4");

    /** Any release of Java 1.5. */
    public static final VersionConstraint JAVA_1_5 = safeParseRange("1.5");

    /** Any release of Java 1.6. */
    public static final VersionConstraint JAVA_1_6 = safeParseRange("1.6");

    /** Any release of Java 1.7. */
    public static final VersionConstraint JAVA_1_7 = safeParseRange("1.7");

    /** Any release of Java 1.8. */
    public static final VersionConstraint JAVA_1_8 = safeParseRange("1.8");

    /** Any release of Java 9. */
    public static final VersionConstraint JAVA_9 = safeParseRange("9");

    /** Any release of Java 10. */
    public static final VersionConstraint JAVA_10 = safeParseRange("10");

    /** Any release of Java 11. */
    public static final VersionConstraint JAVA_11 = safeParseRange("11");

    /** Any release of Java 12. */
    public static final VersionConstraint JAVA_12 = safeParseRange("12");

    /** Any release of Java 13. */
    public static final VersionConstraint JAVA_13 = safeParseRange("13");

    /** Any release of Java 14. */
    public static final VersionConstraint JAVA_14 = safeParseRange("14");

    /** Any release of Java 15. */
    public static final VersionConstraint JAVA_15 = safeParseRange("15");

    /** Any release of Java 16. */
    public static final VersionConstraint JAVA_16 = safeParseRange("16");

    /** Any release of Java 17. */
    public static final VersionConstraint JAVA_17 = safeParseRange("17");

    /** Any release of Java 18. */
    public static final VersionConstraint JAVA_18 = safeParseRange("18");

    /** Any release of Java 19. */
    public static final VersionConstraint JAVA_19 = safeParseRange("19");

    /** Any release of Java 20. */
    public static final VersionConstraint JAVA_20 = safeParseRange("20");

    /** Any release of Java 21. */
    public static final VersionConstraint JAVA_21 = safeParseRange("21");

    /** Any release of Java 22. */
    public static final VersionConstraint JAVA_22 = safeParseRange("22");

    /** Any release of Java 23. */
    public static final VersionConstraint JAVA_23 = safeParseRange("23");

    /** Any release of Java 24. */
    public static final VersionConstraint JAVA_24 = safeParseRange("24");

    /** Any release of Java 25. */
    public static final VersionConstraint JAVA_25 = safeParseRange("25");

    /** Any release of Java 26. */
    public static final VersionConstraint JAVA_26 = safeParseRange("26");

    /** Any release of Java 27. */
    public static final VersionConstraint JAVA_27 = safeParseRange("27");


    /** Any release of Java 1.2 or greater. */
    public static final VersionConstraint JAVA_1_2_PLUS = safeParseRange("[1.2,)");

    /** Any release of Java 1.3 or greater. */
    public static final VersionConstraint JAVA_1_3_PLUS = safeParseRange("[1.3,)");

    /** Any release of Java 1.4 or greater. */
    public static final VersionConstraint JAVA_1_4_PLUS = safeParseRange("[1.4,)");

    /** Any release of Java 1.5 or greater. */
    public static final VersionConstraint JAVA_1_5_PLUS = safeParseRange("[1.5,)");

    /** Any release of Java 1.6 or greater. */
    public static final VersionConstraint JAVA_1_6_PLUS = safeParseRange("[1.6,)");

    /** Any release of Java 1.7 or greater. */
    public static final VersionConstraint JAVA_1_7_PLUS = safeParseRange("[1.7,)");

    /** Any release of Java 1.8 or greater. */
    public static final VersionConstraint JAVA_1_8_PLUS = safeParseRange("[1.8,)");

    /** Any release of Java 9 or greater. */
    public static final VersionConstraint JAVA_9_PLUS = safeParseRange("[9,)");

    /** Any release of Java 10 or greater. */
    public static final VersionConstraint JAVA_10_PLUS = safeParseRange("[10,)");

    /** Any release of Java 11 or greater. */
    public static final VersionConstraint JAVA_11_PLUS = safeParseRange("[11,)");

    /** Any release of Java 12 or greater. */
    public static final VersionConstraint JAVA_12_PLUS = safeParseRange("[12,)");

    /** Any release of Java 13 or greater. */
    public static final VersionConstraint JAVA_13_PLUS = safeParseRange("[13,)");

    /** Any release of Java 14 or greater. */
    public static final VersionConstraint JAVA_14_PLUS = safeParseRange("[14,)");

    /** Any release of Java 15 or greater. */
    public static final VersionConstraint JAVA_15_PLUS = safeParseRange("[15,)");

    /** Any release of Java 16 or greater. */
    public static final VersionConstraint JAVA_16_PLUS = safeParseRange("[16,)");

    /** Any release of Java 17 or greater. */
    public static final VersionConstraint JAVA_17_PLUS = safeParseRange("[17,)");

    /** Any release of Java 18 or greater. */
    public static final VersionConstraint JAVA_18_PLUS = safeParseRange("[18,)");

    /** Any release of Java 19 or greater. */
    public static final VersionConstraint JAVA_19_PLUS = safeParseRange("[19,)");

    /** Any release of Java 20 or greater. */
    public static final VersionConstraint JAVA_20_PLUS = safeParseRange("[20,)");

    /** Any release of Java 21 or greater. */
    public static final VersionConstraint JAVA_21_PLUS = safeParseRange("[21,)");

    /** Any release of Java 22 or greater. */
    public static final VersionConstraint JAVA_22_PLUS = safeParseRange("[22,)");

    /** Any release of Java 23 or greater. */
    public static final VersionConstraint JAVA_23_PLUS = safeParseRange("[23,)");

    /** Any release of Java 24 or greater. */
    public static final VersionConstraint JAVA_24_PLUS = safeParseRange("[24,)");

    /** Any release of Java 25 or greater. */
    public static final VersionConstraint JAVA_25_PLUS = safeParseRange("[25,)");

    /** Any release of Java 26 or greater. */
    public static final VersionConstraint JAVA_26_PLUS = safeParseRange("[26,)");

    /** Any release of Java 27 or greater. */
    public static final VersionConstraint JAVA_27_PLUS = safeParseRange("[27,)");


    @NoCoverageGenerated
    private JavaVersionScheme() {
    }

    /**
     * Parses the specified version string and returns a new instance of this class. The string must conform to
     * the <a href="https://openjdk.org/jeps/322">JEP 322</a> format expected by {@link Runtime.Version}. In addition,
     * old Java language version formats are supported. For example:
     * <ul>
     *     <li>11</li>
     *     <li>11.0.33</li>
     *     <li>17.0.33-alpha+14-cthing</li>
     *     <li>17.0.33.2-alpha+14-cthing</li>
     *     <li>8u17</li>
     *     <li>5.0u10</li>
     *     <li>1.4</li>
     *     <li>1.4.2</li>
     *     <li>1.4.2_151</li>
     *     <li>1.4.2_151-b034</li>
     *     <li>1.4.2_151-internal-b034</li>
     * </ul>
     *
     * @param version Version string to parse
     * @return Version object
     * @throws VersionParsingException if there was a problem parsing the version
     */
    public static JavaVersion parseVersion(final String version) throws VersionParsingException {
        return JavaVersion.parse(version);
    }

    /**
     * Parses a Java language version range. The following examples illustrate valid version ranges:
     * <ul>
     *     <li>17: Equivalent to [17,18). Any version of Java 17</li>
     *     <li>[17.0.11]: Only version 17.0.11</li>
     *     <li>[17,21): Any version of Java 17, 18, 19, or 20</li>
     *     <li>(17,21]: Any version of Java 18, 19, 20, or 21</li>
     *     <li>[17,): Any version of Java 17 or greater</li>
     *     <li>(17,): Any version greater than Java 17</li>
     *     <li>(,): Any version of Java</li>
     * </ul>
     *
     * @param versionRange Version range
     * @return Version range object
     * @throws VersionParsingException if there was a problem parsing the version range
     */
    public static VersionConstraint parseRange(final String versionRange) throws VersionParsingException {
        final String trimmedRange = versionRange.trim();

        // A standalone version is treated as a range from that version inclusive to the
        // next feature version exclusive. Accommodations are made for old versions.
        if (!trimmedRange.startsWith("[") && !trimmedRange.startsWith("(")) {
            final JavaVersion min = JavaVersion.parse(trimmedRange);

            if (trimmedRange.startsWith("1.0")) {
                return new VersionConstraint(min, JavaVersion.parse("1.1"), true, false);
            }

            final String maxFeature = Integer.toString(min.getFeature() + 1, 10);
            final JavaVersion max = JavaVersion.parse(trimmedRange.startsWith("1.") ? "1." + maxFeature : maxFeature);
            return new VersionConstraint(min, max, true, false);
        }

        final boolean minIncluded;
        final boolean maxIncluded;

        minIncluded = trimmedRange.startsWith("[");

        if (trimmedRange.endsWith("]")) {
            maxIncluded = true;
        } else if (trimmedRange.endsWith(")")) {
            maxIncluded = false;
        } else {
            throw new VersionParsingException("Invalid version range '" + trimmedRange
                                                      + "', a range must end with either ] or )");
        }

        final JavaVersion minVersion;
        final JavaVersion maxVersion;

        final String rangeBuffer = trimmedRange.substring(1, trimmedRange.length() - 1);

        final int index = rangeBuffer.indexOf(',');
        if (index < 0) {
            if (!minIncluded || !maxIncluded) {
                throw new VersionParsingException("Invalid version range '" + trimmedRange
                                                          + "', single version must be surrounded by []");
            }

            minVersion = JavaVersion.parse(rangeBuffer);
            maxVersion = minVersion;
        } else {
            final String minVersionStr = rangeBuffer.substring(0, index).trim();
            final String maxVersionStr = rangeBuffer.substring(index + 1).trim();

            // More than two bounds, e.g. (1,2,3)
            if (maxVersionStr.contains(",")) {
                throw new VersionParsingException("Invalid version range '" + trimmedRange
                                                          + "', bounds may not contain additional ','");
            }

            minVersion = minVersionStr.isEmpty() ? null : JavaVersion.parse(minVersionStr);
            maxVersion = maxVersionStr.isEmpty() ? null : JavaVersion.parse(maxVersionStr);

            if (maxVersion != null && minVersion != null && maxVersion.compareTo(minVersion) < 0) {
                throw new VersionParsingException("Invalid version range '" + trimmedRange
                                                          + "', lower bound must not be greater than upper bound");
            }
        }

        return new VersionConstraint(minVersion, maxVersion, minIncluded, maxIncluded);
    }

    /**
     * Creates a version constraint without throwing a checked exception. For use by the constant ranges (e.g. JAVA_9).
     *
     * @param versionRange Version range
     * @return Version range object
     * @see #parseRange(String)
     */
    private static VersionConstraint safeParseRange(final String versionRange) {
        try {
            return parseRange(versionRange);
        } catch (final VersionParsingException ex) {
            throw new IllegalStateException(ex);
        }
    }

    /**
     * Indicates whether the specified version corresponds to the specified Java language version. For example,
     * <pre>
     * assertThat(JavaVersionScheme.isVersion(JavaVersionScheme.JAVA_17, "17").isTrue();
     * assertThat(JavaVersionScheme.isVersion(JavaVersionScheme.JAVA_17, "17.0.11").isTrue();
     * assertThat(JavaVersionScheme.isVersion(JavaVersionScheme.JAVA_17, "21").isFalse();
     * </pre>
     *
     * @param expectedVersion Version of the Java language to test against
     * @param version Version to test
     * @return {@code true} if the specified version corresponds to the expected version of Java
     * @throws VersionParsingException if there was a problem parsing the specified version
     */
    public static boolean isVersion(final VersionConstraint expectedVersion, final String version)
            throws VersionParsingException {
        return isVersion(expectedVersion, JavaVersion.parse(version));
    }

    /**
     * Indicates whether the specified version corresponds to the specified Java language version.
     *
     * @param expectedVersion Version of the Java language to test against
     * @param version Version to test
     * @return {@code true} if the specified version corresponds to the expected version of Java
     */
    public static boolean isVersion(final VersionConstraint expectedVersion, final JavaVersion version) {
        return expectedVersion.allows(version);
    }
}
