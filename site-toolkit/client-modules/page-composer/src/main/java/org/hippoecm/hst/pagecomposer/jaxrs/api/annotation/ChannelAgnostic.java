/**
 * Copyright 2019 Hippo B.V. (http://www.onehippo.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.hippoecm.hst.pagecomposer.jaxrs.api.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * If added to a method, then the method is allowed regardless the specific channel being present on
 * {@link org.hippoecm.hst.pagecomposer.jaxrs.services.PageComposerContextService} or no channel at all being present
 * on the {@link org.hippoecm.hst.pagecomposer.jaxrs.services.PageComposerContextService}
 */
@Documented
@Retention(RUNTIME)
@Target({METHOD})
public @interface ChannelAgnostic {
}