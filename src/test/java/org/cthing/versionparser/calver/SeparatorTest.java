/*
 * Copyright 2023 C Thing Software
 * SPDX-License-Identifier: Apache-2.0
 */
package org.cthing.versionparser.calver;

import java.util.Optional;
import java.util.stream.Stream;

import javax.annotation.Nullable;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.params.provider.Arguments.arguments;


public class SeparatorTest {

    static Stream<Arguments> separatorProvider() {
        return Stream.of(
                arguments("-", Separator.DASH),
                arguments(" - ", Separator.DASH),
                arguments(".", Separator.PERIOD),
                arguments("_", Separator.UNDERSCORE),
                arguments("|", null),
                arguments("", null)
        );
    }

    @Test
    public void testProperties() {
        assertThat(Separator.DASH.getDelimiter()).isEqualTo("-");
        assertThat(Separator.DASH.getRegex()).isEqualTo("\\-");
    }

    @ParameterizedTest
    @MethodSource("separatorProvider")
    public void testFrom(final String delimiter, @Nullable final Separator separator) {
        final Optional<Separator> separatorOpt = Separator.from(delimiter);
        if (separator == null) {
            assertThat(separatorOpt).isEmpty();
        } else {
            assertThat(separatorOpt).contains(separator);
        }
    }
}
