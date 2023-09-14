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

package org.cthing.versionparser;

import javax.annotation.Nullable;

import org.cthing.versionparser.maven.MvnVersionScheme;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;


public class VersionRangeTest {

    @Test
    public void testConstructionOpenRange() {
        final VersionRange range = new VersionRange(null, null, false, false);
        assertThat(range.getMinVersion()).isNull();
        assertThat(range.getMaxVersion()).isNull();
        assertThat(range.isMinIncluded()).isFalse();
        assertThat(range.isMaxIncluded()).isFalse();
        assertThat(range.isAny()).isTrue();
        assertThat(range.isSingleVersion()).isFalse();
        assertThat(range).hasToString("(,)");
    }

    @Test
    public void testConstructionSingleVersion() {
        final Version version = version("1.2.3");
        final VersionRange range = new VersionRange(version, version, true, true);
        assertThat(range.getMinVersion()).isEqualTo(version);
        assertThat(range.getMaxVersion()).isEqualTo(version);
        assertThat(range.isMinIncluded()).isTrue();
        assertThat(range.isMaxIncluded()).isTrue();
        assertThat(range.isAny()).isFalse();
        assertThat(range.isSingleVersion()).isTrue();
        assertThat(range).hasToString("[1.2.3]");
    }

    @Test
    public void testConstructionFull() {
        final Version version1 = version("1.2.3");
        final Version version2 = version("3.0");
        VersionRange range = new VersionRange(version1, version2, true, false);
        assertThat(range.getMinVersion()).isEqualTo(version1);
        assertThat(range.getMaxVersion()).isEqualTo(version2);
        assertThat(range.isMinIncluded()).isTrue();
        assertThat(range.isMaxIncluded()).isFalse();
        assertThat(range.isAny()).isFalse();
        assertThat(range.isSingleVersion()).isFalse();
        assertThat(range).hasToString("[1.2.3,3.0)");

        final Version version3 = version("1.2.3");
        range = new VersionRange(version3, version3, false, false);
        assertThat(range.getMinVersion()).isEqualTo(version3);
        assertThat(range.getMaxVersion()).isEqualTo(version3);
        assertThat(range.isMinIncluded()).isFalse();
        assertThat(range.isMaxIncluded()).isFalse();
        assertThat(range.isAny()).isFalse();
        assertThat(range.isSingleVersion()).isFalse();
        assertThat(range).hasToString("(1.2.3,1.2.3)");
    }

    @Test
    public void testConstructionBad() {
        final Version version1 = version("3.0");
        final Version version2 = version("1.2.3");
        assertThatIllegalArgumentException().isThrownBy(() -> new VersionRange(version1, version2, true, false));
    }

    @Test
    public void testAllows() {
        assertThat(range("1.5", "2.0", true, true).allows(version("1.6"))).isTrue();
        assertThat(range("1.5", "2.0", true, true).allows(version("1.5"))).isTrue();
        assertThat(range("1.5", "2.0", true, true).allows(version("2.0"))).isTrue();
        assertThat(range("1.5", "2.0", true, true).allows(version("1.4"))).isFalse();
        assertThat(range("1.5", "2.0", true, true).allows(version("2.1"))).isFalse();

        assertThat(range("1.5", "2.0", false, true).allows(version("1.6"))).isTrue();
        assertThat(range("1.5", "2.0", false, true).allows(version("2.0"))).isTrue();
        assertThat(range("1.5", "2.0", false, true).allows(version("1.5"))).isFalse();
        assertThat(range("1.5", "2.0", false, true).allows(version("1.4"))).isFalse();
        assertThat(range("1.5", "2.0", false, true).allows(version("2.1"))).isFalse();

        assertThat(range("1.5", "2.0", true, false).allows(version("1.6"))).isTrue();
        assertThat(range("1.5", "2.0", true, false).allows(version("1.5"))).isTrue();
        assertThat(range("1.5", "2.0", true, false).allows(version("2.0"))).isFalse();
        assertThat(range("1.5", "2.0", true, false).allows(version("1.4"))).isFalse();
        assertThat(range("1.5", "2.0", true, false).allows(version("2.1"))).isFalse();

        assertThat(range("1.5", "2.0", true, false).allows(version("2.0-beta"))).isTrue();
        assertThat(range("1.5", "2.0.min", true, false).allows(version("2.0-beta"))).isFalse();

        assertThat(range("1.5", null, true, false).allows(version("5.0"))).isTrue();
        assertThat(range("1.5", null, true, false).allows(version("1.6"))).isTrue();
        assertThat(range("1.5", null, true, false).allows(version("1.5"))).isTrue();
        assertThat(range("1.5", null, true, false).allows(version("1.4"))).isFalse();

        assertThat(range(null, "2.0", false, true).allows(version("2.0"))).isTrue();
        assertThat(range(null, "2.0", false, true).allows(version("0.1"))).isTrue();
        assertThat(range(null, "2.0", false, true).allows(version("2.1"))).isFalse();

        assertThat(range(null, null, false, false).allows(version("0.1"))).isTrue();
        assertThat(range(null, null, false, false).allows(version("3.4.1"))).isTrue();
    }

