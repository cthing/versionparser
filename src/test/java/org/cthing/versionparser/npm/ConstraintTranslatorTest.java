/*
 * Copyright 2023 C Thing Software
 * SPDX-License-Identifier: Apache-2.0
 */

package org.cthing.versionparser.npm;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.cthing.versionparser.npm.ConstraintTranslator.translate;


public class ConstraintTranslatorTest {

    @Test
    public void testNoChange() {
        assertThat(translate("<1.0.0")).isEqualTo("<1.0.0");
        assertThat(translate(">=1.0.0 <2.0.0")).isEqualTo(">=1.0.0 <2.0.0");
    }

    @Test
    public void testGreaterThanOrEqualZero() {
        assertThat(translate("latest")).isEqualTo(">=0.0.0");
        assertThat(translate("latest.integration")).isEqualTo(">=0.0.0");
        assertThat(translate("*")).isEqualTo(">=0.0.0");
    }

    @Test
    public void testHyphen() {
        assertThat(translate("1.2.0 - 1.5.0")).isEqualTo(">=1.2.0 <=1.5.0");
        assertThat(translate("1.0.0 - 3.0.0")).isEqualTo(">=1.0.0 <=3.0.0");
        assertThat(translate("1.0.0 - 3.0")).isEqualTo(">=1.0.0 <3.1.0-0");
        assertThat(translate("1.0.0 - 3")).isEqualTo(">=1.0.0 <4.0.0-0");
    }

    @Test
    public void testCaret() {
        assertThat(translate("^1.2.3")).isEqualTo(">=1.2.3 <2.0.0-0");
        assertThat(translate("^0.2.3")).isEqualTo(">=0.2.3 <0.3.0-0");
        assertThat(translate("^0.0.3")).isEqualTo(">=0.0.3 <0.0.4-0");
        assertThat(translate("^1.0.x")).isEqualTo(">=1.0.0 <2.0.0-0");
        assertThat(translate("^1.0")).isEqualTo(">=1.0.0 <2.0.0-0");
        assertThat(translate("^1.x")).isEqualTo(">=1.0.0 <2.0.0-0");
        assertThat(translate("^1")).isEqualTo(">=1.0.0 <2.0.0-0");
        assertThat(translate("^0.0.3-abc")).isEqualTo(">=0.0.3-abc <0.0.4-0");
        assertThat(translate("^1.0.0-abc")).isEqualTo(">=1.0.0-abc <2.0.0-0");
        assertThat(translate("^0.3.0-abc")).isEqualTo(">=0.3.0-abc <0.4.0-0");
        assertThat(translate("^0.1.x")).isEqualTo(">=0.1.0 <0.2.0-0");
    }

    @Test
    public void testTilde() {
        assertThat(translate("~1.2.3")).isEqualTo(">=1.2.3 <1.3.0-0");
        assertThat(translate("~1.2.3-abc")).isEqualTo(">=1.2.3-abc <1.3.0-0");
        assertThat(translate("~0.2.3")).isEqualTo(">=0.2.3 <0.3.0-0");
        assertThat(translate("~1.2")).isEqualTo(">=1.2.0 <1.3.0-0");
        assertThat(translate("~1.2")).isEqualTo(">=1.2.0 <1.3.0-0");
        assertThat(translate("~1")).isEqualTo(">=1.0.0 <2.0.0-0");
    }

    @Test
    public void testXRange() {
        assertThat(translate("1.2.x")).isEqualTo(">=1.2.0 <1.3.0-0");
        assertThat(translate("1.2.X")).isEqualTo(">=1.2.0 <1.3.0-0");
        assertThat(translate("1.x")).isEqualTo(">=1.0.0 <2.0.0-0");
        assertThat(translate("1.x")).isEqualTo(">=1.0.0 <2.0.0-0");
        assertThat(translate("1.2.x-abc")).isEqualTo(">=1.2.0 <1.3.0-0");
        assertThat(translate("=1.2.x")).isEqualTo(">=1.2.0 <1.3.0-0");
        assertThat(translate(">1.0.0 <1.2.x")).isEqualTo(">1.0.0 <1.2.0");
        assertThat(translate(">1.0.0 <2.x")).isEqualTo(">1.0.0 <2.0.0");
        assertThat(translate(">1.0.0 <2.0.x")).isEqualTo(">1.0.0 <2.0.0");
        assertThat(translate(">1.0.0 <=2.x")).isEqualTo(">1.0.0 <3.0.0");
        assertThat(translate(">1.0.0 <=2.0.x")).isEqualTo(">1.0.0 <2.1.0");
        assertThat(translate("<4.0.0 >2.0.x")).isEqualTo("<4.0.0 >=2.1.0");
        assertThat(translate("<4.0.0 >=2.0.x")).isEqualTo("<4.0.0 >=2.0.0");
        assertThat(translate("<4.0.0 >=2.x")).isEqualTo("<4.0.0 >=2.0.0");
        assertThat(translate("<4.0.0 >2.x")).isEqualTo("<4.0.0 >=3.0.0");
        assertThat(translate(">1.0.0 <=2.x")).isEqualTo(">1.0.0 <3.0.0");
    }
}
