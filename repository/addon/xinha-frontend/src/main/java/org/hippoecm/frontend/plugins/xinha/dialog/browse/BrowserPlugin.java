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

package org.hippoecm.frontend.plugins.xinha.dialog.browse;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.form.AjaxButton;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.panel.FeedbackPanel;
import org.apache.wicket.model.IModel;
import org.hippoecm.frontend.model.IModelListener;
import org.hippoecm.frontend.plugin.IPluginContext;
import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.hippoecm.frontend.plugins.standards.browse.AbstractBrowseView;
import org.hippoecm.frontend.plugins.xinha.dialog.DialogBehavior;
import org.hippoecm.frontend.plugins.xinha.dialog.IDialog;
import org.hippoecm.frontend.service.render.RenderPlugin;

public abstract class BrowserPlugin extends RenderPlugin {
    private static final long serialVersionUID = 1L;

    @SuppressWarnings("unused")
    private final static String SVN_ID = "$Id$";

    final protected BrowseView browseView;

    final private Form form;
    final protected AjaxButton ok;
    final protected AjaxButton cancel;
    final protected FeedbackPanel feedback;

    public BrowserPlugin(IPluginContext context, final IPluginConfig config) {
        super(context, config);

        browseView = new BrowseView(context, config) {
            private static final long serialVersionUID = 1L;

            @Override
            protected String getExtensionPoint() {
                return config.getString("dialog.list");
            }

        };

        add(form = new Form("form"));

        form.add(ok = new AjaxButton("ok", form) {
            private static final long serialVersionUID = 1L;

            @Override
            protected void onSubmit(AjaxRequestTarget target, Form form) {
                getDialog().ok(target);
            }

        });
        ok.setEnabled(false);

        form.add(cancel = new AjaxButton("close", form) {
            private static final long serialVersionUID = 1L;

            @Override
            public void onSubmit(AjaxRequestTarget target, Form form) {
                getDialog().cancel(target);
            }
        });

        //TODO: feedback is written in the page feedbackpanel, not this one in the modalwindow
        form.add(feedback = new FeedbackPanel("dialog.feedback"));
        feedback.setOutputMarkupId(true);
    }

    private IDialog getDialog() {
        String id = getPluginConfig().getString(DialogBehavior.DIALOG_SERVICE_ID);
        return getPluginContext().getService(id, IDialog.class);
    }

    protected abstract void onDocumentChanged(IModel model);

    abstract public class BrowseView extends AbstractBrowseView {
        private static final long serialVersionUID = 1L;

        protected BrowseView(IPluginContext context, IPluginConfig config) {
            super(context, config);

            context.registerService(new IModelListener() {

                public void updateModel(IModel model) {
                    onDocumentChanged(model);
                }

            }, config.getString("model.document"));
        }
    }

}
