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
import org.cthing.versionparser.VersionConstraint;
import org.cthing.versionparser.VersionParsingException;
import org.cthing.versionparser.gem.GemVersion;
import org.cthing.versionparser.gem.GemVersionScheme;

import static org.assertj.core.api.Assertions.assertThat;


/**
 * Parse RubyGems versions and version constraints.
 */
public final class RubyGemsExample {

    private RubyGemsExample() {
    }

    /**
     * Performs operations on RubyGems versions and version constraints.
     *
     * @param args Not used
     * @throws VersionParsingException if there was a problem parsing.
     */
    public static void main(final String[] args) throws VersionParsingException {
        // Parse versions
        final GemVersion version1 = GemVersionScheme.parseVersion("1.2.3");
        final Version version2 = GemVersionScheme.parseVersion("2.0.7");

        // Obtain information from the parsed version
        assertThat(version1.getOriginalVersion()).isEqualTo("1.2.3");
        assertThat(version1.isPreRelease()).isFalse();
        assertThat(version1.getComponents()).containsExactly("1", "2", "3");

        // Verify ordering
        assertThat(version1.compareTo(version2)).isEqualTo(-1);

        // Parse version constraints
        final VersionConstraint constraint1 = GemVersionScheme.parseConstraint("~>1.0");
        final VersionConstraint constraint2 = GemVersionScheme.parseConstraint(">=1.5.0", "<3.0.0");

        // Perform constraint checking
        assertThat(constraint1.allows(version1)).isTrue();
        assertThat(constraint1.allows(version2)).isFalse();

        // Perform constraint set operations
        assertThat(constraint1.intersect(constraint2)).isEqualTo(GemVersionScheme.parseConstraint(">=1.5.0", "<2.ZZZ"));
        assertThat(constraint1.union(constraint2)).isEqualTo(GemVersionScheme.parseConstraint(">=1.0.0", "<3.0.0"));
    }
}
