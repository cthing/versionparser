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
