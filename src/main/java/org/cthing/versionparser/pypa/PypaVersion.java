/*
 * Copyright 2026 C Thing Software
 * SPDX-License-Identifier: Apache-2.0
 */
package org.cthing.versionparser.pypa;

import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.cthing.versionparser.AbstractVersion;
import org.cthing.versionparser.Version;
import org.cthing.versionparser.VersionParsingException;
import org.jspecify.annotations.Nullable;


/**
 * Represents a version conforming to the Python Packaging Authority (PyPA)
 * <a href="https://packaging.python.org/en/latest/specifications/version-specifiers/">Version Specifiers</a>
 * specification.
 *
 * <p>
 * This class parses a string version into a structured comparable value. The general form of a PyPA version is:
 * </p>
 * <pre>
 * {@literal [N!]N(.N)*[{a|b|rc}N][.postN][.devN][+<local version label>]}
 * </pre>
 *
 * <p>
 * with the following six parts:
 * </p>
 * <ul>
 *   <li>Epoch segment: N!</li>
 *   <li>Release segment: N(.N)*</li>
 *   <li>Pre-release segment: {a|b|rc}N</li>
 *   <li>Post-release segment: .postN</li>
 *   <li>Development release segment: .devN</li>
 *   <li>Local segment: +label</li>
 * </ul>
 *
 * <p>
 * Refer to
 * the <a href="https://packaging.python.org/en/latest/specifications/version-specifiers/">Version Specifiers</a>
 * specification for details on each section and how PyPA versions are ordered.
 * </p>
 */
public final class PypaVersion extends AbstractVersion {

    /**
     * Normalized pre-release phase identifier. The order in which the values are listed is their
     * sorting order (i.e. "a" is less than "b").
     */
    public enum PrePhase {
        /** Alpha. */
        a,
        /** Beta. */
        b,
        /** Release candidate. */
        rc
    }


    /**
     * If the version consists of only digits separated by periods, this regular expression can avoid
     * complex parsing.
     */
    private static final Pattern SIMPLE_VERSION_PATTERN = Pattern.compile("[0-9]+(\\.[0-9]+)*");

    /**
     * The <a href="https://packaging.python.org/en/latest/specifications/version-specifiers/#appendix-parsing-version-strings-with-regular-expressions>
     * version regular expression</a> provided in the PyPA specification.
     */
    private static final Pattern COMPLEX_VERSION_PATTERN = Pattern.compile(
            """
            ^v?\
            (?:(?<epoch>[0-9]+)!)?\
            (?<release>[0-9]+(?:\\.[0-9]+)*)\
            (?<pre>[-_.]?(?<preL>a|b|c|rc|alpha|beta|pre|preview)[-_.]?(?<preN>[0-9]+)?)?\
            (?<post>-(?<postN1>[0-9]+)|[-_.]?(?<postL>post|rev|r)[-_.]?(?<postN2>[0-9]+)?)?\
            (?<dev>[-_.]?(?<devL>dev)[-_.]?(?<devN>[0-9]+)?)?\
            (?:\\+(?<local>[a-z0-9](?:[-_.a-z0-9]*[a-z0-9])?))?$\
            """, Pattern.CASE_INSENSITIVE
    );
    private static final Pattern DOT_DELIMITER_PATTERN = Pattern.compile("\\.");
    private static final Pattern DASH_UNDERSCORE_PATTERN = Pattern.compile("[\\-_]");
    private static final Predicate<String> IS_DIGITS = Pattern.compile("[0-9]+").asMatchPredicate();
    private static final int NULL_HASH = 961;   // Use 31^2 to distinguish null from a 0 integer.

    private final int epoch;
    private final List<Integer> release;
    @Nullable
    private final PrePhase prePhase;
    @Nullable
    private final Integer pre;
    @Nullable
    private final Integer post;
    @Nullable
    private final Integer dev;
    @Nullable
    private final String local;

    private final List<Integer> trimmedRelease;
    private final boolean isTrimmed;

