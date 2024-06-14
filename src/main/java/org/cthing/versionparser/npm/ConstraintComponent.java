/*
 * Copyright 2023 C Thing Software
 * SPDX-License-Identifier: Apache-2.0
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

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

import org.cthing.versionparser.VersionParsingException;
import org.cthing.versionparser.semver.SemanticVersion;


/**
 * Represents a single component in a version constraint. For example, the version constraint {@code [1.0,2.0)}
 * consists of the components {@code [1.0} and {@code 2.0)}, which expressed in the NPM low level constraint language
 * is {@code >=1.0} and {@code <2.0}. Similarly, the constraint {@code [1.0]} consists of a single component
 * {@code =1.0}.
 */
class ConstraintComponent {

    /**
     * Version constraint operators.
     */
    enum Operator {
        /**
         * Requires a version exactly equal to the constraint version. Corresponds to the {@code [x]} constraint.
         */
        EQ("="),

        /**
         * Requires a version less than the constraint version. Corresponds to the {@code x)} constraint.
         */
        LT("<"),

        /**
         * Requires a version less than or equal to the constraint version. Corresponds to the {@code x]} constraint.
         */
        LTE("<="),

        /**
         * Requires a version greater than the constraint version. Corresponds to the {@code (x} constraint.
         */
        GT(">"),

        /**
         * Requires a version greater than or equal to the constraint version. Corresponds to the {@code [x} constraint.
         */
        GTE(">=");

        private static final Map<String, Operator> OPERATORS = new HashMap<>();

        private final String string;

        static {
            for (final Operator operator : values()) {
                OPERATORS.put(operator.string, operator);
            }
        }

        /**
         * Creates an operator instance with the specified string representation.
         *
         * @param string Operator string representation
         */
        Operator(final String string) {
            this.string = string;
        }

        /**
         * String representation of the range operator.
         *
         * @return range operator as string
         */
        String asString() {
            return this.string;
        }

        /**
         * Obtains the operator corresponding to the specified operator string.
         *
         * @param operatorStr String representation of the operator
         * @return Operator corresponding to the specified string.
         */
        static Operator value(final String operatorStr) {
            if (operatorStr.isEmpty()) {
                return EQ;
            }

            final Operator op = OPERATORS.get(operatorStr);
            if (op == null) {
                throw new IllegalArgumentException(String.format(Locale.ROOT, "Range operator for '%s' not found",
                                                                 operatorStr));
            }
            return op;
        }
    }


    private final SemanticVersion version;
    private final Operator operator;

    /**
     * Constructs a component representing the specified version and operator.
     *
     * @param version Component version
     * @param operator Component operator
     */
    ConstraintComponent(final SemanticVersion version, final Operator operator) {
        this.version = version;
        this.operator = operator;
    }

    /**
     * Constructs a component representing the specified version and operator.
     *
     * @param version Component version
     * @param operator Component operator
     * @throws VersionParsingException if there was a problem parsing the specified version.
     */
    ConstraintComponent(final String version, final String operator) throws VersionParsingException {
        this(SemanticVersion.parse(version), Operator.value(operator));
    }

    /**
     * Obtains the component version.
     *
     * @return Component version.
     */
    public SemanticVersion getVersion() {
        return this.version;
    }

    /**
     * Obtains the component operator.
     *
     * @return Component operator.
     */
    public Operator getOperator() {
        return this.operator;
    }

    @Override
    public boolean equals(final Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        final ConstraintComponent comp = (ConstraintComponent)obj;
        return Objects.equals(this.version, comp.version) && this.operator == comp.operator;
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.version, this.operator);
    }

    @Override
    public String toString() {
        return this.operator.asString() + this.version;
    }
}
