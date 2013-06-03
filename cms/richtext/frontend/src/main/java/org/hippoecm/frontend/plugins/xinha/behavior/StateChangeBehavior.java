/*
 *  Copyright 2013 Hippo B.V. (http://www.onehippo.com)
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

package org.hippoecm.frontend.plugins.xinha.behavior;

import org.apache.wicket.Request;
import org.apache.wicket.RequestCycle;
import org.apache.wicket.ajax.AbstractDefaultAjaxBehavior;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.hippoecm.frontend.plugins.xinha.AbstractXinhaPlugin;

/**
 * Simple behavior that allows the Xinha client to inform the plugin on the server about simple state changes
 * that might require a server side action, whether it be directly after the state has changed or simply ensure
 * that a reload of the Xinha instance will render with the expected state. It uses the
 * {@link AbstractXinhaPlugin.Configuration} to store the new state and allows custom behavior by implementing the
 * {@link StateChangeBehavior#onStateChanged} method.
 */
public abstract class StateChangeBehavior extends AbstractDefaultAjaxBehavior {

    public static final String FULL_SCREEN = "fullScreen";
    public static final String ACTIVATED = "activated";

    private AbstractXinhaPlugin.Configuration configuration;

    public StateChangeBehavior(AbstractXinhaPlugin.Configuration configuration) {
        this.configuration = configuration;
    }

    @Override
    protected void respond(final AjaxRequestTarget target) {
        Request request = RequestCycle.get().getRequest();

        if (request.getParameter(FULL_SCREEN) != null) {
            boolean fullScreen = Boolean.parseBoolean(request.getParameter(FULL_SCREEN));
            if (configuration.isRenderFullscreen() != fullScreen) {
                configuration.setRenderFullscreen(fullScreen);

                onStateChanged(FULL_SCREEN, fullScreen, target);
            }
        }

        if (request.getParameter(ACTIVATED) != null) {
            boolean activated = Boolean.parseBoolean(request.getParameter(ACTIVATED));
            if (configuration.getEditorStarted() != activated) {
                configuration.setEditorStarted(activated);
                configuration.setFocusAfterLoad(activated);

                onStateChanged(ACTIVATED, activated, target);
            }
        }
    }

    protected abstract void onStateChanged(final String param, final boolean value, final AjaxRequestTarget target);

}
