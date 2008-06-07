/*
 * Copyright 2007 Hippo
 *
 * Licensed under the Apache License, Version 2.0 (the  "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.hippoecm.frontend.plugins.cms.edit.sa;

import javax.jcr.RepositoryException;

import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.frontend.plugins.standards.perspective.Perspective;
import org.hippoecm.frontend.sa.plugin.IPluginContext;
import org.hippoecm.frontend.sa.plugin.config.IPluginConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class EditPerspective extends Perspective {
    private static final long serialVersionUID = 1L;

    private static final Logger log = LoggerFactory.getLogger(EditPerspective.class);

    public EditPerspective(IPluginContext context, IPluginConfig properties) {
        super(context, properties);

        for (String extension : new String[] { "editorPlugin", "workflowPlugin" }) {
            addExtensionPoint(extension);
        }

        onModelChanged();
    }

    @Override
    public void onModelChanged() {
        JcrNodeModel nodeModel = (JcrNodeModel) getModel();
        try {
            if (nodeModel != null && nodeModel.getNode() != null) {
                setTitle(nodeModel.getNode().getName());
            }
        } catch (RepositoryException ex) {
            log.error(ex.getMessage());
        }
    }

}
