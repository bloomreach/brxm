package org.hippoecm.frontend.yui.dragdrop.node;

import org.apache.wicket.Request;
import org.apache.wicket.RequestCycle;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.frontend.model.PluginModel;
import org.hippoecm.frontend.plugin.channel.Notification;

public class DragNodeBehavior extends NodeDragDropBehavior {
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
