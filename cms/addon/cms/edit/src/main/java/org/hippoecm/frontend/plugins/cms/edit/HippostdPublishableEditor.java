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

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;

import org.apache.wicket.Session;
import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.frontend.plugin.IPluginContext;
import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.hippoecm.frontend.service.EditorException;
import org.hippoecm.frontend.session.UserSession;
import org.hippoecm.repository.HippoStdNodeType;
import org.hippoecm.repository.api.HippoNodeType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * An editor that takes a hippo:handle for its JcrNodeModel and displays one of the variants.
 * The variant documents must be of type hippostd:publishable.
 * <p>
 * Algorithm to determine what is shown:
 * <code>
 * when draft exists:
 *   show draft in edit mode
 * else:
 *   when unpublished exists:
 *     show unpublished in preview mode
 *   else
 *     show published in preview mode
 * </code>
 * <p>
 * The editor model is the variant that is shown.
 */
class HippostdPublishableEditor extends AbstractCmsEditor<JcrNodeModel> {
    private static final long serialVersionUID = 1L;

    @SuppressWarnings("unused")
    private final static String SVN_ID = "$Id$";

    private static final Logger log = LoggerFactory.getLogger(HippostdPublishableEditor.class);

    private JcrNodeModel editorModel;

    HippostdPublishableEditor(final EditorManagerPlugin manager, IPluginContext context, IPluginConfig config,
            JcrNodeModel model) throws CmsEditorException {
        super(manager, context, config, model, getMode(model));
    }

    @Override
    protected JcrNodeModel getEditorModel() {
        switch(getMode()) {
        case EDIT:
            return getDraftModel(super.getEditorModel());
        case VIEW:
        default:
            return getPreviewModel(super.getEditorModel());
        }
    }

    @Override
    protected void start() throws CmsEditorException {
        super.start();
        editorModel = getEditorModel();
    }

    @Override
    void refresh() {
        final JcrNodeModel handle = getModel();
        // verify that a document exists, i.e. the document has not been deleted
        try {
            Mode newMode = getMode(handle);
            if (newMode != super.getMode()) {
                setMode(newMode);
            } else {
                JcrNodeModel newModel = getEditorModel();
                if (!newModel.equals(editorModel)) {
                    stop();
                    start();
                }
            }
        } catch (EditorException ex) {
            log.error("Could not close editor for empty handle");
        } catch (CmsEditorException ex) {
            try {
                close();
            } catch (EditorException ex2) {
                log.error("Could not close editor for empty handle");
            }
        }
    }

    static Mode getMode(JcrNodeModel nodeModel) throws CmsEditorException {
        // select draft if it exists
        JcrNodeModel draftDocument = getDraftModel(nodeModel);
        if (draftDocument != null) {
            return Mode.EDIT;
        }

        // show preview
        JcrNodeModel previewDocument = getPreviewModel(nodeModel);
        if (previewDocument != null) {
            return Mode.VIEW;
        }

        throw new CmsEditorException("unable to find draft or unpublished variants");
    }

    static JcrNodeModel getPreviewModel(JcrNodeModel handle) {
        try {
            Node handleNode = handle.getNode();
            Node published = null;
            if (handleNode.isNodeType(HippoNodeType.NT_HANDLE)) {
                for (NodeIterator iter = handleNode.getNodes(); iter.hasNext();) {
                    Node child = iter.nextNode();
                    if (child.getName().equals(handleNode.getName())) {
                        if (child.hasProperty(HippoStdNodeType.HIPPOSTD_STATE)) {
                            String state = child.getProperty(HippoStdNodeType.HIPPOSTD_STATE).getString();
                            if (state.equals(HippoStdNodeType.UNPUBLISHED)) {
                                return new JcrNodeModel(child);
                            } else if (state.equals(HippoStdNodeType.PUBLISHED)) {
                                published = child;
                            }
                        } else {
                            published = child;
                        }
                    }
                }
                if (published != null) {
                    return new JcrNodeModel(published);
                }
            } else {
                return handle;
            }
        } catch (RepositoryException ex) {
            log.error(ex.getMessage());
        }
        return null;
    }

    static JcrNodeModel getDraftModel(JcrNodeModel handle) {
        String user = ((UserSession) Session.get()).getCredentials().getString("username");
        try {
            Node handleNode = handle.getNode();
            if (handleNode.isNodeType(HippoNodeType.NT_HANDLE)) {
                for (NodeIterator iter = handleNode.getNodes(); iter.hasNext();) {
                    Node child = iter.nextNode();
                    if (child.getName().equals(handleNode.getName())) {
                        if (child.hasProperty(HippoStdNodeType.HIPPOSTD_STATE)
                                && child.getProperty(HippoStdNodeType.HIPPOSTD_STATE).getString().equals(HippoStdNodeType.DRAFT)
                                && child.getProperty(HippoStdNodeType.HIPPOSTD_HOLDER).getString().equals(user)) {
                            return new JcrNodeModel(child);
                        }
                    }
                }
            }
        } catch (RepositoryException ex) {
            log.error(ex.getMessage());
        }
        return null;
    }

}
