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
package org.hippoecm.frontend.editor.editor;

import java.util.List;

import org.apache.wicket.feedback.FeedbackMessage;
import org.apache.wicket.feedback.IFeedbackMessageFilter;
import org.hippoecm.frontend.PluginRequestTarget;
import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.frontend.plugin.IPluginContext;
import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.hippoecm.frontend.plugins.yui.feedback.YuiFeedbackPanel;
import org.hippoecm.frontend.service.render.RenderPlugin;
import org.hippoecm.frontend.service.render.RenderService;

public class EditorPlugin extends RenderPlugin {
    @SuppressWarnings("unused")
    private final static String SVN_ID = "$Id$";

    private static final long serialVersionUID = 1L;

    private EditorForm form;
    private YuiFeedbackPanel feedback;

    public EditorPlugin(final IPluginContext context, final IPluginConfig config) {
        super(context, config);

        add(form = newForm());

        feedback = new YuiFeedbackPanel("feedback", new IFeedbackMessageFilter() {
            private static final long serialVersionUID = 1L;

            public boolean accept(FeedbackMessage message) {
                if (config.getString(RenderService.FEEDBACK) != null) {
                    List<IFeedbackMessageFilter> filters = context.getServices(
                            config.getString(RenderService.FEEDBACK), IFeedbackMessageFilter.class);
                    for (IFeedbackMessageFilter filter : filters) {
                        if (filter.accept(message)) {
                            return true;
                        }
                    }
                }
                return false;
            }
        }, context);
        add(feedback);
    }

    @Override
    public void onModelChanged() {
        if (!form.getModel().equals(getDefaultModel())) {
            form.destroy();
            replace(form = newForm());
        }
    }

    @Override
    public void render(PluginRequestTarget target) {
        super.render(target);
        feedback.render(target);
        if (form != null) {
            form.render(target);
        }
    }

    protected EditorForm newForm() {
        return new EditorForm("form", (JcrNodeModel) getDefaultModel(), this, getPluginContext(), getPluginConfig());
    }

}
