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
package org.cthing.versionparser.annotations;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import javax.annotation.Nonnull;
import javax.annotation.meta.TypeQualifierDefault;


/**
 * Declares that fields, method return types, method parameters, and type parameters within the annotated package
 * are not {@code null} by default. Items that can be null should be annotated with either
 * {@link javax.annotation.CheckForNull} or {@link javax.annotation.Nullable}. This annotation differs from
 * {@link javax.annotation.ParametersAreNonnullByDefault} which only marks method parameters as not {@code null}
 * by default.
 */
@Documented
@Nonnull
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.PACKAGE)
@TypeQualifierDefault({
        ElementType.PARAMETER,
        ElementType.TYPE_PARAMETER,
        ElementType.FIELD,
        ElementType.METHOD,
        ElementType.RECORD_COMPONENT
})
public @interface PackageNonnullByDefault {
}