    /**
     * Constructs a PyPA version from the specified components.
     *
     * @param version Original version string
     * @param epoch Version scheme epoch
     * @param release Release numbers (e.g. major, minor, micro)
     * @param prePhase Pre-release phase identifier
     * @param pre Pre-release number
     * @param post Post-release number
     * @param dev Development release number
     * @param local Local label
     */
    private PypaVersion(final String version, final int epoch, final List<Integer> release,
                        @Nullable final PrePhase prePhase, @Nullable final Integer pre,
                        @Nullable final Integer post, @Nullable final Integer dev,
                        @Nullable final String local) {
        super(version);

        this.epoch = epoch;
        this.release = List.copyOf(release);
        this.prePhase = prePhase;
        this.pre = pre;
        this.post = post;
        this.dev = dev;
        this.local = local;

        this.trimmedRelease = trimRelease(this.release);
        this.isTrimmed = this.release.equals(this.trimmedRelease);
    }

    /**
     * Constructs a PyPA version from the specified components.
     *
     * @param epoch Version scheme epoch
     * @param release Release numbers (e.g. major, minor, micro)
     * @param prePhase Pre-release phase identifier
     * @param pre Pre-release number
     * @param post Post-release number
     * @param dev Development release number
     * @param local Local label
     */
    private PypaVersion(final int epoch, final List<Integer> release,
                        @Nullable final PrePhase prePhase, @Nullable final Integer pre,
                        @Nullable final Integer post, @Nullable final Integer dev,
                        @Nullable final String local) {
        this(makeCanonicalString(epoch, release, prePhase, pre, post, dev, local),
             epoch, release, prePhase, pre, post, dev, local);
    }

    /**
     * Parses the specified version string and returns a new instance of this class. To parse a version, call
     * {@link PypaVersionScheme#parseVersion(String)}.
     *
     * @param version Version string to parse
     * @return Version object
     */
    static PypaVersion parse(final String version) throws VersionParsingException {
        final String strippedVersion = version.strip();

        if (SIMPLE_VERSION_PATTERN.matcher(strippedVersion).matches()) {
            final List<Integer> release = DOT_DELIMITER_PATTERN.splitAsStream(strippedVersion)
                                                               .map(Integer::valueOf)
                                                               .toList();
            return new PypaVersion(version, 0, release, null, null, null, null, null);
        }

        final Matcher matcher = COMPLEX_VERSION_PATTERN.matcher(strippedVersion);
        if (!matcher.matches()) {
            throw new VersionParsingException("Invalid PyPA version: " + strippedVersion);
        }

        final int epoch = matcher.group("epoch") != null ? Integer.parseInt(matcher.group("epoch")) : 0;

        final List<Integer> release = DOT_DELIMITER_PATTERN.splitAsStream(matcher.group("release"))
                                                           .map(Integer::valueOf)
                                                           .toList();

        PrePhase prePhase = null;
        Integer pre = null;
        if (matcher.group("pre") != null) {
            prePhase = normalizePrePhase(matcher.group("preL"));
            final String preN =  matcher.group("preN");
            pre = (preN != null) ? Integer.parseInt(preN) : 0;
        }

        Integer post = null;
        if (matcher.group("post") != null) {
            final String postN1 = matcher.group("postN1");
            final String postN2 = matcher.group("postN2");
            post = (postN1 != null) ? Integer.parseInt(postN1) : (postN2 != null ? Integer.parseInt(postN2) : 0);
        }

        Integer dev = null;
        if (matcher.group("dev") != null) {
            final String devN = matcher.group("devN");
            dev = (devN != null) ? Integer.parseInt(devN) : 0;
        }

        final String local = normalizeLocal(matcher.group("local"));

        return new PypaVersion(version, epoch, release, prePhase, pre, post, dev, local);
    }

    /**
     * Obtains the epoch portion of the version.
     *
     * @return Epoch portion of the version
     */
    public int getEpoch() {
        return this.epoch;
    }

