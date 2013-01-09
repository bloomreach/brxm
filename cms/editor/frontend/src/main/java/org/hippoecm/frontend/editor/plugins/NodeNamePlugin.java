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
package org.hippoecm.frontend.editor.plugins;

import java.util.Arrays;
import java.util.List;

import javax.jcr.RepositoryException;

import org.apache.wicket.markup.html.basic.Label;
import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.frontend.plugin.IPluginContext;
import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.hippoecm.frontend.service.render.RenderPlugin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class NodeNamePlugin extends RenderPlugin {

    private static final long serialVersionUID = 1L;

    static final Logger log = LoggerFactory.getLogger(NodeNamePlugin.class);

    public NodeNamePlugin(IPluginContext context, IPluginConfig config) {
        super(context, config);

        List<String> captions = Arrays.asList(config.getStringArray("caption"));
        if(captions != null && captions.size() > 0) {
            add(new Label("name", captions.get(0)));
        } else {
            add(new Label("name", ""));
        }

        JcrNodeModel nodeModel = (JcrNodeModel)getDefaultModel();
        try {
            add(new Label("field", nodeModel.getNode().getName()));
        } catch (RepositoryException e) {
            log.error("Could not retrieve name of node (" + nodeModel.getItemModel().getPath() + ")", e);
        }
    }

}
