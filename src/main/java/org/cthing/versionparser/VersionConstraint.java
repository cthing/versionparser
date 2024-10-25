/*
 * Copyright 2023 C Thing Software
 * SPDX-License-Identifier: Apache-2.0
 */

package org.cthing.versionparser;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.TreeSet;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.jspecify.annotations.Nullable;


/**
 * Represents zero (empty constraint) or more version ranges. A version range is a contiguous set of versions
 * (e.g. version 1.0 inclusive through version 2.0 exclusive). Constraints can be weak or strong (the default).
 * A weak constraint is one that can be ignored by a dependency resolution algorithm if necessary (e.g. Maven
 * undecorated versions). Typically, an instance of this class is created by using a versioning scheme class
 * to parse a version constraint string. The notation for version constraints is specific to a version scheme
 * (e.g. Maven). Please refer to the Javadoc for the specific version scheme classes for information about the
 * notation and the method to call to create an instance of this class.
 */
public class VersionConstraint {

    /** Constraint allowing any version. */
    public static final VersionConstraint ANY = new VersionConstraint();

    /** Constraint not allowing any version. */
    public static final VersionConstraint EMPTY = new VersionConstraint(List.of());

    private final List<VersionRange> ranges;
    private final boolean weak;

    /**
     * Constructs a constraint that allows any version (i.e. {@code null} minimum and maximum versions and
     * exclusive at both ends).
     */
    private VersionConstraint() {
        this(null, null, false, false);
    }

    /**
     * Constructs a strong version constraint consisting of a single version (i.e. the minimum version equals the
     * maximum version inclusive at both ends).
     *
     * @param version Single version comprising the range
     */
    public VersionConstraint(final Version version) {
        this(version, version, true, true, false);
    }

    /**
     * Constructs a version constraint consisting of a single version (i.e. the minimum version equals the maximum
     * version inclusive at both ends).
     *
     * @param version Single version comprising the range
     * @param weak {@code true} if this constraint can be ignored by dependency resolution algorithms
     */
    public VersionConstraint(final Version version, final boolean weak) {
        this(version, version, true, true, weak);
    }

    /**
     * Constructs a strong version range from the specified minimum and maximum versions.
     *
     * @param minVersion Version defining the lower bound of the range. Specify {@code null} if there is no lower
     *      bound.
     * @param maxVersion Version defining the upper bound of the range Specify {@code null} if there is no upper
     *      bound.
     * @param minIncluded {@code true} if the minimum version is included in the range. If the minimum version is
     *      {@code null}, this parameter must be set to {@code false}.
     * @param maxIncluded {@code true} if the maximum version is included in the range. If the maximum version is
     *      {@code null}, this parameter must be set to {@code false}.
     */
    public VersionConstraint(@Nullable final Version minVersion, @Nullable final Version maxVersion,
                             final boolean minIncluded, final boolean maxIncluded) {
        this(minVersion, maxVersion, minIncluded, maxIncluded, false);
    }

    /**
     * Constructs a version range from the specified minimum and maximum versions.
     *
     * @param minVersion Version defining the lower bound of the range. Specify {@code null} if there is no lower
     *      bound.
     * @param maxVersion Version defining the upper bound of the range Specify {@code null} if there is no upper
     *      bound.
     * @param minIncluded {@code true} if the minimum version is included in the range. If the minimum version is
     *      {@code null}, this parameter must be set to {@code false}.
     * @param maxIncluded {@code true} if the maximum version is included in the range. If the maximum version is
     *      {@code null}, this parameter must be set to {@code false}.
     * @param weak {@code true} if this constraint can be ignored by dependency resolution algorithms
     */
    public VersionConstraint(@Nullable final Version minVersion, @Nullable final Version maxVersion,
                             final boolean minIncluded, final boolean maxIncluded, final boolean weak) {
        this.ranges = List.of(new VersionRange(minVersion, maxVersion, minIncluded, maxIncluded));
        this.weak = weak;
    }

    /**
     * Constructs a strong constraint consisting of the union of version ranges.
     *
     * @param ranges Version ranges that comprise this union
     */
    public VersionConstraint(final List<VersionRange> ranges) {
        this(ranges, false);
    }

