/*
 * Copyright 2019 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.hst.pagecomposer.jaxrs.api;

import org.hippoecm.hst.configuration.hosting.Mount;
import org.hippoecm.hst.core.request.HstRequestContext;

abstract class AbstractPageContext implements PageActionContext {

    private transient HstRequestContext requestContext;
    private transient Mount editingMount;

    AbstractPageContext(final HstRequestContext requestContext,
                               final Mount editingMount) {
        this.requestContext = requestContext;
        this.editingMount = editingMount;
    }

    /**
     * @return the {@link HstRequestContext} that originated this {@link AbstractPageContext}. It will never be {@code null}
     */
    public HstRequestContext getRequestContext() {
        return requestContext;
    }

    /**
     * @return the {@link Mount} that belongs to the channel from which the action originated.
     * This method never returns {@code null}.
     */
    public Mount getEditingMount() {
        return editingMount;
    }

    @Override
    public String toString() {
        return "AbstractPageContext{" +
                "requestContext=" + requestContext +
                ", editingMount=" + editingMount +
                '}';
    }
}
