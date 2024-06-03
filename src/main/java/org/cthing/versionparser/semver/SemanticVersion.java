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

package org.cthing.versionparser.semver;

import java.math.BigInteger;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.annotation.Nullable;

import org.cthing.versionparser.AbstractVersion;
import org.cthing.versionparser.Version;
import org.cthing.versionparser.VersionParsingException;


/**
 * Represents an artifact version adhering to the <a href="https://semver.org/">Semantic Versioning 2.0</a>
 * specification. To create an instance of this class, call the {@link #parse(String)} method.
 * <p>
 * Note that the natural ordering of this class is <b>not</b> consistent with {@link #equals(Object)}
 * due to the build metadata being ignored when version precedence is determined but considered for equality.
 * </p>
 */
@SuppressWarnings("RegExpRedundantEscape")
public final class SemanticVersion extends AbstractVersion {

    /** Minimum version of an artifact. */
    public static final SemanticVersion ZERO = new SemanticVersion("0.0.0", 0, 0, 0, List.of(), List.of());

    /** Represents a numeric identifier in a semantic version. */
    public static final String NUMERIC_IDENTIFIER = "0|[1-9]\\d*";

    private static final String NON_NUMERIC_IDENTIFIER = "\\d*[a-zA-Z-][a-zA-Z0-9-]*";
    private static final String VERSION_CORE = String.format(Locale.ROOT, "(%s)\\.(%s)\\.(%s)", NUMERIC_IDENTIFIER,
                                                             NUMERIC_IDENTIFIER, NUMERIC_IDENTIFIER);
    private static final String PRERELEASE_IDENTIFIER = String.format(Locale.ROOT, "(?:%s|%s)", NUMERIC_IDENTIFIER,
                                                                      NON_NUMERIC_IDENTIFIER);

    /** Represents the optional pre-release portion of a semantic version. */
    public static final String PRERELEASE = String.format(Locale.ROOT, "(?:-(%s(?:\\.%s)*))", PRERELEASE_IDENTIFIER,
                                                           PRERELEASE_IDENTIFIER);
    private static final String BUILD_IDENTIFIER = "[0-9A-Za-z-]+";

    /** Represents the optional build metadata portion of a semantic version. */
    public static final String BUILD = String.format(Locale.ROOT, "(?:\\+(%s(?:\\.%s)*))", BUILD_IDENTIFIER,
                                                      BUILD_IDENTIFIER);

    /** Represents a semantic version. */
    public static final String SEMVER = String.format(Locale.ROOT, "v?%s%s?%s?", VERSION_CORE, PRERELEASE, BUILD);

    private static final Pattern SEMVER_PATTERN = Pattern.compile("^" + SEMVER + "$");

    private static final Pattern SEPARATOR_PATTERN = Pattern.compile("\\.");

    private static final Pattern HAS_DIGITS_PATTERN = Pattern.compile(".*\\d.*");
    private static final String EXTRACT_DIGITS = "(?<=\\D)(?=\\d)";

    private static final BigInteger MAX_INT = BigInteger.valueOf(Integer.MAX_VALUE);
    private static final String UNDEFINED_MARKER = "uNdeF";

    private final int major;
    private final int minor;
    private final int patch;
    private final List<String> preRelease;
    private final List<String> build;
    private final String coreVersion;
    private final String normalizedVersion;

    /**
     * Creates a version instance.
     *
     * @param version Original version string
     * @param major Major version
     * @param minor Minor version
     * @param patch Patch version
     * @param preRelease Pre-release identifiers or an empty array
     * @param build Build identifier or the empty string
     */
    private SemanticVersion(final String version, final int major, final int minor, final int patch,
                            final List<String> preRelease, final List<String> build) {
        super(version);

        this.major = major;
        this.minor = minor;
        this.patch = patch;
        this.preRelease = preRelease;
        this.build = build;

        final StringBuilder buffer = new StringBuilder();
        buffer.append(this.major).append('.').append(this.minor).append('.').append(this.patch);
        this.coreVersion = buffer.toString();

        if (!this.preRelease.isEmpty()) {
            buffer.append('-').append(String.join(".", this.preRelease));
        }
        if (!this.build.isEmpty()) {
            buffer.append('+').append(String.join(".", this.build));
        }
        this.normalizedVersion = buffer.toString();
    }

