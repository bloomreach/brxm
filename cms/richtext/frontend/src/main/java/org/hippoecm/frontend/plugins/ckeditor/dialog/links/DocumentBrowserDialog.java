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

package org.hippoecm.frontend.plugins.ckeditor.dialog.links;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.html.panel.EmptyPanel;
import org.apache.wicket.markup.html.panel.Fragment;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.PropertyModel;
import org.hippoecm.frontend.plugin.IPluginContext;
import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.hippoecm.frontend.plugins.ckeditor.dialog.AbstractBrowserDialog;
import org.hippoecm.frontend.plugins.ckeditor.dialog.model.DocumentLink;
import org.hippoecm.frontend.widgets.BooleanFieldWidget;
import org.hippoecm.frontend.widgets.ThrottledTextFieldWidget;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DocumentBrowserDialog<T extends DocumentLink> extends AbstractBrowserDialog<T> {

    public static final String CONFIG_OPEN_IN_NEW_WINDOW_ENABLED = "open.in.new.window.enabled";
    public static final boolean DEFAULT_OPEN_IN_NEW_WINDOW_ENABLED = true;

    private static final long serialVersionUID = 1L;
    private static final Logger log = LoggerFactory.getLogger(DocumentBrowserDialog.class);

    private final String editorId;

    public DocumentBrowserDialog(IPluginContext context, IPluginConfig config, IModel<T> model, final String editorId) {
        super(context, config, model);

        this.editorId = editorId;

        add(new ThrottledTextFieldWidget("title", new StringPropertyModel(model, DocumentLink.TITLE)) {
            private static final long serialVersionUID = 1L;

            @Override
            protected void onUpdate(AjaxRequestTarget target) {
                checkState();
            }
        });

        final boolean enableOpenInNewWindow = config.getAsBoolean(CONFIG_OPEN_IN_NEW_WINDOW_ENABLED, DEFAULT_OPEN_IN_NEW_WINDOW_ENABLED);

        if (enableOpenInNewWindow) {
            Fragment fragment = new Fragment("extra", "popup", this);
            fragment.add(new BooleanFieldWidget("popup", new PropertyModel<Boolean>(model, "openInNewWindow")) {
                private static final long serialVersionUID = 1L;

                @Override
                protected void onUpdate(AjaxRequestTarget target) {
                    checkState();
                }
            });
            add(fragment);
        } else {
            add(new EmptyPanel("extra"));
        }

        checkState();
    }

    @Override
    protected void onOk() {
        if (getModelObject().isValid()) {
            getModelObject().save();
        } else {
            error("Please select a document");
        }
    }

    @Override
    protected String getCloseScript() {
        return "CKEDITOR.instances." + editorId + ".execCommand('insertInternalLink', " + getModelObject().toJsString() + ");";
    }

}
