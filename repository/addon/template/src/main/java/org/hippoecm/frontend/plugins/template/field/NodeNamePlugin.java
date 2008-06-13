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
package org.hippoecm.frontend.plugins.template.field;

import java.util.List;

import javax.jcr.RepositoryException;

import org.apache.wicket.markup.html.basic.Label;
import org.hippoecm.frontend.legacy.model.IPluginModel;
import org.hippoecm.frontend.legacy.plugin.Plugin;
import org.hippoecm.frontend.legacy.plugin.PluginDescriptor;
import org.hippoecm.frontend.model.JcrNodeModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class NodeNamePlugin extends Plugin {
    @SuppressWarnings("unused")
    private final static String SVN_ID = "$Id$";

    private static final long serialVersionUID = 1L;

    static final Logger log = LoggerFactory.getLogger(NodeNamePlugin.class);

    public NodeNamePlugin(PluginDescriptor pluginDescriptor, IPluginModel model, Plugin parentPlugin) {
        super(pluginDescriptor, model, parentPlugin);

        List<String> captions = pluginDescriptor.getParameter("caption").getStrings();
        if(captions != null && captions.size() > 0) {
            add(new Label("name", captions.get(0)));
        } else {
            add(new Label("name", ""));
        }

        JcrNodeModel nodeModel = new JcrNodeModel(model);
        try {
            add(new Label("field", nodeModel.getNode().getName()));
        } catch (RepositoryException e) {
            log.error("Could not retrieve name of node (" + nodeModel.getItemModel().getPath() + ")", e);
        }
    }

}
