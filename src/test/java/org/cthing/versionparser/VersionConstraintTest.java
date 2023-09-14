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

import java.util.List;

import org.assertj.core.api.AbstractAssert;
import org.cthing.versionparser.maven.MvnVersionScheme;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.cthing.versionparser.VersionConstraint.ANY;
import static org.cthing.versionparser.VersionConstraint.EMPTY;
import static org.cthing.versionparser.VersionConstraintTest.ConstraintAssert.constraint;
import static org.cthing.versionparser.VersionConstraintTest.ConstraintAssert.version;
import static org.cthing.versionparser.VersionConstraintTest.TestAssertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;


public class VersionConstraintTest {

    @Test
    public void testAny() throws VersionParsingException {
        assertThat(ANY.isEmpty()).isFalse();
        assertThat(ANY.isAny()).isTrue();
        assertThat(ANY.isSingleVersion()).isFalse();
        assertThat(ANY.allows(MvnVersionScheme.parseVersion("1.0.0"))).isTrue();
        assertThat(ANY.allowsAll(constraint("[1.0.0,2.0.0)"))).isTrue();
        assertThat(ANY.allowsAny(constraint("[1.0.0,2.0.0)"))).isTrue();
        assertThat(ANY.intersect(constraint("[1.0.0,2.0.0)"))).isConstraint("[1.0.0,2.0.0)");
        assertThat(ANY.union(constraint("[1.0.0,2.0.0)"))).isAny();
        assertThat(ANY.difference(constraint("[1.0.0,2.0.0)"))).isConstraint("(,1.0.0),[2.0.0,)");
        assertThat(ANY).hasToString("(,)");
    }

    @Test
    public void testEmpty() {
        final VersionConstraint constraint = mock(VersionConstraint.class);
        when(constraint.isEmpty()).thenReturn(true);

        assertThat(EMPTY.isEmpty()).isTrue();
        assertThat(EMPTY.isAny()).isFalse();
        assertThat(EMPTY.isSingleVersion()).isFalse();
        assertThat(EMPTY.allows(MvnVersionScheme.parseVersion("1.0.0"))).isFalse();
        assertThat(EMPTY.allowsAll(constraint)).isTrue();
        assertThat(EMPTY.allowsAny(constraint)).isFalse();
        assertThat(EMPTY.intersect(constraint)).isEmpty();
        assertThat(EMPTY.union(constraint)).isEqualTo(constraint);
        assertThat(EMPTY.difference(constraint)).isEmpty();
        assertThat(EMPTY).hasToString("<empty>");
    }

    @Test
    public void testConstructionSingleVersion() {
        final Version version = version("1.2.3");
        final VersionConstraint constraint = new VersionConstraint(version);
        assertThat(constraint.isEmpty()).isFalse();
        assertThat(constraint.isAny()).isFalse();
        assertThat(constraint.isSingleVersion()).isTrue();
        assertThat(constraint).hasToString("[1.2.3]");
    }

    @Test
    public void testConstructionSingleRange() {
        final Version version1 = version("1.2.3");
        final Version version2 = version("3.0");
        VersionConstraint constraint = new VersionConstraint(version1, version2, true, false);
        assertThat(constraint.isEmpty()).isFalse();
        assertThat(constraint.isAny()).isFalse();
        assertThat(constraint.isSingleVersion()).isFalse();
        assertThat(constraint).hasToString("[1.2.3,3.0)");

        final Version version3 = version("1.2.3");
        constraint = new VersionConstraint(version3, version3, false, false);
        assertThat(constraint.isEmpty()).isFalse();
        assertThat(constraint.isAny()).isFalse();
        assertThat(constraint.isSingleVersion()).isFalse();
        assertThat(constraint).hasToString("(1.2.3,1.2.3)");
    }

    @Test
    public void testConstructionMultipleRanges() {
        final VersionRange range1 = new VersionRange(version("1.5"), version("2.0"), true, false);
        final VersionRange range2 = new VersionRange(version("3.0"), version("5.0"), false, false);
        final VersionRange range3 = new VersionRange(version("3.0"), version("3.0"), true, true);

        VersionConstraint constraint = new VersionConstraint(List.of(range2, range1));
        assertThat(constraint.isEmpty()).isFalse();
        assertThat(constraint.isAny()).isFalse();
        assertThat(constraint.isSingleVersion()).isFalse();
        assertThat(constraint).hasToString("[1.5,2.0),(3.0,5.0)");

        constraint = new VersionConstraint(List.of(range3));
        assertThat(constraint.isEmpty()).isFalse();
        assertThat(constraint.isAny()).isFalse();
        assertThat(constraint.isSingleVersion()).isTrue();
        assertThat(constraint).hasToString("[3.0]");
    }

