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

import java.util.Iterator;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;

import org.apache.wicket.Session;
import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.frontend.model.event.IEvent;
import org.hippoecm.frontend.model.event.IObservable;
import org.hippoecm.frontend.model.event.IObserver;
import org.hippoecm.frontend.plugin.IPluginContext;
import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.hippoecm.frontend.service.EditorException;
import org.hippoecm.frontend.session.UserSession;
import org.hippoecm.repository.HippoStdNodeType;
import org.hippoecm.repository.api.HippoNodeType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class HippostdPublishableEditor extends AbstractCmsEditor<JcrNodeModel> {
    private static final long serialVersionUID = 1L;

    @SuppressWarnings("unused")
    private final static String SVN_ID = "$Id$";

    private static final Logger log = LoggerFactory.getLogger(HippostdPublishableEditor.class);

    private IObserver handleObserver;
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
        try {
            final JcrNodeModel handle = getModel();
            editorModel = getEditorModel();
            if (handle.getNode().isNodeType(HippoNodeType.NT_HANDLE)) {
                getPluginContext().registerService(handleObserver = new IObserver() {
                    private static final long serialVersionUID = 1L;

                    public IObservable getObservable() {
                        return handle;
                    }

                    public void onEvent(Iterator<? extends IEvent> event) {
                        try {
                            setMode(getMode(handle));
                            JcrNodeModel newModel = getEditorModel();
                            if (!newModel.equals(editorModel)) {
                                stop();
                                start();
                            }
                            return;
                        } catch (EditorException ex) {
                            log.warn("Could not update editor", ex);
                        } catch (CmsEditorException ex) {
                            log.warn("Could not update editor", ex);
                        }

                        try {
                            close();
                        } catch (EditorException ex) {
                            log.error("Could not close editor for empty handle");
                        }
                    }

                }, IObserver.class.getName());
            }
        } catch (RepositoryException ex) {
            log.error("Could not subscribe to parent model");
        }
    }

    @Override
    protected void stop() {
        if (handleObserver != null) {
            getPluginContext().unregisterService(handleObserver, IObserver.class.getName());
            handleObserver = null;
        }
        super.stop();
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