    /**
     * Obtains the release components of the version.
     *
     * @return Release components of the version. In the unusual case of no release components, an
     *      empty list is returned.
     */
    public List<Integer> getRelease() {
        return this.release;
    }

    /**
     * Obtains the phase portion of the pre-release segment of the version.
     *
     * @return Pre-release phase portion of the version
     */
    public Optional<PrePhase> getPrePhase() {
        return Optional.ofNullable(this.prePhase);
    }

    /**
     * Obtains the numeric portion of the pre-release segment of the version.
     *
     * @return Pre-release number portion of the version
     */
    public Optional<Integer> getPre() {
        return Optional.ofNullable(this.pre);
    }

    /**
     * Obtains the post-release number portion of the version.
     *
     * @return Post-release number portion of the version
     */
    public Optional<Integer> getPost() {
        return Optional.ofNullable(this.post);
    }

    /**
     * Obtains the development release number portion of the version.
     *
     * @return Development release number portion of the version
     */
    public Optional<Integer> getDev() {
        return Optional.ofNullable(this.dev);
    }

    /**
     * Obtains the local label portion of the version.
     *
     * @return Local label portion of the version
     */
    public Optional<String> getLocal() {
        return Optional.ofNullable(this.local);
    }

    /**
     * Creates this version without the local part.
     *
     * @return This version without the local part. May return this version if it does not contain a local part.
     */
    PypaVersion toPublicVersion() {
        if (this.local == null) {
            return this;
        }

        return new PypaVersion(this.epoch, this.release, this.prePhase, this.pre, this.post, this.dev, null);
    }

    /**
     * If this is a post release version, this method creates its non-post release version. In other words, this
     * version without post, dev, and local parts. For example:
     * <pre>
     *     1.0.post1 -> 1.0
     *     1.0a1.post0 -> 1.0a1
     *     1.0.post0.dev1 -> 1.0
     * </pre>
     *
     * @return Base version for a post release version. May return this version if it does not contain post, dev
     *      and local parts.
     */
    PypaVersion toPostBaseVersion() {
        if (this.post == null && this.dev == null && this.local == null) {
            return this;
        }

        return new PypaVersion(this.epoch, this.release, this.prePhase, this.pre, null, null, null);
    }

    /**
     * Create the earliest pre-release version based on this version by setting the dev portion to 0 and removing
     * the local portion.
     *
     * @return Earliest pre-release version.
     */
    PypaVersion toEarliestPrereleaseVersion() {
        if (this.dev != null && this.dev == 0 && this.local == null) {
            return this;
        }

        return new PypaVersion(this.epoch, this.release, this.prePhase, this.pre, this.post, 0, null);
    }

    /**
     * Creates a trimmed version of this version. The trimmed version has trailing zeroes removed from the
     * version prefix.
     *
     * @return Trimmed version. If the version only consists of zeroes, one zero is preserved.
     */
    PypaVersion toTrimmedVersion() {
        if (this.isTrimmed) {
            return this;
        }

        return new PypaVersion(this.epoch, this.trimmedRelease, this.prePhase, this.pre, this.post,
                               this.dev, this.local);
    }

    @Override
    public boolean isPreRelease() {
        return this.dev != null || this.prePhase != null || this.pre != null;
    }

    /**
     * Indicates whether this is a post-release version. Some projects use post-releases to address
     * minor errors in a final release that do not affect the distributed software (e.g. correcting
     * an error in the release notes).
     *
     * @return {@code true} if this is a post-release version
     */
    public boolean isPostRelease() {
        return this.post != null;
    }

    /**
     * Indicates whether this is a development release version.
     *
     * @return {@code true} if this is a development release version
     */
    public boolean isDevRelease() {
        return this.dev != null;
    }

