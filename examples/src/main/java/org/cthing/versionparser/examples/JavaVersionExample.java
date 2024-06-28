/*
 * Copyright 2023 C Thing Software
 * SPDX-License-Identifier: Apache-2.0
 */
package org.cthing.versionparser.examples;

import org.cthing.versionparser.VersionConstraint;
import org.cthing.versionparser.VersionParsingException;
import org.cthing.versionparser.java.JavaVersion;
import org.cthing.versionparser.java.JavaVersionScheme;

import static org.assertj.core.api.Assertions.assertThat;


/**
 * Parse Java versions and version ranges.
 */
public final class JavaVersionExample {

    private JavaVersionExample() {
    }

    /**
     * Performs operations on Java language versions and version ranges.
     *
     * @param args Not used
     * @throws VersionParsingException if there was a problem parsing.
     */
    public static void main(final String[] args) throws VersionParsingException {
        // Parse versions
        final JavaVersion version1 = JavaVersionScheme.parseVersion("17");
        final JavaVersion version2 = JavaVersionScheme.parseVersion("21.0.3+9-LTS");

        // Obtain information from the parsed version
        assertThat(version1.getOriginalVersion()).isEqualTo("17");
        assertThat(version1.isPreRelease()).isFalse();
        assertThat(version1.getComponents()).containsExactly(17);

        assertThat(version2.getOriginalVersion()).isEqualTo("21.0.3+9-LTS");
        assertThat(version2.getFeature()).isEqualTo(21);
        assertThat(version2.getInterim()).isEqualTo(0);
        assertThat(version2.getUpdate()).isEqualTo(3);
        assertThat(version2.getBuild()).contains(9);
        assertThat(version2.getOptional()).contains("LTS");
        assertThat(version2.getComponents()).containsExactly(21, 0, 3);

        // Verify ordering
        assertThat(version1).isLessThan(version2);

        // Parse version constraints
        final VersionConstraint constraint1 = JavaVersionScheme.parseRange("[17,21)");
        final VersionConstraint constraint2 = JavaVersionScheme.parseRange("(17,22)");

        // Perform constraint checking
        assertThat(constraint1.allows(version1)).isTrue();
        assertThat(constraint1.allows(version2)).isFalse();

        assertThat(constraint2.allows(version1)).isFalse();
        assertThat(constraint2.allows(version2)).isTrue();

        // Perform version tests
        assertThat(JavaVersionScheme.isVersion(JavaVersionScheme.JAVA_17, "17")).isTrue();
        assertThat(JavaVersionScheme.isVersion(JavaVersionScheme.JAVA_17, "17.0.11")).isTrue();
        assertThat(JavaVersionScheme.isVersion(JavaVersionScheme.JAVA_17, "21")).isFalse();

        // Runtime Java version
        assertThat(JavaVersion.RUNTIME_VERSION.getFeature()).isGreaterThanOrEqualTo(17);
    }
}
