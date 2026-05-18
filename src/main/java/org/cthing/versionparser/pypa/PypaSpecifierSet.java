/*
 * Copyright 2026 C Thing Software
 * SPDX-License-Identifier: Apache-2.0
 */
package org.cthing.versionparser.pypa;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.cthing.annotations.AccessForTesting;
import org.cthing.versionparser.VersionParsingException;


/**
 * Represents a set of Python Packaging Authority version specifier. See the
 * <a href="https://packaging.python.org/en/latest/specifications/version-specifiers/#version-specifiers">Version
 * Specifiers</a> specification for details on syntax and processing rules.
 */
public final class PypaSpecifierSet {

    private static final Pattern COMMA_PATTERN = Pattern.compile(",");

    private final List<PypaSpecifier> specifiers;

    private PypaSpecifierSet(final List<PypaSpecifier> specifiers) {
        this.specifiers = specifiers;
    }

    /**
     * Parses the specified specifier. Note that multiple comma separated specifiers are supported by this class.
     *
     * @param specifier Specifier(s) to parse
     * @return Specifier set object
     * @throws VersionParsingException if there was a problem parsing the specifiers
     */
    static PypaSpecifierSet parse(final String specifier) throws VersionParsingException {
        final String[] specs = COMMA_PATTERN.split(specifier);
        final List<PypaSpecifier> specifierList = new ArrayList<>(specs.length);

        for (String spec : specs) {
            final String trimmedSpec = spec.trim();
            final PypaSpecifier s = PypaSpecifier.parse(trimmedSpec);
            specifierList.add(s);
        }
        return new PypaSpecifierSet(specifierList);
    }

    /**
     * Obtains the specifiers that comprise this set.
     *
     * @return Specifiers in the set
     */
    List<PypaSpecifier> getSpecifiers() {
        return Collections.unmodifiableList(this.specifiers);
    }

    /**
     * Indicates if this specifier set allows the specified version.
     *
     * @param version Version to test
     * @return {@code true} if this specifier set allows the specified version
     * @throws VersionParsingException if there was a problem evaluating the specifier
     */
    public boolean allows(final PypaVersion version) throws VersionParsingException {
        for (PypaSpecifier spec : this.specifiers) {
            if (!spec.allows(version)) {
                return false;
            }
        }
        return true;
    }

    /**
     * Indicates if this specifier set allows the specified version.
     *
     * @param version Version to test
     * @return {@code true} if this specifier set allows the specified version
     * @throws VersionParsingException if there was a problem evaluating the specifier
     */
    public boolean allows(final String version) throws VersionParsingException {
        for (PypaSpecifier spec : this.specifiers) {
            if (!spec.allows(version)) {
                return false;
            }
        }
        return true;
    }

    @Override
    public String toString() {
        return this.specifiers.stream().map(PypaSpecifier::toString).collect(Collectors.joining(","));
    }

    /**
     * Provides a canonical string representation of the specifier set. The format of the string depends
     * upon the specific operator but typically consists of the operator and a canonicalized form of
     * the version identifier.
     *
     * @return Canonical representation of the specifier set
     */
    @AccessForTesting
    String toCanonicalString() {
        return this.specifiers.stream().map(PypaSpecifier::toCanonicalString).collect(Collectors.joining(","));
    }

    @Override
    public boolean equals(final Object obj) {
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }

        final PypaSpecifierSet other = (PypaSpecifierSet)obj;
        return toCanonicalString().equals(other.toCanonicalString());
    }

    @Override
    public int hashCode() {
        return Objects.hash(toCanonicalString());
    }
}
