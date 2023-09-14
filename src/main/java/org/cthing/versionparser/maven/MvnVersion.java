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
 * This file is derived from org.eclipse.aether.util.version.GenericVersion.java
 * which is covered by the following copyright and permission notices:
 *
 *   Licensed to the Apache Software Foundation (ASF) under one
 *   or more contributor license agreements.  See the NOTICE file
 *   distributed with this work for additional information
 *   regarding copyright ownership.  The ASF licenses this file
 *   to you under the Apache License, Version 2.0 (the
 *   "License"); you may not use this file except in compliance
 *   with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing,
 *   software distributed under the License is distributed on an
 *   "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *   KIND, either express or implied.  See the License for the
 *   specific language governing permissions and limitations
 *   under the License.
 */

package org.cthing.versionparser.maven;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;

import javax.annotation.Nullable;

import org.cthing.versionparser.AbstractVersion;
import org.cthing.versionparser.Version;


/**
 * Represents the version of an artifact in the <a href="https://maven.apache.org">Maven</a> ecosystem. To obtain
 * an instance of this class, call the  {@link MvnVersionScheme#parseVersion(String)} method.
 * <p>
 * Versions are interpreted as a sequence of numeric and alphabetic components. The characters '-', '_', and '.' as
 * well as the transitions from digit to letter and vice versa delimit the version components. Delimiters are
 * considered equivalent.
 * </p>
 * <p>
 * Numeric components are compared mathematically. Alphabetic components are treated as case-insensitive and compared
 * lexicographically. However, the following qualifier strings are treated specially with the following ordering:
 * {@code alpha}/{@code a} &lt; {@code beta}/{@code b} &lt; {@code milestone}/{@code m} &lt; {@code cr}/{@code rc}
 * &lt; {@code snapshot} &lt; {@code final}/{@code ga} &lt; {@code sp}. Each of these
 * well-known qualifiers are considered smaller/older than other strings. An empty component or string is equivalent to
 * 0.
 * </p>
 * <p>
 * In addition to the above mentioned qualifiers, the tokens {@code min} and {@code max} may be used as the last
 * version component to denote the smallest/greatest version having a given prefix. For example, "1.2.min" denotes
 * the smallest version in the 1.2 line, and "1.2.max" denotes the greatest version in the 1.2 line.
 * </p>
 * <p>
 * Numbers and strings are considered incomparable when compared against each other. Where version components of
 * a different kind collide, comparison assumes that the previous components are padded with trailing 0 or "ga"
 * components, respectively, until the mismatch is resolved (e.g. "1-alpha" = "1.0.0-alpha" &lt; "1.0.1-ga" = "1.0.1").
 * </p>
 */
public final class MvnVersion extends AbstractVersion {

    private final List<Component> components;

    /**
     * Creates a version instance.
     *
     * @param version Original version string
     * @param components Individual items that make up the version
     */
    private MvnVersion(final String version, final List<Component> components) {
        super(version);
        this.components = components;
    }

    /**
     * Provides the parsed components of the version.
     *
     * @return Parsed components of the version. Note that due to removal of padding and other substitutions,
     *      reconstructing the version from the components may not match the original specified version.
     */
    public List<String> getComponents() {
        return this.components.stream().map(Component::toString).toList();
    }

    @Override
    public boolean isPreRelease() {
        return this.components.stream()
                              .filter(Component::isQualifier)
                              .mapToInt(component -> (int)component.value())
                              .anyMatch(qualifier -> qualifier != Tokenizer.QUALIFIER_RELEASE
                                      && qualifier != Tokenizer.QUALIFIER_SP);
    }

    /**
     * Parses the specified version string and returns a new instance of this class. To parse a version, call
     * {@link MvnVersionScheme#parseVersion(String)}.
     *
     * @param version Version string to parse
     * @return Version object
     */
    static MvnVersion parse(final String version) {
        final String trimmedVersion = version.trim();

        final List<Component> comps = new ArrayList<>();
        new Tokenizer(trimmedVersion).forEach(comps::add);

        trimPadding(comps);

        return new MvnVersion(trimmedVersion, Collections.unmodifiableList(comps));
    }

    /**
     * Removes from the specified list of components all trailing zeroes and qualifiers that correspond to releases.
     *
     * @param comps Components of the version to be pruned of trailing zeroes and release qualifiers.
     */
    private static void trimPadding(final List<Component> comps) {
        Boolean number = null;
        int end = comps.size() - 1;
        for (int i = end; i > 0; i--) {
            final Component comp = comps.get(i);

            if (!Boolean.valueOf(comp.isNumber()).equals(number)) {
                end = i;
                number = comp.isNumber();
            }

            if (end == i && (i == comps.size() - 1 || comps.get(i - 1).isNumber() == comp.isNumber())
                    && comp.compareTo(null) == 0) {
                comps.remove(i);
                end--;
            }
        }
    }

