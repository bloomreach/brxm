/*
 *  Copyright 2008-2013 Hippo B.V. (http://www.onehippo.com)
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

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;
import javax.jcr.query.Query;
import javax.jcr.query.QueryManager;

import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.frontend.plugin.IPluginContext;
import org.hippoecm.frontend.plugin.Plugin;
import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.hippoecm.frontend.service.IEditor;
import org.hippoecm.frontend.service.IEditorManager;
import org.hippoecm.frontend.service.ServiceException;
import org.hippoecm.frontend.session.UserSession;
import org.hippoecm.repository.HippoStdNodeType;
import org.hippoecm.repository.api.HippoNodeType;
import org.hippoecm.repository.util.JcrUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AutoEditPlugin extends Plugin {

    private static final long serialVersionUID = 1L;

    public static final Logger log = LoggerFactory.getLogger(AutoEditPlugin.class);

    public AutoEditPlugin(IPluginContext context, IPluginConfig config) {
        super(context, config);
    }
    
    @Override
    public void start() {
        IPluginConfig config = getPluginConfig();
        if (config.getString("editor.id") != null) {
            IPluginContext context = getPluginContext();
            final IEditorManager editService = context.getService(config.getString("editor.id"), IEditorManager.class);
            if (editService != null) {
                try {
                    final String user = UserSession.get().getJcrSession().getUserID();
                    QueryManager qMgr = UserSession.get().getJcrSession().getWorkspace().getQueryManager();
                    // FIXME wrong knowledge in use here
                    final String statement = String.format("select * from %s where %s='%s' and %s='%s'",
                            HippoStdNodeType.NT_PUBLISHABLE,
                            HippoStdNodeType.HIPPOSTD_STATE, HippoStdNodeType.DRAFT,
                            HippoStdNodeType.HIPPOSTD_HOLDER, user);
                    Query query = qMgr.createQuery(statement, Query.SQL);

                    NodeIterator iter = query.execute().getNodes();
                    while (iter.hasNext()) {
                        Node node = iter.nextNode();
                        if (node == null) {
                            continue;
                        }

                        if (JcrUtils.getBooleanProperty(node, HippoStdNodeType.HIPPOSTD_RETAINABLE, false)) {
                            continue;
                        }

                        if (!node.getName().equals(HippoNodeType.HIPPO_PROTOTYPE)) {
                            JcrNodeModel model = new JcrNodeModel(node);
                            try {
                                IEditor editor = editService.getEditor(model);
                                if (editor == null) {
                                    editService.openEditor(model);
                                }
                            } catch (ServiceException ex) {
                                log.error(ex.getMessage());
                            }
                        }
                    }
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