    /**
     * Parses the specified version string and returns a new instance of this class.
     *
     * @param version Version string to parse
     * @return Semantic version object
     * @throws VersionParsingException if there is a problem parsing the version
     */
    public static SemanticVersion parse(final String version) throws VersionParsingException {
        final String trimmedVersion = version.trim();

        final Matcher semverMatcher = SEMVER_PATTERN.matcher(trimmedVersion);
        if (!semverMatcher.matches()) {
            throw new VersionParsingException("Invalid semantic version: " + trimmedVersion);
        }

        final int major = parseInt(semverMatcher.group(1));
        final int minor = parseInt(semverMatcher.group(2));
        final int patch = parseInt(semverMatcher.group(3));
        final List<String> preRelease = toList(semverMatcher.group(4));
        final List<String> build = toList(semverMatcher.group(5));

        return new SemanticVersion(trimmedVersion, major, minor, patch, preRelease, build);
    }

    /**
     * Constructs a semantic version from the specified core version (i.e. major.minor.patch) and the specified
     * pre-release identifier. Calling this method is equivalent to calling the {@link #parse(String)} method
     * with a string in the format {@code major.minor.patch-preReleaseIdentifier}.
     *
     * @param coreVersion Core version in the format {@code major.minor.patch}
     * @param preReleaseIdentifier Pre-release portion of the semantic version. If a blank string is specified,
     *      no pre-release identifier will be added to the returned semantic version.
     * @return Semantic version object
     * @throws VersionParsingException if there is a problem parsing the version
     */
    @SuppressWarnings("ParameterHidesMemberVariable")
    public static SemanticVersion parse(final String coreVersion, final String preReleaseIdentifier)
            throws VersionParsingException {
        return parse(preReleaseIdentifier.isBlank()
                     ? coreVersion
                     : coreVersion.trim() + "-" + preReleaseIdentifier.trim());
    }

    /**
     * Constructs a semantic version from the specified core version (i.e. major.minor.patch) and a flag indicating
     * whether the version represents a release or snapshot. If {@code snapshot} is {@code false}, a semantic
     * version is constructed using only the core version. If {@code snapshot} is {@code true}, a pre-release
     * identifier is appended to the core version. The pre-release identifier is the number of milliseconds since
     * the Unix Epoch. Calling this method is equivalent to calling the {@link #parse(String)} method with a string
     * in the format {@code major.minor.patch-preReleaseIdentifier} where {@code preReleaseIdentifier} is equal to
     * {@code new Date().getTime()}.
     *
     * @param coreVersion Core version in the format {@code major.minor.patch}
     * @param snapshot If {@code true}, a pre-release identifier equal to the current Unix time in milliseconds
     *      is appended to the core version. If {@code false}, no pre-release identifier is appended to the core
     *      version.
     * @return Semantic version object
     * @throws VersionParsingException if there is a problem parsing the version
     */
    @SuppressWarnings("ParameterHidesMemberVariable")
    public static SemanticVersion parse(final String coreVersion, final boolean snapshot)
            throws VersionParsingException {
        return snapshot ? parse(coreVersion, Long.toString(new Date().getTime())) : parse(coreVersion);
    }

    /**
     * Obtains the major version number.
     *
     * @return Major version number.
     */
    public int getMajor() {
        return this.major;
    }

    /**
     * Obtains the minor version number.
     *
     * @return Minor version number.
     */
    public int getMinor() {
        return this.minor;
    }

    /**
     * Obtains the patch version number.
     *
     * @return Patch version number.
     */
    public int getPatch() {
        return this.patch;
    }

    /**
     * Obtains the pre-release identifiers.
     *
     * @return Pre-release identifiers. An empty list is returned if there are no identifiers.
     */
    public List<String> getPreReleaseIdentifiers() {
        return Collections.unmodifiableList(this.preRelease);
    }

    /**
     * Obtains the build metadata.
     *
     * @return Build metadata or an empty list if there is no metadata. The list consists of the build metadata split
     *      across '.'.
     */
    public List<String> getBuild() {
        return Collections.unmodifiableList(this.build);
    }

