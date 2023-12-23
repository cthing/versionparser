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

package org.cthing.versionparser.npm;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.annotation.Nullable;

import org.cthing.annotations.NoCoverageGenerated;
import org.cthing.versionparser.semver.SemanticVersion;

import static org.cthing.versionparser.npm.ConstraintComponent.Operator.EQ;
import static org.cthing.versionparser.npm.ConstraintComponent.Operator.GT;
import static org.cthing.versionparser.npm.ConstraintComponent.Operator.GTE;
import static org.cthing.versionparser.npm.ConstraintComponent.Operator.LT;
import static org.cthing.versionparser.npm.ConstraintComponent.Operator.LTE;


/**
 * Translates high level NPM dependency constraints to the equivalent low level constraints. For example, the
 * following translations are performed:
 * <ul>
 *     <li>{@code ~1.2.3} to {@code ≥1.2.3 <1.3.0}</li>
 *     <li>{@code ~1.2} to {@code ≥1.2.0 <1.3.0}</li>
 *     <li>{@code ~1} to {@code ≥1.0.0 <2.0.0}</li>
 *     <li>{@code ~0.2.3} to {@code ≥0.2.3 <0.3.0}</li>
 *     <li>{@code ~0.2} to {@code ≥0.2.0 <0.3.0}</li>
 *     <li>{@code ~0} to {@code ≥0.0.0 <1.0.0}</li>
 *     <li>{@code ^1.2.3} to {@code ≥1.2.3 <2.0.0}</li>
 *     <li>{@code ^0.2.3} to {@code ≥0.2.3 <0.3.0}</li>
 *     <li>{@code 1.2.3 - 2.3.4} to {@code ≥1.2.3 ≤2.3.4}</li>
 *     <li>{@code 1.2 - 2.3.4} to {@code ≥1.2.0 ≤2.3.4}</li>
 *     <li>{@code 1.2.3 - 2.3} to {@code ≥1.2.3 <2.4.0}</li>
 *     <li>{@code 1.2.3 - 2} to {@code ≥1.2.3 <3.0.0}</li>
 * </ul>
 */
public final class ConstraintTranslator {

    static final String GLTL = "((?:<|>)?=?)";

    private static final String XRANGE_IDENTIFIER = String.format(Locale.ROOT, "%s|x|X|\\*|\\+",
                                                                  SemanticVersion.NUMERIC_IDENTIFIER);
    private static final String XRANGE_PLAIN = String.format(Locale.ROOT,
                                                             "[v=\\s]*(%s)(?:\\.(%s)(?:\\.(%s)(?:%s)?%s?)?)?",
                                                             XRANGE_IDENTIFIER, XRANGE_IDENTIFIER, XRANGE_IDENTIFIER,
                                                             SemanticVersion.PRERELEASE, SemanticVersion.BUILD);
    private static final String LONE_CARET = "(?:\\^)";

    private static final Pattern HYPHEN_PATTERN =
            Pattern.compile(String.format(Locale.ROOT, "^\\s*(%s)\\s+-\\s+(%s)\\s*$", XRANGE_PLAIN, XRANGE_PLAIN));
    private static final Pattern CARET_PATTERN = Pattern.compile(String.format(Locale.ROOT, "^%s%s$", LONE_CARET,
                                                                               XRANGE_PLAIN));
    private static final Pattern TILDE_PATTERN = Pattern.compile(String.format(Locale.ROOT, "^(?:~>?)%s$",
                                                                               XRANGE_PLAIN));
    private static final Pattern SPACE_PATTERN = Pattern.compile("\\s+");
    private static final Pattern XRANGE_PATTERN = Pattern.compile(String.format(Locale.ROOT, "^%s\\s*%s$", GLTL,
                                                                                XRANGE_PLAIN));

    private static final int X_RANGE_MARKER = -1;

    @NoCoverageGenerated
    private ConstraintTranslator() {
    }

    /**
     * Performs translations on the specified version constraint.
     *
     * @param constraint Version constraint to translate
     * @return Translated constraint.
     */
    static String translate(final String constraint) {
        String translated = translateGreaterThanOrEqualZero(constraint);
        translated = translateHyphen(translated);
        translated = translateCaret(translated);
        translated = translateTilde(translated);
        return translateXRange(translated);
    }

    /**
     * Translates {@code latest}, {@code latest.internal} and {@code *} strings into classic range. Translates:
     * <ul>
     *     <li>all ranges to {@code ≥0.0.0}</li>
     * </ul>
     *
     * @param constraint Version constraint to translate
     * @return Translated constraint.
     */
    private static String translateGreaterThanOrEqualZero(final String constraint) {
        return ("latest".equals(constraint) || "latest.integration".equals(constraint) || "*".equals(constraint)
                || constraint.isEmpty()) ? GTE.asString() + SemanticVersion.ZERO : constraint;
    }

