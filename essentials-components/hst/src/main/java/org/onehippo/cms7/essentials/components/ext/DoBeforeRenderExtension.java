/*
 * Copyright 2015 Hippo B.V. (http://www.onehippo.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.onehippo.cms7.essentials.components.ext;

import org.hippoecm.hst.core.component.HstRequest;
import org.hippoecm.hst.core.component.HstResponse;
import org.onehippo.cms7.essentials.components.CommonComponent;

/**
 * Implementation of DoBeforeRenderExtension is executed on every doBeforeRender call (for each configured  component)
 * NOTE: <strong>Extensions</strong> implementing this interface shouldn't contain any state (<strong>must be thread
 * threadsafe)</strong>
 * <p>
 * Extension is configured as Spring managed component in
 * <strong>/site/src/main/resources/META-INF/hst-assembly/overrides/hippo-essentials-spring.xml</strong>
 */
public interface DoBeforeRenderExtension {
    /**
     * Executed after {@code org.onehippo.cms7.essentials.components.CommonComponent#doBeforeRender(request, response)}
     *
     * @param component component instance
     * @param request   HstRequest instance
     * @param response  HstResponse instance
     */
    void execute(final CommonComponent component, HstRequest request, HstResponse response);
}