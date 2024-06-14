/*
 * Copyright 2023 C Thing Software
 * SPDX-License-Identifier: Apache-2.0
 */

package org.cthing.versionparser;

/**
 * Base class for version implementations.
 */
public abstract class AbstractVersion implements Version {

    protected final String originalVersion;

    /**
     * Base constructor.
     *
     * @param originalVersion Originally specified version
     */
    protected AbstractVersion(final String originalVersion) {
        this.originalVersion = originalVersion;
    }

    @Override
    public String getOriginalVersion() {
        return this.originalVersion;
    }

    @Override
    public String toString() {
        return this.originalVersion;
    }
}
