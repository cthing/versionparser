/*
 * Copyright 2023 C Thing Software
 * SPDX-License-Identifier: Apache-2.0
 */
package org.cthing.versionparser.pypa;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.function.Predicate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.cthing.annotations.AccessForTesting;
import org.cthing.versionparser.VersionParsingException;
import org.cthing.versionparser.VersionRange;


/**
 * Represents a Python Packaging Authority version specifier. See the
 * <a href="https://packaging.python.org/en/latest/specifications/version-specifiers/#version-specifiers">Version
 * Specifiers</a> specification for details on syntax and processing rules.
 */
public final class PypaSpecifier {

    /**
     * Provides the actions of a specifier.
     */
    private interface Spec {

        /**
         * Obtains the operator portion of the specifier.
         *
         * @return Specifier operator
         */
        String getOperator();

        /**
         * Obtains the version identifier portion of the specifier.
         *
         * @return Specifier version identifier
         */
        String getVersionId();

        /**
         * Indicates if this specifier allows the specified version.
         *
         * @param prospect Version to test
         * @return {@code true} if this specifier allows the specified version
         * @throws VersionParsingException if there was a problem evaluating the specifier
         */
        boolean allows(PypaVersion prospect) throws VersionParsingException;

        /**
         * Indicates if this specifier allows the specified version.
         *
         * @param prospect Version to test
         * @return {@code true} if this specifier allows the specified version
         * @throws VersionParsingException if there was a problem evaluating the version or specifier
         */
        boolean allows(String prospect) throws VersionParsingException;

        /**
         * Provides a canonical string representation of the specifier. The format of the string depends
         * upon the specific operator but typically consists of the operator and a canonicalized form of
         * the version identifier.
         *
         * @return Canonical representation of the specifier
         */
        String toCanonicalString();

        /**
         * Creates version range(s) to represent the specifier.
         *
         * @return Version range(s) to represent the specifier.
         * @throws UnsupportedOperationException if this method is called on {@link ArbitraryEqualSpec}. The
         *      arbitrary equality specifier cannot be represented as a version range. Use the
         *      {@link ArbitraryEqualSpec#allows(String)} method.
         */
        List<VersionRange> toRanges();
    }


    /**
     * Base class for all specifier implementations.
     */
    @AccessForTesting
    abstract static class AbstractSpec implements Spec {

        protected static final Predicate<String> IS_DIGITS = Pattern.compile("[0-9]+").asMatchPredicate();
        protected static final String WILDCARD_SUFFIX = ".*";
        protected static final int WILDCARD_SUFFIX_LEN = WILDCARD_SUFFIX.length();

        private static final Pattern PREFIX_PATTERN = Pattern.compile("([0-9]+)((?:a|b|c|rc)[0-9]+)");
        private static final Pattern DOT_PATTERN = Pattern.compile("\\.");

        protected final String operator;
        protected final String versionId;

        private AbstractSpec(final String operator, final String versionId) {
            this.operator = operator;
            this.versionId = versionId;
        }

        @Override
        public String getOperator() {
            return this.operator;
        }

        @Override
        public String getVersionId() {
            return this.versionId;
        }

        @Override
        public String toString() {
            return this.operator + this.versionId;
        }

        /**
         * Splits a version string into components.
         *
         * @param version Version string to split
         * @return An immutable list of version components. The version is split around dots and bang.
         *      The list always starts with the epoch (0 if the version does not contain an epoch).
         */
        @AccessForTesting
        static List<String> splitVersion(final String version) {
            final List<String> result = new ArrayList<>(10);

            // Extract the epoch and the rest of the version
            final int lastBangIndex = version.lastIndexOf('!');
            final String epoch = (lastBangIndex != -1) ? version.substring(0, lastBangIndex) : "";
            final String rest = (lastBangIndex != -1) ? version.substring(lastBangIndex + 1) : version;

            // Add the epoch (zero if version does not contain an epoch).
            result.add(epoch.isEmpty() ? "0" : epoch);

            // Split the version around dots
            final String[] items = DOT_PATTERN.split(rest, -1);
            for (final String item : items) {
                final Matcher matcher = PREFIX_PATTERN.matcher(item);

                if (matcher.matches()) {
                    // Add the prefix components
                    for (int i = 1; i <= matcher.groupCount(); i++) {
                        result.add(matcher.group(i));
                    }
                } else {
                    // Add suffix components
                    result.add(item);
                }
            }

            return List.copyOf(result);
        }

