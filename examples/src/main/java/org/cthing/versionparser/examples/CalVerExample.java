/*
 * Copyright 2023 C Thing Software
 * SPDX-License-Identifier: Apache-2.0
 */
package org.cthing.versionparser.examples;

import org.cthing.versionparser.Version;
import org.cthing.versionparser.VersionParsingException;
import org.cthing.versionparser.calver.CalendarVersion;
import org.cthing.versionparser.calver.CalendarVersionScheme;
import org.cthing.versionparser.calver.Component;
import org.cthing.versionparser.calver.ComponentCategory;

import static org.assertj.core.api.Assertions.assertThat;


/**
 * Parse calendar versions.
 */
public final class CalVerExample {

    private CalVerExample() {
    }

    /**
     * Performs operations on calendar versions.
     *
     * @param args Not used
     * @throws VersionParsingException if there was a problem parsing.
     */
    public static void main(final String[] args) throws VersionParsingException {
        // Parse a single version
        final CalendarVersion version1 = CalendarVersionScheme.parse("YYYY.MM.0D-MAJOR", "2023.2.03-4");

        // Parse multiple versions using the same format
        final CalendarVersionScheme scheme = new CalendarVersionScheme("yyyy.major.minor");
        final Version version2 = scheme.parse("2022.1.0");
        final Version version3 = scheme.parse("2022.1.1");

        // Obtain information from the parsed version
        assertThat(version1.getOriginalVersion()).isEqualTo("2023.2.03-4");
        assertThat(version1.isPreRelease()).isFalse();

        // Obtain information about the first component of the version
        final Component component1 = version1.getComponents().get(0);
        assertThat(component1.getValueStr()).isEqualTo("2023");
        assertThat(component1.getValue()).isEqualTo(2023);
        assertThat(component1.getCategory()).isEqualTo(ComponentCategory.YEAR);

        // Verify ordering
        assertThat(version2.compareTo(version3)).isEqualTo(-1);
    }
}