    /**
     * Translates hyphen notation to equality notation. Translates:
     * <ul>
     *     <li>{@code 1.2.3 - 2.3.4} to {@code ≥1.2.3 ≤2.3.4}</li>
     * </ul>
     *
     * @param constraint Version constraint to translate
     * @return Translated constraint.
     */
    private static String translateHyphen(final String constraint) {
        final Matcher matcher = HYPHEN_PATTERN.matcher(constraint);
        return matcher.matches() ? getRangeFrom(matcher) + " " + getRangeTo(matcher) : constraint;
    }

    /**
     * Translates <a href="https://github.com/npm/node-semver#caret-ranges-123-025-004">caret constraint</a>
     * into classic constraint. Translates:
     * <ul>
     *     <li>{@code ^1.2.3} to {@code ≥1.2.3 <2.0.0-0}</li>
     *     <li>{@code ^0.2.3} to {@code ≥0.2.3 <0.3.0-0}</li>
     * </ul>
     * Note that the lowest priority pre-release version (i.e. "-0") is appended to ensure that pre-release
     * versions of the next major version are not included in the allowed versions.
     *
     * @param constraint Version constraint to translate
     * @return Translated constraint.
     */
    private static String translateCaret(final String constraint) {
        final Matcher matcher = CARET_PATTERN.matcher(constraint);
        if (!matcher.matches()) {
            return constraint;
        }

        final int major = parseIntWithXSupport(matcher.group(1));
        final int minor = parseIntWithXSupport(matcher.group(2));
        final int patch = parseIntWithXSupport(matcher.group(3));
        final String preRelease = matcher.group(4);

        final StringBuilder from = new StringBuilder(GTE.asString());
        final StringBuilder to = new StringBuilder(LT.asString());

        if (isX(minor)) {
            from.append(major).append(".0.0");
            to.append(major + 1).append(".0.0-0");
        } else if (isX(patch)) {
            if (major == 0) {
                from.append(major).append('.').append(minor).append(".0");
                to.append(major).append('.').append(minor + 1).append(".0-0");
            } else {
                from.append(major).append('.').append(minor).append(".0");
                to.append(major + 1).append(".0.0-0");
            }
        } else if (isNotBlank(preRelease)) {
            if (major == 0) {
                if (minor == 0) {
                    from.append(major).append('.').append(minor).append('.').append(patch).append('-').append(preRelease);
                    to.append(major).append('.').append(minor).append('.').append(patch + 1).append("-0");
                } else {
                    from.append(major).append('.').append(minor).append('.').append(patch).append('-').append(preRelease);
                    to.append(major).append('.').append(minor + 1).append(".0-0");
                }
            } else {
                from.append(major).append('.').append(minor).append('.').append(patch).append('-').append(preRelease);
                to.append(major + 1).append(".0.0-0");
            }
        } else {
            if (major == 0) {
                if (minor == 0) {
                    from.append(major).append('.').append(minor).append('.').append(patch);
                    to.append(major).append('.').append(minor).append('.').append(patch + 1).append("-0");
                } else {
                    from.append(major).append('.').append(minor).append('.').append(patch);
                    to.append(major).append('.').append(minor + 1).append(".0-0");
                }
            } else {
                from.append(major).append('.').append(minor).append('.').append(patch);
                to.append(major + 1).append(".0.0-0");
            }
        }

        return from.append(' ').append(to).toString();
    }

    /**
     * Translates <a href="https://github.com/npm/node-semver#tilde-ranges-123-12-1">tilde constraint</a>
     * into classic constraint. Translates:
     * <ul>
     *     <li>{@code ~1.2.3} to {@code ≥1.2.3 <1.3.0-0}</li>
     *     <li>{@code ~1.2} to {@code ≥1.2.0 <1.3.0-0}</li>
     *     <li>{@code ~1} to {@code ≥1.0.0 <2.0.0-0}</li>
     *     <li>{@code ~0.2.3} to {@code ≥0.2.3 <0.3.0-0}</li>
     *     <li>{@code ~0.2} to {@code ≥0.2.0 <0.3.0-0}</li>
     *     <li>{@code ~0} to {@code ≥0.0.0 <1.0.0-0}</li>
     * </ul>
     * Note that the lowest priority pre-release version (i.e. "-0") is appended to ensure that pre-release
     * versions of the next major version are not included in the allowed versions.
     *
     * @param constraint Version constraint to translate
     * @return Translated constraint.
     */
    private static String translateTilde(final String constraint) {
        final Matcher matcher = TILDE_PATTERN.matcher(constraint);
        if (!matcher.matches()) {
            return constraint;
        }

        final int major = parseIntWithXSupport(matcher.group(1));
        final int minor = parseIntWithXSupport(matcher.group(2));
        final int patch = parseIntWithXSupport(matcher.group(3));
        final String preRelease = matcher.group(4);

        final StringBuilder from = new StringBuilder(GTE.asString());
        final StringBuilder to = new StringBuilder(LT.asString());

        if (isX(minor)) {
            from.append(major).append(".0.0");
            to.append(major + 1).append(".0.0-0");
        } else if (isX(patch)) {
            from.append(major).append('.').append(minor).append(".0");
            to.append(major).append('.').append(minor + 1).append(".0-0");
        } else if (isNotBlank(preRelease)) {
            from.append(major).append('.').append(minor).append('.').append(patch).append('-').append(preRelease);
            to.append(major).append('.').append(minor + 1).append(".0-0");
        } else {
            from.append(major).append('.').append(minor).append('.').append(patch);
            to.append(major).append('.').append(minor + 1).append(".0-0");
        }

        return from.append(' ').append(to).toString();
    }