    @Test
    public void testAllowsAll() {
        assertThat(range("1.5", "2.0", true, false).allowsAll(range("1.6", "1.6", true, true))).isTrue();
        assertThat(range("1.5", "2.0", true, false).allowsAll(range("1.6", "1.9", true, false))).isTrue();
        assertThat(range("1.5", "2.0", true, false).allowsAll(range("1.5", "2.0", true, false))).isTrue();
        assertThat(range("1.5", "2.0", true, false).allowsAll(range("1.5", "2.0", true, true))).isFalse();
        assertThat(range("1.5", "2.0", true, false).allowsAll(range("1.0", "1.0", true, true))).isFalse();
        assertThat(range("1.5", "2.0", true, false).allowsAll(range("1.0", "1.3", true, true))).isFalse();
        assertThat(range("1.5", "2.0", true, false).allowsAll(range("2.0", "2.3", true, true))).isFalse();
        assertThat(range("1.5", "2.0", true, false).allowsAll(range("4.0", "7.3", true, true))).isFalse();
        assertThat(range("1.5", "2.0", true, false).allowsAll(range(null, null, false, false))).isFalse();
        assertThat(range("1.0", "1.1", true, false).allowsAll(range("1.0.abcd", "1.0.abcd", true, true))).isTrue();
        assertThat(range("1.0", "1.1", true, false).allowsAll(range("1.0-abcd", "1.0-abcd", true, true))).isTrue();
        assertThat(range(null, null, false, false).allowsAll(range("4.0", "7.3", true, true))).isTrue();
        assertThat(range(null, null, false, false).allowsAll(range(null, "7.3", false, true))).isTrue();
        assertThat(range(null, null, false, false).allowsAll(range(null, null, false, false))).isTrue();
    }

    @Test
    public void testAllowsAny() {
        assertThat(range("1.5", "2.0", true, false).allowsAny(range("1.6", "1.6", true, true))).isTrue();
        assertThat(range("1.5", "2.0", true, false).allowsAny(range("1.6", "1.9", true, false))).isTrue();
        assertThat(range("1.5", "2.0", true, false).allowsAny(range("1.5", "2.0", true, false))).isTrue();
        assertThat(range("1.5", "2.0", true, false).allowsAny(range("1.5", "2.0", true, true))).isTrue();
        assertThat(range("1.5", "2.0", true, false).allowsAny(range("1.0", "1.0", true, true))).isFalse();
        assertThat(range("1.5", "2.0", true, false).allowsAny(range("1.0", "1.3", true, true))).isFalse();
        assertThat(range("1.5", "2.0", true, false).allowsAny(range("2.0", "2.3", true, true))).isFalse();
        assertThat(range("1.5", "2.0", true, false).allowsAny(range("4.0", "7.3", true, true))).isFalse();
        assertThat(range("1.5", "2.0", true, false).allowsAny(range(null, null, false, false))).isTrue();
        assertThat(range(null, null, false, false).allowsAny(range("4.0", "7.3", true, true))).isTrue();
        assertThat(range(null, null, false, false).allowsAny(range(null, "7.3", false, true))).isTrue();
        assertThat(range(null, null, false, false).allowsAny(range(null, null, false, false))).isTrue();
    }

