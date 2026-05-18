/*
 * Copyright 2026 C Thing Software
 * SPDX-License-Identifier: Apache-2.0
 */
package org.cthing.versionparser.pypa;

import org.cthing.annotations.NoCoverageGenerated;
import org.cthing.versionparser.VersionParsingException;


/**
 * Represents the
 * <a href="https://packaging.python.org/en/latest/specifications/version-specifiers/#version-specifiers">version scheme</a>
 * specified by the Python Packaging Authority (PyPA) for Python software packages. Refer to that specification
 * for details on version numbering and version constraints.
 */
public final class PypaVersionScheme {

    @NoCoverageGenerated
    private PypaVersionScheme() {
    }

    /**
     * Parses the specified version string into a PyPA version object.
     *
     * @param version Version string to parse
     * @return PyPA version
     * @throws VersionParsingException if the specified version is not a PyPA compliant version
     */
    public static PypaVersion parseVersion(final String version) throws VersionParsingException {
        return PypaVersion.parse(version);
    }

    /**
     * Parses the specified version specifier into a PyPA specifier set object.
     *
     * @param specifier Specifier parse. Multiple specifiers are separated by commas.
     * @return PyPA specifier set
     * @throws VersionParsingException if the specifier is not PyPA compliant
     */
    public static PypaSpecifierSet parseSpecifier(final String specifier) throws VersionParsingException {
        return PypaSpecifierSet.parse(specifier);
    }
}