        /**
         * Joins split version components into version string.
         *
         * @param components Split version elements, where the first element is assumed to be the epoch.
         * @return Version string.
         */
        @AccessForTesting
        static String joinVersion(final List<String> components) {
            if (components.isEmpty()) {
                return "0!";
            }

            final StringBuilder buffer = new StringBuilder();
            final String epoch = components.get(0);

            buffer.append(epoch.isEmpty() ? "0" : epoch).append('!');

            final int size = components.size();
            if (size > 1) {
                buffer.append(components.get(1));
                for (int i = 2; i < size; i++) {
                    buffer.append('.').append(components.get(i));
                }
            }

            return buffer.toString();
        }

        /**
         * Provides a canonical string representation of the specified version.
         *
         * @param version Version to canonicalize
         * @param stripTrailingZeros {@code true} to strip trailing zeros from the release portion of the version
         * @return Canonical representation of the specified version
         */
        @AccessForTesting
        static String canonicalizeVersion(final PypaVersion version, final boolean stripTrailingZeros) {
            return stripTrailingZeros ? version.toTrimmedVersion().toCanonicalString() : version.toCanonicalString();
        }

        /**
         * Counts the leading numeric components in a split version.
         *
         * @param split Version components
         * @return Number of leading numeric components
         */
        @AccessForTesting
        @SuppressWarnings("Convert2streamapi")
        static int countReleaseComps(final List<String> split) {
            int count = 0;
            for (final String segment : split) {
                if (!IS_DIGITS.test(segment)) {
                    break;
                }
                count++;
            }
            return count;
        }

        /**
         * Pads a split version with "0" components to reach the specified target length. Non-numeric suffix
         * components are preserved.
         *
         * @param split Version components
         * @param targetNumericLen Desired number of numeric components
         * @return Newly created immutable list with the specified padding
         */
        @AccessForTesting
        static List<String> padRelease(final List<String> split, final int targetNumericLen) {
            final int currentNumericLen = countReleaseComps(split);
            final int padNeeded = targetNumericLen - currentNumericLen;

            if (padNeeded <= 0) {
                return List.copyOf(split);
            }

            final List<String> result = new ArrayList<>(split.size() + padNeeded);

            // Add existing numeric prefix
            result.addAll(split.subList(0, currentNumericLen));

            // Add zero padding
            for (int i = 0; i < padNeeded; i++) {
                result.add("0");
            }

            // Add existing suffix
            result.addAll(split.subList(currentNumericLen, split.size()));

            return List.copyOf(result);
        }

        /**
         * Increments the last release component of the specified version.
         *
         * @param version Version whose last release component is to be incremented
         * @return New incremented release components
         */
        @AccessForTesting
        static List<Integer> incrementRelease(final PypaVersion version) {
            final List<Integer> release = version.getRelease();
            if (release.isEmpty()) {
                return release;
            }

            final List<Integer> nextRelease = new ArrayList<>(release);
            final int lastIndex = nextRelease.size() - 1;
            nextRelease.set(lastIndex, nextRelease.get(lastIndex) + 1);

            return Collections.unmodifiableList(nextRelease);
        }
    }


    /**
     * Represents the
     * <a href="https://packaging.python.org/en/latest/specifications/version-specifiers/#arbitrary-equality">arbitrary
     * equality</a> specifier (===V).
     */
    private static final class ArbitraryEqualSpec extends AbstractSpec {

        static final String OPERATOR = "===";

        private ArbitraryEqualSpec(final String versionId) {
            super(OPERATOR, versionId);
        }

        @Override
        public boolean allows(final PypaVersion prospect) {
            return allows(prospect.getOriginalVersion());
        }

        @Override
        public boolean allows(final String prospect) {
            return prospect.equalsIgnoreCase(this.versionId);
        }

        @Override
        public String toCanonicalString() {
            return toString();
        }

        @Override
        public List<VersionRange> toRanges() {
            throw new UnsupportedOperationException("Cannot represent the '===V' specifier as a version range");
        }
    }


