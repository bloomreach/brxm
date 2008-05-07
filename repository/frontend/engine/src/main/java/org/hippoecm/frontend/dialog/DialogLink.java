/*
 * Copyright 2007 Hippo
 *
 * Licensed under the Apache License, Version 2.0 (the  "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.hippoecm.frontend.dialog;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.hippoecm.frontend.model.IPluginModel;
import org.hippoecm.frontend.plugin.channel.Channel;
import org.hippoecm.frontend.plugin.channel.ChannelFactory;

public class DialogLink extends Panel {
    private static final long serialVersionUID = 1L;

    
    public DialogLink(String id, IModel linktext, Class clazz, IPluginModel model, Channel channel, ChannelFactory factory) {
        super(id, model);
        Channel proxy = factory.createChannel();
        DialogWindow dialogWindow = new DialogWindow("dialog", model, channel, proxy);
        dialogWindow.setPageCreator(new DynamicDialogFactory(dialogWindow, clazz));
        add(linktext, dialogWindow);
    }
    
     
    public DialogLink(String id, IModel linktext, IDialogFactory dialogFactory, IPluginModel model, Channel channel, ChannelFactory channelFactory) {
        super(id, model);
        Channel proxy = channelFactory.createChannel();
        DialogWindow dialogWindow = new DialogWindow("dialog", model, channel, proxy);
        dialogWindow.setPageCreator(new DynamicDialogFactory(dialogWindow, dialogFactory));
        add(linktext, dialogWindow);        
    }
    
    
    public DialogLink(String id, IModel linktext, DialogWindow window, IModel model) {
        super(id, model);
        add(linktext, window);
    }


    private void add(IModel linktext, final DialogWindow dialogWindow) {
        add(dialogWindow);
        AjaxLink link = new AjaxLink("dialog-link") {
            private static final long serialVersionUID = 1L;
            @Override
            public void onClick(AjaxRequestTarget target) {
                dialogWindow.show(target);
            }
        };
        add(link);
        link.add(new Label("dialog-link-text", linktext));
    }

}
