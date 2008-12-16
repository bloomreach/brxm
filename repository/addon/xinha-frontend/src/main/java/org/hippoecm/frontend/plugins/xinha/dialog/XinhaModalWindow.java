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
package org.hippoecm.frontend.plugins.xinha.dialog;

import org.apache.wicket.ResourceReference;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.behavior.HeaderContributor;
import org.apache.wicket.extensions.ajax.markup.html.modal.ModalWindow;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.markup.html.resources.JavascriptResourceReference;

public class XinhaModalWindow extends ModalWindow {
    @SuppressWarnings("unused")
    private final static String SVN_ID = "$Id$";

    private static final long serialVersionUID = 1L;

    private static ResourceReference JAVASCRIPT = new JavascriptResourceReference(XinhaModalWindow.class,
            "xinha-modal.js");

    public XinhaModalWindow(String id) {
        super(id);

        setInitialWidth(450);
        setInitialHeight(300);
        setCookieName("XinhaModalWindow");
        setResizable(true);

        setTitle("ModalWindow");
        setContent(new Panel(this.getContentId()));
        add(HeaderContributor.forJavaScript(JAVASCRIPT));
    }

    public void onSelect(AjaxRequestTarget target, String returnValue) {
        target.getHeaderResponse().renderOnDomReadyJavascript(
                "if(openModalDialog != null){ openModalDialog.close(" + returnValue + "); }");
        close(target);
    }

    public void onCancel(AjaxRequestTarget target) {
        target.getHeaderResponse().renderOnDomReadyJavascript(
                "if(openModalDialog != null){ openModalDialog.cancel(); }");
        close(target);
    }

}