    @Override
    public int compareTo(final Version obj) {
        if (!(obj instanceof PypaVersion other)) {
            throw new IllegalArgumentException("Expected instance of PypaVersion but received "
                                                       + obj.getClass().getName());
        }

        final int epochCmp = compareEpoch(other);
        if (epochCmp != 0) {
            return epochCmp;
        }

        final int releaseCmp = compareRelease(other);
        if (releaseCmp != 0) {
            return releaseCmp;
        }

        // PEP 440: A development release attached directly to a base version (i.e. no pre-release or post-release
        // segments) sorts before any pre-release. Intercept this construct before comparePre treats the missing
        // prePhase as a final release.
        final boolean thisIsBaseDev = this.prePhase == null && this.post == null && this.dev != null;
        final boolean otherIsBaseDev = other.prePhase == null && other.post == null && other.dev != null;
        if (thisIsBaseDev != otherIsBaseDev) {
            return thisIsBaseDev ? -1 : 1;
        }

        final int preCmp = comparePre(other);
        if (preCmp != 0) {
            return preCmp;
        }

        final int postCmp = comparePost(other);
        if (postCmp != 0) {
            return postCmp;
        }

        final int devCmp = compareDev(other);
        if (devCmp != 0) {
            return devCmp;
        }

        return compareLocal(other);
    }

    /**
     * Compares the epochs of the version.
     *
     * @param other Version whose epoch os to be compared with this class' epoch
     * @return A negative integer if this class' epoch is less than the other version's epoch. A positive
     *      integer if this class' epoch is greater. Zero if the epochs are equal.
     */
    private int compareEpoch(final PypaVersion other) {
        return Integer.compare(this.epoch, other.epoch);
    }

    /**
     * Compares the release portion of the version. Each component is compared numerically.
     *
     * @param other Version whose release components are to be compared with this class' release
     * @return A negative integer if this class' release is less than the other's. Positive integer if
     *      it is greater. Zero if they are equal in length and value.
     */
    private int compareRelease(final PypaVersion other) {
        final int size = Math.max(this.release.size(), other.release.size());
        for (int i = 0; i < size; i++) {
            final int v1 = i < this.release.size() ? this.release.get(i) : 0;
            final int v2 = i < other.release.size() ? other.release.get(i) : 0;
            if (v1 != v2) {
                return Integer.compare(v1, v2);
            }
        }

        return 0;
    }

    /**
     * Compares the pre phase and number portions of the version.
     *
     * @param other Version whose pre phase and number are to be compared against this class' pre phase and number
     * @return A negative integer if this class' pre phase and number are less than other's. Positive if it is
     *      greater than other's. Zero if they are equal. Note that the pre phase is compared lexicographically
     *      while the pre number is compared numerically. Pre phases have been normalized during parsing.
     */
    private int comparePre(final PypaVersion other) {
        if (this.prePhase == other.prePhase && Objects.equals(this.pre, other.pre)) {
            return 0;
        }

        if (this.prePhase == null) {
            return 1;
        }
        if (other.prePhase == null) {
            return -1;
        }

        final int phaseComp = this.prePhase.compareTo(other.prePhase);
        if (phaseComp != 0) {
            return phaseComp;
        }

        return Integer.compare(this.pre != null ? this.pre : 0, other.pre != null ? other.pre : 0);
    }

    /**
     * Compares the post portion of the version.
     *
     * @param other Version whose post is to be compared against this class' post
     * @return A negative integer if this class' post is numerically less than other's post. Positive if it is
     *      greater than other's. Zero if they are equal.
     */
    private int comparePost(final PypaVersion other) {
        if (Objects.equals(this.post, other.post)) {
            return 0;
        }
        if (this.post == null) {
            return -1;
        }
        if (other.post == null) {
            return 1;
        }

        return Integer.compare(this.post, other.post);
    }

    /**
     * Compares the dev portion of the version.
     *
     * @param other Version whose dev is to be compared against this class' dev
     * @return A negative integer if this class' dev is numerically less than other's dev. Positive if it is
     *      greater than other's. Zero if they are equal.
     */
    private int compareDev(final PypaVersion other) {
        if (Objects.equals(this.dev, other.dev)) {
            return 0;
        }
        if (this.dev == null) {
            return 1;
        }
        if (other.dev == null) {
            return -1;
        }

        return Integer.compare(this.dev, other.dev);
    }

