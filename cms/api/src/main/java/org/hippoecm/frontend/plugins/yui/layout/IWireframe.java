/*
 *  Copyright 2008-2013 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.frontend.plugins.yui.layout;

import java.io.Serializable;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.head.HeaderItem;

/**
 * Base wireframe interface, allows wireframes that are linked to their parent to register
 * with their parent through the Wicket component tree model.
 */
public interface IWireframe extends Serializable {

    /**
     * Return the root id of this wireframe
     * 
     * @return the root {@link YuiId} of this wireframe 
     */
    YuiId getYuiId();

    /**
     * Resize the wireframe, e.g. when it has become visible.
     * @param target
     */
    void resize(AjaxRequestTarget target);

    HeaderItem getHeaderItem();

    boolean isRendered();

    void render(AjaxRequestTarget target);
}
