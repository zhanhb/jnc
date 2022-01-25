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
package jnc.provider;

import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import javax.annotation.Nullable;

/**
 * @author zhanhb
 */
@SuppressWarnings({"FinalClass", "PackageVisibleInnerClass"})
interface AnnotationContext {

    static AnnotationContext newContext(AnnotatedElement element) {
        return new Simple(element.getAnnotations());
    }

    static AnnotationContext[] newMethodParameterContexts(Method method) {
        Annotation[][] parameterAnnotations = method.getParameterAnnotations();
        int length = parameterAnnotations.length;
        AnnotationContext[] annotationContexts = new AnnotationContext[length];
        for (int i = 0; i < length; ++i) {
            annotationContexts[i] = new Simple(parameterAnnotations[i]);
        }
        return annotationContexts;
    }

    static AnnotationContext newMockContext(List<Class<? extends Annotation>> annotations, AnnotationContext previous) {
        return new Combine(new Simple(annotations.stream()
                .map(klass -> SimpleAnnotationBuilder.of(klass).build())
                .toArray(Annotation[]::new)), previous);
    }

    default boolean isAnnotationPresent(Class<? extends Annotation> annotationClass) {
        return getAnnotation(annotationClass) != null;
    }

    @Nullable
    <T extends Annotation> T getAnnotation(Class<T> annotationClass);

    final class Simple implements AnnotationContext {

        private final Map<Class<?>, Annotation> annotations;

        Simple(Annotation[] annotations) {
            @SuppressWarnings(value = "CollectionWithoutInitialCapacity")
            Map<Class<?>, Annotation> map = new LinkedHashMap<>();
            for (Annotation annotation : annotations) {
                map.putIfAbsent(annotation.annotationType(), annotation);
            }
            for (Annotation annotation : annotations) {
                for (Annotation transfer : annotation.annotationType().getAnnotations()) {
                    map.putIfAbsent(transfer.annotationType(), transfer);
                }
            }
            this.annotations = map;
        }

        @Nullable
        @SuppressWarnings("unchecked")
        @Override
        public <T extends Annotation> T getAnnotation(Class<T> annotationClass) {
            return (T) annotations.get(annotationClass);
        }

    }

    final class Combine implements AnnotationContext {

        private final AnnotationContext[] upstream;

        Combine(AnnotationContext... upstream) {
            Arrays.stream(upstream).forEach(Objects::requireNonNull);
            this.upstream = upstream;
        }

        @Override
        public <T extends Annotation> T getAnnotation(Class<T> annotationClass) {
            for (AnnotationContext annotationContext : upstream) {
                T annotation = annotationContext.getAnnotation(annotationClass);
                if (annotation != null) {
                    return annotation;
                }
            }
            return null;
        }

    }

}
