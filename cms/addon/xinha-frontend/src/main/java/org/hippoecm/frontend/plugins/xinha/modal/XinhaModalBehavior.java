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
package org.hippoecm.frontend.plugins.xinha.modal;

import java.util.HashMap;
import java.util.Map;

import org.apache.wicket.Request;
import org.apache.wicket.RequestCycle;
import org.apache.wicket.ajax.AbstractDefaultAjaxBehavior;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.hippoecm.frontend.plugins.xinha.XinhaPlugin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class XinhaModalBehavior extends AbstractDefaultAjaxBehavior {
    @SuppressWarnings("unused")
    private final static String SVN_ID = "$Id$";

    private final static Logger log = LoggerFactory.getLogger(XinhaModalBehavior.class);

    private static final long serialVersionUID = 1L;

    @SuppressWarnings("unchecked")
    @Override
    protected void respond(AjaxRequestTarget target) {
        Request request = RequestCycle.get().getRequest();
        String pluginName = request.getParameter("pluginName");

        Map<String, String> params = new HashMap<String, String>();
        Map<String, String> requestParams = request.getRequestParameters().getParameters();
        for (String key : requestParams.keySet()) {
            if (key.startsWith("ModalDialogParam-")) {
                params.put(key.substring("ModalDialogParam-".length()), request.getParameter(key));
            }
        }

        XinhaModalWindow modalWindow = getModalWindow();
        if (modalWindow != null) {
            modalWindow.setTitle("ModalWindow[" + pluginName + "]");
            modalWindow.setContent(createContentPanel(modalWindow, params));
            modalWindow.show(target);
        } else {
            log.error("No modal window found");
        }
    }

    protected XinhaModalWindow getModalWindow() {
        if (getComponent() != null && getComponent() instanceof XinhaPlugin) {
            return ((XinhaPlugin) getComponent()).getModalWindow();
        } else {
            log.error("Not attached to a xinha plugin");
        }
        return null;
    }

    abstract XinhaContentPanel createContentPanel(XinhaModalWindow modalWindow, Map<String, String> params);

}