    @Test
    public void testConstructionBad() {
        final Version version1 = version("3.0");
        final Version version2 = version("1.2.3");
        assertThatIllegalArgumentException().isThrownBy(() -> new VersionConstraint(version1, version2, true, false));
    }

    @Test
    public void testAllows() throws VersionParsingException {
        assertThat(ANY.allows(version("1.2.3"))).isTrue();
        assertThat(EMPTY.allows(version("1.2.3"))).isFalse();

        assertThat(constraint("[1.5,2.0]").allows(version("1.6"))).isTrue();
        assertThat(constraint("[1.5,2.0]").allows(version("1.5"))).isTrue();
        assertThat(constraint("[1.5,2.0]").allows(version("2.0"))).isTrue();
        assertThat(constraint("[1.5,2.0]").allows(version("1.4"))).isFalse();
        assertThat(constraint("[1.5,2.0]").allows(version("2.1"))).isFalse();

        assertThat(constraint("(1.5,2.0]").allows(version("1.6"))).isTrue();
        assertThat(constraint("(1.5,2.0]").allows(version("2.0"))).isTrue();
        assertThat(constraint("(1.5,2.0]").allows(version("1.5"))).isFalse();
        assertThat(constraint("(1.5,2.0]").allows(version("1.4"))).isFalse();
        assertThat(constraint("(1.5,2.0]").allows(version("2.1"))).isFalse();

        assertThat(constraint("[1.5,2.0)").allows(version("1.6"))).isTrue();
        assertThat(constraint("[1.5,2.0)").allows(version("1.5"))).isTrue();
        assertThat(constraint("[1.5,2.0)").allows(version("2.0"))).isFalse();
        assertThat(constraint("[1.5,2.0)").allows(version("1.4"))).isFalse();
        assertThat(constraint("[1.5,2.0)").allows(version("2.1"))).isFalse();

        assertThat(constraint("[1.5,2.0)").allows(version("2.0-beta"))).isTrue();
        assertThat(constraint("[1.5,2.0.min)").allows(version("2.0-beta"))).isFalse();

        assertThat(constraint("[1.5,)").allows(version("5.0"))).isTrue();
        assertThat(constraint("[1.5,)").allows(version("1.6"))).isTrue();
        assertThat(constraint("[1.5,)").allows(version("1.5"))).isTrue();
        assertThat(constraint("[1.5,)").allows(version("1.4"))).isFalse();

        assertThat(constraint("(,2.0]").allows(version("2.0"))).isTrue();
        assertThat(constraint("(,2.0]").allows(version("0.1"))).isTrue();
        assertThat(constraint("(,2.0]").allows(version("2.1"))).isFalse();

        assertThat(constraint("(,)").allows(version("0.1"))).isTrue();
        assertThat(constraint("(,)").allows(version("3.4.1"))).isTrue();

        assertThat(constraint("[1.5,2.0),[3.0,4.0)").allows(version("1.5"))).isTrue();
        assertThat(constraint("[1.5,2.0),[3.0,4.0)").allows(version("3.0"))).isTrue();
        assertThat(constraint("[1.5,2.0),[3.0,4.0)").allows(version("1.6"))).isTrue();
        assertThat(constraint("[1.5,2.0),[3.0,4.0)").allows(version("2.0"))).isFalse();
        assertThat(constraint("[1.5,2.0),[3.0,4.0)").allows(version("4.0"))).isFalse();
        assertThat(constraint("[1.5,2.0),[3.0,4.0)").allows(version("5.0"))).isFalse();
        assertThat(constraint("[1.5,2.0),[3.0,4.0)").allows(version("1.0"))).isFalse();
        assertThat(constraint("[1.5,2.0),[3.0,4.0)").allows(version("2.5"))).isFalse();
    }

