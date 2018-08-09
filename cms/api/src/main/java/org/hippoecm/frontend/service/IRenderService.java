/*
 *  Copyright 2008-2018 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.frontend.service;

import org.apache.wicket.Component;
import org.apache.wicket.util.io.IClusterable;
import org.hippoecm.frontend.PluginRequestTarget;

/**
 * The service interface that is used to create {@link Component} hierarchies.
 */
public interface IRenderService extends IClusterable {

    /**
     * The Wicket {@link Component} that is added to the parent.  The component
     * must have the id that was set with the {@link #bind(IRenderService, String)} method.
     */
    Component getComponent();

    /**
     * Called after user events and JCR events have been handled, but before
     * the rendering has started.  Plugins can register {@link Component}s with
     * the request target to enlist in the rendering phase.
     * <p>
     * Implementations that use extensions must call the same method on those.
     */
    void render(PluginRequestTarget target);

    /**
     * Set focus on the specified child.  Implementations should make the child
     * visible when they themselves are visible, or become visible later.
     * 
     * @param child The extension that requests focus.  This parameter can be
     *              null, in which case the Component should set focus to itself.
     */
    void focus(IRenderService child);

    /**
     * Bind the component to the specified id.  Provides the render service with
     * a reference to the parent render service.
     */
    void bind(IRenderService parent, String id);

    /**
     * Releases the component.  Implementations cannot use the parent reference
     * after this method has been invoked.
     */
    void unbind();

    /**
     * The parent service for this render service.
     */
    @Deprecated
    IRenderService getParentService();

}
