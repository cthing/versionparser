/*
 * Copyright 2023 C Thing Software
 * SPDX-License-Identifier: Apache-2.0
 *
 * This file is derived from
 * org.gradle.api.internal.artifacts.ivyservice.ivyresolve.strategy.VersionParser.java and
 * org.gradle.api.internal.artifacts.ivyservice.ivyresolve.strategy.StaticVersionComparator.java
 * which are covered by the following copyright and permission notices:
 *
 *   Copyright 2014 the original author or authors.
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 */
package org.cthing.versionparser.gradle;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

import org.cthing.versionparser.AbstractVersion;
import org.cthing.versionparser.Version;
import org.jspecify.annotations.Nullable;


/**
 * Represents the version of an artifact in the <a href="https://gradle.org">Gradle</a> ecosystem. For details, see
 * <a href="https://docs.gradle.org/current/userguide/single_versions.html"> Declaring Versions and Ranges</a> in the
 * Gradle User Guide. To obtain an instance of this class, call the {@link GradleVersionScheme#parseVersion(String)}
 * method.
 */
public final class GradleVersion extends AbstractVersion {

    private static final Integer QUALIFIER_DEV = -1;
    private static final Integer QUALIFIER_RC = 1;
    private static final Integer QUALIFIER_SNAPSHOT = 2;
    private static final Integer QUALIFIER_FINAL = 3;
    private static final Integer QUALIFIER_GA = 4;
    private static final Integer QUALIFIER_RELEASE = 5;
    private static final Integer QUALIFIER_SP = 6;

    private static final Map<String, Integer> SPECIAL_MEANINGS = Map.of("dev", QUALIFIER_DEV,
                                                                        "rc", QUALIFIER_RC,
                                                                        "snapshot", QUALIFIER_SNAPSHOT,
                                                                        "final", QUALIFIER_FINAL,
                                                                        "ga", QUALIFIER_GA,
                                                                        "release", QUALIFIER_RELEASE,
                                                                        "sp", QUALIFIER_SP);

    private final List<String> components;
    private final List<@Nullable Long> numericParts;
    private final GradleVersion baseVersion;
    private final boolean preRelease;

    /**
     * Constructs a version from the specified version and its components.
     *
     * @param version Original version string
     * @param parts Components that comprise the version
     * @param baseVersion Version without qualifiers or {@code null} if the version contains no qualifiers.
     */
    private GradleVersion(final String version, final List<String> parts, @Nullable final GradleVersion baseVersion) {
        super(version);

        this.components = Collections.unmodifiableList(parts);
        this.numericParts = parts.stream().map(GradleVersion::parseLong).toList();
        this.baseVersion = baseVersion == null ? this : baseVersion;
        this.preRelease = isQualified() && this.components.stream()
                                                          .map(part -> SPECIAL_MEANINGS.get(part.toLowerCase(Locale.US)))
                                                          .filter(Objects::nonNull)
                                                          .findFirst()
                                                          .filter(qualifier -> (QUALIFIER_DEV.equals(qualifier)
                                                                  || QUALIFIER_RC.equals(qualifier)
                                                                  || QUALIFIER_SNAPSHOT.equals(qualifier))).isPresent();
    }

    /**
     * Indicates whether this version is qualified in any way. For example, 1.2.3 is not qualified, but 1.2-beta-3 is.
     *
     * @return {@code true} if this version is qualified.
     */
    @SuppressWarnings("ObjectEquality")
    public boolean isQualified() {
        return this.baseVersion != this;
    }

    /**
     * Provides the base version for this version, which is the version without any qualifiers. Generally this is the
     * first '.' separated parts of this version (e.g. 1.2.3-beta-4 returns 1.2.3, or 7.0.12beta5 returns 7.0.12).
     *
     * @return Base version for this version, which removes any qualifiers. If there were no qualifiers, the base
     *      version is the same as the original version.
     */
    public GradleVersion getBaseVersion() {
        return this.baseVersion;
    }

    /**
     * Provides all components of this version (e.g. 1.2.3 returns [1,2,3] or 1.2-beta4 returns [1,2,beta,4]).
     *
     * @return All components that comprise this version.
     */
    public List<String> getComponents() {
        return this.components;
    }

    /**
     * Provides all the numeric components of this version, with {@code null} in non-numeric positions
     * (e.g. 1.2.3 returns [1,2,3] or 1.2-beta4 returns [1,2,null,4]).
     *
     * @return All numeric components of this version.
     */
    public List<@Nullable Long> getNumericParts() {
        return this.numericParts;
    }

    @Override
    public boolean isPreRelease() {
        return this.preRelease;
    }