    @Test
    public void testAllowsAll() throws VersionParsingException {
        assertThat(constraint("[1.5,2.0]").allowsAll(EMPTY)).isTrue();
        assertThat(new VersionConstraint(version("1.2.3")).allowsAll(EMPTY)).isTrue();
        assertThat(ANY.allowsAll(EMPTY)).isTrue();

        assertThat(constraint("[1.5,2.0)").allowsAll(constraint("[1.6]"))).isTrue();
        assertThat(constraint("[1.5,2.0)").allowsAll(constraint("[1.6,1.9)"))).isTrue();
        assertThat(constraint("[1.5,2.0)").allowsAll(constraint("[1.5,2.0)"))).isTrue();
        assertThat(constraint("[1.5,2.0)").allowsAll(constraint("[1.5,2.0]"))).isFalse();
        assertThat(constraint("[1.5,2.0)").allowsAll(constraint("[1.0,1.0]"))).isFalse();
        assertThat(constraint("[1.5,2.0)").allowsAll(constraint("[1.0,1.3]"))).isFalse();
        assertThat(constraint("[1.5,2.0)").allowsAll(constraint("[2.0,2.3]"))).isFalse();
        assertThat(constraint("[1.5,2.0)").allowsAll(constraint("[4.0,7.3]"))).isFalse();
        assertThat(constraint("[1.5,2.0)").allowsAll(constraint("(,)"))).isFalse();
        assertThat(constraint("[1.0,1.1)").allowsAll(constraint("[1.0.abcd]"))).isTrue();
        assertThat(constraint("[1.0,1.1)").allowsAll(constraint("[1.0-abcd]"))).isTrue();
        assertThat(constraint("(,)").allowsAll(constraint("[4.0,7.3]"))).isTrue();
        assertThat(constraint("(,)").allowsAll(constraint("(,7.3]"))).isTrue();
        assertThat(constraint("(,)").allowsAll(constraint("(,)"))).isTrue();

        assertThat(constraint("[1.5,2.0)").allowsAll(constraint("[1.6],[1.9]"))).isTrue();
        assertThat(constraint("[1.5,2.0)").allowsAll(constraint("[1.6,1.7],[1.9]"))).isTrue();
        assertThat(constraint("[1.5,2.0)").allowsAll(constraint("[1.2,1.7],[1.9]"))).isFalse();
        assertThat(constraint("[1.5,2.0)").allowsAll(constraint("[1.2,1.4],[1.9]"))).isFalse();
        assertThat(constraint("[1.5,2.0)").allowsAll(constraint("[1.2,1.4],[2.9]"))).isFalse();

        assertThat(constraint("[1.5,2.0),[3.0,4.0)").allowsAll(EMPTY)).isTrue();

        assertThat(constraint("[1.5,2.0),[3.0,4.0)").allowsAll(constraint("[1.5]"))).isTrue();
        assertThat(constraint("[1.5,2.0),[3.0,4.0)").allowsAll(constraint("[1.7]"))).isTrue();
        assertThat(constraint("[1.5,2.0),[3.0,4.0)").allowsAll(constraint("[3.0]"))).isTrue();
        assertThat(constraint("[1.5,2.0),[3.0,4.0)").allowsAll(constraint("[3.5]"))).isTrue();
        assertThat(constraint("[1.5,2.0),[3.0,4.0)").allowsAll(constraint("[1.0]"))).isFalse();
        assertThat(constraint("[1.5,2.0),[3.0,4.0)").allowsAll(constraint("[2.5]"))).isFalse();
        assertThat(constraint("[1.5,2.0),[3.0,4.0)").allowsAll(constraint("[4.0]"))).isFalse();
        assertThat(constraint("[1.5,2.0),[3.0,4.0)").allowsAll(constraint("[4.1]"))).isFalse();

        assertThat(constraint("[1.5,2.0),[3.0,4.0)").allowsAll(constraint("[1.5,2.0)"))).isTrue();
        assertThat(constraint("[1.5,2.0),[3.0,4.0)").allowsAll(constraint("[3.0,4.0)"))).isTrue();
        assertThat(constraint("[1.5,2.0),[3.0,4.0)").allowsAll(constraint("[1.6,1.9)"))).isTrue();
        assertThat(constraint("[1.5,2.0),[3.0,4.0)").allowsAll(constraint("[3.1,3.6)"))).isTrue();
        assertThat(constraint("[1.5,2.0),[3.0,4.0)").allowsAll(constraint("[1.0,2.0)"))).isFalse();
        assertThat(constraint("[1.5,2.0),[3.0,4.0)").allowsAll(constraint("[1.7,2.0]"))).isFalse();
        assertThat(constraint("[1.5,2.0),[3.0,4.0)").allowsAll(constraint("[1.7,2.5]"))).isFalse();
        assertThat(constraint("[1.5,2.0),[3.0,4.0)").allowsAll(constraint("[2.5,3.1]"))).isFalse();
        assertThat(constraint("[1.5,2.0),[3.0,4.0)").allowsAll(constraint("[3.5,6.1]"))).isFalse();
        assertThat(constraint("[1.5,2.0),[3.0,4.0)").allowsAll(constraint("[1.0,1.4]"))).isFalse();
        assertThat(constraint("[1.5,2.0),[3.0,4.0)").allowsAll(constraint("[2.1,2.9]"))).isFalse();
        assertThat(constraint("[1.5,2.0),[3.0,4.0)").allowsAll(constraint("[5.0,6.0]"))).isFalse();

        assertThat(constraint("[1.5,2.0),[3.0,4.0)").allowsAll(constraint("[1.5,2.0),[3.0,4.0)"))).isTrue();
        assertThat(constraint("[1.5,2.0),[3.0,4.0)").allowsAll(constraint("[1.6,1.8),[3.1,3.7]"))).isTrue();
        assertThat(constraint("[1.5,2.0),[3.0,4.0)").allowsAll(constraint("[1.4,1.8),[3.1,3.7]"))).isFalse();
        assertThat(constraint("[1.5,2.0),[3.0,4.0)").allowsAll(constraint("[1.6,1.8),[3.1,4.0]"))).isFalse();
        assertThat(constraint("[1.5,2.0),[3.0,4.0)").allowsAll(constraint("[1.6,1.8),[3.1,4.5]"))).isFalse();
        assertThat(constraint("[1.5,2.0),[3.0,4.0)").allowsAll(constraint("[1.0,1.4],[2.1,2.9],[4.5,6.0]"))).isFalse();
    }

