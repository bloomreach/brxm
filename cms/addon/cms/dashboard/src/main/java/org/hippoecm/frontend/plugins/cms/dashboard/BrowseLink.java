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
package org.hippoecm.frontend.plugins.cms.dashboard;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.panel.Panel;
import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.frontend.plugin.channel.Channel;
import org.hippoecm.frontend.plugin.channel.Request;
import org.hippoecm.repository.api.ISO9075Helper;

public class BrowseLink extends Panel {
    private static final long serialVersionUID = 1L;
    
    public BrowseLink(String id, final JcrNodeModel variant, JcrNodeModel handle, final Channel channel) {
        super(id, variant);
        
        AjaxLink link = new AjaxLink("link", variant) {
            private static final long serialVersionUID = 1L;
            public void onClick(AjaxRequestTarget target) {
                Request request = channel.createRequest("browse", variant);
                channel.send(request);
                request.getContext().apply(target);
            }
        };
        add(link);
        
        String path = handle.getItemModel().getPath();
        path = ISO9075Helper.decodeLocalName(path);
        link.add(new Label("label", path));
    }

}