    /**
     * Compares the local label portion of the version.
     *
     * @param other Version whose local label is to be compared against this class' local label
     * @return A negative integer if this class' local label is less than other's local label. Positive if it is
     *      greater than other's. Zero if they are equal.
     */
    private int compareLocal(final PypaVersion other) {
        if (Objects.equals(this.local, other.local)) {
            return 0;
        }
        if (this.local == null) {
            return -1;
        }
        if (other.local == null) {
            return 1;
        }

        // A local label can consist of multiple segments separated by periods. Note that the local labels
        // were converted during parsing to all lowercase and dashes and underscores were replaced by periods.
        final String[] parts1 = DOT_DELIMITER_PATTERN.split(this.local);
        final String[] parts2 = DOT_DELIMITER_PATTERN.split(other.local);

        final int length = Math.max(parts1.length, parts2.length);
        for (int i = 0; i < length; i++) {
            if (i >= parts1.length) {
                return -1;
            }
            if (i >= parts2.length) {
                return 1;
            }

            final String p1 = parts1[i];
            final String p2 = parts2[i];
            if (p1.equals(p2)) {
                continue;
            }

            final boolean isDigit1 = IS_DIGITS.test(p1);
            final boolean isDigit2 = IS_DIGITS.test(p2);

            if (isDigit1 && isDigit2) {
                // If the segments are both numeric, compare them numerically
                final int comp = compareNumericStrings(p1, p2);
                if (comp != 0) {
                    return comp;
                }
            } else if (isDigit1) {
                // This class' numeric segment is considered greater than the other's non-numeric segment
                return 1;
            } else if (isDigit2) {
                // This class' non-numeric segment is considered less than the other's numeric segment
                return -1;
            } else {
                // If both segments are non-numeric, compare them lexicographically
                final int comp = p1.compareTo(p2);
                if (comp != 0) {
                    return comp;
                }
            }
        }

        // The locals are equal
        return 0;
    }

    /**
     * Compares two string representations of positive integers of arbitrary length. The implementation avoids
     * using a numeric class both to avoid possible overflow and to avoid the overhead of instantiating a
     * class for every value comparison.
     *
     * @param s1 First numeric string to compare
     * @param s2 Second numeric string to compare
     * @return A negative integer if s1 is numerically less than s2. Positive if s1 is numerically greater than
     *      s2. Zero if s1 and s2 are numerically equal.
     */
    private static int compareNumericStrings(final String s1, final String s2) {
        // Skip leading zeros except the last one (e.g. 000 leaves 0).
        int start1 = 0;
        while (start1 < s1.length() - 1 && s1.charAt(start1) == '0') {
            start1++;
        }

        int start2 = 0;
        while (start2 < s2.length() - 1 && s2.charAt(start2) == '0') {
            start2++;
        }

        final int len1 = s1.length() - start1;
        final int len2 = s2.length() - start2;

        // A longer number is strictly greater than a shorter one
        if (len1 != len2) {
            return Integer.compare(len1, len2);
        }

        // If lengths are identical, lexicographical comparison handles the rest
        for (int i = 0; i < len1; i++) {
            final char c1 = s1.charAt(start1 + i);
            final char c2 = s2.charAt(start2 + i);
            if (c1 != c2) {
                return Character.compare(c1, c2);
            }
        }

        return 0;
    }

    /**
     * Normalizes the pre-release phase of the version string. All phrases are normalized to "a", "b" or "rc"
     * depending on their first letter. For example, "alpha" is normalized to "a", "Beta" is normalized to "b".
     *
     * @param phase Pre-release phase
     * @return Pre-release phase identifier
     */
    @Nullable
    private static PrePhase normalizePrePhase(@Nullable final String phase) {
        if (phase == null) {
            return null;
        }

        final String p = phase.toLowerCase(Locale.ENGLISH);
        if (p.startsWith("a")) {
            return PrePhase.a;
        }
        if (p.startsWith("b")) {
            return PrePhase.b;
        }
        return PrePhase.rc;
    }