    @Test
    public void testAllowsAny() throws VersionParsingException {
        assertThat(constraint("[1.5,2.0]").allowsAny(EMPTY)).isFalse();
        assertThat(new VersionConstraint(version("1.2.3")).allowsAny(EMPTY)).isFalse();
        assertThat(ANY.allowsAny(EMPTY)).isFalse();

        assertThat(constraint("[1.5,2.0)").allowsAny(constraint("[1.6]"))).isTrue();
        assertThat(constraint("[1.5,2.0)").allowsAny(constraint("[1.6,1.9)"))).isTrue();
        assertThat(constraint("[1.5,2.0)").allowsAny(constraint("[1.5,2.0)"))).isTrue();
        assertThat(constraint("[1.5,2.0)").allowsAny(constraint("[1.5,2.0]"))).isTrue();
        assertThat(constraint("[1.5,2.0)").allowsAny(constraint("[1.0,1.0]"))).isFalse();
        assertThat(constraint("[1.5,2.0)").allowsAny(constraint("[1.0,1.3]"))).isFalse();
        assertThat(constraint("[1.5,2.0)").allowsAny(constraint("[2.0,2.3]"))).isFalse();
        assertThat(constraint("[1.5,2.0)").allowsAny(constraint("[4.0,7.3]"))).isFalse();
        assertThat(constraint("[1.5,2.0)").allowsAny(constraint("(,)"))).isTrue();
        assertThat(constraint("(,)").allowsAny(constraint("[4.0,7.3]"))).isTrue();
        assertThat(constraint("(,)").allowsAny(constraint("(,7.3]"))).isTrue();
        assertThat(constraint("(,)").allowsAny(constraint("(,)"))).isTrue();

        assertThat(constraint("[1.5,2.0)").allowsAny(constraint("[1.6],[1.9]"))).isTrue();
        assertThat(constraint("[1.5,2.0)").allowsAny(constraint("[1.6,1.7],[1.9]"))).isTrue();
        assertThat(constraint("[1.5,2.0)").allowsAny(constraint("[1.2,1.7],[1.9]"))).isTrue();
        assertThat(constraint("[1.5,2.0)").allowsAny(constraint("[1.2,1.4],[1.9]"))).isTrue();
        assertThat(constraint("[1.5,2.0)").allowsAny(constraint("[1.2,1.4],[2.9]"))).isFalse();

        assertThat(constraint("[1.5,2.0),[3.0,4.0)").allowsAny(EMPTY)).isFalse();

        assertThat(constraint("[1.5,2.0),[3.0,4.0)").allowsAny(constraint("[1.5]"))).isTrue();
        assertThat(constraint("[1.5,2.0),[3.0,4.0)").allowsAny(constraint("[1.7]"))).isTrue();
        assertThat(constraint("[1.5,2.0),[3.0,4.0)").allowsAny(constraint("[3.0]"))).isTrue();
        assertThat(constraint("[1.5,2.0),[3.0,4.0)").allowsAny(constraint("[3.5]"))).isTrue();
        assertThat(constraint("[1.5,2.0),[3.0,4.0)").allowsAny(constraint("[1.0]"))).isFalse();
        assertThat(constraint("[1.5,2.0),[3.0,4.0)").allowsAny(constraint("[2.5]"))).isFalse();
        assertThat(constraint("[1.5,2.0),[3.0,4.0)").allowsAny(constraint("[4.0]"))).isFalse();
        assertThat(constraint("[1.5,2.0),[3.0,4.0)").allowsAny(constraint("[4.1]"))).isFalse();

        assertThat(constraint("[1.5,2.0),[3.0,4.0)").allowsAny(constraint("[1.5,2.0)"))).isTrue();
        assertThat(constraint("[1.5,2.0),[3.0,4.0)").allowsAny(constraint("[3.0,4.0)"))).isTrue();
        assertThat(constraint("[1.5,2.0),[3.0,4.0)").allowsAny(constraint("[1.6,1.9)"))).isTrue();
        assertThat(constraint("[1.5,2.0),[3.0,4.0)").allowsAny(constraint("[3.1,3.6)"))).isTrue();
        assertThat(constraint("[1.5,2.0),[3.0,4.0)").allowsAny(constraint("[1.0,2.0)"))).isTrue();
        assertThat(constraint("[1.5,2.0),[3.0,4.0)").allowsAny(constraint("[1.7,2.0]"))).isTrue();
        assertThat(constraint("[1.5,2.0),[3.0,4.0)").allowsAny(constraint("[1.7,2.5]"))).isTrue();
        assertThat(constraint("[1.5,2.0),[3.0,4.0)").allowsAny(constraint("[2.5,3.1]"))).isTrue();
        assertThat(constraint("[1.5,2.0),[3.0,4.0)").allowsAny(constraint("[3.5,6.1]"))).isTrue();
        assertThat(constraint("[1.5,2.0),[3.0,4.0)").allowsAny(constraint("[1.0,1.4]"))).isFalse();
        assertThat(constraint("[1.5,2.0),[3.0,4.0)").allowsAny(constraint("[2.1,2.9]"))).isFalse();
        assertThat(constraint("[1.5,2.0),[3.0,4.0)").allowsAny(constraint("[5.0,6.0]"))).isFalse();

        assertThat(constraint("[1.5,2.0),[3.0,4.0)").allowsAny(constraint("[1.5,2.0),[3.0,4.0)"))).isTrue();
        assertThat(constraint("[1.5,2.0),[3.0,4.0)").allowsAny(constraint("[1.6,1.8),[3.1,3.7]"))).isTrue();
        assertThat(constraint("[1.5,2.0),[3.0,4.0)").allowsAny(constraint("[1.4,1.8),[3.1,3.7]"))).isTrue();
        assertThat(constraint("[1.5,2.0),[3.0,4.0)").allowsAny(constraint("[1.6,1.8),[3.1,4.0]"))).isTrue();
        assertThat(constraint("[1.5,2.0),[3.0,4.0)").allowsAny(constraint("[1.6,1.8),[3.1,4.5]"))).isTrue();
        assertThat(constraint("[1.5,2.0),[3.0,4.0)").allowsAny(constraint("[1.0,1.4],[2.1,2.9],[4.5,6.0]"))).isFalse();
    }

