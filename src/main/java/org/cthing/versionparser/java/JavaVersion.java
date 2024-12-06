/*
 * Copyright 2024 C Thing Software
 * SPDX-License-Identifier: Apache-2.0
 */

package org.cthing.versionparser.java;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.cthing.annotations.AccessForTesting;
import org.cthing.versionparser.AbstractVersion;
import org.cthing.versionparser.Version;
import org.cthing.versionparser.VersionParsingException;


/**
 * Represents a version of the Java language. This class is based on the {@link Runtime.Version} class and adds
 * the ability to represent older versions of Java (8 and older), which follow version schemes than that supported
 * by the {@link Runtime.Version} class. In addition to the <a href="https://openjdk.org/jeps/322">JEP 322</a> format,
 * the following examples illustrate the older formats that are supported:
 * <ul>
 *     <li>8u17</li>
 *     <li>5.0u10</li>
 *     <li>1.4</li>
 *     <li>1.4.2</li>
 *     <li>1.4.2_151</li>
 *     <li>1.4.2_151-b034</li>
 *     <li>1.4.2_151-internal-b034</li>
 * </ul>
 *
 * <p>
 * An instance of {@link JavaVersion} representing the runtime Java version is available as {@link #RUNTIME_VERSION}.
 * </p>
 */
public final class JavaVersion extends AbstractVersion {

    /** Java language version of the Java runtime executing this class. */
    public static final JavaVersion RUNTIME_VERSION;
    static {
        final Runtime.Version version = Runtime.version();
        RUNTIME_VERSION = new JavaVersion(version.toString(), version);
    }

    private static final Pattern OLD_FORMAT_REGEX = Pattern.compile("^1\\.\\d+|\\d+_\\d+|\\d+u\\d+|-b\\d+");
    private static final Pattern OLD_VERSION_1_REGEX =
            Pattern.compile("^1\\.(?<feature>\\d+)(?:\\.(?<interim>\\d+))?(?:_(?<update>\\d+))?-(?<opt>.+)[\\-+]b(?<build>\\d+)");
    private static final Pattern OLD_VERSION_2_REGEX =
            Pattern.compile("^1\\.(?<feature>\\d+)(?:\\.(?<interim>\\d+))?(?:_(?<update>\\d+))?(?:[\\-+]b(?<build>\\d+))?(?:-(?<opt>.+))?");
    private static final Pattern OLD_VERSION_3_REGEX =
            Pattern.compile("^(?<feature>\\d+)(?:\\.(?<interim>\\d+))?u(?<update>\\d+)");


    private final Runtime.Version javaVersion;

    private JavaVersion(final String originalVersion, final Runtime.Version javaVersion) {
        super(originalVersion);

        this.javaVersion = javaVersion;
    }

    /**
     * Parses the specified version string and returns a new instance of this class. To parse a version, call
     * {@link JavaVersionScheme#parseVersion(String)}.
     *
     * @param version Version string to parse
     * @return Version object
     */
    static JavaVersion parse(final String version) throws VersionParsingException {
        final String trimmedVersion = version.trim();

        final String canonicalVersion = OLD_FORMAT_REGEX.matcher(trimmedVersion).find()
                                        ? canonicalize(trimmedVersion)
                                        : trimmedVersion;

        try {
            return new JavaVersion(trimmedVersion, Runtime.Version.parse(canonicalVersion));
        } catch (final IllegalArgumentException ex) {
            throw new VersionParsingException("Invalid Java version", ex);
        }
    }

    @Override
    public boolean isPreRelease() {
        return this.javaVersion.pre().isPresent();
    }

    /**
     * Obtains the build number, if present.
     *
     * @return Build number
     * @see Runtime.Version#build()
     */
    public Optional<Integer> getBuild() {
        return this.javaVersion.build();
    }

    /**
     * Obtains the feature component of the version.
     *
     * @return Feature component of the version.
     * @see Runtime.Version#feature()
     */
    public int getFeature() {
        return this.javaVersion.feature();
    }

    /**
     * Obtains the interim component of the version.
     *
     * @return Interim component of the version.
     * @see Runtime.Version#interim()
     */
    public int getInterim() {
        return this.javaVersion.interim();
    }

