/*
 *  Copyright 2011 Hippo.
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

import org.apache.wicket.AttributeModifier;
import org.apache.wicket.ResourceReference;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.image.Image;
import org.apache.wicket.model.Model;
import org.hippoecm.frontend.plugin.IPluginContext;
import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.hippoecm.frontend.service.render.RenderPlugin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CheckInOutPlugin extends RenderPlugin<Node> {

    private static final long serialVersionUID = 1L;
    private static final Logger log = LoggerFactory.getLogger(CheckInOutPlugin.class);

    private final Image icon;
    private final AjaxLink<Void> link;

    public CheckInOutPlugin(IPluginContext context, IPluginConfig config) {
        super(context, config);

        // set up label component
        final Label label = new Label("link-text", new Model<String>() {
            private static final long serialVersionUID = 1L;

            @Override
            public String getObject() {
                return isVersionable() ? (isCheckedOut() ? "Check In" : "Check Out") : "Not Versionable";
            }
        });
        label.setOutputMarkupId(true);
        label.add(new AttributeModifier("style", true, new Model<String>() {
            private static final long serialVersionUID = 1L;

            @Override
            public String getObject() {
                if (!isVersionable()) {
                    return "color:grey";
                }
                return isCheckedOut() ? "color:green" : "color:red";
            }
        }));
        // set up icon component
        icon = new Image("icon") {
            private static final long serialVersionUID = 1L;
            private final ResourceReference emptyGif = new ResourceReference(CheckInOutPlugin.class, "empty.gif");
            private final ResourceReference checkedinIcon = new ResourceReference(CheckInOutPlugin.class,
                                                                                  "checkedin.png");
            private final ResourceReference checkedoutIcon = new ResourceReference(CheckInOutPlugin.class,
                                                                                   "checkedout.png");

            @Override
            protected ResourceReference getImageResourceReference() {
                if (!isVersionable()) {
                    return emptyGif;
                }
                return isCheckedOut() ? checkedoutIcon : checkedinIcon;
            }
        };
        icon.setOutputMarkupId(true);
        add(icon);
        // set up link component
        link = new AjaxLink<Void>("link") {
            private static final long serialVersionUID = 1L;

            @Override
            public void onClick(AjaxRequestTarget target) {
                if (isVersionable()) {
                    if (isCheckedOut()) {
                        checkin();
                    } else {
                        checkout();
                    }
                }
                target.addComponent(label);
                target.addComponent(icon);
            }
        };
        link.add(label);
        link.setEnabled(isVersionable());
        add(link);
    }

    private boolean isCheckedOut() {
        try {
            final Node node = getModelObject();
            if (node != null) {
                return node.isCheckedOut();
            }
        } catch (RepositoryException e) {
            log.error("An error occurred determining if node is checked out.", e);
        }
        return false;
    }

    private boolean isVersionable() {
        try {
            return getModelObject() != null && getModelObject().isNodeType("mix:versionable");
        } catch (RepositoryException e) {
            log.error("An error occurred determining if node is versionable.", e);
            return false;
        }
    }

    private void checkin() {
        try {
            getModelObject().checkin();
        } catch (RepositoryException e) {
            log.error("An error occurred trying to check in node.", e);
        }
    }

    private void checkout() {
        try {
            getModelObject().checkout();
        } catch (RepositoryException e) {
            log.error("An error occurred trying to check out node.", e);
        }
    }

    @Override
    protected void onModelChanged() {
        link.setEnabled(isVersionable());
        icon.setEnabled(isVersionable());
        redraw();
    }

}
