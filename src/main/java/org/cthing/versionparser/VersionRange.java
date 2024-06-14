/*
 * Copyright 2023 C Thing Software
 * SPDX-License-Identifier: Apache-2.0
 */

package org.cthing.versionparser;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Supplier;

import javax.annotation.Nullable;


/**
 * Represents a single range of versions. A version range is a contiguous set of versions (e.g. version 1.0 inclusive
 * through version 2.0 exclusive). While this class can be directly instantiated, a more typical use case is to use
 * one of the version scheme classes to parse a version constraint, which contains instances of this class.
 */
public class VersionRange implements Comparable<VersionRange> {

    @Nullable
    private final Version minVersion;

    @Nullable
    private final Version maxVersion;

    private final boolean minIncluded;
    private final boolean maxIncluded;

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
     * @throws IllegalArgumentException if the minimum version is greater than the maximum version.
     */
    public VersionRange(@Nullable final Version minVersion, @Nullable final Version maxVersion,
                        final boolean minIncluded, final boolean maxIncluded) {
        if (minVersion != null && maxVersion != null && minVersion.compareTo(maxVersion) > 0) {
            throw new IllegalArgumentException("Minimum version must be less than maximum version");
        }

        this.minVersion = minVersion;
        this.maxVersion = maxVersion;
        this.minIncluded = minIncluded;
        this.maxIncluded = maxIncluded;
    }

    /**
     * Constructs a version range consisting of a single version (i.e. the minimum version equals the maximum
     * version inclusive at both ends).
     *
     * @param version Single version comprising the range
     */
    public VersionRange(final Version version) {
        this(version, version, true, true);
    }

    /**
     * Obtains the lower bound version in the range.
     *
     * @return Lower bound version or {@code null} if the lower bound in unlimited.
     */
    @Nullable
    public Version getMinVersion() {
        return this.minVersion;
    }

    /**
     * Obtains the upper bound version in the range.
     *
     * @return Upper bound version or {@code null} if the upper bound in unlimited.
     */
    @Nullable
    public Version getMaxVersion() {
        return this.maxVersion;
    }

    /**
     * Indicates whether the lower bound version in included in the range.
     *
     * @return {@code true} if the lower bound version is included in the range.
     */
    public boolean isMinIncluded() {
        return this.minIncluded;
    }

    /**
     * Indicates whether the upper bound version in included in the range.
     *
     * @return {@code true} if the upper bound version is included in the range.
     */
    public boolean isMaxIncluded() {
        return this.maxIncluded;
    }

    /**
     * Indicates whether this range allows all versions.
     *
     * @return {@code true} if this range allows all versions.
     */
    boolean isAny() {
        return (this.minVersion == null) && (this.maxVersion == null);
    }

    /**
     * Indicates whether this range represents a single version (e.g. [4.0.0]).
     *
     * @return {@code true} if this range represents a single version.
     */
    boolean isSingleVersion() {
        return compare(this.minVersion, this.maxVersion) == 0 && this.minIncluded && this.maxIncluded;
    }

    /**
     * Indicates if this range allows the specified version.
     *
     * @param version Version to test
     * @return {@code true} if the specified version is allowed by this range.
     */
    boolean allows(final Version version) {
        if (this.minVersion != null) {
            if ((version.compareTo(this.minVersion) < 0)
                    || (!this.minIncluded && version.compareTo(this.minVersion) == 0)) {
                return false;
            }
        }

        if (this.maxVersion != null) {
            return (version.compareTo(this.maxVersion) <= 0)
                    && (this.maxIncluded || version.compareTo(this.maxVersion) != 0);
        }

        return true;
    }

    /**
     * Indicates if this range allows all versions that are allowed by the specified range.
     *
     * @param other Range defining the versions to test against this range
     * @return {@code true} if this range allows all versions allowed by the specified range.
     */
    boolean allowsAll(final VersionRange other) {
        return !other.allowsLower(this) && !other.allowsHigher(this);
    }

    /**
     * Indicates if this range allows any of the versions allowed by the specified range.
     *
     * @param other Range defining the versions to test against this range
     * @return {@code true} if this range allows any of the versions allowed by the specified range.
     */
    boolean allowsAny(final VersionRange other) {
        return !other.strictlyLower(this) && !other.strictlyHigher(this);
    }