    /**
     * Constructs a constraint consisting of the union of version ranges.
     *
     * @param ranges Version ranges that comprise this union
     * @param weak {@code true} if this constraint can be ignored by dependency resolution algorithms
     */
    public VersionConstraint(final List<VersionRange> ranges, final boolean weak) {
        this.ranges = ranges.isEmpty() ? List.of() : new TreeSet<>(ranges).stream().toList();
        this.weak = weak;
    }

    /**
     * Obtains the version ranges comprising this constraint.
     *
     * @return Version ranges comprising this constraint. If the constraint is empty (i.e. contains no version
     *      ranges), an empty list is returned.
     */
    public List<VersionRange> getRanges() {
        return this.ranges;
    }

    /**
     * Indicates whether this constraint does not contain any versions.
     *
     * @return {@code true} if this constraint does not contain any versions.
     */
    public boolean isEmpty() {
        return this.ranges.isEmpty();
    }

    /**
     * Indicates whether this constraint contains any versions.
     *
     * @return {@code true} if this constraint contains at least one version.
     */
    public boolean isNotEmpty() {
        return !this.ranges.isEmpty();
    }

    /**
     * Indicates whether this constraint allows all versions.
     *
     * @return {@code true} if this constraint allows all versions.
     */
    public boolean isAny() {
        return this.ranges.stream().anyMatch(VersionRange::isAny);
    }

    /**
     * Indicates whether this constraint can be ignored by a dependency resolution algorithm. For example, Maven
     * treats an undecorated dependency version constraint (e.g. {@code 1.0.0}) as "soft" (i.e. weak), meaning that
     * during dependency mediation, the version can be replaced by a different version if necessary.
     *
     * @return {@code true} if this is a weak constraint.
     */
    public boolean isWeak() {
        return this.weak;
    }

    /**
     * Indicates whether this constraint represents a single version (e.g. [4.0.0]).
     *
     * @return {@code true} if this constraint represents a single version.
     */
    public boolean isSingleVersion() {
        return this.ranges.size() == 1 && this.ranges.get(0).isSingleVersion();
    }

    /**
     * Indicates if the specified version is allowed by this constraint.
     *
     * @param version Version to test
     * @return {@code true} if the specified version is allowed by this constraint.
     */
    public boolean allows(final Version version) {
        return isNotEmpty() && this.ranges.stream().anyMatch(range -> range.allows(version));
    }

    /**
     * Indicates if this constraint allows all versions that are allowed by the specified constraint.
     *
     * @param other Constraint defining the versions to be allowed by this constraint
     * @return {@code true} if this constraint allows all versions allowed by the specified constraint.
     */
    public boolean allowsAll(final VersionConstraint other) {
        if (isEmpty()) {
            return other.isEmpty();
        }

        // Move forward through both lists of ranges because they are in ascending.
        int ourRangesIdx = 0;
        int otherRangesIdx = 0;

        while (ourRangesIdx < this.ranges.size() && otherRangesIdx < other.ranges.size()) {
            final VersionRange ourRange = this.ranges.get(ourRangesIdx);
            final VersionRange otherRange = other.ranges.get(otherRangesIdx);

            if (ourRange.allowsAll(otherRange)) {
                otherRangesIdx++;
            } else {
                ourRangesIdx++;
            }
        }

        // If our ranges have allowed all of their ranges, all of them will be consumed.
        return otherRangesIdx >= other.ranges.size();
    }

    /**
     * Indicates if this constraint allows any of the versions allowed by the specified constraint.
     *
     * @param other Constraint defining the versions any of which are allowed by this constraint
     * @return {@code true} if this constraint allows any of the versions allowed by the specified constraint.
     */
    public boolean allowsAny(final VersionConstraint other) {
        if (isEmpty()) {
            return false;
        }

        // Because both lists of ranges are ordered by minimum version, we can safely move through them linearly here.
        int ourRangesIdx = 0;
        int otherRangesIdx = 0;

        while (ourRangesIdx < this.ranges.size() && otherRangesIdx < other.ranges.size()) {
            final VersionRange ourRange = this.ranges.get(ourRangesIdx);
            final VersionRange otherRange = other.ranges.get(otherRangesIdx);

            if (ourRange.allowsAny(otherRange)) {
                return true;
            }

            // Move the constraint with the lower max value forward. This ensures that we keep both lists in sync
            // as much as possible.
            if (otherRange.allowsHigher(ourRange)) {
                ourRangesIdx++;
            } else {
                otherRangesIdx++;
            }
        }

        return false;
    }