    /**
     * Parses the specified version string and returns a new instance of this class.
     *
     * @param version Version string to parse
     * @return Version object
     */
    static GradleVersion parse(final String version) {
        final List<String> parts = new ArrayList<>();
        boolean digit = false;
        boolean lastWasDelimiter = false;
        int startPart = 0;
        int pos = 0;
        int endBase = 0;
        int endBaseStr = 0;

        for ( ; pos < version.length(); pos++) {
            final char ch = version.charAt(pos);
            if (ch == '.' || ch == '_' || ch == '-' || ch == '+') {
                if (lastWasDelimiter && ch == '+') {
                    lastWasDelimiter = false;
                } else {
                    parts.add(version.substring(startPart, pos));
                    startPart = pos + 1;
                    lastWasDelimiter = true;
                }
                digit = false;
                if (ch != '.' && endBaseStr == 0) {
                    endBase = parts.size();
                    endBaseStr = pos;
                }
            } else if (ch >= '0' && ch <= '9') {
                if (!digit && pos > startPart) {
                    if (endBaseStr == 0) {
                        endBase = parts.size() + 1;
                        endBaseStr = pos;
                    }
                    parts.add(version.substring(startPart, pos));
                    startPart = pos;
                }
                digit = true;
                lastWasDelimiter = false;
            } else {
                if (digit) {
                    if (endBaseStr == 0) {
                        endBase = parts.size() + 1;
                        endBaseStr = pos;
                    }
                    parts.add(version.substring(startPart, pos));
                    startPart = pos;
                }
                digit = false;
                lastWasDelimiter = false;
            }
        }
        if (pos > startPart) {
            parts.add(version.substring(startPart, pos));
        }
        GradleVersion base = null;
        if (endBaseStr > 0) {
            base = new GradleVersion(version.substring(0, endBaseStr), parts.subList(0, endBase), null);
        }
        return new GradleVersion(version, parts, base);
    }

    /**
     * Parses the specified string to a {@code Long} value.
     *
     * @param str String to parse
     * @return Long value represented by the string or {@code null} if the string does not represent an integer value.
     */
    @Nullable
    private static Long parseLong(@Nullable final String str) {
        if (str == null || str.isEmpty()) {
            return null;
        }

        final char firstChar = str.charAt(0);
        if (firstChar == '-' || firstChar == '+') {
            if (str.length() == 1 || !Character.isDigit(str.charAt(1))) {
                return null;
            }
        } else if (!Character.isDigit(firstChar)) {
            return null;
        }
        try {
            return Long.parseLong(str, 10);     // SUPPRESS CHECKSTYLE base 10
        } catch (final NumberFormatException ignore) {
            return null;
        }
    }

    @Override
    public int compareTo(final Version obj) {
        if (getClass() != obj.getClass()) {
            throw new IllegalArgumentException("Expected instance of GradleVersion but received "
                                                       + obj.getClass().getName());
        }

        final GradleVersion otherVersion = (GradleVersion)obj;

        if (toString().equals(otherVersion.toString())) {
            return 0;
        }

        int partIdx = 0;
        for ( ; partIdx < this.components.size() && partIdx < otherVersion.components.size(); partIdx++) {
            final String part1 = this.components.get(partIdx);
            final String part2 = otherVersion.components.get(partIdx);

            final Long numericPart1 = this.numericParts.get(partIdx);
            final Long numericPart2 = otherVersion.numericParts.get(partIdx);

            final boolean is1Number = numericPart1 != null;
            final boolean is2Number = numericPart2 != null;

            if (part1.equals(part2)) {
                continue;
            }
            if (is1Number && !is2Number) {
                return 1;
            }
            if (!is1Number && is2Number) {
                return -1;
            }
            if (is1Number) {
                final int result = numericPart1.compareTo(numericPart2);
                if (result == 0) {
                    continue;
                }
                return result;
            }
            // both are strings, we compare them taking into account special meaning
            final Integer sm1 = SPECIAL_MEANINGS.get(part1.toLowerCase(Locale.US));
            Integer sm2 = SPECIAL_MEANINGS.get(part2.toLowerCase(Locale.US));
            if (sm1 != null) {
                sm2 = sm2 == null ? Integer.valueOf(0) : sm2;
                return sm1 - sm2;
            }
            if (sm2 != null) {
                return -sm2;
            }
            return part1.compareTo(part2);
        }
        if (partIdx < this.components.size()) {
            return this.numericParts.get(partIdx) == null ? -1 : 1;
        }
        if (partIdx < otherVersion.components.size()) {
            return otherVersion.numericParts.get(partIdx) == null ? 1 : -1;
        }

        return 0;
    }

    @Override
    public boolean equals(final Object obj) {
        if (obj == this) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }

        return compareTo((Version)obj) == 0;
    }

    @Override
    public int hashCode() {
        int code = 7;
        for (int i = 0; i < this.components.size(); i++) {
            final Long numericPart = this.numericParts.get(i);
            code = 31 * code + ((numericPart == null) ? this.components.get(i).hashCode() : numericPart.hashCode());
        }
        return code;
    }
}
