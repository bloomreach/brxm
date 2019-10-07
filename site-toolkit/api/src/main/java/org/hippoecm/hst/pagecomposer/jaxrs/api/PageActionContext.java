/*
 *  Copyright 2019 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.hst.pagecomposer.jaxrs.api;

import org.hippoecm.hst.configuration.hosting.Mount;
import org.hippoecm.hst.core.request.HstRequestContext;

/**
 * Super interface for all page actions that require a context. Used in {@link PageEvent}.
 */
public interface PageActionContext {

    /**
     * @return the {@link HstRequestContext} that originated this {@link PageActionContext}. It will never be {@code
     * null}
     */
    public HstRequestContext getRequestContext();

    /**
     * @return the {@link Mount} that belongs to the channel from which the copy action originated. This method never
     * returns {@code null}.
     */
    public Mount getEditingMount();

}