    /**
     * Normalizes the local portion of the version string. The string is converted to all lowercase and underscores
     * and dashes are converted to periods.
     *
     * @param value Local portion of the version string
     * @return Normalized local string or {@code null} if it was passed in
     */
    @Nullable
    private static String normalizeLocal(@Nullable final String value) {
        return (value != null) ? DASH_UNDERSCORE_PATTERN.matcher(value.toLowerCase(Locale.ENGLISH)).replaceAll(".")
                               : null;
    }

    /**
     * Trims the specified release components by removing trailing zeroes.
     *
     * @param releaseComps Release components to trim
     * @return Newly created trimmed component list. May return the passed in component list if it is already trimmed.
     *      If the list only consists of zeroes, one zero is preserved.
     */
    private static List<Integer> trimRelease(final List<Integer> releaseComps) {
        int i = releaseComps.size() - 1;
        while (i > 0 && releaseComps.get(i) == 0) {
            i--;
        }

        if (i == releaseComps.size() - 1) {
            return releaseComps;
        }

        return releaseComps.subList(0, i + 1);
    }

    /**
     * Creates a canonical representation of the version. All segments and delimiters have been normalized.
     *
     * @return Canonical representation of the version.
     */
    public String toCanonicalString() {
        return makeCanonicalString(this.epoch, this.release, this.prePhase, this.pre, this.post, this.dev, this.local);
    }

    /**
     * Creates a canonical representation of the version.
     *
     * @param epoch Version scheme epoch
     * @param release Release numbers (e.g. major, minor, micro)
     * @param prePhase Pre-release phase identifier
     * @param pre Pre-release number
     * @param post Post-release number
     * @param dev Development release number
     * @param local Local label
     * @return Canonical representation of the version.
     */
    @SuppressWarnings("ParameterHidesMemberVariable")
    public static String makeCanonicalString(final int epoch, final List<Integer> release,
                                             @Nullable final PrePhase prePhase, @Nullable final Integer pre,
                                             @Nullable final Integer post, @Nullable final Integer dev,
                                             @Nullable final String local) {
        final StringBuilder buffer = new StringBuilder();

        if (epoch > 0) {
            buffer.append(epoch).append('!');
        }

        final int size = release.size();
        for (int i = 0; i < size; i++) {
            if (i > 0) {
                buffer.append('.');
            }
            buffer.append(release.get(i));
        }

        if (prePhase != null) {
            buffer.append(prePhase);
        }
        if (pre != null) {
            buffer.append(pre);
        }
        if (post != null) {
            buffer.append(".post").append(post);
        }
        if (dev != null) {
            buffer.append(".dev").append(dev);
        }
        if (local != null) {
            buffer.append('+').append(local);
        }

        return buffer.toString();
    }

    @Override
    public boolean equals(final Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof PypaVersion that)) {
            return false;
        }

        return this.epoch == that.epoch
                && Objects.equals(this.release, that.release)
                && this.prePhase == that.prePhase
                && Objects.equals(this.pre, that.pre)
                && Objects.equals(this.post, that.post)
                && Objects.equals(this.dev, that.dev)
                && Objects.equals(this.local, that.local);
    }

    @Override
    public int hashCode() {
        int result = 1;
        result = 31 * result + hashField(this.epoch);
        result = 31 * result + this.release.hashCode();
        result = 31 * result + hashField(this.prePhase);
        result = 31 * result + hashField(this.pre);
        result = 31 * result + hashField(this.post);
        result = 31 * result + hashField(this.dev);
        return 31 * result + hashField(this.local);
    }

    /**
     * Handles nulls by returning a value that is not 0. This is needed to prevent hash collisions in the
     * version object due to fields that are null and fields with 0 values.
     *
     * @param field Field whose hash value is to be computed
     * @return Hash value for specified field
     */
    private int hashField(@Nullable final Object field) {
        return field == null ? NULL_HASH : field.hashCode();
    }
}
