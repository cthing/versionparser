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

package org.cthing.versionparser.gem;

import java.util.List;
import java.util.stream.Stream;

import org.cthing.versionparser.Version;
import org.cthing.versionparser.VersionParsingException;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;
import static org.cthing.versionparser.gem.GemVersionTest.Order.EQ;
import static org.cthing.versionparser.gem.GemVersionTest.Order.GT;
import static org.cthing.versionparser.gem.GemVersionTest.Order.LT;
import static org.junit.jupiter.params.provider.Arguments.arguments;


public class GemVersionTest {

    enum Order {
        LT,
        EQ,
        GT,
    }

    @Nested
    class ComponentTest {

        @Test
        public void testConstructNumber() {
            final GemVersion.Component component = new GemVersion.Component(123);
            assertThat(component.isString()).isFalse();
            assertThat(component.isNumber()).isTrue();
            assertThat(component.getNumber()).isEqualTo(123);
            assertThatIllegalStateException().isThrownBy(component::getString);
            assertThat(component).hasToString("123");
        }

        @Test
        public void testConstructString() {
            final GemVersion.Component component = new GemVersion.Component("abc");
            assertThat(component.isString()).isTrue();
            assertThat(component.isNumber()).isFalse();
            assertThat(component.getString()).isEqualTo("abc");
            assertThatIllegalStateException().isThrownBy(component::getNumber);
            assertThat(component).hasToString("abc");
        }

        @Test
        public void testEquality() {
            final GemVersion.Component component1 = new GemVersion.Component("abc");
            final GemVersion.Component component2 = new GemVersion.Component("abc");
            final GemVersion.Component component3 = new GemVersion.Component("xyz");
            final GemVersion.Component component4 = new GemVersion.Component(1);
            final GemVersion.Component component5 = new GemVersion.Component(1);
            final GemVersion.Component component6 = new GemVersion.Component(2);

            assertThat(component1).isEqualTo(component2);
            assertThat(component1).hasSameHashCodeAs(component2);
            assertThat(component1).isNotEqualTo(component3);
            assertThat(component1).doesNotHaveSameHashCodeAs(component3);

            assertThat(component4).isEqualTo(component5);
            assertThat(component4).hasSameHashCodeAs(component5);
            assertThat(component4).isNotEqualTo(component6);
            assertThat(component4).doesNotHaveSameHashCodeAs(component6);

            assertThat(component1).isNotEqualTo(component4);
            assertThat(component1).doesNotHaveSameHashCodeAs(component4);
        }

        @Test
        @SuppressWarnings("EqualsWithItself")
        public void testOrdering() {
            final GemVersion.Component component1 = new GemVersion.Component("def");
            final GemVersion.Component component2 = new GemVersion.Component("def");
            final GemVersion.Component component3 = new GemVersion.Component("abc");
            final GemVersion.Component component4 = new GemVersion.Component(2);
            final GemVersion.Component component5 = new GemVersion.Component(2);
            final GemVersion.Component component6 = new GemVersion.Component(1);

            assertThat(component1.compareTo(component1)).isEqualTo(0);
            assertThat(component1.compareTo(component2)).isEqualTo(0);
            assertThat(component1.compareTo(component3)).isGreaterThan(0);
            assertThat(component3.compareTo(component1)).isLessThan(0);

            assertThat(component4.compareTo(component4)).isEqualTo(0);
            assertThat(component4.compareTo(component5)).isEqualTo(0);
            assertThat(component4.compareTo(component6)).isGreaterThan(0);
            assertThat(component6.compareTo(component4)).isLessThan(0);

            assertThat(component1.compareTo(component4)).isLessThan(0);
            assertThat(component4.compareTo(component1)).isGreaterThan(0);
        }
    }

    static Stream<Arguments> partitionProvider() {
        return Stream.of(
                arguments("", List.of()),
                arguments("1", List.of(number(1L))),
                arguments("a", List.of(string("a"))),
                arguments("1.0", List.of(number(1L), number(0L))),
                arguments("1.0.a", List.of(number(1L), number(0L), string("a"))),
                arguments("1.0.a7", List.of(number(1L), number(0L), string("a"), number(7L))),
                arguments("1.0-beta3", List.of(number(1L), number(0L), string("beta"), number(3L)))
        );
    }

    @ParameterizedTest
    @MethodSource("partitionProvider")
    public void testPartitionComponents(final String version, final List<GemVersion.Component> components) {
        assertThat(GemVersion.partitionComponents(version)).isEqualTo(components);
    }

    static Stream<Arguments> correctProvider() {
        return Stream.of(
                arguments("", true),
                arguments("1", true),
                arguments("1.0.0-beta3", true),
                arguments("junk", false),
                arguments("1.0\n2.0", false),
                arguments("1..2", false),
                arguments("1.2 3.4", false),
                arguments("2.3422222.222.222222222.22222.ads0as.dasd0.ddd2222.2.qd3e.", false)
        );
    }

    @ParameterizedTest
    @MethodSource("correctProvider")
    public void testIsCorrect(final String version, final boolean correct) {
        assertThat(GemVersion.isCorrect(version)).isEqualTo(correct);
        if (!correct) {
            assertThatExceptionOfType(VersionParsingException.class).isThrownBy(() -> GemVersion.parse(version));
        }
    }