    @Test
    public void testIntersectRanges() {
        assertThat(range("1.5", "1.5", true, true).intersect(range("1.5", "1.5", true, true)))
                .contains(range("1.5", "1.5", true, true));
        assertThat(range("1.5", "2.0", true, true).intersect(range("1.7", "1.7", true, true)))
                .contains(range("1.7", "1.7", true, true));
        assertThat(range("1.5", "2.0", true, true).intersect(range("1.6", "1.9", true, true)))
                .contains(range("1.6", "1.9", true, true));
        assertThat(range("1.5", "2.0", true, true).intersect(range("1.6", "1.9", true, false)))
                .contains(range("1.6", "1.9", true, false));
        assertThat(range("1.5", "2.0", true, true).intersect(range("1.7", "4.0", true, false)))
                .contains(range("1.7", "2.0", true, true));
        assertThat(range("1.5", "2.0", true, true).intersect(range("1.2", "1.8", true, true)))
                .contains(range("1.5", "1.8", true, true));
        assertThat(range("1.5", "2.0", true, true).intersect(range("1.2", null, true, false)))
                .contains(range("1.5", "2.0", true, true));
        assertThat(range("1.5", null, true, false).intersect(range("1.2", "1.8", true, true)))
                .contains(range("1.5", "1.8", true, true));
        assertThat(range(null, "3.0", false, false).intersect(range(null, "1.8", false, true)))
                .contains(range(null, "1.8", false, true));
        assertThat(range("1.5", "2.0", true, false).intersect(range("1.2", "1.4", true, true))).isEmpty();
        assertThat(range("1.5", "2.0", true, false).intersect(range(null, null, false, false)))
                .contains(range("1.5", "2.0", true, false));
        assertThat(range(null, null, false, false).intersect(range("1.5", "2.0", true, false)))
                .contains(range("1.5", "2.0", true, false));
        assertThat(range(null, null, false, false).intersect(range(null, null, false, false)))
                .contains(range(null, null, false, false));
    }

    @Test
    public void testDifferenceRanges() {
        assertThat(range("1.2", " 2.0", true, false).difference(range("1.5", "1.5", true, true)))
                .containsExactly(range("1.2", "1.5", true, false), range("1.5", "2.0", false, false));
        assertThat(range("1.2", "2.0", true, false).difference(range("1.2", "1.2", true, true)))
                .containsExactly(range("1.2", "2.0", false, false));
        assertThat(range("1.2", "2.0", true, true).difference(range("2.0", "2.0", true, true)))
                .containsExactly(range("1.2", "2.0", true, false));
        assertThat(range("1.2", "2.0", true, false).difference(range("1.5", "1.7", true, true)))
                .containsExactly(range("1.2", "1.5", true, false), range("1.7", "2.0", false, false));
        assertThat(range("1.2", "2.0", true, false).difference(range("1.5", "1.7", true, false)))
                .containsExactly(range("1.2", "1.5", true, false), range("1.7", "2.0", true, false));
        assertThat(range("1.2", "2.0", true, false).difference(range("1.5", "1.7", false, true)))
                .containsExactly(range("1.2", "1.5", true, true), range("1.7", "2.0", false, false));
        assertThat(range("1.2", "2.0", true, false).difference(range("1.5", "2.7", false, true)))
                .containsExactly(range("1.2", "1.5", true, true));
        assertThat(range("1.2", "2.0", true, false).difference(range("1.0", "1.5", false, true)))
                .containsExactly(range("1.5", "2.0", false, false));
        assertThat(range("1.2", "2.0", true, false).difference(range("3.0", "5.5", false, true)))
                .containsExactly(range("1.2", "2.0", true, false));
        assertThat(range("1.2", "2.0", true, false).difference(range("1.2", "1.4", false, true)))
                .containsExactly(range("1.2", "1.2", true, true), range("1.4", "2.0", false, false));
        assertThat(range("1.2", "2.0", true, true).difference(range("1.5", "2.0", false, false)))
                .containsExactly(range("1.2", "1.5", true, true), range("2.0", "2.0", true, true));
        assertThat(range("1.2", "2.0", true, false).difference(range(null, null, false, false))).isEmpty();
    }

