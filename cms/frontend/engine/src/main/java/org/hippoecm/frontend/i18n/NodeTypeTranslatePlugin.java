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
package org.hippoecm.frontend.i18n;

import java.util.Map;

import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.nodetype.NoSuchNodeTypeException;

import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.hippoecm.frontend.plugin.IPlugin;
import org.hippoecm.frontend.plugin.IPluginContext;
import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.hippoecm.frontend.session.UserSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class NodeTypeTranslatePlugin extends AbstractTranslateService implements IPlugin {
    private static final long serialVersionUID = 1L;

    final static Logger log = LoggerFactory.getLogger(NodeTypeTranslatePlugin.class);

    public NodeTypeTranslatePlugin(IPluginContext context, IPluginConfig config) {
        super(context, config);
    }

    public IModel getModel(Map<String, String> criteria) {
        String key = criteria.get("hippo:key");
        try {
            Session jcrSession = ((UserSession) org.apache.wicket.Session.get()).getJcrSession();
            if (jcrSession.getWorkspace().getNodeTypeManager().getNodeType(key) != null) {
                return new Model(key);
            }
        } catch (NoSuchNodeTypeException ex) {
            // ignore
        } catch (RepositoryException ex) {
            log.error(ex.getMessage());
        }
        return null;
    }

}