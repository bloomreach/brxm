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
package org.hippoecm.frontend.plugins.yui.feedback;

import java.util.Map;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.feedback.IFeedbackMessageFilter;
import org.apache.wicket.markup.html.IHeaderResponse;
import org.apache.wicket.markup.html.panel.FeedbackPanel;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.util.collections.MiniMap;
import org.hippoecm.frontend.plugin.IPluginContext;
import org.hippoecm.frontend.plugins.yui.AbstractYuiBehavior;
import org.hippoecm.frontend.plugins.yui.HippoNamespace;
import org.hippoecm.frontend.plugins.yui.YuiPluginHelper;
import org.hippoecm.frontend.plugins.yui.header.IYuiContext;
import org.hippoecm.frontend.plugins.yui.webapp.IYuiManager;

public class YuiFeedbackPanel extends Panel {
    @SuppressWarnings("unused")
    private final static String SVN_ID = "$Id$";

    private static final long serialVersionUID = 1L;

    private FeedbackPanel feedback;

    public YuiFeedbackPanel(String id, IFeedbackMessageFilter filter, IPluginContext context) {
        super(id);

        add(feedback = new FeedbackPanel("feedback", filter));
        feedback.setOutputMarkupId(true);
        feedback.add(new NotifyUserBehavior(YuiPluginHelper.getManager(context)));
        setOutputMarkupId(true);
    }

    /**
     * Plugins should call this method in their own render(PluginRequestTarget) method,
     * to give the panel the chance to update itself.
     */
    public void render(AjaxRequestTarget target) {
        if (target != null) {
            if (feedback.anyMessage()) {
                target.addComponent(feedback);
            }
        }
    }

    class NotifyUserBehavior extends AbstractYuiBehavior {
        private static final long serialVersionUID = 1L;

        public NotifyUserBehavior(IYuiManager manager) {
            super(manager);
        }

        @Override
        public void addHeaderContribution(IYuiContext helper) {
            helper.addModule(HippoNamespace.NS, "feedbackmanager");

            Map<String, Object> params = new MiniMap(1);
            params.put("id", getMarkupId());
            helper.addTemplate(YuiFeedbackPanel.class, "feedback.js", params);
        }

        @Override
        public void renderHead(IHeaderResponse response) {
            super.renderHead(response);

            if (feedback.anyMessage()) {
                response.renderJavascript("var module = YAHOO.hippo.FeedbackManager.get(\"" + getMarkupId() + "\"); "
                        + "module.show(); YAHOO.lang.later(4000, module, 'hide');", "feedback-" + getMarkupId());
            }
        }
    };

}