    /**
     * Represents the
     * <a href="https://packaging.python.org/en/latest/specifications/version-specifiers/#compatible-release">compatible
     * release</a> specifier (~=V).
     */
    private static final class CompatibleSpec extends AbstractSpec {

        static final String OPERATOR = "~=";

        private final PypaVersion version;
        private final String canonicalVersion;
        private final GreaterThanEqualSpec geOperator;
        private final EqualSpec eqOperator;

        private CompatibleSpec(final String versionId) throws VersionParsingException {
            super(OPERATOR, versionId);

            this.version = PypaVersion.parse(versionId);
            this.canonicalVersion = canonicalizeVersion(this.version, false);
            this.geOperator = new GreaterThanEqualSpec(this.versionId);
            this.eqOperator = makeEqualSpecifier();
        }

        /**
         * Constructs the equal specifier appropriate to the compatibility specified version identifier.
         * The equality specifier consists of the original version identifier with the last digit removed
         * and the wildcard suffix appended.
         *
         * @return Newly constructed equal specifier
         * @throws VersionParsingException if there was a problem constructing the specifier
         */
        @SuppressWarnings("Convert2streamapi")
        private EqualSpec makeEqualSpecifier() throws VersionParsingException {
            final List<String> versionComps = splitVersion(this.versionId);

            final List<String> releaseComps = new ArrayList<>(versionComps.size());
            for (String comp : versionComps) {
                if (!IS_DIGITS.test(comp)) {
                    break;
                }
                releaseComps.add(comp);
            }
            assert !releaseComps.isEmpty();
            releaseComps.remove(releaseComps.size() - 1);

            final String wildcardVersionId = joinVersion(releaseComps) + WILDCARD_SUFFIX;
            return new EqualSpec(wildcardVersionId);
        }

        @Override
        public boolean allows(final PypaVersion prospect) throws VersionParsingException {
            return this.geOperator.allows(prospect) && this.eqOperator.allows(prospect);
        }

        @Override
        public boolean allows(final String prospect) throws VersionParsingException {
            return allows(PypaVersion.parse(prospect));
        }

        @Override
        public String toCanonicalString() {
            return this.operator + this.canonicalVersion;
        }

        @Override
        public List<VersionRange> toRanges() {
            final List<Integer> release = this.version.getRelease();
            final int size = release.size();

            final List<Integer> nextRelease;
            if (size <= 1) {
                nextRelease = List.of();
            } else {
                nextRelease = new ArrayList<>(release.subList(0, size - 1));
                final int lastIndex = nextRelease.size() - 1;
                nextRelease.set(lastIndex, nextRelease.get(lastIndex) + 1);
            }

            final PypaVersion maxVersion = this.version.replace(mod -> mod.withRelease(nextRelease)
                                                                          .withoutPrePhase()
                                                                          .withoutPre()
                                                                          .withoutPost()
                                                                          .withDev(0)
                                                                          .withoutLocal());

            return List.of(new VersionRange(this.version, maxVersion, true, false));
        }
    }


    /**
     * Base class for the equality specifiers.
     */
    private abstract static class AbstractEqualitySpec extends AbstractSpec {

        protected final boolean hasWildcard;
        protected final PypaVersion version;
        protected final PypaVersion baseDevVersion;
        protected final PypaVersion nextVersion;

        private final String canonicalVersion;

        private AbstractEqualitySpec(final String operator, final String versionId) throws VersionParsingException {
            super(operator, versionId);

            this.hasWildcard = this.versionId.endsWith(WILDCARD_SUFFIX);

            if (this.hasWildcard) {
                final String versionIdPart = this.versionId.substring(0, this.versionId.length() - WILDCARD_SUFFIX_LEN);
                this.version = PypaVersion.parse(versionIdPart);
                this.canonicalVersion = this.versionId;
            } else {
                this.version = PypaVersion.parse(this.versionId);
                this.canonicalVersion = canonicalizeVersion(this.version, true);
            }

            this.baseDevVersion = this.version.replace(mod -> mod.withoutPrePhase()
                                                                 .withoutPre()
                                                                 .withoutPost()
                                                                 .withDev(0)
                                                                 .withoutLocal());
            this.nextVersion = this.baseDevVersion.replace(mod -> mod.withRelease(incrementRelease(this.version)));
        }

