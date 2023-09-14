/*
 * Copyright 2023 C Thing Software
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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
