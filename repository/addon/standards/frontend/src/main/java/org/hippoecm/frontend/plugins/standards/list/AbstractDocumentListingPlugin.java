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
package org.hippoecm.frontend.plugins.standards.list;

import java.util.ArrayList;
import java.util.List;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;

import org.apache.wicket.markup.repeater.data.IDataProvider;
import org.apache.wicket.markup.repeater.data.ListDataProvider;
import org.apache.wicket.model.IModel;
import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.frontend.plugin.IPluginContext;
import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.hippoecm.repository.api.HippoNodeType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class AbstractDocumentListingPlugin extends AbstractListingPlugin {
    @SuppressWarnings("unused")
    private final static String SVN_ID = "$Id$";

    private static final long serialVersionUID = 1L;
    protected static final Logger log = LoggerFactory.getLogger(AbstractDocumentListingPlugin.class);

    public AbstractDocumentListingPlugin(IPluginContext context, IPluginConfig config) {
        super(context, config);
    }

    @Override
    protected IDataProvider getRows() {
        JcrNodeModel model = (JcrNodeModel) getModel();
        final List<IModel> entries = new ArrayList<IModel>();
        Node node = (Node) model.getNode();
        try {
            while (node != null) {
                if (!(node.isNodeType(HippoNodeType.NT_DOCUMENT) && !node.isNodeType("hippostd:folder"))
                        && !node.isNodeType(HippoNodeType.NT_HANDLE) && !node.isNodeType(HippoNodeType.NT_TEMPLATETYPE)
                        && !node.isNodeType(HippoNodeType.NT_REQUEST) && !node.isNodeType("rep:root")) {
                    NodeIterator childNodesIterator = node.getNodes();
                    while (childNodesIterator.hasNext()) {
                        entries.add(new JcrNodeModel(childNodesIterator.nextNode()));
                    }
                    break;
                }
                if (!node.isNodeType("rep:root")) {
                    model = model.getParentModel();
                    node = model.getNode();
                } else {
                    break;
                }
            }
        } catch (RepositoryException e) {
            log.error(e.getMessage());
        }

        return new ListDataProvider(entries) {
            private static final long serialVersionUID = 1L;
            @Override
            public void detach() {
                for (IModel entry : entries) {
                    entry.detach();
                }
                super.detach();
            }
        };
    }

}