    /**
     * Creates a constraint that only allows versions allowed by both this range and the specified range.
     *
     * @param other Range to intersect with this range
     * @return Ranges that only allows versions allowed by both this range and the specified range.
     *      Returns an empty optional if there is no intersection between this range and the specified range.
     */
    Optional<VersionRange> intersect(final VersionRange other) {
        // Intersect the two ranges.
        final Version intersectMinVersion;
        final boolean intersectMinIncluded;
        if (allowsLower(other)) {
            if (strictlyLower(other)) {
                return Optional.empty();
            }
            intersectMinVersion = other.minVersion;
            intersectMinIncluded = other.minIncluded;
        } else {
            if (other.strictlyLower(this)) {
                return Optional.empty();
            }
            intersectMinVersion = this.minVersion;
            intersectMinIncluded = this.minIncluded;
        }

        final Version intersectMaxVersion;
        final boolean intersectMaxIncluded;
        if (allowsHigher(other)) {
            intersectMaxVersion = other.maxVersion;
            intersectMaxIncluded = other.maxIncluded;
        } else {
            intersectMaxVersion = this.maxVersion;
            intersectMaxIncluded = this.maxIncluded;
        }

        if (intersectMinVersion == null && intersectMaxVersion == null) {
            // Open range.
            return Optional.of(new VersionRange(null, null, false, false));
        }

        // If the range is just a single version.
        if (compare(intersectMinVersion, intersectMaxVersion) == 0) {
            // Because we already verified that the lower range isn't strictly lower, there must be some overlap.
            if (!intersectMinIncluded || !intersectMaxIncluded) {
                throw new IllegalStateException("Min and max not included in intersection");
            }
            assert intersectMinVersion != null;
            return Optional.of(new VersionRange(intersectMinVersion));
        }

        // If we got here, there is an actual range.
        return Optional.of(new VersionRange(intersectMinVersion, intersectMaxVersion,
                                            intersectMinIncluded, intersectMaxIncluded));
    }

    /**
     * Creates a list of range that allows versions allowed by this range but not by the specified range.
     *
     * @param other Range to form the difference with this range
     * @return A list of ranges that allows versions allowed by this range but not by the specified range.
     */
    List<VersionRange> difference(final VersionRange other) {
        if (!allowsAny(other)) {
            return List.of(this);
        }

        final VersionRange before;
        if (!allowsLower(other)) {
            before = null;
        } else if (compare(this.minVersion, other.minVersion) == 0) {
            if (!this.minIncluded || other.minIncluded) {
                throw new IllegalStateException("Minimum included cannot equal other minimum included");
            }
            if (this.minVersion == null) {
                throw new IllegalStateException("Minimum version cannot be null");
            }
            before = new VersionRange(this.minVersion);
        } else {
            before = new VersionRange(this.minVersion, other.minVersion, this.minIncluded, !other.minIncluded);
        }

        final VersionRange after;
        if (!allowsHigher(other)) {
            after = null;
        } else if (compare(this.maxVersion, other.maxVersion) == 0) {
            if (!this.maxIncluded || other.maxIncluded) {
                throw new IllegalStateException("Maximum included cannot equal other maximum included");
            }
            if (this.maxVersion == null) {
                throw new IllegalStateException("Maximum version cannot be null");
            }
            after = new VersionRange(this.maxVersion);
        } else {
            after = new VersionRange(other.maxVersion, this.maxVersion, !other.maxIncluded, this.maxIncluded);
        }

        if (before == null && after == null) {
            return List.of();
        }

        if (before == null) {
            return List.of(after);
        }

        if (after == null) {
            return List.of(before);
        }

        return List.of(before, after);
    }

    /**
     * Indicates whether this range is immediately adjacent to the specified range without overlapping. The
     * ranges are tested for adjacency on either side.
     *
     * @param other Range to test
     * @return {@code true} if this is immediately adjacent to the specified range without overlapping.
     */
    boolean isAdjacent(final VersionRange other) {
        if (compare(this.maxVersion, other.minVersion) == 0) {
            return this.maxIncluded ^ other.minIncluded;
        }
        if (compare(this.minVersion, other.maxVersion) == 0) {
            return this.minIncluded ^ other.maxIncluded;
        }
        return false;
    }

    /**
     * Merges this and the specified adjacent or overlapping range into a single range. The specified range is
     * assumed to be adjacent or overlapping this range, no check is performed.
     *
     * @param other Adjacent or overlapping ange to be merged with this range
     * @return A new version range representing the merger of this range and the specified ranges
     */
    VersionRange merge(final VersionRange other) {
        final Version unionMinVersion;
        final boolean unionMinIncluded;
        if (allowsLower(other)) {
            unionMinVersion = this.minVersion;
            unionMinIncluded = this.minIncluded;
        } else {
            unionMinVersion = other.minVersion;
            unionMinIncluded = other.minIncluded;
        }

        final Version unionMaxVersion;
        final boolean unionMaxIncluded;
        if (allowsHigher(other)) {
            unionMaxVersion = this.maxVersion;
            unionMaxIncluded = this.maxIncluded;
        } else {
            unionMaxVersion = other.maxVersion;
            unionMaxIncluded = other.maxIncluded;
        }

        return new VersionRange(unionMinVersion, unionMaxVersion, unionMinIncluded, unionMaxIncluded);
    }

