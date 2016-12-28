/*
 * Copyright 2016 C Thing Software
 * All rights reserved.
 */
package org.cthing.versionparser;

import java.util.List;

import org.junit.Test;

import static org.assertj.core.api.Assertions.*;

public class VersionTests {
    @Test
    public void testParsing() {
        verifyParsing("1", "", true, true, 0, 1, 1);
        verifyParsing("1.20", "", true, true, 0, 2, 1, 20);
        verifyParsing("1.20.32", "", true, true, 0, 3, 1, 20, 32);
        verifyParsing("1.20.32.6", "", true, true, 0, 4, 1, 20, 32, 6);
        verifyParsing("1.20.32.7.201606060606", "", true, true, 0, 5, 1, 20, 32, 7, 201606060606L);
        verifyParsing("1.20.32.8.10.21", "", true, true, 0, 6, 1, 20, 32, 8, 10, 21);
        verifyParsing("20030203.000129", "", true, true, 0, 2, 20030203, 129);
        verifyParsing("1.2.+", "", true, true, 0, 3, 1, 2, Long.MAX_VALUE);

        verifyParsing("1.2.3Beta", "Beta", false, true, -500, 3, 1, 2, 3);
        verifyParsing("1.2.3Beta2", "Beta2", false, true, -498, 3, 1, 2, 3);
        verifyParsing("1.2.3.RELEASE", "RELEASE", true, true, 0, 3, 1, 2, 3);
        verifyParsing("1.2.3-Final", "Final", true, true, 0, 3, 1, 2, 3);
        verifyParsing("1.2.3-alpha", "alpha", false, true, -2000, 3, 1, 2, 3);
        verifyParsing("1.2.3-alpha-1", "alpha-1", false, true, -1999, 3, 1, 2, 3);
        verifyParsing("1.2-rc", "rc", false, true, -100, 2, 1, 2);
        verifyParsing("1.2-rc1", "rc1", false, true, -99, 2, 1, 2);
        verifyParsing("1.2-M2", "M2", false, true, -998, 2, 1, 2);
        verifyParsing("0.98f", "f", true, true, 102, 2, 0, 98);
        verifyParsing("1.2-M", "M", true, true, 109, 2, 1, 2);
        verifyParsing("beta", "beta", false, true, -500, 0);

        verifyParsing("1.2.3Foo", "Foo", true, false, 0, 3, 1, 2, 3);
        verifyParsing("1.2.3-4.5.6-Foo", "Foo", true, false, 0, 6, 1, 2, 3, 4, 5, 6);
    }

    @Test
    public void testCompare() {
        verifyCompare("1", "1", 0);
        verifyCompare("1.2", "1.2", 0);
        verifyCompare("1.2.3", "1.2.3", 0);
        verifyCompare("  1.2.3", "1.2.3  ", 0);
        verifyCompare("1.2.3.4", "1.2.3.4", 0);
        verifyCompare("1.2.3.4-beta1", "1.2.3.4-beta1", 0);
        verifyCompare("1.2.+", "1.2.3", 0);
        verifyCompare("1.2.+", "1.2.+", 0);

        verifyCompare("1", "1.0", 0);
        verifyCompare("1.2.3-Beta1", "1.2.3BETA1", 0);
        verifyCompare("1.2.3", "1.2.3.RELEASE", 0);

        verifyCompare("1", "2", -1);
        verifyCompare("2", "1", 1);
        verifyCompare("1.1", "1.2", -1);
        verifyCompare("1.2", "1.1", 1);
        verifyCompare("1", "1.2", -1);
        verifyCompare("1.2", "1", 1);

        verifyCompare("1.1", "1.1-alpha1", 1);
        verifyCompare("1.1", "1.1-beta1", 1);
        verifyCompare("1.1", "1.1-rc1", 1);
        verifyCompare("1.1", "1.1-m1", 1);
        verifyCompare("1.1", "1.1-SNAPSHOT", 1);
        verifyCompare("1.1-alpha2", "1.1-alpha1", 1);
        verifyCompare("1.1-beta1", "1.1-alpha1", 1);
        verifyCompare("1.1-rc1", "1.1-beta1", 1);
        verifyCompare("1.1-rc1", "1.1-m1", 1);
        verifyCompare("0.98g", "0.98b", 1);

        verifyCompare("1.1-FOO", "1.1-BOO", 1);
    }

    @Test
    public void testEquality() {
        verifyEquality("", "", true);
        verifyEquality("1", "1", true);
        verifyEquality("1", "1.0.0", true);
        verifyEquality("1-Beta1", "1.0.0.beta-1", true);
        verifyEquality("1.2.3", "1.2.3", true);
        verifyEquality("1.2.3", "1.2-3", true);

        verifyEquality("1", "2", false);
        verifyEquality("1.2.3-beta1", "1.2.3-beta2", false);
        verifyEquality("1.2.3", "1.2.3-beta2", false);
        verifyEquality("1.2.3", "2.45.6", false);
    }

    private void verifyParsing(final String version, final String trailing, final boolean released,
                               final boolean trailingRecognized, final int trailingValue, final int numComponents,
                               final long... expectedComponents) {
        final Version parsedVersion = new Version(version);
        final List<Long> components = parsedVersion.getComponents();
        assertThat(components).hasSize(numComponents);
        for (int idx = 0; idx < numComponents; idx++) {
            assertThat(components.get(idx)).isEqualTo(expectedComponents[idx]);
        }
        assertThat(parsedVersion.getTrailing()).isEqualTo(trailing);
        assertThat(parsedVersion.isTrailingRecognized()).isEqualTo(trailingRecognized);
        assertThat(parsedVersion.getTrailingValue()).isEqualTo(trailingValue);
        assertThat(parsedVersion.isReleased()).isEqualTo(released);
    }

    private void verifyCompare(final String version1, final String version2, final int result) {
        final Version v1 = new Version(version1);
        final Version v2 = new Version(version2);
        assertThat(v1.compareTo(v2)).isEqualTo(result);
    }

    private void verifyEquality(final String version1, final String version2, final boolean isEqual) {
        final Version v1 = new Version(version1);
        final Version v2 = new Version(version2);
        assertThat(v1.equals(v2)).isEqualTo(isEqual);
        assertThat(v1.hashCode() == v2.hashCode()).isEqualTo(isEqual);
    }
}
