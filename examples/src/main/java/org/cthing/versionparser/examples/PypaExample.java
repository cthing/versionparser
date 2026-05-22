/*
 * Copyright 2026 C Thing Software
 * SPDX-License-Identifier: Apache-2.0
 */
package org.cthing.versionparser.examples;

import org.cthing.versionparser.Version;
import org.cthing.versionparser.VersionConstraint;
import org.cthing.versionparser.VersionParsingException;
import org.cthing.versionparser.pypa.PypaSpecifierSet;
import org.cthing.versionparser.pypa.PypaVersion;
import org.cthing.versionparser.pypa.PypaVersionScheme;

import static org.assertj.core.api.Assertions.assertThat;


/**
 * Parse Python Packaging Authority (PyPA) versions and specifiers.
 */
public final class PypaExample {

    private PypaExample() {
    }

    /**
     * Performs operations on PyPA versions and version constraints.
     *
     * @param args Not used
     * @throws VersionParsingException if there was a problem parsing.
     */
    public static void main(final String[] args) throws VersionParsingException {

        // Parse versions
        final PypaVersion version1 = PypaVersionScheme.parseVersion("1.2.3rc1");
        final Version version2 = PypaVersionScheme.parseVersion("2.0.7");

        // Obtain information from the parsed version
        assertThat(version1.getOriginalVersion()).isEqualTo("1.2.3rc1");
        assertThat(version1.getEpoch()).isEqualTo(0);
        assertThat(version1.getRelease()).containsExactly(1, 2, 3);
        assertThat(version1.getPrePhase()).contains(PypaVersion.PrePhase.rc);
        assertThat(version1.getPre()).contains(1);
        assertThat(version1.getPost()).isEmpty();
        assertThat(version1.getDev()).isEmpty();
        assertThat(version1.getLocal()).isEmpty();
        assertThat(version1.isPreRelease()).isTrue();
        assertThat(version1.isPostRelease()).isFalse();
        assertThat(version1.isDevRelease()).isFalse();

        // Verify ordering
        assertThat(version1.compareTo(version2)).isEqualTo(-1);

        // Perform specifier checking
        final PypaSpecifierSet specifier1 = PypaVersionScheme.parseSpecifier("<2.0.0");
        final PypaSpecifierSet specifier2 = PypaVersionScheme.parseSpecifier(">2.0,<3.0");
        assertThat(specifier1.allows(version1)).isTrue();
        assertThat(specifier2.allows(version1)).isFalse();

        // Perform constraint checking
        final VersionConstraint constraint1 = PypaVersionScheme.parseConstraint("<2.0.0");
        final VersionConstraint constraint2 = PypaVersionScheme.parseConstraint(">2.0,>3.0");
        assertThat(constraint1.allows(version1)).isTrue();
        assertThat(constraint1.allows(version2)).isFalse();
        assertThat(constraint2.allows(version1)).isFalse();
        assertThat(constraint2.allows(version2)).isTrue();
    }
}
