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

    /** Represents the range of versions corresponding to Java 1.0. */
    public static final VersionConstraint JAVA_1_0;

    /** Represents the range of versions corresponding to Java 1.1. */
    public static final VersionConstraint JAVA_1_1;

    /** Represents the range of versions corresponding to Java 1.2. */
    public static final VersionConstraint JAVA_1_2;

    /** Represents the range of versions corresponding to Java 1.3. */
    public static final VersionConstraint JAVA_1_3;

    /** Represents the range of versions corresponding to Java 1.4. */
    public static final VersionConstraint JAVA_1_4;

    /** Represents the range of versions corresponding to Java 1.5. */
    public static final VersionConstraint JAVA_1_5;

    /** Represents the range of versions corresponding to Java 1.6. */
    public static final VersionConstraint JAVA_1_6;

    /** Represents the range of versions corresponding to Java 1.7. */
    public static final VersionConstraint JAVA_1_7;

    /** Represents the range of versions corresponding to Java 1.8. */
    public static final VersionConstraint JAVA_1_8;

    /** Represents the range of versions corresponding to Java 9. */
    public static final VersionConstraint JAVA_9;

    /** Represents the range of versions corresponding to Java 10. */
    public static final VersionConstraint JAVA_10;

    /** Represents the range of versions corresponding to Java 11. */
    public static final VersionConstraint JAVA_11;

    /** Represents the range of versions corresponding to Java 12. */
    public static final VersionConstraint JAVA_12;

    /** Represents the range of versions corresponding to Java 13. */
    public static final VersionConstraint JAVA_13;

    /** Represents the range of versions corresponding to Java 14. */
    public static final VersionConstraint JAVA_14;

    /** Represents the range of versions corresponding to Java 15. */
    public static final VersionConstraint JAVA_15;

    /** Represents the range of versions corresponding to Java 16. */
    public static final VersionConstraint JAVA_16;

    /** Represents the range of versions corresponding to Java 17. */
    public static final VersionConstraint JAVA_17;

    /** Represents the range of versions corresponding to Java 18. */
    public static final VersionConstraint JAVA_18;

    /** Represents the range of versions corresponding to Java 19. */
    public static final VersionConstraint JAVA_19;

    /** Represents the range of versions corresponding to Java 20. */
    public static final VersionConstraint JAVA_20;

    /** Represents the range of versions corresponding to Java 21. */
    public static final VersionConstraint JAVA_21;

    /** Represents the range of versions corresponding to Java 22. */
    public static final VersionConstraint JAVA_22;

    /** Represents the range of versions corresponding to Java 23. */
    public static final VersionConstraint JAVA_23;

    /** Represents the range of versions corresponding to Java 24. */
    public static final VersionConstraint JAVA_24;

    /** Represents the range of versions corresponding to Java 25. */
    public static final VersionConstraint JAVA_25;

    /** Represents the range of versions corresponding to Java 26. */
    public static final VersionConstraint JAVA_26;

    /** Represents the range of versions corresponding to Java 27. */
    public static final VersionConstraint JAVA_27;

    static {
        try {
            JAVA_1_0 = parseRange("1.0");
            JAVA_1_1 = parseRange("1.1");
            JAVA_1_2 = parseRange("1.2");
            JAVA_1_3 = parseRange("1.3");
            JAVA_1_4 = parseRange("1.4");
            JAVA_1_5 = parseRange("1.5");
            JAVA_1_6 = parseRange("1.6");
            JAVA_1_7 = parseRange("1.7");
            JAVA_1_8 = parseRange("1.8");
            JAVA_9 = parseRange("9");
            JAVA_10 = parseRange("10");
            JAVA_11 = parseRange("11");
            JAVA_12 = parseRange("12");
            JAVA_13 = parseRange("13");
            JAVA_14 = parseRange("14");
            JAVA_15 = parseRange("15");
            JAVA_16 = parseRange("16");
            JAVA_17 = parseRange("17");
            JAVA_18 = parseRange("18");
            JAVA_19 = parseRange("19");
            JAVA_20 = parseRange("20");
            JAVA_21 = parseRange("21");
            JAVA_22 = parseRange("22");
            JAVA_23 = parseRange("23");
            JAVA_24 = parseRange("24");
            JAVA_25 = parseRange("25");
            JAVA_26 = parseRange("26");
            JAVA_27 = parseRange("27");
        } catch (final VersionParsingException ex) {
            throw new IllegalStateException(ex);
        }
    }

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
            final JavaVersion min = parseVersion(trimmedRange);

            if (trimmedRange.startsWith("1.0")) {
                return new VersionConstraint(min, parseVersion("1.1"), true, false);
            }

            final String maxFeature = Integer.toString(min.getFeature() + 1, 10);
            final JavaVersion max = parseVersion(trimmedRange.startsWith("1.") ? "1." + maxFeature : maxFeature);
            return new VersionConstraint(min, max, true, false);
        }

        String rangeBuffer = trimmedRange;

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

        rangeBuffer = rangeBuffer.substring(1, rangeBuffer.length() - 1);

        final int index = rangeBuffer.indexOf(',');
        if (index < 0) {
            if (!minIncluded || !maxIncluded) {
                throw new VersionParsingException("Invalid version range '" + trimmedRange
                                                          + "', single version must be surrounded by []");
            }

            final String version = rangeBuffer.trim();
            minVersion = JavaVersion.parse(version);
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

            if (maxVersion != null && minVersion != null) {
                if (maxVersion.compareTo(minVersion) < 0) {
                    throw new VersionParsingException("Invalid version range '" + trimmedRange
                                                              + "', lower bound must not be greater than upper bound");
                }
            }
        }

        return new VersionConstraint(minVersion, maxVersion, minIncluded, maxIncluded);
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
        return isVersion(expectedVersion, parseVersion(version));
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