    @Test
    public void testIntersect() throws VersionParsingException {
        assertThat(constraint("[1.5]").intersect(EMPTY)).isEmpty();

        assertThat(constraint("[1.5]").intersect(constraint("[1.5]"))).isConstraint("[1.5]");
        assertThat(constraint("[1.5,2.0]").intersect(constraint("[1.7]"))).isConstraint("[1.7]");

        assertThat(constraint("[1.5,2.0]").intersect(constraint("[1.6,1.9]"))).isConstraint("[1.6,1.9]");
        assertThat(constraint("[1.5,2.0]").intersect(constraint("[1.6,1.9)"))).isConstraint("[1.6,1.9)");
        assertThat(constraint("[1.5,2.0]").intersect(constraint("[1.7,4.0)"))).isConstraint("[1.7,2.0]");
        assertThat(constraint("[1.5,2.0]").intersect(constraint("[1.2,1.8]"))).isConstraint("[1.5,1.8]");
        assertThat(constraint("[1.5,2.0]").intersect(constraint("[1.2,)"))).isConstraint("[1.5,2.0]");
        assertThat(constraint("[1.5,)").intersect(constraint("[1.2,1.8]"))).isConstraint("[1.5,1.8]");
        assertThat(constraint("(,3.0)").intersect(constraint("(,1.8]"))).isConstraint("(,1.8]");
        assertThat(constraint("[1.5,2.0)").intersect(constraint("[1.2,1.4]"))).isEmpty();
        assertThat(constraint("[1.5,2.0)").intersect(constraint("(,)"))).isConstraint("[1.5,2.0)");
        assertThat(constraint("(,)").intersect(constraint("[1.5,2.0)"))).isConstraint("[1.5,2.0)");
        assertThat(constraint("(,)").intersect(constraint("(,)"))).isConstraint("(,)");

        assertThat(constraint("[1.5,2.0)").intersect(constraint("[1.6],[1.8]"))).isConstraint("[1.6],[1.8]");
        assertThat(constraint("[1.5,2.0)").intersect(constraint("[1.2],[1.8]"))).isConstraint("[1.8]");
        assertThat(constraint("[1.5,2.0)").intersect(constraint("[1.6],[1.8,2.0]"))).isConstraint("[1.6],[1.8,2.0)");
        assertThat(constraint("[1.5,2.0)").intersect(constraint("[1.2],[1.8,2.0)"))).isConstraint("[1.8,2.0)");
        assertThat(constraint("[1.5,2.0)").intersect(constraint("(,),[1.8,2.0)"))).isConstraint("[1.5,2.0)");
        assertThat(constraint("[1.5,2.0)").intersect(constraint("(,1.6],[1.8,3.0)"))).isConstraint("[1.5,1.6],[1.8,2.0)");

        assertThat(constraint("[1.5,2.0),[3.0,4.0)").intersect(EMPTY)).isEmpty();

        assertThat(constraint("[1.5,2.0),[3.0,4.0)").intersect(constraint("[1.6,1.6]"))).isConstraint("[1.6,1.6]");
        assertThat(constraint("[1.5,2.0),[3.0,4.0)").intersect(constraint("[3.1,3.1]"))).isConstraint("[3.1,3.1]");
        assertThat(constraint("[1.5,2.0),[3.0,4.0)").intersect(constraint("[1.0,1.0]"))).isEmpty();
        assertThat(constraint("[1.5,2.0),[3.0,4.0)").intersect(constraint("[2.1,2.1]"))).isEmpty();
        assertThat(constraint("[1.5,2.0),[3.0,4.0)").intersect(constraint("[4.1,4.1]"))).isEmpty();

        assertThat(constraint("[1.5,2.0),[3.0,4.0)").intersect(constraint("[1.6,1.8]"))).isConstraint("[1.6,1.8]");
        assertThat(constraint("[1.5,2.0),[3.0,4.0)").intersect(constraint("[3.1,3.5]"))).isConstraint("[3.1,3.5]");
        assertThat(constraint("[1.5,2.0),[3.0,4.0)").intersect(constraint("[1.5,2.0]"))).isConstraint("[1.5,2.0)");
        assertThat(constraint("[1.5,2.0),[3.0,4.0)").intersect(constraint("[3.0,4.0]"))).isConstraint("[3.0,4.0)");
        assertThat(constraint("[1.5,2.0),[3.0,4.0)").intersect(constraint("[1.0,1.8]"))).isConstraint("[1.5,1.8]");
        assertThat(constraint("[1.5,2.0),[3.0,4.0)").intersect(constraint("[3.5,5.0]"))).isConstraint("[3.5,4.0)");
        assertThat(constraint("[1.5,2.0),[3.0,4.0)").intersect(constraint("[1.0,1.3]"))).isEmpty();
        assertThat(constraint("[1.5,2.0),[3.0,4.0)").intersect(constraint("[2.1,2.9]"))).isEmpty();
        assertThat(constraint("[1.5,2.0),[3.0,4.0)").intersect(constraint("[5.0,7.0]"))).isEmpty();

        assertThat(constraint("[1.5,2.0),[3.0,4.0)").intersect(constraint("[1.6,1.8],[3.2,3.4]")))
                .isConstraint("[1.6,1.8],[3.2,3.4]");
        assertThat(constraint("[1.5,2.0),[3.0,4.0)").intersect(constraint("[1.0,1.5],[2.8,3.3]")))
                .isConstraint("[1.5,1.5],[3.0,3.3]");
        assertThat(constraint("[1.5,2.0),[3.0,4.0)").intersect(constraint("[1.0,1.4],[2.1,2.9],[4.1,6.0)"))).isEmpty();
    }