        @Override
        public boolean allows(final PypaVersion prospect) throws VersionParsingException {
            return this.hasWildcard ? allowsWildcard(prospect) : allowsPlain(prospect);
        }

        /**
         * Indicates whether the specified version matches the wildcard version identifier.
         *
         * @param prospect Version to test
         * @return {@code true} if the specified version matches
         */
        private boolean allowsWildcard(final PypaVersion prospect) {
            // Split the version identifier into components. Note that the wildcard suffix has
            // already been removed from the version identifier.
            final String canonicalVersionPart = canonicalizeVersion(this.version, false);
            final List<String> versionPartComps = splitVersion(canonicalVersionPart);

            // Split the prospect into components
            final String canonicalProspect = canonicalizeVersion(prospect.toPublicVersion(), false);
            final List<String> prospectComps = splitVersion(canonicalProspect);

            // Ensure the prospect version is of the appropriate length for comparison with the version identifier.
            final List<String> paddedProspectComps = padRelease(prospectComps, countReleaseComps(versionPartComps));
            final List<String> shortenedProspectComps =
                    paddedProspectComps.subList(0, Math.min(paddedProspectComps.size(), versionPartComps.size()));

            // Compare the prospect with the version identifier.
            return shortenedProspectComps.equals(versionPartComps);
        }

        /**
         * Indicates whether the specified version matches the non-wildcard version identifier.
         *
         * @param prospect Version to test
         * @return {@code true} if the specified version matches
         */
        private boolean allowsPlain(final PypaVersion prospect) {
            // If the version identifier does not contain a local portion, any local portion on the prospect
            // must be removed before comparison.
            if (this.version.getLocal().isEmpty()) {
                return prospect.toPublicVersion().compareTo(this.version) == 0;
            }

            return prospect.compareTo(this.version) == 0;
        }

        @Override
        public String toCanonicalString() {
            return this.operator + this.canonicalVersion;
        }
    }


    /**
     * Represents the
     * <a href="https://packaging.python.org/en/latest/specifications/version-specifiers/#version-matching">version
     * matching</a> specifier (==V).
     */
    private static final class EqualSpec extends AbstractEqualitySpec {

        static final String OPERATOR = "==";

        private EqualSpec(final String versionId) throws VersionParsingException {
            super(OPERATOR, versionId);
        }

        @Override
        public boolean allows(final String prospect) throws VersionParsingException {
            return allows(PypaVersion.parse(prospect));
        }

        @Override
        public List<VersionRange> toRanges() {
            if (this.hasWildcard) {
                return List.of(new VersionRange(this.baseDevVersion, this.nextVersion, true, false));
            }

            if (this.version.getLocal().isEmpty()) {
                final PypaVersion maxVersion = this.version.toBoundaryVersion(PypaVersion.BoundaryType.AfterLocals);
                return List.of(new VersionRange(this.version, maxVersion, true, true));
            }

            return List.of(new VersionRange(this.version));
        }
    }


    /**
     * Represents the
     * <a href="https://packaging.python.org/en/latest/specifications/version-specifiers/#version-exclusion">version
     * exclusion</a> specifier (!=V).
     */
    private static final class NotEqualSpec extends AbstractEqualitySpec {

        static final String OPERATOR = "!=";

        private NotEqualSpec(final String versionId) throws VersionParsingException {
            super(OPERATOR, versionId);
        }

        @Override
        public boolean allows(final PypaVersion prospect) throws VersionParsingException {
            return !super.allows(prospect);
        }

        @Override
        public boolean allows(final String prospect) throws VersionParsingException {
            return !super.allows(PypaVersion.parse(prospect));
        }

        @Override
        public List<VersionRange> toRanges() {
            if (this.hasWildcard) {
                return List.of(new VersionRange(null, this.baseDevVersion, false, false),
                               new VersionRange(this.nextVersion, null, true, false));
            }

            if (this.version.getLocal().isEmpty()) {
                final PypaVersion maxVersion = this.version.toBoundaryVersion(PypaVersion.BoundaryType.AfterLocals);
                return List.of(new VersionRange(null, this.version, false, false),
                               new VersionRange(maxVersion, null, false, false));
            }

            return List.of(new VersionRange(null, this.version, false, false),
                           new VersionRange(this.version, null, false, false));
        }
    }


