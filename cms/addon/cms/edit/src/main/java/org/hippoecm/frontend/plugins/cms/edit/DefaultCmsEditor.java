/*
 *  Copyright 2009 Hippo.
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

import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.frontend.plugin.IPluginContext;
import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.hippoecm.frontend.service.EditorException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class DefaultCmsEditor extends AbstractCmsEditor<JcrNodeModel> {
    private static final long serialVersionUID = 1L;

    @SuppressWarnings("unused")
    private final static String SVN_ID = "$Id$";

    private static final Logger log = LoggerFactory.getLogger(DefaultCmsEditor.class);

    DefaultCmsEditor(EditorManagerPlugin manager, IPluginContext context, IPluginConfig config, JcrNodeModel model,
            Mode mode) throws CmsEditorException {
        super(manager, context, config, model, mode);
    }

    @Override
    void refresh() {
        JcrNodeModel nodeModel = (JcrNodeModel) getModel();

        // close editor if model no longer exists
        if (!nodeModel.getItemModel().exists()) {
            try {
                close();
            } catch (EditorException ex) {
                log.warn("failed to close editor for non-existing document");
            }
        }
    }

}