    /**
     * Creates a constraint that only allows versions allowed by both this and the specified constraints.
     * The following examples show the intersection of this constraint T and the specified constraint C:
     * <pre>
     * T = []                    C = []                    T &#8745; C = []
     * T = []                    C = (,)                   T &#8745; C = []
     * T = [1.5]                 C = []                    T &#8745; C = []
     * T = [1.5,2.0]             C = [1.7]                 T &#8745; C = [1.7]
     * T = [1.5,2.0]             C = [1.6,1.9]             T &#8745; C = [1.6,1.9]
     * T = [1.5,)                C = [1.2,1.8]             T &#8745; C = [1.5,1.8]
     * T = (,3.0)                C = (,1.8]                T &#8745; C = (,1.8]
     * T = [1.5,2.0)             C = [1.6],[1.8]           T &#8745; C = [1.6],[1.8]
     * T = [1.5,2.0),[3.0,4.0)   C = []                    T &#8745; C = []
     * T = [1.5,2.0),[3.0,4.0)   C = [1.6,1.6]             T &#8745; C = [1.6,1.6]
     * T = [1.5,2.0),[3.0,4.0)   C = [1.6,1.8]             T &#8745; C = [1.6,1.8]
     * T = [1.5,2.0),[3.0,4.0)   C = [1.6,1.8],[3.2,3.4]   T &#8745; C = [1.6,1.8],[3.2,3.4]
     * </pre>
     *
     * @param other Constraint to intersect with this constraint
     * @return A new constraint that only allows versions allowed by both this and the specified constraints.
     *      Returns an empty constraint if there is no intersection between this and the specified constraints.
     */
    public VersionConstraint intersect(final VersionConstraint other) {
        if (isEmpty()) {
            return this;
        }

        // Because both lists of ranges are ordered by minimum version, we can safely move through them linearly here.
        final List<VersionRange> intersectionRanges = new ArrayList<>();
        int ourRangesIdx = 0;
        int otherRangesIdx = 0;

        while (ourRangesIdx < this.ranges.size() && otherRangesIdx < other.ranges.size()) {
            final VersionRange ourRange = this.ranges.get(ourRangesIdx);
            final VersionRange otherRange = other.ranges.get(otherRangesIdx);

            ourRange.intersect(otherRange).ifPresent(intersectionRanges::add);

            // Move the constraint with the lower max value forward. This ensures that we keep both lists in sync
            // as much as possible, and that large ranges have a chance to match multiple small ranges that they
            // contain.
            if (otherRange.allowsHigher(ourRange)) {
                ourRangesIdx++;
            } else {
                otherRangesIdx++;
            }
        }

        if (intersectionRanges.isEmpty()) {
            return EMPTY;
        }

        return new VersionConstraint(intersectionRanges);
    }

