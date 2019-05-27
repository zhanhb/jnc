/*
 * Copyright 2019 zhanhb.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package jnc.foreign.internal;

import java.lang.annotation.Annotation;
import javax.annotation.Nullable;

/**
 * @author zhanhb
 */
interface AnnotationContext {

    @Nullable
    <T extends Annotation> T getAnnotation(Class<T> klass);

    /**
     * Don't use equals, hashCode or toString, for it's same as Object does.
     */
    default <T extends Annotation> T getAnnotationOrDefault(Class<T> klass, T defaultValue) {
        T annotation = getAnnotation(klass);
        return annotation != null ? annotation : defaultValue;
    }

}