    @Test
    public void testIsAdjacent() {
        assertThat(range("0.1", "1.0", false, true).isAdjacent(range("1.0", "2.0", false, false))).isTrue();
        assertThat(range("1.0", "2.0", false, false).isAdjacent(range("0.1", "1.0", false, true))).isTrue();
        assertThat(range("0.1", "1.0", false, false).isAdjacent(range("1.0", "2.0", true, false))).isTrue();
        assertThat(range("0.1", "1.0", false, false).isAdjacent(range("1.0.0", "2.0", true, false))).isTrue();
        assertThat(range(null, "1.0", false, false).isAdjacent(range("1.0.0", "2.0", true, false))).isTrue();
        assertThat(range(null, "1.0", false, false).isAdjacent(range("1.0.0", null, true, false))).isTrue();
        assertThat(range("0", "0", true, true).isAdjacent(range("0", "2.0", false, false))).isTrue();
        assertThat(range("0.1", "1.0", false, true).isAdjacent(range("1.0", "2.0", true, false))).isFalse();
        assertThat(range("0.1", "1.0", false, false).isAdjacent(range("1.0", "2.0", false, false))).isFalse();
        assertThat(range("0.1", "1.5", false, true).isAdjacent(range("1.0", "2.0", true, false))).isFalse();
        assertThat(range("0.1", "1.5", false, true).isAdjacent(range("1.0", "2.0", false, false))).isFalse();
        assertThat(range("0.1", "0.5", false, true).isAdjacent(range("1.0", "2.0", false, false))).isFalse();
        assertThat(range("0.1", null, false, false).isAdjacent(range(null, "2.0", false, false))).isFalse();
        assertThat(range("0.1", null, false, false).isAdjacent(range("1.0", "2.0", true, false))).isFalse();
        assertThat(range("0", "0", true, true).isAdjacent(range("0", "2.0", true, false))).isFalse();
    }

    @Test
    public void testMerge() {
        assertThat(range(null, null, false, false).merge(range(null, null, false, false)))
                .isEqualTo(range(null, null, false, false));
        assertThat(range(null, null, false, false).merge(range("1.5", "1.5", true, true)))
                .isEqualTo(range(null, null, false, false));
        assertThat(range("1.5", "1.5", true, true).merge(range(null, null, false, false)))
                .isEqualTo(range(null, null, false, false));

        assertThat(range("1.0", "2.0", true, false).merge(range("2.0", "3.0", true, false)))
                .isEqualTo(range("1.0", "3.0", true, false));
        assertThat(range("2.0", "3.0", true, false).merge(range("1.0", "2.0", true, false)))
                .isEqualTo(range("1.0", "3.0", true, false));
        assertThat(range("1.0", "2.5", true, false).merge(range("1.7", "3.0", true, false)))
                .isEqualTo(range("1.0", "3.0", true, false));
        assertThat(range("1.0", "4.0", true, false).merge(range("1.7", "3.0", true, false)))
                .isEqualTo(range("1.0", "4.0", true, false));
        assertThat(range("2.0", "3.0", true, false).merge(range("1.0", "4.0", true, false)))
                .isEqualTo(range("1.0", "4.0", true, false));
    }

    @Test
    public void testAllowsLower() {
        assertThat(range("0.1", "1.0", false, true).allowsLower(range("1.2", "2.0", false, false))).isTrue();
        assertThat(range("0.1", "1.5", false, true).allowsLower(range("1.2", "2.0", false, false))).isTrue();
        assertThat(range("1.5", "3.5", false, true).allowsLower(range("1.2", "2.0", false, false))).isFalse();
        assertThat(range("3.1", "3.5", false, true).allowsLower(range("1.2", "2.0", false, false))).isFalse();
        assertThat(range("1.0", "2.0", true, false).allowsLower(range("1.0", "2.0", true, false))).isFalse();
        assertThat(range("1.0", "2.0", true, false).allowsLower(range("1.0", "2.0", false, false))).isTrue();
        assertThat(range(null, "1.5", false, false).allowsLower(range("1.0", "2.0", false, false))).isTrue();
        assertThat(range("1.0", "1.5", false, false).allowsLower(range(null, "3.0", false, false))).isFalse();
        assertThat(range(null, null, false, false).allowsLower(range("1.0", "2.0", false, false))).isTrue();
        assertThat(range(null, null, false, false).allowsLower(range(null, "2.0", false, false))).isFalse();
    }

