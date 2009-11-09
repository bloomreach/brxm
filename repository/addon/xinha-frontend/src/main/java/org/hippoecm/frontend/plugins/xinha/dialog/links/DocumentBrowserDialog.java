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

package org.hippoecm.frontend.plugins.xinha.dialog.links;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.model.IModel;
import org.hippoecm.frontend.plugin.IPluginContext;
import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.hippoecm.frontend.plugins.xinha.dialog.AbstractBrowserDialog;
import org.hippoecm.frontend.plugins.xinha.services.links.InternalXinhaLink;
import org.hippoecm.frontend.plugins.xinha.services.links.XinhaLink;
import org.hippoecm.frontend.widgets.BooleanFieldWidget;
import org.hippoecm.frontend.widgets.TextFieldWidget;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DocumentBrowserDialog extends AbstractBrowserDialog {
    private static final long serialVersionUID = 1L;

    @SuppressWarnings("unused")
    private final static String SVN_ID = "$Id$";

    static final Logger log = LoggerFactory.getLogger(DocumentBrowserDialog.class);

    public DocumentBrowserDialog(IPluginContext context, IPluginConfig config, IModel model) {
        super(context, config, model);

        add(new TextFieldWidget("title", getPropertyModel(InternalXinhaLink.class, XinhaLink.TITLE)) {
            private static final long serialVersionUID = 1L;

            @Override
            protected void onUpdate(AjaxRequestTarget target) {
                checkState();
            }
        });

        add(new BooleanFieldWidget("popup", getPropertyModel(InternalXinhaLink.class, "target")) {
            private static final long serialVersionUID = 1L;

            @Override
            protected void onUpdate(AjaxRequestTarget target) {
                checkState();
            }
        });

        checkState();
    }

    @Override
    protected void onOk() {
        InternalXinhaLink link = (InternalXinhaLink) getModelObject();
        if (link.isValid()) {
            link.save();
        } else {
            error("Please select a document");
        }
    }

}
