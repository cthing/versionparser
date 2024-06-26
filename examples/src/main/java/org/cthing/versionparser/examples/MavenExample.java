/*
 * Copyright 2023 C Thing Software
 * SPDX-License-Identifier: Apache-2.0
 */
package org.cthing.versionparser.examples;

import org.cthing.versionparser.Version;
import org.cthing.versionparser.VersionConstraint;
import org.cthing.versionparser.VersionParsingException;
import org.cthing.versionparser.maven.MvnVersion;
import org.cthing.versionparser.maven.MvnVersionScheme;

import static org.assertj.core.api.Assertions.assertThat;


/**
 * Parse Maven versions and version constraints.
 */
public final class MavenExample {

    private MavenExample() {
    }

    /**
     * Performs operations on Maven versions and version constraints.
     *
     * @param args Not used
     * @throws VersionParsingException if there was a problem parsing.
     */
    public static void main(final String[] args) throws VersionParsingException {
        // Parse versions
        final MvnVersion version1 = MvnVersionScheme.parseVersion("1.2.3");
        final Version version2 = MvnVersionScheme.parseVersion("2.0.7");

        // Obtain information from the parsed version
        assertThat(version1.getOriginalVersion()).isEqualTo("1.2.3");
        assertThat(version1.isPreRelease()).isFalse();
        assertThat(version1.getComponents()).containsExactly("1", "2", "3");

        // Verify ordering
        assertThat(version1.compareTo(version2)).isEqualTo(-1);

        // Parse version constraints
        final VersionConstraint constraint1 = MvnVersionScheme.parseConstraint("[1.0.0,2.0.0)");
        final VersionConstraint constraint2 = MvnVersionScheme.parseConstraint("[1.5.0,3.0.0)");

        // Perform constraint checking
        assertThat(constraint1.allows(version1)).isTrue();
        assertThat(constraint1.allows(version2)).isFalse();

        // Perform constraint set operations
        assertThat(constraint1.intersect(constraint2)).isEqualTo(MvnVersionScheme.parseConstraint("[1.5.0,2.0.0)"));
        assertThat(constraint1.union(constraint2)).isEqualTo(MvnVersionScheme.parseConstraint("[1.0.0,3.0.0)"));
    }
}