    static Stream<Arguments> parsingProvider() {
        return Stream.of(
                arguments("", "", List.of("0"), false),
                arguments("   ", "   ", List.of("0"), false),
                arguments("0", "0", List.of("0"), false),
                arguments("1", "1", List.of("1"), false),
                arguments("1.0", "1.0", List.of("1"), false),
                arguments(" 1.0", " 1.0", List.of("1"), false),
                arguments("1.0 ", "1.0 ", List.of("1"), false),
                arguments(" 1.0 ", " 1.0 ", List.of("1"), false),
                arguments("1.0.0", "1.0.0", List.of("1"), false),
                arguments("1.0.0.a.1.0", "1.0.0.a.1.0", List.of("1", "a", "1"), true),
                arguments("1.2.3-1", "1.2.3-1", List.of("1", "2", "3", "pre", "1"), true)
        );
    }

    @ParameterizedTest
    @MethodSource("parsingProvider")
    public void testParsing(final String versionStr, final String rep, final List<String> components,
                            final boolean prerelease) throws VersionParsingException {
        final GemVersion version = GemVersion.parse(versionStr);
        assertThat(version).hasToString(rep).isInstanceOf(Version.class);
        assertThat(version.getOriginalVersion()).isEqualTo(rep);
        assertThat(version.getComponents()).isEqualTo(components);
        assertThat(version.isPreRelease()).isEqualTo(prerelease);
    }

    @Test
    public void testParsingBad() {
        assertThatExceptionOfType(VersionParsingException.class).isThrownBy(() -> GemVersion.parse("__asdf__"));
    }

    static Stream<Arguments> equalityProvider() {
        return Stream.of(
                arguments("", "", true),
                arguments("1", "1", true),
                arguments("1.2", "1.2", true),
                arguments("1.2", "1.2.0", false),
                arguments("1.2", "1.3", false),
                arguments("1.2.b1", "1.2.b.1", false)
        );
    }

    @ParameterizedTest
    @MethodSource("equalityProvider")
    public void testEquality(final String version1, final String version2, final boolean eq) {
        if (eq) {
            assertThat(version1).isEqualTo(version2);
            assertThat(version1).hasSameHashCodeAs(version2);
        } else {
            assertThat(version1).isNotEqualTo(version2);
            assertThat(version1).doesNotHaveSameHashCodeAs(version2);
        }
    }

    static Stream<Arguments> orderingProvider() {
        return Stream.of(
                arguments("", "", EQ),
                arguments("", "0", EQ),
                arguments("1.0", "1.0", EQ),
                arguments("1.0", "1.0.0", EQ),
                arguments("1.8.2", "0.0.0", GT),
                arguments("1.8.2", "1.8.2.a", GT),
                arguments("1.8.2.b", "1.8.2.a", GT),
                arguments("1.8.2.a", "1.8.2", LT),
                arguments("1.8.2.a10", "1.8.2.a9", GT),

                arguments("0.beta.1", "0.0.beta.1", EQ),
                arguments("0.0.beta", "0.0.beta.1", LT),
                arguments("0.0.beta", "0.beta.1", LT),

                arguments("5.a", "5.0.0.rc2", LT),
                arguments("5.x", "5.0.0.rc2", GT),

                arguments("1.9.3", "1.9.3", EQ),
                arguments("1.9.3", "1.9.2.99", GT),
                arguments("1.9.3", "1.9.3.1", LT),

                arguments("1.0.0-alpha", "1.0.0-alpha.1", LT),
                arguments("1.0.0-alpha.1", "1.0.0-beta.2", LT),
                arguments("1.0.0-beta.2", "1.0.0-beta.11", LT),
                arguments("1.0.0-beta.11", "1.0.0-rc.1", LT),
                arguments("1.0.0-rc1", "1.0.0", LT),
                arguments("1.0.0-1", "1", LT)
        );
    }

    @ParameterizedTest
    @MethodSource("orderingProvider")
    public void testOrdering(final String version1, final String version2, final Order order)
            throws VersionParsingException {
        final Version v1 = GemVersion.parse(version1);
        final Version v2 = GemVersion.parse(version2);

        assertThat(v1).isEqualByComparingTo(v1);
        assertThat(v2).isEqualByComparingTo(v2);

        switch (order) {
            case LT -> {
                assertThat(v1).isLessThan(v2);
                assertThat(v2).isGreaterThan(v1);
                assertThat(v2).isNotEqualTo(v1);
                assertThat(v1).isNotEqualTo(v2);
                assertThat(v1).doesNotHaveSameHashCodeAs(v2);
            }
            case EQ -> {
                assertThat(v1).isEqualByComparingTo(v2);
                assertThat(v2).isEqualByComparingTo(v1);
            }
            case GT -> {
                assertThat(v1).isGreaterThan(v2);
                assertThat(v2).isLessThan(v1);
                assertThat(v2).isNotEqualTo(v1);
                assertThat(v1).isNotEqualTo(v2);
                assertThat(v1).doesNotHaveSameHashCodeAs(v2);
            }
            default -> throw new IllegalStateException("Unexpected value: " + order);
        }
    }

    static Stream<Arguments> nextProvider() {
        return Stream.of(
                arguments("", "1"),
                arguments("0", "1"),
                arguments("1", "2"),
                arguments("1.0", "2"),
                arguments("1.1", "2"),
                arguments("1.3.0", "1.4"),
                arguments("1.0.4", "1.1"),
                arguments("1.2.3.4", "1.2.4")
        );
    }

    @ParameterizedTest
    @MethodSource("nextProvider")
    public void testToNextVersion(final String version, final String nextVersion) throws VersionParsingException {
        final GemVersion v1 = GemVersion.parse(version);
        final GemVersion v2 = GemVersion.parse(nextVersion);
        assertThat(v1.toNextVersion()).isEqualTo(v2);
        // Test cached
        assertThat(v1.toNextVersion()).isEqualTo(v2);
    }

    private static GemVersion.Component number(final long value) {
        return new GemVersion.Component(value);
    }

    private static GemVersion.Component string(final String value) {
        return new GemVersion.Component(value);
    }
}