    /**
     * Indicates whether this range allows lower versions than the specified range.
     *
     * @param other Version range to test
     * @return {@code true} if this range allows lower versions than the specified range.
     */
    boolean allowsLower(final VersionRange other) {
        if (this.minVersion == null) {
            return other.minVersion != null;
        }
        if (other.minVersion == null) {
            return false;
        }

        final int result = this.minVersion.compareTo(other.minVersion);
        if (result < 0) {
            return true;
        }
        if (result > 0) {
            return false;
        }
        return this.minIncluded && !other.minIncluded;
    }

    /**
     * Indicates whether this range allows higher versions than the specified range.
     *
     * @param other Version range to test
     * @return {@code true} if this range allows higher versions than the specified range.
     */
    boolean allowsHigher(final VersionRange other) {
        if (this.maxVersion == null) {
            return other.maxVersion != null;
        }
        if (other.maxVersion == null) {
            return false;
        }

        final int result = this.maxVersion.compareTo(other.maxVersion);
        if (result > 0) {
            return true;
        }
        if (result < 0) {
            return false;
        }
        return this.maxIncluded && !other.maxIncluded;
    }

    /**
     * Indicates whether this range allows only versions lower than those allowed by the specified range.
     *
     * @param other Range to test
     * @return {@code true} if this range allows only version lower than those allowed by the specified range.
     */
    boolean strictlyLower(final VersionRange other) {
        if (this.maxVersion == null || other.minVersion == null) {
            return false;
        }

        final int result = this.maxVersion.compareTo(other.minVersion);
        if (result < 0) {
            return true;
        }
        if (result > 0) {
            return false;
        }
        return !this.maxIncluded || !other.minIncluded;
    }

    /**
     * Indicates whether this range allows only versions higher than those allowed by the specified range.
     *
     * @param other Range to test
     * @return {@code true} if this range allows only versions higher than those allowed by the specified range.
     */
    boolean strictlyHigher(final VersionRange other) {
        return other.strictlyLower(this);
    }

    /**
     * Performs null-safe comparison of the specified versions.
     *
     * @param version1 First version to compare
     * @param version2 Second version to compare
     * @return Zero if the two versions are equal, less than zero if the first version is less than the second version,
     *      and greater than zero if the first version is greater than the second. If both versions are {@code null},
     *      they are considered equal. A {@code null} version is considered less than a non-null version.
     */
    @SuppressWarnings("ObjectEquality")
    private static int compare(@Nullable final Version version1, @Nullable final Version version2) {
        if (version1 == version2) {
            return 0;
        }
        if (version1 == null) {
            return -1;
        }
        if (version2 == null) {
            return 1;
        }
        return version1.compareTo(version2);
    }

    @Override
    public int compareTo(final VersionRange other) {
        final Supplier<Integer> compareMax = () -> {
            if (this.maxVersion == null) {
                return (other.maxVersion == null) ? 0 : 1;
            }
            if (other.maxVersion == null) {
                return -1;
            }

            final int result = this.maxVersion.compareTo(other.maxVersion);
            if (result != 0) {
                return result;
            }
            if (this.maxIncluded != other.maxIncluded) {
                return this.maxIncluded ? 1 : -1;
            }
            return 0;
        };


        if (this.minVersion == null) {
            return (other.minVersion == null) ? compareMax.get() : -1;
        }
        if (other.minVersion == null) {
            return 1;
        }

        final int result = this.minVersion.compareTo(other.minVersion);
        if (result != 0) {
            return result;
        }
        if (this.minIncluded != other.minIncluded) {
            return this.minIncluded ? -1 : 1;
        }
        return compareMax.get();
    }

    @Override
    public boolean equals(final Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }

        return compareTo((VersionRange)obj) == 0;
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.minVersion, this.maxVersion, this.minIncluded, this.maxIncluded);
    }

    @Override
    public String toString() {
        final StringBuilder builder = new StringBuilder();

        if (this.minVersion == null) {
            builder.append("(,");
        } else {
            builder.append(this.minIncluded ? '[' : '(');
            builder.append(this.minVersion);

            if (compare(this.minVersion, this.maxVersion) != 0 || !this.minIncluded || !this.maxIncluded) {
                builder.append(',');
            }
        }

        if (this.maxVersion == null) {
            builder.append(')');
        } else {
            if (compare(this.maxVersion, this.minVersion) != 0 || !this.minIncluded || !this.maxIncluded) {
                builder.append(this.maxVersion);
            }
            builder.append(this.maxIncluded ? ']' : ')');
        }

        return builder.toString();
    }
}
