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
package org.hippoecm.frontend.plugins.yui.dragdrop.node;

import org.apache.wicket.Request;
import org.apache.wicket.RequestCycle;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.hippoecm.frontend.legacy.model.PluginModel;
import org.hippoecm.frontend.legacy.plugin.channel.Notification;
import org.hippoecm.frontend.model.JcrNodeModel;

@Deprecated
public class DragNodeBehavior extends NodeDragDropBehavior {
    @SuppressWarnings("unused")
    private final static String SVN_ID = "$Id$";

    private static final long serialVersionUID = 1L;

    public DragNodeBehavior() {
        super();
    }

    public DragNodeBehavior(JcrNodeModel nodeModel) {
        super(nodeModel);
    }

    public DragNodeBehavior(JcrNodeModel nodeModel, String... groups) {
        super(nodeModel, groups);
    }

    @Override
    public void onDrop(AjaxRequestTarget target) {
        if (getPlugin().getTopChannel() != null) {
            Request request = RequestCycle.get().getRequest();

            PluginModel mdl = new PluginModel();
            mdl.put("targetId", request.getParameter("targetId"));
            mdl.put("node", getNodePath());

            Notification notification = getPlugin().getTopChannel().createNotification("drop", mdl);
            getPlugin().getTopChannel().publish(notification);
            notification.getContext().apply(target);
        }
    }

    @Override
    protected String getHeaderContributorFilename() {
        return "DragNode.js";
    }

}
