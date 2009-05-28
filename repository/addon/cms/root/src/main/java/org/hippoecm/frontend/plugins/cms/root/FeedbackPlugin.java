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

package org.hippoecm.frontend.plugins.cms.root;

import org.apache.wicket.feedback.FeedbackMessage;
import org.apache.wicket.feedback.IFeedbackMessageFilter;
import org.apache.wicket.markup.html.panel.FeedbackPanel;
import org.hippoecm.frontend.PluginRequestTarget;
import org.hippoecm.frontend.plugin.IPluginContext;
import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.hippoecm.frontend.plugins.yui.AbstractYuiBehavior;
import org.hippoecm.frontend.plugins.yui.YuiPluginHelper;
import org.hippoecm.frontend.plugins.yui.header.IYuiContext;
import org.hippoecm.frontend.plugins.yui.webapp.IYuiManager;
import org.hippoecm.frontend.service.render.RenderPlugin;

public class FeedbackPlugin extends RenderPlugin {
    @SuppressWarnings("unused")
    private final static String SVN_ID = "$Id$";

    private static final long serialVersionUID = 1L;

    private FeedbackPanel feedback;

    public FeedbackPlugin(IPluginContext context, IPluginConfig config) {
        super(context, config);

        add(feedback = new FeedbackPanel("feedback", new IFeedbackMessageFilter() {
            private static final long serialVersionUID = 1L;

            public boolean accept(FeedbackMessage message) {
                return true;
            }
        }));
        feedback.setOutputMarkupId(true);
        feedback.add(new NotifyUserBehavior(YuiPluginHelper.getManager(context)));
    }

    @Override
    public void render(PluginRequestTarget target) {
        super.render(target);

        if (target != null) {
            target.addComponent(feedback);

            if ((feedback.anyMessage())) {
                target.appendJavascript("YAHOO.hippo.container.status.show();");
                target.appendJavascript("YAHOO.lang.later(4000, YAHOO.hippo.container.status, 'hide');");
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
            helper.addModule("container");

            String t = "YAHOO.namespace('hippo.container'); YAHOO.hippo.container.status ="
                    + "new YAHOO.widget.Overlay(\"global-feedback-container\", { xy:[693,77], " + "visible:false,"
                    + "zIndex:1000," + "width:\"210px\", height: \"16px\","
                    + "effect:{effect:YAHOO.widget.ContainerEffect.FADE,duration:0.25} } );";

            t += "YAHOO.hippo.container.status.render(document.body);";
            t += "YAHOO.hippo.container.status.hideMacGeckoScrollbars();";
            t += "YAHOO.util.Event.addListener(\"global-feedback-container\", \"click\", YAHOO.hippo.container.status.hide, YAHOO.hippo.container.status, true);";
            helper.addOnDomLoad(t);
        }
    };

}
