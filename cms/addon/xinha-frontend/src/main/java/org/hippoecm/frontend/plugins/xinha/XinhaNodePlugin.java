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
package org.hippoecm.frontend.plugins.xinha;

import javax.jcr.Node;
import javax.jcr.Property;
import javax.jcr.RepositoryException;

import org.apache.wicket.model.IModel;
import org.hippoecm.frontend.model.IModelReference;
import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.frontend.model.properties.JcrPropertyModel;
import org.hippoecm.frontend.model.properties.JcrPropertyValueModel;
import org.hippoecm.frontend.plugin.IPluginContext;
import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class XinhaNodePlugin extends AbstractXinhaPlugin {
    @SuppressWarnings("unused")
    private final static String SVN_ID = "$Id$";

    private static final long serialVersionUID = 1L;

    static final Logger log = LoggerFactory.getLogger(XinhaNodePlugin.class);

    public XinhaNodePlugin(IPluginContext context, final IPluginConfig config) {
        super(context, config);
    }

    protected JcrPropertyValueModel getValueModel() {
        JcrNodeModel nodeModel = (JcrNodeModel) getDefaultModel();
        return getContentModel(nodeModel);
    }

    @Override
    protected IModel<String> getBaseModel() {
        IPluginConfig config = getPluginConfig();
        if (!config.containsKey("model.compareTo")) {
            return null;
        }
        IModelReference modelRef = getPluginContext().getService(config.getString("model.compareTo"),
                IModelReference.class);
        if (modelRef == null || modelRef.getModel() == null) {
            return null;
        }
        return getContentModel((JcrNodeModel) modelRef.getModel());
    }

    private JcrPropertyValueModel getContentModel(JcrNodeModel nodeModel) {
        try {
            Node node = nodeModel.getNode();
            if (node == null) {
                return null;
            }
            Property prop = node.getProperty("hippostd:content");
            return new JcrPropertyValueModel(new JcrPropertyModel(prop));
        } catch (RepositoryException ex) {
            log.error(ex.getMessage());
        }
        return null;
    }

}
