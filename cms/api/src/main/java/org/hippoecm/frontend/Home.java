/*
 *  Copyright 2008 Hippo.
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
package org.hippoecm.frontend;

import org.apache.wicket.Component;
import org.apache.wicket.markup.html.WebPage;
import org.hippoecm.frontend.behaviors.IContextMenu;
import org.hippoecm.frontend.behaviors.IContextMenuManager;
import org.hippoecm.frontend.service.IRenderService;

public abstract class Home extends WebPage implements IRenderService, IContextMenuManager {

    private static final long serialVersionUID = 1L;

    public Component getComponent() {
        return this;
    }

    /**
     * Refresh the JCR session, i.e. invalidate (cached) subtrees for which an event has been received.
     */
    public abstract void refresh();

    /**
     * Notify refreshables and listeners in the page for which events have been received.
     */
    public abstract void processEvents();

    public abstract void render(PluginRequestTarget target);

    public void focus(IRenderService child) {
    }

    public void bind(IRenderService parent, String wicketId) {
    }

    public void unbind() {
    }

    public IRenderService getParentService() {
        return null;
    }

    public String getServiceId() {
        return null;
    }

    public abstract void showContextMenu(IContextMenu active);

}
