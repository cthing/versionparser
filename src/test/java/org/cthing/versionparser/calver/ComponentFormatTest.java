/*
 * Copyright 2023 C Thing Software
 * SPDX-License-Identifier: Apache-2.0
 */
package org.cthing.versionparser.calver;

import java.util.Optional;
import java.util.stream.Stream;

import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.params.provider.Arguments.arguments;


public class ComponentFormatTest {

    static Stream<Arguments> formatProvider() {
        return Stream.of(
                arguments("MAJOR", ComponentFormat.MAJOR),
                arguments("major", ComponentFormat.MAJOR),
                arguments(" major ", ComponentFormat.MAJOR),
                arguments("MINOR", ComponentFormat.MINOR),
                arguments("PATCH", ComponentFormat.PATCH),
                arguments("YYYY", ComponentFormat.YYYY),
                arguments("yyyy", ComponentFormat.YYYY),
                arguments("YY", ComponentFormat.YY),
                arguments("0Y", ComponentFormat.ZERO_Y),
                arguments("MM", ComponentFormat.MM),
                arguments("0M", ComponentFormat.ZERO_M),
                arguments("WW", ComponentFormat.WW),
                arguments("0W", ComponentFormat.ZERO_W),
                arguments("DD", ComponentFormat.DD),
                arguments("0D", ComponentFormat.ZERO_D),
                arguments("ABC", null),
                arguments("", null)
        );
    }

    @Test
    public void testProperties() {
        assertThat(ComponentFormat.YY.getCategory()).isEqualTo(ComponentCategory.YEAR);
        assertThat(ComponentFormat.YY.getFormat()).isEqualTo("YY");
        assertThat(ComponentFormat.YY.getRegex()).isEqualTo("([\\d]{1,3})");
    }

    @ParameterizedTest
    @MethodSource("formatProvider")
    public void testFrom(final String formatStr, @Nullable final ComponentFormat format) {
        final Optional<ComponentFormat> formatOpt = ComponentFormat.from(formatStr);
        if (format == null) {
            assertThat(formatOpt).isEmpty();
        } else {
            assertThat(formatOpt).contains(format);
        }
    }
}
