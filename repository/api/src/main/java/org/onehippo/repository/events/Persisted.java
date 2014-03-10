/*
 * Copyright 2012-2013 Hippo B.V. (http://www.onehippo.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.onehippo.repository.events;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation for single-argument (void) methods that indicates that the method is a listener
 * to persisted events.
 * <p>
 * Methods with the annotation will receive all persisted events.
 * @deprecated No longer used. Use the {@link PersistedHippoEventListener} interface instead
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
@Deprecated
public @interface Persisted {

    String name();
}