    /**
     * Base class for specifiers that provide ordered comparison (e.g. &lt;=V).
     */
    private abstract static class AbstractOrderingSpec extends AbstractSpec {

        protected final PypaVersion version;

        private final String canonicalVersion;

        private AbstractOrderingSpec(final String operator, final String versionId) throws VersionParsingException {
            super(operator, versionId);

            this.version = PypaVersion.parse(versionId);
            this.canonicalVersion = canonicalizeVersion(this.version, true);
        }

        @Override
        public boolean allows(final String prospect) throws VersionParsingException {
            return allows(PypaVersion.parse(prospect));
        }

        @Override
        public String toCanonicalString() {
            return this.operator + this.canonicalVersion;
        }
    }


    /**
     * Represents the
     * <a href="https://packaging.python.org/en/latest/specifications/version-specifiers/#inclusive-ordered-comparison">
     * less than or equal</a> specifier (&lt;=V).
     */
    private static final class LessThanEqualSpec extends AbstractOrderingSpec {

        static final String OPERATOR = "<=";

        private LessThanEqualSpec(final String versionId) throws VersionParsingException {
            super(OPERATOR, versionId);
        }

        @Override
        public boolean allows(final PypaVersion prospect) {
            return prospect.toPublicVersion().compareTo(this.version) <= 0;
        }

        @Override
        public List<VersionRange> toRanges() {
            final PypaVersion maxVersion = this.version.toBoundaryVersion(PypaVersion.BoundaryType.AfterLocals);
            return List.of(new VersionRange(null, maxVersion, false, true));
        }
    }


    /**
     * Represents the
     * <a href="https://packaging.python.org/en/latest/specifications/version-specifiers/#exclusive-ordered-comparison">
     * less than</a> specifier (&lt;V).
     */
    private static final class LessThanSpec extends AbstractOrderingSpec {

        static final String OPERATOR = "<";

        static final PypaVersion MIN_VERSION;
        static {
            try {
                MIN_VERSION = PypaVersion.parse("0.dev0");
            } catch (final VersionParsingException ex) {
                throw new IllegalStateException("Unable to parse version 0.dev0", ex);
            }
        }

        private final PypaVersion earliestVersion;

        private LessThanSpec(final String versionId) throws VersionParsingException {
            super(OPERATOR, versionId);

            this.earliestVersion = this.version.replace(mod -> mod.withDev(0).withoutLocal());
        }

        @Override
        public boolean allows(final PypaVersion prospect) {
            if (prospect.compareTo(this.version) >= 0) {
                return false;
            }

            if (!this.version.isPreRelease() && prospect.isPreRelease()) {
                return prospect.compareTo(this.earliestVersion) < 0;
            }

            return true;
        }

        @Override
        public List<VersionRange> toRanges() {
            // <V excludes pre-releases of V when V is not a pre-release. V.dev0 is the earliest pre-release of V.
            final PypaVersion maxVersion;

            if (this.version.isPreRelease()) {
                maxVersion = this.version;
            } else {
                maxVersion = this.version.replace(mod -> mod.withDev(0).withoutLocal());
            }

            if (maxVersion.compareTo(MIN_VERSION) <= 0) {
                return List.of();
            }

            return List.of(new VersionRange(null, maxVersion, false, false));
        }
    }


    /**
     * Represents the
     * <a href="https://packaging.python.org/en/latest/specifications/version-specifiers/#inclusive-ordered-comparison">
     * greater than or equal</a> specifier (&gt;=V).
     */
    private static final class GreaterThanEqualSpec extends AbstractOrderingSpec {

        static final String OPERATOR = ">=";

        private GreaterThanEqualSpec(final String versionId) throws VersionParsingException {
            super(OPERATOR, versionId);
        }

        @Override
        public boolean allows(final PypaVersion prospect) {
            return prospect.toPublicVersion().compareTo(this.version) >= 0;
        }

        @Override
        public List<VersionRange> toRanges() {
            return List.of(new VersionRange(this.version, null, true, false));
        }
    }


