/*
 * Copyright 2023 C Thing Software
 * SPDX-License-Identifier: Apache-2.0
 */
package org.cthing.versionparser.examples;

import org.cthing.versionparser.Version;
import org.cthing.versionparser.VersionConstraint;
import org.cthing.versionparser.VersionParsingException;
import org.cthing.versionparser.npm.NpmVersionScheme;
import org.cthing.versionparser.semver.SemanticVersion;

import static org.assertj.core.api.Assertions.assertThat;


/**
 * Parse semantic versions and NPM version constraints.
 */
public final class NpmExample {

    private NpmExample() {
    }

    /**
     * Performs operations on semantic versions and NPM version constraints.
     *
     * @param args Not used
     * @throws VersionParsingException if there was a problem parsing.
     */
    public static void main(final String[] args) throws VersionParsingException {
        // Parse versions
        final SemanticVersion version1 = NpmVersionScheme.parseVersion("1.2.3");
        final Version version2 = NpmVersionScheme.parseVersion("2.0.7");

        // Obtain information from the parsed version
        assertThat(version1.getOriginalVersion()).isEqualTo("1.2.3");
        assertThat(version1.isPreRelease()).isFalse();
        assertThat(version1.getMajor()).isEqualTo(1);
        assertThat(version1.getMinor()).isEqualTo(2);
        assertThat(version1.getPatch()).isEqualTo(3);

        // Verify ordering
        assertThat(version1.compareTo(version2)).isEqualTo(-1);

        // Parse version constraints
        final VersionConstraint constraint1 = NpmVersionScheme.parseConstraint("^1.0.0");
        final VersionConstraint constraint2 = NpmVersionScheme.parseConstraint(">=1.5.0 <3.0.0");

        // Perform constraint checking
        assertThat(constraint1.allows(version1)).isTrue();
        assertThat(constraint1.allows(version2)).isFalse();

        // Perform constraint set operations
        assertThat(constraint1.intersect(constraint2)).isEqualTo(NpmVersionScheme.parseConstraint(">=1.5.0 <2.0.0-0"));
        assertThat(constraint1.union(constraint2)).isEqualTo(NpmVersionScheme.parseConstraint(">=1.0.0 <3.0.0"));
    }
}