    /**
     * Creates a constraint that allows versions allowed by either this or the specified constraint.
     * The following examples show the union of this constraint T and the specified constraint C:
     * <pre>
     * T = []                    C = []          T &#8746; C = []
     * T = []                    C= (,)          T &#8746; C = (,)
     * T = [1.5]                 C = []          T &#8746; C = [1.5]
     * T = [1.5]                 C = [1.5]       T &#8746; C = [1.5]
     * T = [1.5]                 C = [1.6]       T &#8746; C = [1.5],[1.6]
     * T = [1.5,1.7)             C = [1.6]       T &#8746; C = [1.5,1.7)
     * T = (1.5,)                C = [1.3,2.0]   T &#8746; C = [1.3,)
     * T = (,1.7]                C = [1.5,2.0]   T &#8746; C = (,2.0]
     * T = (,)                   C = [1.5,2.0]   T &#8746; C = (,)
     * T = [1.2,2.0)             C = (,)         T &#8746; C = (,)
     * T = (1.0,2.0)             C = (2.0,3.0)   T &#8746; C = (1.0,2.0),(2.0,3.0)
     * T = [1.5,2.0),[3.0,4.0)   C = [1.6,1.6]   T &#8746; C = [1.5,2.0),[3.0,4.0)
     * </pre>
     *
     * @param other Constraint to for a union with this constraint
     * @return A new constraint that allows versions allowed by either this or the specified constraint.
     */
    public VersionConstraint union(final VersionConstraint other) {
        if (isEmpty()) {
            return other;
        }

        final List<VersionRange> flattenedRanges = Stream.concat(this.ranges.stream(), other.ranges.stream())
                                                         .sorted(Comparable::compareTo)
                                                         .toList();
        if (flattenedRanges.isEmpty()) {
            return EMPTY;
        }

        if (flattenedRanges.stream().anyMatch(VersionRange::isAny)) {
            return ANY;
        }

        final List<VersionRange> mergedRanges = new ArrayList<>();
        for (final VersionRange range : flattenedRanges) {
            if (mergedRanges.isEmpty()) {
                mergedRanges.add(range);
            }

            // Merge this constraint with the previous one if they are adjacent or overlapping. Otherwise,
            // just add the range to the constraint.
            final VersionRange lastRange = mergedRanges.get(mergedRanges.size() - 1);
            if (lastRange.allowsAny(range) || lastRange.isAdjacent(range)) {
                final VersionRange mergedRange = lastRange.merge(range);
                mergedRanges.set(mergedRanges.size() - 1, mergedRange);
            } else {
                mergedRanges.add(range);
            }
        }

        return new VersionConstraint(mergedRanges);
    }

