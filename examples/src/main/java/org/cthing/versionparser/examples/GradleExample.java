/*
 * Copyright 2023 C Thing Software
 * SPDX-License-Identifier: Apache-2.0
 */
package org.cthing.versionparser.examples;

import org.cthing.versionparser.Version;
import org.cthing.versionparser.VersionConstraint;
import org.cthing.versionparser.VersionParsingException;
import org.cthing.versionparser.gradle.GradleVersion;
import org.cthing.versionparser.gradle.GradleVersionScheme;

import static org.assertj.core.api.Assertions.assertThat;


/**
 * Parse Gradle versions and version constraints.
 */
public final class GradleExample {

    private GradleExample() {
    }

    /**
     * Performs operations on Gradle versions and version constraints.
     *
     * @param args Not used
     * @throws VersionParsingException if there was a problem parsing.
     */
    public static void main(final String[] args) throws VersionParsingException {
        // Parse versions
        final GradleVersion version1 = GradleVersionScheme.parseVersion("1.2.3-SNAPSHOT");
        final Version version2 = GradleVersionScheme.parseVersion("2.0.7");

        // Obtain information from the parsed version
        assertThat(version1.getOriginalVersion()).isEqualTo("1.2.3-SNAPSHOT");
        assertThat(version1.isPreRelease()).isTrue();
        assertThat(version1.getComponents()).containsExactly("1", "2", "3", "SNAPSHOT");

        // Verify ordering
        assertThat(version1.compareTo(version2)).isEqualTo(-1);

        // Parse version constraints
        final VersionConstraint constraint1 = GradleVersionScheme.parseConstraint("[1.0.0,2.0.0[");
        final VersionConstraint constraint2 = GradleVersionScheme.parseConstraint("[1.5.0,2.+]");

        // Perform constraint checking
        assertThat(constraint1.allows(version1)).isTrue();
        assertThat(constraint1.allows(version2)).isFalse();

        // Perform constraint set operations
        assertThat(constraint1.intersect(constraint2)).isEqualTo(GradleVersionScheme.parseConstraint("[1.5.0,2.+]"));
        assertThat(constraint1.union(constraint2)).isEqualTo(GradleVersionScheme.parseConstraint("[1.0.0,2.0.0)"));
    }
}
