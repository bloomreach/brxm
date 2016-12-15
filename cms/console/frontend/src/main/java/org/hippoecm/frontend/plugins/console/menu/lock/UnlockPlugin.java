/*
 *  Copyright 2012-2016 Hippo B.V. (http://www.onehippo.com)
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

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.markup.html.basic.Label;
import org.hippoecm.frontend.model.ReadOnlyModel;
import org.hippoecm.frontend.plugin.IPluginContext;
import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.hippoecm.frontend.plugins.standards.list.resolvers.CssClass;
import org.hippoecm.frontend.plugins.standards.list.resolvers.TitleAttribute;
import org.hippoecm.frontend.service.render.RenderPlugin;
import org.hippoecm.frontend.session.UserSession;
import org.onehippo.repository.util.JcrConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class UnlockPlugin extends RenderPlugin<Node> {

    private static final Logger log = LoggerFactory.getLogger(UnlockPlugin.class);

    private final AjaxLink<Void> link;

    public UnlockPlugin(final IPluginContext context, final IPluginConfig config) {
        super(context, config);

        final Label label = new Label("link-text", ReadOnlyModel.of(() -> {
            if (isLockable()) {
                return isLocked() ? "Unlock" : "Not locked";
            }
            return "Lock/Unlock";
        }));
        label.setOutputMarkupId(true);

        label.add(CssClass.append(ReadOnlyModel.of(() -> {
            if (isLockable()) {
                return isLocked() ? "dropdown-link-green" : "dropdown-link-red";
            }
            return "dropdown-link-disabled";
        })));

        // set up link component
        link = new AjaxLink<Void>("link") {
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
            return node != null && node.isNodeType(JcrConstants.MIX_LOCKABLE);
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
        if (!isLockable()) {
            add(TitleAttribute.set("Only lockable nodes can be locked or unlocked."));
        } else {
            add(TitleAttribute.clear());
        }
        redraw();
    }

}
