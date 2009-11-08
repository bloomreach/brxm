/*
 *  Copyright 2009 Hippo.
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
package org.hippoecm.frontend.plugins.console.menu.check;

import javax.jcr.Node;
import javax.jcr.RepositoryException;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.util.value.IValueMap;
import org.hippoecm.frontend.dialog.AbstractDialog;
import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.frontend.plugins.console.menu.MenuPlugin;
import org.hippoecm.frontend.service.ITitleDecorator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CheckInOutDialog extends AbstractDialog implements ITitleDecorator {
    @SuppressWarnings("unused")
    private final static String SVN_ID = "$Id$";
    private static final long serialVersionUID = 1L;
    static final Logger log = LoggerFactory.getLogger(CheckInOutDialog.class);

    private JcrNodeModel model;
    private String status;
    private String action;

    private AjaxLink actionLink;
    private Label actionLabel;
    private Label statusLabel;

    public CheckInOutDialog(MenuPlugin plugin) {
        model = (JcrNodeModel) plugin.getModel();

        PropertyModel actionModel = new PropertyModel(this, "action");
        actionLabel = new Label("action-link-text", actionModel);
        actionLabel.setOutputMarkupId(true);

        PropertyModel statusModel = new PropertyModel(this, "status");
        statusLabel = new Label("status-label", statusModel);
        statusLabel.setOutputMarkupId(true);

        actionLink = new AjaxLink("action-link") {
            private static final long serialVersionUID = 1L;

            @Override
            public void onClick(AjaxRequestTarget target) {
                try {
                    Node node = model.getNode();
                    if (node.isNodeType("mix:versionable")) {
                        if (node.isCheckedOut()) {
                            node.checkin();
                            status = "Checked In";
                            action = "Check Node Out";
                        } else {
                            node.checkout();
                            status = "Checked Out";
                            action = "Check Node In";
                        }
                    }
                } catch (RepositoryException e) {
                    log.error("Unable to check in/out node.", e);
                } finally {
                    // Update labels
                    target.addComponent(statusLabel);
                    target.addComponent(actionLabel);
                }
            }
        };

        try {
            Node node = model.getNode();
            add(new Label("node", node.getPath()));

            if (!node.isNodeType("mix:versionable")) {
                actionLink.setVisible(false);
                status = "Node is not versionable";
            } else if (node.getSession().hasPendingChanges()) {
                actionLink.setVisible(false);
                status = "There are pending changes, please save or reset the session first";
            } else {
                if (model.getNode().isCheckedOut()) {
                    status = "Checked Out";
                    action = "Check In";
                } else {
                    status = "Checked In";
                    action = "Check Out";
                }
            }
        } catch (RepositoryException e) {
            log.error(e.getMessage());
            add(new Label("node", e.getClass().getName()));
            status = e.getMessage();
        }

        actionLink.setOutputMarkupId(true);
        actionLink.add(actionLabel);
        add(actionLink);
        add(statusLabel);

        setOkLabel("Close");
        setCancelVisible(false);
        setFocusOnOk(); 
    }

    public IModel getTitle() {
        try {
            return new Model("Check in or out :" + model.getNode().getPath());
        } catch (RepositoryException e) {
            log.warn("Unable to get node name from model for title", e);
            return new Model("Check In or Check Out a node");
        }
    }
    
    @Override
    public IValueMap getProperties() {
        return SMALL;
    }

}