    /**
     * Obtains the version in the standard semantic version format without the pre-release or build metadata portions.
     * For example, if the full version is {@code 1.0.1-123+abc}, the core version is {@code 1.0.1}.
     *
     * @return Major, minor and patch portions of the version.
     */
    public String getCoreVersion() {
        return this.coreVersion;
    }

    /**
     * Obtains the version in the standard semantic version format. For example, if the original version is
     * {@code v1.0.0}, this method returns {@code 1.0.0}.
     *
     * @return Version in standard semantic version format.
     */
    public String getNormalizedVersion() {
        return this.normalizedVersion;
    }

    @Override
    public boolean isPreRelease() {
        return !this.preRelease.isEmpty();
    }

    private static int parseInt(final String intStr) throws VersionParsingException {
        final BigInteger secureNumber = new BigInteger(intStr);
        if (MAX_INT.compareTo(secureNumber) < 0) {
            throw new VersionParsingException(String.format(Locale.ROOT, "Value [%s] is too big.", intStr));
        }
        return secureNumber.intValueExact();
    }

    private static List<String> toList(@Nullable final String str) {
        return str == null ? List.of() : List.of(SEPARATOR_PATTERN.split(str));
    }

    private int coreCompare(final SemanticVersion other) {
        int result = Integer.compare(this.major, other.major);
        if (result != 0) {
            return result;
        }

        result = Integer.compare(this.minor, other.minor);
        return (result == 0) ? Integer.compare(this.patch, other.patch) : result;
    }

    private int compareIdentifiers(final String a, final String b) {
        try {
            final int aInt = Integer.parseInt(a);
            final int bInt = Integer.parseInt(b);
            return Integer.compare(aInt, bInt);
        } catch (final NumberFormatException ignore) {
            //ignore
        }

        if (hasDigits(a, b)) {
            final String[] tokenArr1 = a.split(EXTRACT_DIGITS);
            final String[] tokenArr2 = b.split(EXTRACT_DIGITS);
            if (tokenArr1[0].equals(tokenArr2[0])) {
                final int aInt = Integer.parseInt(tokenArr1[1]);
                final int bInt = Integer.parseInt(tokenArr2[1]);
                return Integer.compare(aInt, bInt);
            }
        }

        final int result = a.compareTo(b);
        if (result > 0) {
            return 1;
        }
        return (result < 0) ? -1 : 0;
    }

    private boolean hasDigits(final String a, final String b) {
        return HAS_DIGITS_PATTERN.matcher(a).matches() && HAS_DIGITS_PATTERN.matcher(b).matches();
    }

    private int preReleaseCompare(final SemanticVersion other) {
        if (!this.preRelease.isEmpty() && other.preRelease.isEmpty()) {
            return -1;
        }
        if (this.preRelease.isEmpty() && !other.preRelease.isEmpty()) {
            return 1;
        }
        if (this.preRelease.isEmpty()) {
            return 0;
        }

        final int maxElements = Math.max(this.preRelease.size(), other.preRelease.size());

        int i = 0;
        do {
            final String a = safeGet(this.preRelease, i);
            final String b = safeGet(other.preRelease, i);

            i++;

            if (UNDEFINED_MARKER.equals(a) && UNDEFINED_MARKER.equals(b)) {
                return 0;
            }
            if (UNDEFINED_MARKER.equals(b)) {
                return 1;
            }
            if (UNDEFINED_MARKER.equals(a)) {
                return -1;
            }
            if (a.equals(b)) {
                continue;
            }

            return compareIdentifiers(a, b);
        } while (maxElements > i);

        return 0;
    }

    private String safeGet(final List<String> list, final int i) {
        assert i >= 0;
        return i < list.size() ? list.get(i) : UNDEFINED_MARKER;
    }

    @Override
    public int compareTo(final Version obj) {
        if (getClass() != obj.getClass()) {
            throw new IllegalArgumentException("Expected instance of SemanticVersion but received "
                                                       + obj.getClass().getName());
        }

        final SemanticVersion other = (SemanticVersion)obj;
        final int result = coreCompare(other);
        return (result == 0) ? preReleaseCompare(other) : result;
    }

    @Override
    public boolean equals(final Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        return Objects.equals(this.originalVersion, ((SemanticVersion)obj).originalVersion);
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.originalVersion);
    }
}