    @Test
    public void testUnion() throws VersionParsingException {
        assertThat(constraint("[1.5]").union(EMPTY)).isConstraint("[1.5]");

        assertThat(constraint("[1.5]").union(constraint("[1.5]"))).isConstraint("[1.5]");
        assertThat(constraint("[1.5]").union(constraint("[1.6]"))).isConstraint("[1.5],[1.6]");

        assertThat(constraint("[1.5,1.6]").union(constraint("[1.6]"))).isConstraint("[1.5,1.6]");
        assertThat(constraint("[1.5,1.7)").union(constraint("[1.6]"))).isConstraint("[1.5,1.7)");
        assertThat(constraint("[1.5,1.7]").union(constraint("[1.6]"))).isConstraint("[1.5,1.7]");
        assertThat(constraint("(1.5,1.7]").union(constraint("[1.6]"))).isConstraint("(1.5,1.7]");
        assertThat(constraint("(1.5,1.7]").union(constraint("[1.5]"))).isConstraint("[1.5,1.7]");
        assertThat(constraint("(1.5,1.7]").union(constraint("[1.5,2.0]"))).isConstraint("[1.5,2.0]");
        assertThat(constraint("(1.5,)").union(constraint("[1.3,2.0]"))).isConstraint("[1.3,)");
        assertThat(constraint("(,1.7]").union(constraint("[1.5,2.0]"))).isConstraint("(,2.0]");
        assertThat(constraint("(,)").union(constraint("[1.5,2.0]"))).isConstraint("(,)");
        assertThat(constraint("[1.2,2.0)").union(constraint("(,)"))).isConstraint("(,)");
        assertThat(constraint("(,)").union(constraint("(,)"))).isConstraint("(,)");
        assertThat(constraint("(1.0,2.0)").union(constraint("(2.0,3.0)"))).isConstraint("(1.0,2.0),(2.0,3.0)");

        assertThat(constraint("[1.5,2.0)").union(constraint("[1.5],[3.0]"))).isConstraint("[1.5,2.0),[3.0]");
        assertThat(constraint("[1.5,2.0)").union(constraint("[1.2,1.8],[3.0]"))).isConstraint("[1.2,2.0),[3.0]");
        assertThat(constraint("[1.5,2.0)").union(constraint("[1.2,1.6],[1.7,3.0]"))).isConstraint("[1.2,3.0]");

        assertThat(constraint("[1.5,2.0),[3.0,4.0)").union(constraint("[1.6,1.6]")))
                .isConstraint("[1.5,2.0),[3.0,4.0)");
        assertThat(constraint("[1.5,2.0),[3.0,4.0)").union(constraint("[1.4,1.4]")))
                .isConstraint("[1.4,1.4],[1.5,2.0),[3.0,4.0)");
        assertThat(constraint("[1.5,2.0),[3.0,4.0)").union(constraint("[1.4,1.6]")))
                .isConstraint("[1.4,2.0),[3.0,4.0)");
        assertThat(constraint("[1.5,2.0),[3.0,4.0)").union(constraint("[3.1,4.5]")))
                .isConstraint("[1.5,2.0),[3.0,4.5]");
    }

