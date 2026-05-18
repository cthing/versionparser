/*
 * Copyright 2026 C Thing Software
 * SPDX-License-Identifier: Apache-2.0
 */
package org.cthing.versionparser.pypa;

import org.cthing.versionparser.VersionParsingException;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;


class PypaVersionSchemeTest {

    @Test
    void testParseVersion() throws VersionParsingException {
        assertThat(PypaVersionScheme.parseVersion("1!1.0b2.post345.dev456")).hasToString("1!1.0b2.post345.dev456");
    }

    @Test
    void testParseSpecifier() throws VersionParsingException {
        assertThat(PypaVersionScheme.parseSpecifier(">1.0,<2.0")).hasToString(">1.0,<2.0");
    }
}
