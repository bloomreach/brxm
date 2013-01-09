/*
 *  Copyright 2011-2013 Hippo B.V. (http://www.onehippo.com)
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

    private final AjaxLink<Void> link;

    public CheckInOutPlugin(IPluginContext context, IPluginConfig config) {
        super(context, config);

        // set up label component
        final Label label = new Label("link-text", new Model<String>() {
            private static final long serialVersionUID = 1L;

            @Override
            public String getObject() {
                if (!isVersionable()) {
                    return "";
                }
                return isCheckedOut() ? "Check In" : "Check Out";
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
            }
        };
        link.add(label);
        link.setEnabled(isVersionable());
        add(link);
    }

    private boolean isCheckedOut() {
        try {
            final Node node = getModelObject();
            return node != null && node.isCheckedOut();
        } catch (RepositoryException e) {
            log.error("An error occurred determining if node is checked out.", e);
        }
        return false;
    }

    private boolean isVersionable() {
        try {
            final Node node = getModelObject();
            return node != null && node.isNodeType("mix:versionable");
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
        redraw();
    }

}