    @Test
    public void testDifference() throws VersionParsingException {
        assertThat(constraint("[1.5]").difference(EMPTY)).isConstraint("[1.5]");
        assertThat(constraint("[1.5]").difference(constraint("[1.5]"))).isEmpty();

        assertThat(constraint("[1.2,2.0)").difference(constraint("[1.5,1.5]"))).isConstraint("[1.2,1.5),(1.5,2.0)");
        assertThat(constraint("[1.2,2.0)").difference(constraint("[1.2,1.2]"))).isConstraint("(1.2,2.0)");
        assertThat(constraint("[1.2,2.0]").difference(constraint("[2.0,2.0]"))).isConstraint("[1.2,2.0)");
        assertThat(constraint("[1.2,2.0)").difference(constraint("[1.5,1.7]"))).isConstraint("[1.2,1.5),(1.7,2.0)");
        assertThat(constraint("[1.2,2.0)").difference(constraint("[1.5,1.7)"))).isConstraint("[1.2,1.5),[1.7,2.0)");
        assertThat(constraint("[1.2,2.0)").difference(constraint("(1.5,1.7]"))).isConstraint("[1.2,1.5],(1.7,2.0)");
        assertThat(constraint("[1.2,2.0)").difference(constraint("(1.5,2.7]"))).isConstraint("[1.2,1.5]");
        assertThat(constraint("[1.2,2.0)").difference(constraint("(1.0,1.5]"))).isConstraint("(1.5,2.0)");
        assertThat(constraint("[1.2,2.0)").difference(constraint("(3.0,5.5]"))).isConstraint("[1.2,2.0)");
        assertThat(constraint("[1.2,2.0)").difference(constraint("(1.2,1.4]"))).isConstraint("[1.2],(1.4,2.0)");
        assertThat(constraint("[1.2,2.0]").difference(constraint("(1.5,2.0)"))).isConstraint("[1.2,1.5],[2.0]");
        assertThat(constraint("[1.2,2.0)").difference(constraint("(,)"))).isEmpty();

        assertThat(constraint("[1.2,2.0)").difference(constraint("[1.5,1.5],[1.7,1.7]")))
                .isConstraint("[1.2,1.5),(1.5,1.7),(1.7,2.0)");
        assertThat(constraint("[1.2,2.0)").difference(constraint("[1.5,1.6],[1.8,3.0]")))
                .isConstraint("[1.2,1.5),(1.6,1.8)");
        assertThat(constraint("[1.5,2.0)").difference(constraint("[1.0,1.1],(1.2,1.4]"))).isConstraint("[1.5,2.0)");
        assertThat(constraint("[1.5,2.0)").difference(constraint("[2.1,3.0),(4.2,5.4]"))).isConstraint("[1.5,2.0)");
        assertThat(constraint("[1.5,2.0)").difference(constraint("[1.0,3.0),(4.2,5.4]"))).isEmpty();

        assertThat(constraint("[1.5],[2.0,3.0)").difference(EMPTY)).isConstraint("[1.5],[2.0,3.0)");
        assertThat(constraint("[1.5],[2.0,3.0)").difference(constraint("[1.5],[2.0,3.0)"))).isEmpty();

        assertThat(constraint("[1.5],[2.0,3.0)").difference(constraint("[1.5,1.5]"))).isConstraint("[2.0,3.0)");
        assertThat(constraint("[1.5],[2.0,3.0)").difference(constraint("[2.0,2.0]"))).isConstraint("[1.5],(2.0,3.0)");
        assertThat(constraint("[1.5],[2.0,3.0)").difference(constraint("[2.5,2.5]")))
                .isConstraint("[1.5],[2.0,2.5),(2.5,3.0)");
        assertThat(constraint("[1.5],[2.0,3.0)").difference(constraint("[2.5,2.7]")))
                .isConstraint("[1.5],[2.0,2.5),(2.7,3.0)");
        assertThat(constraint("[1.5],[2.0,3.0)").difference(constraint("[2.5,4.0)"))).isConstraint("[1.5],[2.0,2.5)");
        assertThat(constraint("[1.5],[2.0,3.0)").difference(constraint("[1.0,2.5)"))).isConstraint("[2.5,3.0)");
        assertThat(constraint("[1.5],[2.0,3.0)").difference(constraint("[1.0,10.0)"))).isEmpty();
        assertThat(constraint("[1.5],[2.0,3.0),[4.0,7.0)").difference(constraint("[1.0,2.5),[2.9]")))
                .isConstraint("[2.5,2.9),(2.9,3.0),[4.0,7.0)");
        assertThat(constraint("[1.5],[2.0,3.0),[4.0,7.0)").difference(constraint("[1.0,2.5),[2.9]")))
                .isConstraint("[2.5,2.9),(2.9,3.0),[4.0,7.0)");
        assertThat(constraint("[1.0,3.0),(4.0,6.0)").difference(constraint("[2.0,5.0)")))
                .isConstraint("[1.0,2.0),[5.0,6.0)");
        assertThat(constraint("[1.0,3.0),(4.0,6.0)").difference(constraint("[2.0,2.0]")))
                .isConstraint("[1.0,2.0),(2.0,3.0),(4.0,6.0)");
        assertThat(constraint("[1.0,3.0),(4.0,6.0)").difference(constraint("[2.0],[7.0,8.0)")))
                .isConstraint("[1.0,2.0),(2.0,3.0),(4.0,6.0)");

        assertThat(EMPTY.difference(EMPTY)).isEmpty();
        assertThat(EMPTY.difference(constraint("[1.2,2.0)"))).isEmpty();
    }