    /**
     * Translates <a href="https://github.com/npm/node-semver#x-ranges-12x-1x-12-">X-Ranges</a> into classic
     * constraint.
     *
     * @param constraint Version constraint to translate
     * @return Translated constraint.
     */
    private static String translateXRange(final String constraint) {
        final String[] constraintVersions = SPACE_PATTERN.split(constraint);

        final List<String> objects = new ArrayList<>();
        for (final String constraintVersion : constraintVersions) {
            final Matcher matcher = XRANGE_PATTERN.matcher(constraintVersion);
            if (!matcher.matches()) {
                continue;
            }

            final String fullRange = matcher.group(0);
            String compareSign = matcher.group(1);
            int major = parseIntWithXSupport(matcher.group(2));
            int minor = parseIntWithXSupport(matcher.group(3));
            int patch = parseIntWithXSupport(matcher.group(4));

            if (compareSign.equals(EQ.asString()) && isX(patch)) {
                compareSign = "";
            }

            if (!compareSign.isEmpty() && isX(patch)) {
                patch = 0;
                if (compareSign.equals(GT.asString())) {
                    compareSign = GTE.asString();

                    if (isX(minor)) {
                        major++;
                        minor = 0;
                    } else {
                        minor++;
                    }
                } else if (compareSign.equals(LTE.asString())) {
                    compareSign = LT.asString();
                    if (isX(minor)) {
                        major++;
                        minor = 0;
                    } else {
                        minor++;
                    }
                } else if (isX(minor)) {
                    minor = 0;
                }

                final String from = compareSign + major + '.' + minor + '.' + patch;
                objects.add(from);
            } else if (isX(minor)) {
                final String from = GTE.asString() + major + ".0.0";
                final String to = LT.asString() + (major + 1) + ".0.0-0";
                objects.add(from);
                objects.add(to);
            } else if (isX(patch)) {
                final String from = GTE.asString() + major + '.' + minor + ".0";
                final String to = LT.asString() + major + '.' + (minor + 1) + ".0-0";
                objects.add(from);
                objects.add(to);
            } else {
                objects.add(fullRange);
            }
        }

        return objects.isEmpty() ? constraint : String.join(" ", objects);
    }

    private static String getRangeFrom(final Matcher matcher) {
        final int fromMajor = parseIntWithXSupport(matcher.group(2));
        final int fromMinor = parseIntWithXSupport(matcher.group(3));

        if (isX(fromMinor)) {
            return GTE.asString() + fromMajor + ".0.0";
        }

        final int fromPatch = parseIntWithXSupport(matcher.group(4));
        if (isX(fromPatch)) {
            return GTE.asString() + fromMajor + '.' + fromMinor + ".0";
        }

        return GTE.asString() + matcher.group(1);
    }

    private static String getRangeTo(final Matcher matcher) {
        final int toMajor = parseIntWithXSupport(matcher.group(8));
        final int toMinor = parseIntWithXSupport(matcher.group(9));

        if (isX(toMinor)) {
            return LT.asString() + (toMajor + 1) + ".0.0-0";
        }

        final int toPatch = parseIntWithXSupport(matcher.group(10));
        if (isX(toPatch)) {
            return LT.asString() + toMajor + '.' + (toMinor + 1) + ".0-0";
        }

        return LTE.asString() + matcher.group(7);
    }

    private static int parseIntWithXSupport(@Nullable final String id) {
        return (id == null || "x".equalsIgnoreCase(id) || "*".equals(id) || "+".equals(id))
               ? X_RANGE_MARKER
               : Integer.parseInt(id);
    }

    private static boolean isX(final Integer id) {
        return id == X_RANGE_MARKER;
    }

    private static boolean isNotBlank(@Nullable final String id) {
        return id != null && !id.isEmpty();
    }
}