    /**
     * Creates a constraint that allows versions allowed by this constraint but not by the specified constraint.
     * The following examples show the difference between this constraint T and the specified constraint C:
     * <pre>
     * T = [1.5]                 C = []                    T - C = [1.5]
     * T = [1.5]                 C = [1.5]                 T - C = []
     * T = [1.2,2.0)             C = [1.5,1.5]             T - C = [1.2,1.5),(1.5,2.0)
     * T = [1.2,2.0)             C = [1.2,1.2]             T - C = (1.2,2.0)
     * T = [1.2,2.0)             C = (,)                   T - C = []
     * T = [1.2,2.0)             C = [1.5,1.5],[1.7,1.7]   T - C = [1.2,1.5),(1.5,1.7),(1.7,2.0)
     * T = [1.5],[2.0,3.0)       C = []                    T - C = [1.5],[2.0,3.0)
     * T = [1.5],[2.0,3.0)       C = [1.5,1.5]             T - C = [2.0,3.0)
     * T = [1.0,3.0),(4.0,6.0)   C = [2.0],[7.0,8.0)       T - C = [1.0,2.0),(2.0,3.0),(4.0,6.0)
     * T = []                    C = []                    T - C = []
     * T = []                    C = [1.2,2.0)             T - C = []
     * </pre>
     *
     * @param other Constraint to form the difference with this constraint
     * @return A new constraint that allows versions allowed by this constraint but not by the specified constraint.
     */
    public VersionConstraint difference(final VersionConstraint other) {
        if (isEmpty()) {
            return this;
        }
        if (other.isEmpty()) {
            return this;
        }

        final List<VersionRange> differenceRanges = new ArrayList<>();

        int ourRangesIdx = 0;
        int otherRangesIdx = 0;

        VersionRange current = this.ranges.get(ourRangesIdx);

        while (true) {
            // If the current ranges are disjoint, move the lowest one forward.
            VersionRange otherCurrentRange = other.ranges.get(otherRangesIdx);
            if (otherCurrentRange.strictlyLower(current)) {
                if (++otherRangesIdx < other.ranges.size()) {
                    continue;
                }

                // If there are no more of other ranges, none of the rest of our ranges need to be subtracted,
                // so we can add them as-is.
                differenceRanges.add(current);
                while (++ourRangesIdx < this.ranges.size()) {
                    differenceRanges.add(this.ranges.get(ourRangesIdx));
                }
                break;
            }

            otherCurrentRange = other.ranges.get(otherRangesIdx);
            if (otherCurrentRange.strictlyHigher(current)) {
                differenceRanges.add(current);
                if (++ourRangesIdx >= this.ranges.size()) {
                    break;
                }
                current = this.ranges.get(ourRangesIdx);
                continue;
            }

            // At this point [otherRanges.current] overlaps [current].
            otherCurrentRange = other.ranges.get(otherRangesIdx);
            final List<VersionRange> curentDifferenceRanges = current.difference(otherCurrentRange);
            if (curentDifferenceRanges.size() > 1) {
                // If other range split [current] in half, we only need to continue checking future ranges against
                // the latter half.
                if (curentDifferenceRanges.size() != 2) {
                    throw new IllegalStateException("Difference union must contain exactly two ranges, contains "
                                                            + curentDifferenceRanges.size());
                }
                differenceRanges.add(curentDifferenceRanges.get(0));
                current = curentDifferenceRanges.get(1);

                // Since other range split [current], it definitely doesn't allow higher
                // versions, so we should move other ranges forward.

                if (++otherRangesIdx >= other.ranges.size()) {
                    // If there are no more of other ranges, none of the rest of our ranges need to be subtracted,
                    // so we can add them as-is.
                    differenceRanges.add(current);
                    while (++ourRangesIdx < this.ranges.size()) {
                        differenceRanges.add(this.ranges.get(ourRangesIdx));
                    }
                    break;
                }
            } else if (curentDifferenceRanges.isEmpty()) {
                if (++ourRangesIdx >= this.ranges.size()) {
                    break;
                }
                current = this.ranges.get(ourRangesIdx);
            } else {
                current = curentDifferenceRanges.get(0);

                // Move the constraint with the lower max value forward. This ensures that we keep both lists in sync
                // as much as possible, and that large ranges have a chance to subtract or be subtracted by multiple
                // small ranges that they contain.
                otherCurrentRange = other.ranges.get(otherRangesIdx);
                if (current.allowsHigher(otherCurrentRange)) {
                    if (++otherRangesIdx >= other.ranges.size()) {
                        // If there are no more of their ranges, none of the rest of our ranges need to be subtracted,
                        // so we can add them as-is.
                        differenceRanges.add(current);
                        while (++ourRangesIdx < this.ranges.size()) {
                            differenceRanges.add(this.ranges.get(ourRangesIdx));
                        }
                        break;
                    }
                } else {
                    differenceRanges.add(current);
                    if (++ourRangesIdx >= this.ranges.size()) {
                        break;
                    }
                    current = this.ranges.get(ourRangesIdx);
                }
            }
        }

        return differenceRanges.isEmpty() ? EMPTY : new VersionConstraint(differenceRanges);
    }

    /**
     * Creates a constraint that is the complement of this constraint. The following examples show the constraint C
     * and its complement C':
     * <pre>
     * C = []                          C' = (,)
     * C = [1.5]                       C' = (,1.5),(1.5,)
     * C = [1.2,2.0)                   C' = (,1.2),[2.0,)
     * C = [1.5],[2.0,3.0)             C' = (,1.5),(1.5,2.0),[3.0,)
     * C = [1.5],[2.0,3.0),[4.0,7.0)   C' = (,1.5),(1.5,2.0),[3.0,4.0),[7.0,)
     * </pre>
     *
     * @return Complement of this constraint.
     */
    public VersionConstraint complement() {
        return ANY.difference(this);
    }

    @Override
    public boolean equals(final Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }

        final VersionConstraint that = (VersionConstraint)obj;
        return this.weak == that.weak && Objects.equals(this.ranges, that.ranges);
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.ranges, this.weak);
    }

    @Override
    public String toString() {
        return isEmpty() ? "<empty>" : this.ranges.stream().map(Object::toString).collect(Collectors.joining(","));
    }
}