    @Test
    public void testAllowsHigher() {
        assertThat(range("1.2", "2.0", false, false).allowsHigher(range("0.1", "1.0", false, true))).isTrue();
        assertThat(range("1.2", "2.0", false, false).allowsHigher(range("0.1", "1.5", false, true))).isTrue();
        assertThat(range("1.2", "2.0", false, false).allowsHigher(range("1.5", "3.5", false, true))).isFalse();
        assertThat(range("1.2", "2.0", false, false).allowsHigher(range("3.1", "3.5", false, true))).isFalse();
        assertThat(range("1.0", "2.0", true, false).allowsHigher(range("1.0", "2.0", true, false))).isFalse();
        assertThat(range("1.0", "2.0", true, true).allowsHigher(range("1.0", "2.0", true, false))).isTrue();
        assertThat(range("1.0", "2.0", false, false).allowsHigher(range(null, "1.5", false, false))).isTrue();
        assertThat(range("1.0", "2.0", false, false).allowsHigher(range(null, null, false, false))).isFalse();
        assertThat(range(null, null, false, false).allowsHigher(range("1.0", "2.0", false, false))).isTrue();
        assertThat(range(null, "2.0", false, false).allowsHigher(range(null, null, false, false))).isFalse();
    }

    @Test
    public void testStrictlyLower() {
        assertThat(range("0.1", "1.0", false, true).strictlyLower(range("1.2", "2.0", false, false))).isTrue();
        assertThat(range("0.1", "1.5", false, true).strictlyLower(range("1.2", "2.0", false, false))).isFalse();
        assertThat(range("1.5", "3.5", false, true).strictlyLower(range("1.2", "2.0", false, false))).isFalse();
        assertThat(range("3.1", "3.5", false, true).strictlyLower(range("1.2", "2.0", false, false))).isFalse();
        assertThat(range("1.0", "2.0", true, false).strictlyLower(range("1.0", "2.0", true, false))).isFalse();
        assertThat(range("1.0", "2.0", true, false).strictlyLower(range("1.0", "2.0", false, false))).isFalse();
        assertThat(range(null, "1.5", false, false).strictlyLower(range("1.0", "2.0", false, false))).isFalse();
        assertThat(range(null, "1.0", false, false).strictlyLower(range("1.0", "2.0", false, false))).isTrue();
        assertThat(range(null, null, false, false).strictlyLower(range("1.0", "2.0", false, false))).isFalse();
        assertThat(range(null, null, false, false).strictlyLower(range(null, "2.0", false, false))).isFalse();
    }

    @Test
    public void testStrictlyHigher() {
        assertThat(range("1.2", "2.0", false, false).strictlyHigher(range("0.1", "1.0", false, true))).isTrue();
        assertThat(range("1.2", "2.0", false, false).strictlyHigher(range("0.1", "1.5", false, true))).isFalse();
        assertThat(range("1.2", "2.0", false, false).strictlyHigher(range("1.5", "3.5", false, true))).isFalse();
        assertThat(range("1.2", "2.0", false, false).strictlyHigher(range("3.1", "3.5", false, true))).isFalse();
        assertThat(range("1.0", "2.0", true, false).strictlyHigher(range("1.0", "2.0", true, false))).isFalse();
        assertThat(range("1.0", "2.0", true, false).strictlyHigher(range("1.0", "2.0", true, false))).isFalse();
        assertThat(range("1.0", "2.0", false, false).strictlyHigher(range(null, "1.5", false, false))).isFalse();
        assertThat(range("1.5", "2.0", true, false).strictlyHigher(range(null, "1.5", false, false))).isTrue();
        assertThat(range("1.0", "2.0", false, false).strictlyHigher(range(null, null, false, false))).isFalse();
        assertThat(range(null, null, false, false).strictlyHigher(range("1.0", "2.0", false, false))).isFalse();
        assertThat(range(null, "2.0", false, false).strictlyHigher(range(null, null, false, false))).isFalse();
    }

