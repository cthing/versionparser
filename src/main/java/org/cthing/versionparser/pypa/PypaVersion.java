/*
 * Copyright 2026 C Thing Software
 * SPDX-License-Identifier: Apache-2.0
 */
package org.cthing.versionparser.pypa;

import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Consumer;
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


    enum BoundaryType {
        /**
         * Indicates a standard, concrete release version with no active range boundary logic.
         */
        None,

        /**
         * Represents a theoretical point on the version timeline sitting immediately after all
         * possible local version labels for a given release segment, but directly before any
         * associated post-release versions.
         *
         * <p>
         * Follows the timeline ordering rule:
         * <pre>{@code Version < Version+local < AfterLocals(Version) < Version.post0}</pre>
         * </p>
         */
        AfterLocals,

        /**
         * Represents the absolute terminal point of a version lifecycle on the timeline, sitting
         * immediately after all possible post-release variants have been accounted for.
         *
         * <p>
         * Follows the timeline ordering rule:
         * <pre>{@code Version.postN < AfterPosts(Version) < NextVersion.dev0}</pre>
         * </p>
         */
        AfterPosts
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
    private static final Pattern LOCAL_PATTERN = Pattern.compile("[a-z0-9]+(?:[._-][a-z0-9]+)*",
                                                                 Pattern.CASE_INSENSITIVE);
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
    private final BoundaryType boundaryType;

    private final List<Integer> trimmedRelease;
    private final boolean isTrimmed;


    /**
     * Provides the capability to copy an existing version object with modifications to its values. This
     * is used by the {@link #replace(Consumer)} method, which is used by the {@link PypaSpecifier} class.
     */
    final class Modifier {
        private int modEpoch;
        private List<Integer> modRelease;
        @Nullable
        private PrePhase modPrePhase;
        @Nullable
        private Integer modPre;
        @Nullable
        private Integer modPost;
        @Nullable
        private Integer modDev;
        @Nullable
        private String modLocal;

        private Modifier() {
            this.modEpoch = PypaVersion.this.epoch;
            this.modRelease = List.copyOf(PypaVersion.this.release);
            this.modPrePhase = PypaVersion.this.prePhase;
            this.modPre = PypaVersion.this.pre;
            this.modPost = PypaVersion.this.post;
            this.modDev = PypaVersion.this.dev;
            this.modLocal = PypaVersion.this.local;
        }

        /**
         * Sets the epoch.
         *
         * @param modifiedEpoch new value for the epoch
         * @return This modifier
         */
        Modifier withEpoch(final int modifiedEpoch) {
            if (modifiedEpoch < 0) {
                throw new IllegalArgumentException("Epoch cannot be negative");
            }

            this.modEpoch = modifiedEpoch;
            return this;
        }

        /**
         * Removes the epoch (i.e. sets it to 0).
         *
         * @return This modifier
         */
        Modifier withoutEpoch() {
            this.modEpoch = 0;
            return this;
        }

        /**
         * Sets the release values.
         *
         * @param modifiedRelease New release values
         * @return This modifier
         */
        Modifier withRelease(final Collection<Integer> modifiedRelease) {
            if (modifiedRelease.isEmpty()) {
                throw new IllegalArgumentException("Release cannot be empty");
            }
            if (modifiedRelease.stream().anyMatch(v -> v < 0)) {
                throw new IllegalArgumentException("Release values cannot be negative");
            }

            this.modRelease = List.copyOf(modifiedRelease);
            return this;
        }

        /**
         * Removes the release numbers (i.e. sets a list with one 0 entry).
         *
         * @return This modifier
         */
        Modifier withoutRelease() {
            this.modRelease = List.of(0);
            return this;
        }

        /**
         * Sets the pre phase value.
         *
         * @param modifiedPrePhase Pre phase value
         * @return This modifier
         */
        Modifier withPrePhase(final PrePhase modifiedPrePhase) {
            this.modPrePhase = modifiedPrePhase;
            return this;
        }

        /**
         * Removes the pre phase value.
         *
         * @return This modifier
         */
        Modifier withoutPrePhase() {
            this.modPrePhase = null;
            return this;
        }

        /**
         * Sets the pre number.
         *
         * @param modifiedPre Pre number
         * @return This modifier
         */
        Modifier withPre(final int modifiedPre) {
            if (modifiedPre < 0) {
                throw new IllegalArgumentException("Pre cannot be negative");
            }

            this.modPre = modifiedPre;
            return this;
        }

        /**
         * Removes the pre number.
         *
         * @return This modifier
         */
        Modifier withoutPre() {
            this.modPre = null;
            return this;
        }

        /**
         * Sets the post number.
         *
         * @param modifiedPost Post number
         * @return This modifier
         */
        Modifier withPost(final int modifiedPost) {
            if (modifiedPost < 0) {
                throw new IllegalArgumentException("Post cannot be negative");
            }

            this.modPost = modifiedPost;
            return this;
        }

        /**
         * Removes the post number.
         *
         * @return This modifier
         */
        Modifier withoutPost() {
            this.modPost = null;
            return this;
        }

        /**
         * Sets the dev number.
         *
         * @param modifiedDev Dev number
         * @return This modifier
         */
        Modifier withDev(final int modifiedDev) {
            if (modifiedDev < 0) {
                throw new IllegalArgumentException("Dev cannot be negative");
            }

            this.modDev = modifiedDev;
            return this;
        }

        /**
         * Removes the dev number.
         *
         * @return This modifier
         */
        Modifier withoutDev() {
            this.modDev = null;
            return this;
        }

        /**
         * Sets the local value.
         *
         * @param modifiedLocal Local value
         * @return This modifier
         */
        Modifier withLocal(final String modifiedLocal) {
            if (!LOCAL_PATTERN.matcher(modifiedLocal).matches()) {
                throw new IllegalArgumentException("Invalid local name: " + modifiedLocal);
            }

            this.modLocal = modifiedLocal;
            return this;
        }

        /**
         * Removes the local value.
         *
         * @return This modifier
         */
        Modifier withoutLocal() {
            this.modLocal = null;
            return this;
        }

        /**
         * Creates a new version object with the modified values.
         *
         * @return Newly create version object
         */
        PypaVersion modify() {
            return new PypaVersion(this.modEpoch, this.modRelease, this.modPrePhase, this.modPre, this.modPost,
                                   this.modDev, this.modLocal, PypaVersion.this.boundaryType);
        }
    }


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
                        @Nullable final String local, final BoundaryType boundaryType) {
        super(version);

        this.epoch = epoch;
        this.release = List.copyOf(release);
        this.prePhase = prePhase;
        this.pre = pre;
        this.post = post;
        this.dev = dev;
        this.local = local;
        this.boundaryType = boundaryType;

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
                        @Nullable final String local, final BoundaryType boundaryType) {
        this(makeCanonicalString(epoch, release, prePhase, pre, post, dev, local),
             epoch, release, prePhase, pre, post, dev, local, boundaryType);
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
            return new PypaVersion(version, 0, release, null, null, null, null, null, BoundaryType.None);
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

        return new PypaVersion(version, epoch, release, prePhase, pre, post, dev, local, BoundaryType.None);
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

    BoundaryType getBoundaryType() {
        return this.boundaryType;
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

        return new PypaVersion(this.epoch, this.release, this.prePhase, this.pre, this.post, this.dev, null,
                               BoundaryType.None);
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
                               this.dev, this.local, BoundaryType.None);
    }

    /**
     * Creates a boundary version marker based on this version with the specified behavior type.
     *
     * @param type Boundary behavior type
     * @return Newly create boundary version marker
     */
    PypaVersion toBoundaryVersion(final BoundaryType type) {
        if (this.boundaryType != BoundaryType.None) {
            throw new IllegalStateException("Cannot create a boundary version from a boundary version");
        }

        return new PypaVersion(this.epoch, this.trimmedRelease, this.prePhase, this.pre, this.post, this.dev, null,
                               type);
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

    /**
     * Creates a new version object based on this version but with the specified values modified.
     *
     * @param modifierProc Passed an instance of the modifier to allow modification of version values
     * @return Newly created version if values have been changed or the existing version object if
     *      no changes were made.
     */
    public PypaVersion replace(final Consumer<Modifier> modifierProc) {
        final Modifier modifier = new Modifier();
        modifierProc.accept(modifier);

        final PypaVersion newVersion = modifier.modify();
        return this.equals(newVersion) ? this : newVersion;
    }

    @Override
    public int compareTo(final Version obj) {
        if (!(obj instanceof PypaVersion other)) {
            throw new IllegalArgumentException("Expected instance of PypaVersion but received "
                                                       + obj.getClass().getName());
        }

        // Intercept boundary version comparisons
        if (this.boundaryType != BoundaryType.None || other.boundaryType != BoundaryType.None) {
            return compareWithBoundaries(other);
        }

        final int baseCmp = compareBaseTo(other);
        if (baseCmp != 0) {
            return baseCmp;
        }

        return compareLocal(other);
    }

    /**
     * Handles comparison when one or both of the elements are boundary version markers.
     *
     * @param other Version to compare against
     * @return A negative integer if this version is less than the other version. A positive
     *      integer if this version is greater. Zero if the versions are equal.
     */
    private int compareWithBoundaries(final PypaVersion other) {
        // Case 1: Both instances are boundary version markers.
        if (this.boundaryType != BoundaryType.None && other.boundaryType != BoundaryType.None) {
            // Compare the base components (epoch, release, pre, post, dev) of both boundaries.
            // If the underlying base versions differ (e.g., AfterLocals(1.2) vs AfterLocals(1.3)), that
            // difference determines sorting.
            final int baseCmp = compareBaseTo(other);
            if (baseCmp != 0) {
                return baseCmp;
            }

            // If the base versions are completely identical, the tie-breaker is determined by the order of the enums.
            // By definition, BoundaryType enum ordering dictates that 'AfterLocals' sorts before 'AfterPosts'.
            return this.boundaryType.compareTo(other.boundaryType);
        }

        // Case 2: This instance is a boundary marker, and the other instance is a concrete standard version.
        if (this.boundaryType != BoundaryType.None) {
            // Check if this boundary's base version components mathematically sort ahead of or match the
            // standard version. For example: evaluating boundary AfterLocals(1.2) against standard version 1.1 or 1.2.
            // If this base is greater, the boundary naturally sorts later. If the bases match
            // (e.g., AfterLocals(1.2) vs 1.2), the boundary wins because a boundary represents a theoretical point
            // sitting strictly past its standard version.
            if (compareBaseTo(other) >= 0) {
                return 1;
            }

            // If the boundary's base version component sorts earlier than the standard version
            // (e.g., AfterLocals(1.2) vs 1.2.post1), determine if the standard version belongs to the exact same
            // release lineage/family. If it belongs to the same family, it means the standard version sits within
            // the scope that this boundary encapsulates, placing the boundary after the version. If it belongs to
            // a different family, the boundary sorts before the version.
            return isSameBaseRelease(other) ? 1 : -1;
        }

        // Case 3: This instance is a concrete standard version, and the other instance is a boundary version marker.
        // This flips the perspective of Case 2 to evaluate a real version against a theoretical boundary point.
        if (other.compareBaseTo(this) >= 0) {
            // If the other boundary's base version component is greater than or matches this version, the
            // boundary sits further ahead on the timeline (e.g., 1.2 vs AfterLocals(1.2)). Therefore, this
            // standard version must sort earlier.
            return -1;
        }

        // If the other boundary's base version component sorts earlier than this version
        // (e.g., 1.2.post1 vs AfterLocals(1.2)), evaluate whether this version belongs to that boundary's shared
        // release lineage/family. If it is part of the same lineage, the version is encapsulated by that boundary's
        // scope, placing this version before the boundary on the timeline. If it is from a completely separate
        // lineage, this version sorts after the boundary.
        return other.isSameBaseRelease(this) ? -1 : 1;
    }

    /**
     * Compare the base version segments ignoring boundary tags and local flags.
     *
     * @param other Version to test
     * @return A negative integer if the appropriate segments of this version are less than the other version's
     *      segments. A positive integer if this version's appropriate segments are greater. Zero if the appropriate
     *      segments of each version are equal.
     */
    private int compareBaseTo(final PypaVersion other) {
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

        return compareDev(other);
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
     * Determines whether a standard version belongs to the exact same base release lineage or "family" as
     * this boundary version marker.
     *
     * <p>
     * A standard version matches a boundary's lineage if it shares the same epoch, its significant
     * release segments perfectly match the boundary's trimmed release segments (with any trailing segments
     * being strictly zero), and it has an identical pre-release phase and number. Finally, boundary-specific
     * constraints are evaluated based on the {@link BoundaryType}:
     * </p>
     * <ul>
     *   <li>
     *     <b>{@link BoundaryType#AfterLocals}:</b> Matches only if the test version has the exact same
     *     {@code post} and {@code dev} release values as the boundary.
     *   </li>
     *   <li>
     *     <b>{@link BoundaryType#AfterPosts}:</b> Matches if the test version has the exact same
     *     {@code dev} value as the boundary, OR if the test version explicitly contains a {@code post} release segment.
     *   </li>
     * </ul>
     *
     * <p>Examples:</p>
     * <pre>
     * Boundary Version Type    Test Version String   Result   Reason
     * ---------------------    -------------------   ------   ------
     * 1.2 (AfterLocals)        1.2                   true     Perfect lineage and segment match
     * 1.2 (AfterLocals)        1.2.0.0               true     Trailing zeros are ignored when establishing lineage
     * 1.2 (AfterLocals)        1.2+ubuntu            true     Local segments are ignored for base release matching
     * 1.2 (AfterLocals)        1.2.post1             false    Post values mismatch (boundary has no post, test
     *                                                         has post 1)
     * 1.2 (AfterLocals)        1.3                   false    Release segment mismatch
     * 1.2.post1 (AfterPosts)   1.2.post2             true     Any version containing a post release matches an
     *                                                         AfterPosts boundary family
     * 1.2a1 (AfterPosts)       1.2a2                 false    Pre-release segment mismatch (alpha 1 vs alpha 2)
     * </pre>
     *
     * @param other The standard version to evaluate against this boundary.
     * @return {@code true} if the test version falls inside this boundary's base release lineage;
     *         {@code false} otherwise.
     * @throws IllegalStateException if this version instance is not an active boundary marker
     *                               (i.e., its type is {@link BoundaryType#None}).
     */
    private boolean isSameBaseRelease(final PypaVersion other) {
        // This method is only applicable to boundary versions
        if (this.boundaryType == BoundaryType.None) {
            throw new IllegalStateException("isSameBaseRelease can only be called on boundary versions.");
        }

        // Versions in different release epochs (e.g., 1!1.0 vs 2.0) are never in the same family.
        if (other.epoch != this.epoch) {
            return false;
        }

        // A version cannot match the boundary prefix if it has fewer digits than the boundary's trimmed release.
        final List<Integer> otherRelease = other.release;
        final int trimmedLength = this.trimmedRelease.size();
        if (otherRelease.size() < trimmedLength) {
            return false;
        }

        // Compare digits at the exact same index position between the version and the boundary.
        // If a mismatch is detected in core release numbers (e.g., 1.2 vs 1.3), they are different families.
        for (int i = 0; i < trimmedLength; i++) {
            if (!otherRelease.get(i).equals(this.trimmedRelease.get(i))) {
                return false;
            }
        }

        // Check all remaining trailing digits present in the test version beyond the boundary's trimmed length.
        // Trailing segments must be strictly zero (e.g., 1.2.0 is in 1.2's family, but 1.2.1 is not). The version
        // is not in the boundary's family if any extra trailing release digit provides a non-zero value.
        for (int i = trimmedLength; i < otherRelease.size(); i++) {
            if (otherRelease.get(i) != 0) {
                return false;
            }
        }

        // Evaluate the pre-release phase segments (like 'a1' or 'rc2'). If pre-release types or indices do not
        // align perfectly, they belong to separate release families.
        if (comparePre(other) != 0) {
            return false;
        }

        // Determine if this boundary represents the pivot directly following all local labels (+local).
        // With AfterLocals the candidate must have the exact same post-release and development-release values.
        if (this.boundaryType == BoundaryType.AfterLocals) {
            return Objects.equals(other.post, this.post) && Objects.equals(other.dev, this.dev);
        }

        // With AfterPosts a candidate matches if its dev segment matches or if it contains any post-release segment.
        return Objects.equals(other.dev, this.dev) || other.post != null;
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
                && Objects.equals(this.local, that.local)
                && this.boundaryType == that.boundaryType;
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
        result = 31 * result + hashField(this.boundaryType);
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
