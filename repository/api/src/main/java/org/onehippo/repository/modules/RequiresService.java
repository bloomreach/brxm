/*
 *  Copyright 2013-2015 Hippo B.V. (http://www.onehippo.com)
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package org.onehippo.repository.modules;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Use this annotation on implementations of {@link DaemonModule}s to
 * inform the system about which services it uses from the
 * {@link org.onehippo.cms7.services.HippoServiceRegistry}
 * and which are provided by other {@link DaemonModule}s.
 * Together with the {@link ProvidesService} annotation, this annotation
 * determines the order in which modules are started.
 * <p>
 *   The optional optional attribute informs the system not to fail loading the module in the absence of the service.
 *   I.e. if there is no module that provides the service.
 *   If you specify the optional attribute it needs to be an array of booleans that is equal in length to the array
 *   of types that is specified in the types attribute.
 * </p>
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface RequiresService {
    Class<?>[] types();
    boolean[] optional() default {};
}