    @Override
    public int compareTo(final Version obj) {
        if (getClass() != obj.getClass()) {
            throw new IllegalArgumentException("Expected instance of MvnVersion but received "
                                                       + obj.getClass().getName());
        }

        final List<Component> those = ((MvnVersion)obj).components;
        final List<Component> these = this.components;

        boolean isNumber = true;

        for (int index = 0; ; index++) {
            if (index >= these.size() && index >= those.size()) {
                return 0;
            }
            if (index >= these.size()) {
                return -comparePadding(those, index, null);
            }
            if (index >= those.size()) {
                return comparePadding(these, index, null);
            }

            final Component thisComponent = these.get(index);
            final Component thatComponent = those.get(index);

            if (thisComponent.isNumber() != thatComponent.isNumber()) {
                return (isNumber == thisComponent.isNumber())
                       ? comparePadding(these, index, isNumber)
                       : -comparePadding(those, index, isNumber);
            }

            final int rel = thisComponent.compareTo(thatComponent);
            if (rel != 0) {
                return rel;
            }

            isNumber = thisComponent.isNumber();
        }
    }

    private static int comparePadding(final List<Component> comps, final int index, @Nullable final Boolean number) {
        int rel = 0;
        for (int i = index; i < comps.size(); i++) {
            final Component comp = comps.get(i);

            if (number != null && number != comp.isNumber()) {
                break;
            }

            rel = comp.compareTo(null);
            if (rel != 0) {
                break;
            }
        }
        return rel;
    }

    @Override
    public boolean equals(final Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }

