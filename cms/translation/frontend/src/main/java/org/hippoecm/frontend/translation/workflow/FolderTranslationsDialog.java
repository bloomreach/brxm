/*
 *  Copyright 2010-2013 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.frontend.translation.workflow;

import javax.jcr.Node;
import javax.jcr.RepositoryException;

import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LoadableDetachableModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.util.value.IValueMap;
import org.apache.wicket.util.value.ValueMap;
import org.hippoecm.addon.workflow.AbstractWorkflowDialog;
import org.hippoecm.addon.workflow.IWorkflowInvoker;
import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.frontend.translation.ILocaleProvider;
import org.hippoecm.frontend.translation.components.folder.FolderTranslationView;
import org.hippoecm.frontend.translation.components.folder.model.JcrT9Tree;
import org.hippoecm.frontend.translation.components.folder.model.T9Node;
import org.hippoecm.frontend.translation.components.folder.model.T9Tree;
import org.hippoecm.repository.translation.HippoTranslationNodeType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FolderTranslationsDialog extends AbstractWorkflowDialog<Node> {

    private static final long serialVersionUID = 1L;

    static final Logger log = LoggerFactory.getLogger(FolderTranslationsDialog.class);

    private IModel<String> title;

    private IModel<T9Node> t9NodeModel;

    public FolderTranslationsDialog(IWorkflowInvoker action, IModel<String> title, IModel<Node> folderModel,
            ILocaleProvider provider) {
        super(folderModel, action);

        this.title = title;

        IModel<T9Tree> treeModel = new LoadableDetachableModel<T9Tree>() {
            private static final long serialVersionUID = 1L;

            @Override
            protected T9Tree load() {
                return new JcrT9Tree(FolderTranslationsDialog.this.getModelObject());
            }
        };
        try {
            Node node = folderModel.getObject();
            if (!node.isNodeType(HippoTranslationNodeType.NT_TRANSLATED)) {
                throw new RuntimeException("invalid folder model");
            }
            t9NodeModel = new Model<T9Node>(treeModel.getObject().getNode(node.getIdentifier()));
        } catch (RepositoryException ex) {
            throw new RuntimeException("error determining node type of folder node", ex);
        }
        add(new FolderTranslationView("folder-translations-view", treeModel, t9NodeModel, provider));
    }

    @Override
    protected void onOk() {
        Node node = getModelObject();
        if (node != null) {
            try {
                node.setProperty(HippoTranslationNodeType.ID, t9NodeModel.getObject().getT9id());
                node.getSession().save();
                node.getSession().refresh(false);
            } catch (RepositoryException e) {
                log.error("could not set property hippotranslation:id for linked folder "
                        + new JcrNodeModel(node).getItemModel().getPath(), e);
            }
        } else {
            log.warn("Could not store folder links");
        }
//        super.onOk();
    }

    public IModel getTitle() {
        return title;
    }

    @Override
    public IValueMap getProperties() {
        return new ValueMap("width=680,height=388").makeImmutable();
    }

    @Override
    protected void onDetach() {
        title.detach();
        super.onDetach();
    }
}
