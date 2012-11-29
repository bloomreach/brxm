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
package org.hippoecm.frontend.plugins.console.menu;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.PropertyIterator;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.SimpleCredentials;
import javax.jcr.query.Query;
import javax.jcr.query.QueryResult;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.apache.wicket.Application;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.model.Model;

import org.hippoecm.frontend.plugin.IPluginContext;
import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.hippoecm.frontend.service.render.RenderPlugin;
import org.hippoecm.frontend.session.UserSession;
import org.hippoecm.repository.api.HippoNode;

public class CheckPlugin extends RenderPlugin {

    private static final long serialVersionUID = 1L;

    static final Logger log = LoggerFactory.getLogger(CheckPlugin.class);

    public CheckPlugin(IPluginContext context, IPluginConfig config) {
        super(context, config);
        final Label message;
        add(message = new Label("message", new Model("")));
        message.setOutputMarkupId(true);
        add(new AjaxLink("check-link") {
            private static final long serialVersionUID = 1L;
            @Override
            public void onClick(AjaxRequestTarget target) {
                String result = CheckPlugin.this.check();
                message.setDefaultModel(new Model(result));
                setModel(new Model(result));
                target.addComponent(message);
            }
        });

        UserSession session = UserSession.get();
        /* Uncommented at this time, the console can always show this plugin */
        if (session.getApplication().getConfigurationType().equals(Application.DEPLOYMENT)) {
            setVisible(false);
        }
    }

    private String check() {
        try {
            Session session = UserSession.get().getJcrSession();
            check(session);
        } catch (RepositoryException ex) {
            log.error("error during user consistency check", ex);
            return "error";
        }
        try {
            Session session = UserSession.get().getJcrSession().getRepository().login(new SimpleCredentials("admin", "admin".toCharArray()));
            check(session);
            session.logout();
        } catch (RepositoryException ex) {
            log.error("error during root consistency check", ex);
            return "Error";
        }
        return null;
    }

    private void check(Session session) throws RepositoryException {
        check(session.getRootNode());

        Query query = session.getWorkspace().getQueryManager().createQuery("//element(*,nt:base)", Query.XPATH);
        QueryResult result = query.execute();
        for (NodeIterator iter = result.getNodes(); iter.hasNext();) {
            Node node = iter.nextNode();
            if (node != null && log.isTraceEnabled()) {
                log.trace("query: {}", node.getPath());
            }
        }
    }

    private void check(Node node) throws RepositoryException {
        if (log.isTraceEnabled()) {
            log.trace("traverse: {}", node.getPath());
        }
        if (node instanceof HippoNode) {
            Node canonical = ((HippoNode)node).getCanonicalNode();
            if (canonical == null || !canonical.isSame(node)) {
                return;
            }
        }
        if (node.getPath().equals("/jcr:system")) {
            return;
        }
        for (NodeIterator iter = node.getNodes(); iter.hasNext();) {
            Node child = iter.nextNode();
            if (child != null) {
                check(child);
            }
        }
        for (PropertyIterator iter = node.getProperties(); iter.hasNext();) {
            iter.nextProperty();
        }
    }
}
