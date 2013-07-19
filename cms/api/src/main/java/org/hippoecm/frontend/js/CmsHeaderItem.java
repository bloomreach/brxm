/*
 * Copyright 2012-2013 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.frontend.js;

import java.util.Arrays;

import org.apache.wicket.markup.head.HeaderItem;
import org.apache.wicket.markup.head.JavaScriptReferenceHeaderItem;
import org.apache.wicket.request.Response;
import org.apache.wicket.request.resource.JavaScriptResourceReference;
import org.apache.wicket.request.resource.ResourceReference;

public class CmsHeaderItem extends HeaderItem {

    private static final ResourceReference FUTURE_REFERENCE = new JavaScriptResourceReference(CmsHeaderItem.class, "future.js");
    private static final ResourceReference GLOBAL_REFERENCE = new JavaScriptResourceReference(CmsHeaderItem.class, "global.js");

    private static final CmsHeaderItem INSTANCE = new CmsHeaderItem();

    public static CmsHeaderItem get() {
        return INSTANCE;
    }

    private CmsHeaderItem() {}

    @Override
    public Iterable<?> getRenderTokens() {
        return Arrays.asList("hippo-cms-header-item");
    }

    @Override
    public void render(final Response response) {
        JavaScriptReferenceHeaderItem.forReference(FUTURE_REFERENCE).render(response);
        JavaScriptReferenceHeaderItem.forReference(GLOBAL_REFERENCE).render(response);
    }
}