    /**
     * Obtains the optional portion of the version, if present.
     *
     * @return Optional portion of the version.
     * @see Runtime.Version#optional()
     */
    public Optional<String> getOptional() {
        return this.javaVersion.optional();
    }

    /**
     * Obtains the patch portion of the version.
     *
     * @return Patch portion of the version.
     * @see Runtime.Version#patch()
     */
    public int getPatch() {
        return this.javaVersion.patch();
    }

    /**
     * Obtains the pre-release portion of the version, if present.
     *
     * @return Pre-release portion of the version.
     * @see Runtime.Version#pre()
     */
    public Optional<String> getPre() {
        return this.javaVersion.pre();
    }

    /**
     * Obtains the update portion of the version.
     *
     * @return Update portion of the version.
     * @see Runtime.Version#update()
     */
    public int getUpdate() {
        return this.javaVersion.update();
    }

    /**
     * Obtains the version number components (i.e. feature, interim, update, patch). Only the components
     * actually specified are in the returned list. For example, if the version is specified as 11, the
     * returned list only contains [11]. If the version is specified as 11.1, the list contains [11, 1].
     *
     * @return Version number components.
     */
    public List<Integer> getComponents() {
        return this.javaVersion.version();
    }

    /**
     * Converts old Java version schemes to the <a href="https://openjdk.org/jeps/322">JEP 322</a> format expected
     * by {@link Runtime.Version#parse(String)}. For example:
     * <ul>
     *     <li>1.4 -&gt; 4</li>
     *     <li>1.4.2 -&gt; 4.2</li>
     *     <li>1.4.2_20 -&gt; 4.2.20</li>
     *     <li>1.4.2_10-b02 -&gt; 4.2.10+2</li>
     *     <li>1.5.0_10-foo -&gt; 5.0.10-foo</li>
     *     <li>5.0u16 -&gt; 5.0.16</li>
     *     <li>8u17 -&gt; 8.0.17</li>
     * </ul>
     *
     * @param version Version string to canonicalize
     * @return Version string compatible with {@link Runtime.Version#parse(String)}.
     */
    @AccessForTesting
    static String canonicalize(final String version) {
        Matcher matcher = OLD_VERSION_1_REGEX.matcher(version);
        boolean matchFound = matcher.matches();
        if (!matchFound) {
            matcher = OLD_VERSION_2_REGEX.matcher(version);
            matchFound = matcher.matches();
        }
        if (matchFound) {
            final StringBuilder canonicalVersion = new StringBuilder();

            int feature = Integer.parseInt(matcher.group("feature"));
            if (feature == 0) {
                feature = 1;
            }
            canonicalVersion.append(feature);

            final String interimStr = matcher.group("interim");
            if (interimStr != null) {
                canonicalVersion.append('.').append(Integer.parseInt(interimStr));
            }

            final String updateStr = matcher.group("update");
            if (updateStr != null) {
                canonicalVersion.append('.').append(Integer.parseInt(updateStr));
            }

            final String buildStr = matcher.group("build");
            if (buildStr != null) {
                canonicalVersion.append('+').append(Integer.parseInt(buildStr));
            }

            final String opt = matcher.group("opt");
            if (opt != null) {
                canonicalVersion.append('-').append(opt);
            }

            return canonicalVersion.toString();
        }

        matcher = OLD_VERSION_3_REGEX.matcher(version);
        if (!matcher.matches()) {
            return version;
        }

        final StringBuilder canonicalVersion = new StringBuilder();

        canonicalVersion.append(Integer.parseInt(matcher.group("feature")));

        final String interimStr = matcher.group("interim");
        if (interimStr == null) {
            canonicalVersion.append(".0");
        } else {
            canonicalVersion.append('.').append(Integer.parseInt(interimStr));
        }

        return canonicalVersion.append('.').append(Integer.parseInt(matcher.group("update"))).toString();
    }

    @Override
    public int compareTo(final Version other) {
        if (getClass() != other.getClass()) {
            throw new IllegalArgumentException("Expected instance of JavaVersion but received "
                                                       + other.getClass().getName());
        }

        return this.javaVersion.compareTo(((JavaVersion)other).javaVersion);
    }

    @Override
    public boolean equals(final Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }

        return Objects.equals(this.javaVersion, ((JavaVersion)obj).javaVersion);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(this.javaVersion);
    }
}