    /**
     * Represents the
     * <a href="https://packaging.python.org/en/latest/specifications/version-specifiers/#exclusive-ordered-comparison">
     * greater than</a> specifier (&gt;V).
     */
    private static final class GreaterThanSpec extends AbstractOrderingSpec {

        static final String OPERATOR = ">";

        private GreaterThanSpec(final String versionId) throws VersionParsingException {
            super(OPERATOR, versionId);
        }

        @Override
        public boolean allows(final PypaVersion prospect) {
            if (prospect.compareTo(this.version) <= 0) {
                return false;
            }

            if (!this.version.isPostRelease() && prospect.isPostRelease()) {
                final PypaVersion postBaseVesion = prospect.replace(mod -> mod.withoutPost()
                                                                              .withoutDev()
                                                                              .withoutLocal());
                if (postBaseVesion.compareTo(this.version) == 0) {
                    return false;
                }
            }

            return prospect.getLocal().isEmpty() || prospect.toPublicVersion().compareTo(this.version) != 0;
        }

        @Override
        public List<VersionRange> toRanges() {
            if (this.version.getDev().isPresent()) {
                // >V.devN: dev versions have no post-releases, so the
                final int nextDev = this.version.getDev().get() + 1;
                final PypaVersion minVersion = this.version.replace(mod -> mod.withDev(nextDev)
                                                                              .withoutLocal());
                return List.of(new VersionRange(minVersion, null, true, false));
            }

            if (this.version.getPost().isPresent()) {
                // >V.postN: next real version is V.post(N+1).dev0.
                final int nextPost = this.version.getPost().get() + 1;
                final PypaVersion minVersion = this.version.replace(mod -> mod.withPost(nextPost)
                                                                              .withDev(0)
                                                                              .withoutLocal());
                return List.of(new VersionRange(minVersion, null, true, false));
            }

            // >V (final or pre-release V): exclude V itself, V+local, and every V.postN per PEP 440.
            final PypaVersion minVersion = this.version.toBoundaryVersion(PypaVersion.BoundaryType.AfterPosts);
            return List.of(new VersionRange(minVersion, null, false, false));
        }
    }


    /**
     * Represents the constructor of a specifier class.
     */
    @FunctionalInterface
    public interface SpecConstructor {

        /**
         * Constructs the specifier.
         *
         * @param versionId Version identifier for the specifier
         * @return Newly constructed specifier
         * @throws VersionParsingException if there was a problem parsing the version identifier
         */
        Spec construct(String versionId) throws VersionParsingException;
    }


    /**
     * Contains the information to find and instantiate a specifier.
     *
     * @param operator Specifier operator
     * @param constructor Constructor to create the specifier
     */
    private record RegisteredSpec(String operator, SpecConstructor constructor) {
    }


    /** Registry of specifiers. */
    private static final List<RegisteredSpec> SPECS = List.of(
            new RegisteredSpec(ArbitraryEqualSpec.OPERATOR, ArbitraryEqualSpec::new),
            new RegisteredSpec(EqualSpec.OPERATOR, EqualSpec::new),
            new RegisteredSpec(NotEqualSpec.OPERATOR, NotEqualSpec::new),
            new RegisteredSpec(CompatibleSpec.OPERATOR, CompatibleSpec::new),
            new RegisteredSpec(LessThanEqualSpec.OPERATOR, LessThanEqualSpec::new),
            new RegisteredSpec(LessThanSpec.OPERATOR, LessThanSpec::new),
            new RegisteredSpec(GreaterThanEqualSpec.OPERATOR, GreaterThanEqualSpec::new),
            new RegisteredSpec(GreaterThanSpec.OPERATOR, GreaterThanSpec::new)
    );