        return compareTo((Version)obj) == 0;
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(this.components);
    }


    /**
     * Performs the parsing of a version into components. The components are categorized whether they are
     * numeric, string, or qualifier keyword.
     */
    private static final class Tokenizer implements Iterable<Component>, Iterator<Component> {

        private enum State {
            INITIAL,
            LETTER,
            LEADING_ZERO,
            REGULAR_DIGIT,
        }

        private record Token(String value, boolean isNumber, boolean terminatedByNumber) {
        }

        static final int QUALIFIER_ALPHA = -5;
        static final int QUALIFIER_BETA = -4;
        static final int QUALIFIER_MILESTONE = -3;
        static final int QUALIFIER_RC = -2;
        static final int QUALIFIER_SNAPSHOT = -1;
        static final int QUALIFIER_RELEASE = 0;
        static final int QUALIFIER_SP = 1;

        private static final int MAX_INTEGER_CHARS = 10;

        private static final Map<String, Integer> QUALIFIERS = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
        static {
            QUALIFIERS.put("alpha", QUALIFIER_ALPHA);
            QUALIFIERS.put("beta", QUALIFIER_BETA);
            QUALIFIERS.put("milestone", QUALIFIER_MILESTONE);
            QUALIFIERS.put("cr", QUALIFIER_RC);
            QUALIFIERS.put("rc", QUALIFIER_RC);
            QUALIFIERS.put("snapshot", QUALIFIER_SNAPSHOT);
            QUALIFIERS.put("ga", QUALIFIER_RELEASE);
            QUALIFIERS.put("final", QUALIFIER_RELEASE);
            QUALIFIERS.put("release", QUALIFIER_RELEASE);
            QUALIFIERS.put("", QUALIFIER_RELEASE);
            QUALIFIERS.put("sp", QUALIFIER_SP);
        }

        private final String version;
        private int index;
        @Nullable private Token token;

        Tokenizer(final String version) {
            this.version = version.isEmpty() ? "0" : version;
        }

        @Override
        public Iterator<Component> iterator() {
            return this;
        }

        @Override
        public boolean hasNext() {
            final int n = this.version.length();
            if (this.index >= n) {
                return false;
            }

            State state = State.INITIAL;
            int start = this.index;
            int end = n;

            boolean terminatedByNumber = false;

            for ( ; this.index < n; this.index++) {
                final char c = this.version.charAt(this.index);

                if (c == '.' || c == '-' || c == '_') {
                    end = this.index;
                    this.index++;
                    break;
                }

                final int digit = Character.digit(c, 10);
                if (digit >= 0) {   // Character is a digit
                    // If we were seeing letters, we have a transition delimiter
                    if (state == State.LETTER) {
                        end = this.index;
                        terminatedByNumber = true;
                        break;
                    }
                    // If we are still seeing leading zeros, strip them so that Integer/BigInteger are happy.
                    if (state == State.LEADING_ZERO) {
                        start++;
                    }
                    state = (state == State.REGULAR_DIGIT || digit > 0) ? State.REGULAR_DIGIT : State.LEADING_ZERO;
                } else {    // Character is a letter
                    // If we were seeing digits, we have a transition delimiter
                    if (state == State.REGULAR_DIGIT || state == State.LEADING_ZERO) {
                        end = this.index;
                        break;
                    }
                    state = State.LETTER;
                }
            }

            if (end - start > 0) {
                final String value = this.version.substring(start, end);
                final boolean isNumber = (state == State.REGULAR_DIGIT || state == State.LEADING_ZERO);
                this.token = new Token(value, isNumber, terminatedByNumber);
            } else {
                this.token = new Token("0", true, terminatedByNumber);
            }

            return true;
        }

        @Override
        public Component next() {
            if (this.token == null) {
                throw new IllegalStateException("token cannot be null");
            }

            if (this.token.isNumber) {
                try {
                    return (this.token.value.length() < MAX_INTEGER_CHARS)
                           ? new Component(Component.KIND_INT, Integer.valueOf(this.token.value), this.token.value)
                           : new Component(Component.KIND_BIGINT, new BigInteger(this.token.value), this.token.value);
                } catch (final NumberFormatException ex) {
                    throw new IllegalStateException(ex);
                }
            }

            if (this.index >= this.version.length()) {
                if ("min".equalsIgnoreCase(this.token.value)) {
                    return Component.MIN;
                }
                if ("max".equalsIgnoreCase(this.token.value)) {
                    return Component.MAX;
                }
            }

            if (this.token.terminatedByNumber && this.token.value.length() == 1) {
                switch (this.token.value.charAt(0)) {
                    case 'a', 'A' -> {
                        return new Component(Component.KIND_QUALIFIER, QUALIFIER_ALPHA, this.token.value);
                    }
                    case 'b', 'B' -> {
                        return new Component(Component.KIND_QUALIFIER, QUALIFIER_BETA, this.token.value);
                    }
                    case 'm', 'M' -> {
                        return new Component(Component.KIND_QUALIFIER, QUALIFIER_MILESTONE, this.token.value);
                    }
                    default -> {
                    }
                }
            }

            final Integer qualifier = QUALIFIERS.get(this.token.value);
            return (qualifier == null)
                   ? new Component(Component.KIND_STRING, this.token.value.toLowerCase(Locale.ENGLISH), this.token.value)
                   : new Component(Component.KIND_QUALIFIER, qualifier, this.token.value);
        }

        @Override
        public String toString() {
            return (this.token == null) ? "" : this.token.value;
        }
    }


    /**
     * Represents a component of a version string. For example, the version "1.2.3", consists of three components "1",
     * "2", and "3".
     *
     * @param kind Type of the component
     * @param value Value of the component
     * @param original Component as specific in the version
     */
    private record Component(int kind, Object value, String original) implements Comparable<Component> {

        static final int KIND_MAX = 8;
        static final int KIND_BIGINT = 5;
        static final int KIND_INT = 4;
        static final int KIND_STRING = 3;
        static final int KIND_QUALIFIER = 2;
        static final int KIND_MIN = 0;
        static final Component MAX = new Component(KIND_MAX, "max", "max");
        static final Component MIN = new Component(KIND_MIN, "min", "min");

        public boolean isNumber() {
            return (this.kind & KIND_QUALIFIER) == 0;    // i.e. kind != string/qualifier
        }

        public boolean isQualifier() {
            return this.kind == KIND_QUALIFIER;
        }

        public boolean isEmpty() {
            return switch (this.kind) {
                case KIND_MAX, KIND_MIN -> false;
                case KIND_BIGINT -> BigInteger.ZERO.equals(this.value);
                case KIND_INT, KIND_QUALIFIER -> ((Integer)this.value) == 0;
                case KIND_STRING -> ((CharSequence)this.value).isEmpty();
                default -> throw new IllegalStateException("unknown version component kind " + this.kind);
            };
        }

        @Override
        @SuppressWarnings("OverlyStrongTypeCast")
        public int compareTo(@Nullable final Component other) {
            int rel;
            if (other == null) {
                // null in this context denotes the pad item (0 or "ga")
                rel = switch (this.kind) {
                    case KIND_MIN -> -1;
                    case KIND_MAX, KIND_BIGINT, KIND_STRING -> 1;
                    case KIND_INT, KIND_QUALIFIER -> (Integer)this.value;
                    default -> throw new IllegalStateException("unknown version component kind " + this.kind);
                };
            } else {
                rel = this.kind - other.kind;
                if (rel == 0) {
                    rel = switch (this.kind) {
                        case KIND_MAX, KIND_MIN -> rel;
                        case KIND_BIGINT -> ((BigInteger)this.value).compareTo((BigInteger)other.value);
                        case KIND_INT, KIND_QUALIFIER -> ((Integer)this.value).compareTo((Integer)other.value);
                        case KIND_STRING -> ((String)this.value).compareToIgnoreCase((String)other.value);
                        default -> throw new IllegalStateException("unknown version component kind: " + this.kind);
                    };
                }
            }
            return rel;
        }

        @Override
        public String toString() {
            return isQualifier() ? this.original : String.valueOf(this.value);
        }

        @Override
        public boolean equals(final Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj == null || getClass() != obj.getClass()) {
                return false;
            }
            return compareTo((Component)obj) == 0;
        }

        @Override
        public int hashCode() {
            return Objects.hash(this.kind, this.value);
        }
    }
}