    @Test
    public void testOrdering() {
        assertThat(range("1.5", "1.5", true, true)).isLessThan(range("2.0", "2.0", true, true));
        assertThat(range("2.0", "2.0", true, true)).isGreaterThan(range("1.5", "1.5", true, true));
        assertThat(range("1.5", "1.5", true, true)).isEqualByComparingTo(range("1.5", "1.5", true, true));

        assertThat(range("1.5", "2.0", true, true)).isLessThan(range("2.1", "3.0", true, true));
        assertThat(range("2.1", "3.0", true, true)).isGreaterThan(range("1.5", "2.0", true, true));
        assertThat(range("1.5", "2.0", true, true)).isEqualByComparingTo(range("1.5", "2.0", true, true));

        assertThat(range("1.5", "2.0", true, true)).isLessThan(range("1.7", "3.0", true, true));
        assertThat(range("2.1", "3.0", true, true)).isGreaterThan(range("1.5", "2.2", true, true));

        assertThat(range("1.5", "2.0", true, false)).isLessThan(range("1.5", "2.0", true, true));
        assertThat(range("1.5", "2.0", true, true)).isGreaterThan(range("1.5", "2.0", true, false));
        assertThat(range("1.5", "2.0", true, true)).isLessThan(range("1.5", "2.0", false, true));
        assertThat(range("1.5", "2.0", true, true)).isLessThan(range("1.5", null, true, false));
        assertThat(range("1.0", "3.0", false, false)).isLessThan(range("1.0", null, false, false));

        assertThat(range("1.5", "1.5", true, true)).isLessThan(range("1.5", "2.0", true, true));
        assertThat(range(null, null, false, false)).isGreaterThan(range(null, "2.0", false, true));
        assertThat(range(null, null, false, false)).isLessThan(range("2.0", null, true, false));
        assertThat(range(null, null, false, false)).isEqualByComparingTo(range(null, null, false, false));
    }

    @Test
    public void testEquality() {
        final VersionRange range1 = range("1.5", "1.5", true, true);
        final VersionRange range2 = range("1.5", "1.5", true, true);
        final VersionRange range3 = range("1.5", "2.0", true, false);
        final VersionRange range4 = range("1.5", "2.0", true, false);
        final VersionRange range5 = range("1.7", "2.0", true, false);
        final VersionRange range6 = range(null, "2.0", false, false);
        final VersionRange range7 = range(null, "2.0", false, false);
        final VersionRange range8 = range(null, null, false, false);
        final VersionRange range9 = range(null, null, false, false);
        final VersionRange range10 = range("1.5", "2.0", true, true);

        //noinspection EqualsWithItself
        assertThat(range1).isEqualTo(range1);
        assertThat(range1).isEqualTo(range2);
        assertThat(range1).hasSameHashCodeAs(range2);
        assertThat(range3).isEqualTo(range4);
        assertThat(range3).hasSameHashCodeAs(range4);
        assertThat(range6).isEqualTo(range7);
        assertThat(range6).hasSameHashCodeAs(range7);
        assertThat(range8).isEqualTo(range9);
        assertThat(range8).hasSameHashCodeAs(range9);
        assertThat(range4).isNotEqualTo(range5);
        assertThat(range4).doesNotHaveSameHashCodeAs(range5);
        assertThat(range3).isNotEqualTo(range10);
        assertThat(range3).doesNotHaveSameHashCodeAs(range10);
        assertThat(range3).isNotEqualTo(null);
    }

    private Version version(final String version) {
        return MvnVersionScheme.parseVersion(version);
    }

    private VersionRange range(@Nullable final String minVersion, @Nullable final String maxVersion,
                               final boolean includeMin, final boolean includeMax) {
        return new VersionRange(minVersion == null ? null : version(minVersion),
                            maxVersion == null ? null : version(maxVersion),
                            includeMin, includeMax);
    }
}
