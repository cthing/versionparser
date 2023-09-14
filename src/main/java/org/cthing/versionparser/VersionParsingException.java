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
