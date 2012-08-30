/*
 *  Copyright 2012 Hippo.
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
package org.hippoecm.frontend.plugins.console.menu.lock;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.lock.LockManager;

import org.apache.wicket.AttributeModifier;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.model.Model;
import org.hippoecm.frontend.plugin.IPluginContext;
import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.hippoecm.frontend.service.render.RenderPlugin;
import org.hippoecm.frontend.session.UserSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class UnlockPlugin extends RenderPlugin<Node> {

    private static final long serialVersionUID = 1L;
    private static final Logger log = LoggerFactory.getLogger(UnlockPlugin.class);

    private final AjaxLink<Void> link;

    public UnlockPlugin(IPluginContext context, IPluginConfig config) {
        super(context, config);

        final Label label = new Label("link-text", new Model<String>() {
            private static final long serialVersionUID = 1L;

            @Override
            public String getObject() {
                if (!isLockable()) {
                    return "";
                }
                return isLocked() ? "Unlock" : "Not locked";
            }
        });
        label.setOutputMarkupId(true);
        label.add(new AttributeModifier("style", true, new Model<String>() {
            private static final long serialVersionUID = 1L;

            @Override
            public String getObject() {
                if (!isLockable()) {
                    return "color:grey";
                }
                return isLocked() ? "color:green" : "color:red";
            }
        }));
        // set up link component
        link = new AjaxLink<Void>("link") {
            private static final long serialVersionUID = 1L;

            @Override
            public void onClick(AjaxRequestTarget target) {
                unlock();
            }
        };
        link.add(label);
        link.setEnabled(isLockable());
        add(link);
    }

    private boolean isLocked() {
        try {
            final Node node = getModelObject();
            return node != null && node.isLocked();
        } catch (RepositoryException e) {
            log.error("An error occurred determining if node is locked.", e);
            return false;
        }
    }

    private boolean isLockable() {
        try {
            final Node node = getModelObject();
            return node != null && node.isNodeType("mix:lockable");
        } catch (RepositoryException e) {
            log.error("An error occurred determining if node is lockable.", e);
            return false;
        }
    }

    private void unlock() {
        try {
            final LockManager lockManager = UserSession.get().getJcrSession().getWorkspace().getLockManager();
            lockManager.unlock(getModelObject().getPath());
        } catch (RepositoryException e) {
            log.error("An error occurred trying to unlock node.", e);
        }
    }

    @Override
    protected void onModelChanged() {
        link.setEnabled(isLocked());
        redraw();
    }

}
