/*
 *  Copyright 2019-2023 Bloomreach
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

import javax.jcr.Node;

import org.hippoecm.hst.core.request.HstRequestContext;

/**
 * Page create context data used in {@link PageCreateEvent}
 */
public interface PageCreateContext extends PageActionContext {

    /**
     * @return the in {@link HstRequestContext#getSession() session} created (but not yet persisted) site map item JCR {@link Node}
     * as a result of this create page action
     */
    public Node getNewSiteMapItemNode();

    /**
     * @return the in {@link HstRequestContext#getSession() session} created (but not yet persisted) page JCR {@link Node}
     * as a result of this create page action
     */
    public Node getNewPageNode();
}
