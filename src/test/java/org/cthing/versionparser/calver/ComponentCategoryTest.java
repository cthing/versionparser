/*
 * Copyright 2023 C Thing Software
 * SPDX-License-Identifier: Apache-2.0
 */
package org.cthing.versionparser.calver;

import org.cthing.versionparser.VersionParsingException;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.cthing.versionparser.calver.ComponentCategory.DAY;
import static org.cthing.versionparser.calver.ComponentCategory.MAJOR;
import static org.cthing.versionparser.calver.ComponentCategory.MINOR;
import static org.cthing.versionparser.calver.ComponentCategory.MONTH;
import static org.cthing.versionparser.calver.ComponentCategory.PATCH;
import static org.cthing.versionparser.calver.ComponentCategory.WEEK;
import static org.cthing.versionparser.calver.ComponentCategory.YEAR;


class ComponentCategoryTest {

    @Test
    void testValidate() {
        assertThatCode(() -> {
            YEAR.validate(1900);
            YEAR.validate(2023);
            MONTH.validate(1);
            MONTH.validate(6);
            MONTH.validate(12);
            DAY.validate(1);
            DAY.validate(15);
            DAY.validate(31);
            WEEK.validate(1);
            WEEK.validate(20);
            WEEK.validate(52);
            MAJOR.validate(0);
            MAJOR.validate(16);
            MINOR.validate(0);
            MINOR.validate(16);
            PATCH.validate(0);
            PATCH.validate(16);
        }).doesNotThrowAnyException();

        assertThatExceptionOfType(VersionParsingException.class).isThrownBy(() -> YEAR.validate(1000))
                                                                .withMessage("Invalid year '1000' (year >= 1900)");
        assertThatExceptionOfType(VersionParsingException.class).isThrownBy(() -> MONTH.validate(0))
                                                                .withMessage("Invalid month '0' (1 <= month <= 12)");
        assertThatExceptionOfType(VersionParsingException.class).isThrownBy(() -> MONTH.validate(13))
                                                                .withMessage("Invalid month '13' (1 <= month <= 12)");
        assertThatExceptionOfType(VersionParsingException.class).isThrownBy(() -> DAY.validate(0))
                                                                .withMessage("Invalid day '0' (1 <= day <= 31)");
        assertThatExceptionOfType(VersionParsingException.class).isThrownBy(() -> DAY.validate(32))
                                                                .withMessage("Invalid day '32' (1 <= day <= 31)");
        assertThatExceptionOfType(VersionParsingException.class).isThrownBy(() -> WEEK.validate(0))
                                                                .withMessage("Invalid week '0' (1 <= week <= 52)");
        assertThatExceptionOfType(VersionParsingException.class).isThrownBy(() -> WEEK.validate(53))
                                                                .withMessage("Invalid week '53' (1 <= week <= 52)");
        assertThatExceptionOfType(VersionParsingException.class).isThrownBy(() -> MAJOR.validate(-1))
                                                                .withMessage("Invalid major version '-1' (major >= 0)");
        assertThatExceptionOfType(VersionParsingException.class).isThrownBy(() -> MINOR.validate(-1))
                                                                .withMessage("Invalid minor version '-1' (minor >= 0)");
        assertThatExceptionOfType(VersionParsingException.class).isThrownBy(() -> PATCH.validate(-1))
                                                                .withMessage("Invalid patch version '-1' (patch >= 0)");
    }
}
