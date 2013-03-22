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
package org.hippoecm.frontend.plugins.yui.feedback;

import java.util.Map;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.feedback.IFeedbackMessageFilter;
import org.apache.wicket.markup.html.panel.FeedbackPanel;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.LoadableDetachableModel;
import org.apache.wicket.util.collections.MiniMap;
import org.hippoecm.frontend.plugins.yui.AbstractYuiBehavior;
import org.hippoecm.frontend.plugins.yui.HippoNamespace;
import org.hippoecm.frontend.plugins.yui.header.IYuiContext;

public class YuiFeedbackPanel extends Panel {

    private static final long serialVersionUID = 1L;

    private FeedbackPanel feedback;

    public YuiFeedbackPanel(String id, IFeedbackMessageFilter filter) {
        super(id);

        add(feedback = new FeedbackPanel("feedback", filter));
        feedback.setOutputMarkupId(true);
        feedback.add(new NotifyUserBehavior());
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

    public boolean hasMessages() {
        return feedback.anyMessage();
    }

    class NotifyUserBehavior extends AbstractYuiBehavior {
        private static final long serialVersionUID = 1L;

        @Override
        public void addHeaderContribution(IYuiContext helper) {
            helper.addModule(HippoNamespace.NS, "feedbackmanager");

            Map<String, Object> params = new MiniMap<String,Object>(1);
            params.put("id", feedback.getMarkupId());
            helper.addTemplate(YuiFeedbackPanel.class, "feedback.js", params);
            helper.addOnDomLoad(new LoadableDetachableModel<String>() {
                private static final long serialVersionUID = 1L;

                @Override
                protected String load() {
                    if (feedback.anyMessage()) {
                        return "YAHOO.hippo.FeedbackManager.delayedHide(\"" + feedback.getMarkupId() + "\", 4000);";
                    }
                    return "";
                }
            });

        }
    };

}
