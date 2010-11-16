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
package org.hippoecm.frontend.editor;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;

import org.apache.wicket.model.IModel;
import org.hippoecm.frontend.plugin.IPluginContext;
import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.hippoecm.frontend.service.EditorException;
import org.hippoecm.repository.api.HippoNodeType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class TemplateTypeEditor extends AbstractCmsEditor<Node> {
    private static final long serialVersionUID = 1L;

    @SuppressWarnings("unused")
    private final static String SVN_ID = "$Id$";

    private static final Logger log = LoggerFactory.getLogger(TemplateTypeEditor.class);

    TemplateTypeEditor(IEditorContext manager, IPluginContext context, IPluginConfig config, IModel<Node> model,
            Mode mode) throws EditorException {
        super(manager, context, config, model, mode);
    }

    @Override
    public void refresh() {
        Node node = getModel().getObject();
        if (node == null) {
            try {
                close();
            } catch (EditorException e) {
                log.error("Could not close editor for null node", e);
            }
            return;
        }
        try {
            if (node.hasNode(HippoNodeType.HIPPO_PROTOTYPES)) {
                NodeIterator prototypes = node.getNode(HippoNodeType.HIPPO_PROTOTYPES).getNodes();
                while (prototypes.hasNext()) {
                    Node prototype = prototypes.nextNode();
                    if (prototype.isNodeType("nt:unstructured")) {
                        // any mode is fine
                        return;
                    }
                }

                // no draft present
                setMode(Mode.VIEW);
            }
        } catch (EditorException e) {
            log.warn("Could not set editor mode", e);
        } catch (RepositoryException e) {
            log.error("Error determining node type", e);
        }
    }

}
