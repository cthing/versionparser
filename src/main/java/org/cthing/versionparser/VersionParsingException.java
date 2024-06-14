/*
 * Copyright 2023 C Thing Software
 * SPDX-License-Identifier: Apache-2.0
 */

package org.cthing.versionparser;

import java.io.Serial;

import javax.annotation.Nullable;


/**
 * Indicates an error was encountered while parsing or working with a version or version constraint.
 */
public class VersionParsingException extends Exception {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * Constructs an exception with the specified message.
     *
     * @param message Details of the exception for later retrieval by the {@link Throwable#getMessage()} method
     */
    public VersionParsingException(final String message) {
        super(message);
    }

    /**
     * Constructs an exception with the specified message.
     *
     * @param message Details of the exception for later retrieval by the {@link Throwable#getMessage()} method
     * @param throwable Underlying cause of the exception for later retrieval by the {@link Throwable#getCause()}
     *      method. A {@code null} value indicates that the cause is nonexistent or unknown.
     */
    VersionParsingException(final String message, @Nullable final Throwable throwable) {
        super(message, throwable);
    }
}
