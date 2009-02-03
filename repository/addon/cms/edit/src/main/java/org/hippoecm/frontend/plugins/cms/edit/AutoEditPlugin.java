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
package org.hippoecm.frontend.plugins.cms.edit;

import java.util.ArrayList;
import java.util.List;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;
import javax.jcr.query.Query;
import javax.jcr.query.QueryManager;

import org.apache.wicket.Session;
import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.frontend.model.event.IRefreshable;
import org.hippoecm.frontend.plugin.IPlugin;
import org.hippoecm.frontend.plugin.IPluginContext;
import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.hippoecm.frontend.service.IEditService;
import org.hippoecm.frontend.session.UserSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AutoEditPlugin implements IPlugin, IRefreshable {
    @SuppressWarnings("unused")
    private final static String SVN_ID = "$Id$";

    private static final long serialVersionUID = 1L;

    public static final Logger log = LoggerFactory.getLogger(AutoEditPlugin.class);

    private IPluginContext context;
    private IPluginConfig config;
    private List<JcrNodeModel> editors;

    public AutoEditPlugin(IPluginContext context, IPluginConfig config) {
        this.context = context;
        this.config = config;

        editors = new ArrayList<JcrNodeModel>();

        context.registerService(this, IRefreshable.class.getName());

        refresh();
    }

    public void refresh() {
        if (config.getString("editor.id") != null) {
            final IEditService editService = context.getService(config.getString("editor.id"), IEditService.class);
            if (editService != null) {
                try {
                    final String user = ((UserSession) Session.get()).getCredentials().getString("username"); // FIXME: no guarantee to have username here
                    QueryManager qMgr = ((UserSession) Session.get()).getJcrSession().getWorkspace().getQueryManager();
                    Query query = qMgr.createQuery("select * from hippostd:publishable where hippostd:state='draft' " // FIXME wrong knowledge in use here
                            + "and hippostd:holder='" + user + "'", Query.SQL);

                    List<JcrNodeModel> newEditors = new ArrayList<JcrNodeModel>();
                    NodeIterator iter = query.execute().getNodes();
                    while (iter.hasNext()) {
                        Node node = iter.nextNode();
                        if (node == null) {
                            continue;
                        }
                        if (!node.getName().equals("hippo:prototype")) {
                            JcrNodeModel model = new JcrNodeModel(node);
                            if (!editors.contains(model)) {
                                editService.edit(new JcrNodeModel(node));
                            }
                            newEditors.add(model);
                        }
                    }
                    editors = newEditors;
                } catch (RepositoryException ex) {
                    log.error(ex.getMessage());
                }
            } else {
                log.warn("No edit service found");
            }
        } else {
            log.warn("No edit service configured (editor.id)");
        }
    }
}
