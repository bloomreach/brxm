/*
 *  Copyright 2011-2016 Hippo B.V. (http://www.onehippo.com)
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
import org.hippoecm.frontend.model.ReadOnlyModel;
import org.hippoecm.frontend.plugin.IPluginContext;
import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.hippoecm.frontend.plugins.standards.list.resolvers.CssClass;
import org.hippoecm.frontend.plugins.standards.list.resolvers.TitleAttribute;
import org.hippoecm.frontend.service.render.RenderPlugin;
import org.onehippo.repository.util.JcrConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CheckInOutPlugin extends RenderPlugin<Node> {

    private static final Logger log = LoggerFactory.getLogger(CheckInOutPlugin.class);

    private final AjaxLink<Void> link;

    public CheckInOutPlugin(final IPluginContext context, final IPluginConfig config) {
        super(context, config);

        // set up label component
        final Label label = new Label("link-text", ReadOnlyModel.of(() -> {
            if (isVersionable()) {
                return isCheckedOut() ? "Check In" : "Check Out";
            }
            return "Check In/Out";
        }));
        label.setOutputMarkupId(true);

        label.add(CssClass.append(ReadOnlyModel.of(() -> {
            if (isVersionable()) {
                return isCheckedOut() ? "dropdown-link-green" : "dropdown-link-red";
            }
            return "dropdown-link-disabled";
        })));
        // set up link component
        link = new AjaxLink<Void>("link") {

            @Override
            public void onClick(AjaxRequestTarget target) {
                if (isVersionable()) {
                    if (isCheckedOut()) {
                        checkin();
                    } else {
                        checkout();
                    }
                }
                target.add(label);
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
            return node != null && node.isNodeType(JcrConstants.MIX_VERSIONABLE);
        } catch (RepositoryException e) {
            log.error("An error occurred determining if node is versionable.", e);
            return false;
        }
    }

    private void checkin() {
        try {
            getSession().getJcrSession().getWorkspace().getVersionManager().checkin(getModelObject().getPath());
        } catch (RepositoryException e) {
            log.error("An error occurred trying to check in node.", e);
        }
    }

    private void checkout() {
        try {
            getSession().getJcrSession().getWorkspace().getVersionManager().checkout(getModelObject().getPath());
        } catch (RepositoryException e) {
            log.error("An error occurred trying to check out node.", e);
        }
    }

    @Override
    protected void onModelChanged() {
        link.setEnabled(isVersionable());

        if (!isVersionable()) {
            add(TitleAttribute.set("Only versionable nodes can be checked in or out."));
        } else {
            add(TitleAttribute.clear());
        }
        redraw();
    }

}
