/*
 * Copyright 2026 C Thing Software
 * SPDX-License-Identifier: Apache-2.0
 */
package org.cthing.versionparser.examples;

import org.cthing.versionparser.Version;
import org.cthing.versionparser.VersionConstraint;
import org.cthing.versionparser.VersionParsingException;
import org.cthing.versionparser.debian.DebVersion;
import org.cthing.versionparser.debian.DebVersionScheme;

import static org.assertj.core.api.Assertions.assertThat;


/**
 * Parse Debian package versions and version constraints.
 */
public final class DebianExample {

    private DebianExample() {
    }

    /**
     * Performs operations on Debian package versions and version constraints.
     *
     * @param args Not used
     * @throws VersionParsingException if there was a problem parsing.
     */
    public static void main(final String[] args) throws VersionParsingException {
        // Parse versions
        final DebVersion version1 = DebVersionScheme.parseVersion("22.07.5-2ubuntu1.5");
        final Version version2 = DebVersionScheme.parseVersion("20.01.2-1ubuntu1.5");

        // Obtain information from the parsed version
        assertThat(version1.getOriginalVersion()).isEqualTo("22.07.5-2ubuntu1.5");
        assertThat(version1.isPreRelease()).isFalse();
        assertThat(version1.getEpoch()).isEqualTo(0);
        assertThat(version1.getUpstream()).isEqualTo("22.07.5");
        assertThat(version1.getRevision()).isEqualTo("2ubuntu1.5");

        // Verify ordering
        assertThat(version1.compareTo(version2)).isEqualTo(1);

        // Parse version constraints
        final VersionConstraint constraint1 = DebVersionScheme.parseConstraint(">20");
        final VersionConstraint constraint2 = DebVersionScheme.parseConstraint(">21 <=23");

        // Perform constraint checking
        assertThat(constraint1.allows(version2)).isTrue();
        assertThat(constraint2.allows(version1)).isTrue();
        assertThat(constraint2.allows(version2)).isFalse();
    }
}