    private static final Pattern SPECIFIER_PATTERN = Pattern.compile(
        """
        ^(?:\
        ===\\s*[^\\s;)]*\
        |\
        (?:==|!=)\\s*v?(?:[0-9]+!)?[0-9]+(?:\\.[0-9]+)*\
        (?:\\.\\*\
        |\
        (?:[-_.]?(alpha|beta|preview|pre|a|b|c|rc)[-_.]?[0-9]*)?\
        (?:-[0-9]+|[-_.]?(post|rev|r)[-_.]?[0-9]*)?\
        (?:[-_.]?dev[-_.]?[0-9]*)?\
        (?:\\+[a-z0-9]+(?:[-_.][a-z0-9]+)*)?\
        )?\
        |\
        ~=\\s*v?(?:[0-9]+!)?[0-9]+(?:\\.[0-9]+)+\
        (?:[-_.]?(alpha|beta|preview|pre|a|b|c|rc)[-_.]?[0-9]*)?\
        (?:-[0-9]+|[-_.]?(post|rev|r)[-_.]?[0-9]*)?\
        (?:[-_.]?dev[-_.]?[0-9]*)?\
        |\
        (?:<=|>=|<|>)\\s*v?\
        (?:[0-9]+!)?[0-9]+(?:\\.[0-9]+)*\
        (?:[-_.]?(alpha|beta|preview|pre|a|b|c|rc)[-_.]?[0-9]*)?\
        (?:-[0-9]+|[-_.]?(post|rev|r)[-_.]?[0-9]*)?\
        (?:[-_.]?dev[-_.]?[0-9]*)?\
        )$\
        """, Pattern.CASE_INSENSITIVE);

    private final Spec spec;

    private PypaSpecifier(final Spec spec) {
        this.spec = spec;
    }

    /**
     * Parses the specified specifier. Note that multiple specifiers are not supported by this class. Use the
     * {@link PypaSpecifierSet}) class to parse multiple comma separated specifiers.
     *
     * @param specifier Specifier to parse
     * @return Specifier object
     * @throws VersionParsingException if there was a problem parsing the specifier
     */
    static PypaSpecifier parse(final String specifier) throws VersionParsingException {
        if (!SPECIFIER_PATTERN.matcher(specifier).matches()) {
            throw new VersionParsingException("Invalid version specifier: " + specifier);
        }

        for (final RegisteredSpec registeredOperator : SPECS) {
            if (specifier.startsWith((registeredOperator.operator()))) {
                final String versionStr = specifier.substring(registeredOperator.operator().length()).strip();
                final Spec operator = registeredOperator.constructor().construct(versionStr);
                return new PypaSpecifier(operator);
            }
        }

        throw new VersionParsingException("Unknown operator in specified: " + specifier);
    }

    /**
     * Obtains the operator portion of the specifier.
     *
     * @return Specifier operator
     */
    String getOperator() {
        return this.spec.getOperator();
    }

    /**
     * Obtains the version identifier portion of the specifier.
     *
     * @return Specifier version identifier
     */
    String getVersionId() {
        return this.spec.getVersionId();
    }

    /**
     * Indicates if this specifier allows the specified version.
     *
     * @param version Version to test
     * @return {@code true} if this specifier allows the specified version
     * @throws VersionParsingException if there was a problem evaluating the specifier
     */
    boolean allows(final PypaVersion version) throws VersionParsingException {
        return this.spec.allows(version);
    }

    /**
     * Indicates if this specifier allows the specified version.
     *
     * @param version Version to test
     * @return {@code true} if this specifier allows the specified version
     * @throws VersionParsingException if there was a problem evaluating the version or specifier
     */
    boolean allows(final String version) throws VersionParsingException {
        return this.spec.allows(version);
    }

    /**
     * Creates version range(s) to represent the specifier.
     *
     * @return Version range(s) to represent the specifier.
     * @throws UnsupportedOperationException if this method is called on a {@code ===V} specifier. The
     *      arbitrary equality specifier cannot be represented as a version range. Use the
     *      {@link #allows(String)} method.
     */
    @AccessForTesting
    List<VersionRange> toRanges() {
        return this.spec.toRanges();
    }

    @Override
    public String toString() {
        return this.spec.toString();
    }

    /**
     * Provides a canonical string representation of the specifier. The format of the string depends
     * upon the specific operator but typically consists of the operator and a canonicalized form of
     * the version identifier.
     *
     * @return Canonical representation of the specifier
     */
    @AccessForTesting
    String toCanonicalString() {
        return this.spec.toCanonicalString();
    }

    @Override
    public boolean equals(final Object obj) {
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }

        final PypaSpecifier other = (PypaSpecifier)obj;
        return toCanonicalString().equals(other.toCanonicalString());
    }

    @Override
    public int hashCode() {
        return Objects.hash(toCanonicalString());
    }
}