    @SuppressWarnings("UnusedReturnValue")
    public static class ConstraintAssert extends AbstractAssert<ConstraintAssert, VersionConstraint> {

        public ConstraintAssert(final VersionConstraint actual) {
            super(actual, ConstraintAssert.class);
        }

        public ConstraintAssert isConstraint(final String constraintStr) throws VersionParsingException {
            isNotNull();

            final VersionConstraint constraint = MvnVersionScheme.parseConstraint(constraintStr);
            if (!this.actual.equals(constraint)) {
                failWithMessage("Expected constraint %s but was %s", constraintStr, this.actual);
            }
            return this;
        }

        public ConstraintAssert isEmpty() {
            isNotNull();

            if (!this.actual.equals(EMPTY)) {
                failWithMessage("Expected EMPTY constraint but was %s", this.actual);
            }
            return this;
        }

        public ConstraintAssert isAny() {
            isNotNull();

            if (!this.actual.equals(ANY)) {
                failWithMessage("Expected ANY constraint but was %s", this.actual);
            }
            return this;
        }

        public static Version version(final String version) {
            return MvnVersionScheme.parseVersion(version);
        }

        public static VersionConstraint constraint(final String constraintStr) throws VersionParsingException {
            return MvnVersionScheme.parseConstraint(constraintStr);
        }
    }


    public static final class TestAssertions {

        private TestAssertions() {
        }

        public static ConstraintAssert assertThat(final VersionConstraint actual) {
            return new ConstraintAssert(actual);
        }
    }
}
