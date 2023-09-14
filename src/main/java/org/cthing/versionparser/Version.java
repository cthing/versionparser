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

/**
 * Represents an artifact version. The primary characteristic of a version is that it has a semantically meaningful
 * sort order (e.g. version 1.2 is less than version 1.10).
 */
public interface Version extends Comparable<Version> {

    /**
     * Obtains the version as it was originally specified.
     *
     * @return Originally specified version.
     */
    String getOriginalVersion();

    /**
     * Indicates whether this version represents an artifact that is in the pre-release state (e.g. alpha, beta, rc).
     * The exact criteria that determine whether an artifact is in the pre-release state is specific to a version
     * scheme (e.g. Maven). Please refer to the Javadoc for the specific version classes.
     *
     * @return {@code true} if this version represents an artifact in the pre-release state.
     */
    boolean isPreRelease();
}
