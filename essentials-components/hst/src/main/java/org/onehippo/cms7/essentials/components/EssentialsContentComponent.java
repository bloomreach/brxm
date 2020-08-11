/*
 * Copyright 2014-2019 Hippo B.V. (http://www.onehippo.com)
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

package org.onehippo.cms7.essentials.components;

import org.hippoecm.hst.core.component.HstRequest;
import org.hippoecm.hst.core.component.HstResponse;

/**
 * Hippo HST component for simple (document detail) request handling, it just fetches sitemap mapped bean.
 *
 *   ### NOTE: this component class should not be referenced from an hst:catalog component,
 *             because such a component does not work (i.e. redirect to 404 page) if the
 *             component is dropped onto / rendered on a page with no content associated
 *             (through the matching sitemap item).
 *
 * Use {@code EssentialsDocumentComponent} for picked documents (e.g. banner and alike)
 */

public class EssentialsContentComponent extends CommonComponent {

    @Override
    public void doBeforeRender(final HstRequest request, final HstResponse response) {
        super.doBeforeRender(request, response);
        setContentBeanWith404(request, response);
    }
}
