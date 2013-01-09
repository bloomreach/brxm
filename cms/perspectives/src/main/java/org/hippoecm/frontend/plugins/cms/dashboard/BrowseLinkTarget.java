/*
 *  Copyright 2009-2013 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.frontend.plugins.cms.dashboard;

import java.util.Iterator;

import javax.jcr.ItemNotFoundException;
import javax.jcr.Node;
import javax.jcr.RepositoryException;

import org.apache.commons.lang.StringUtils;
import org.apache.wicket.model.IModel;
import org.hippoecm.frontend.i18n.model.NodeTranslator;
import org.hippoecm.frontend.model.JcrItemModel;
import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.frontend.model.event.IEvent;
import org.hippoecm.frontend.model.event.IObservationContext;
import org.hippoecm.frontend.model.ocm.JcrObject;
import org.hippoecm.frontend.session.UserSession;
import org.hippoecm.repository.api.HippoNodeType;
import org.hippoecm.repository.api.NodeNameCodec;
import org.hippoecm.repository.util.JcrUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BrowseLinkTarget extends JcrObject {

    private static final long serialVersionUID = 1L;

    private static final Logger log = LoggerFactory.getLogger(BrowseLinkTarget.class);

    public BrowseLinkTarget(String path) throws RepositoryException {
        super(resolveModel(path));
    }

    public String getName() {
        return new NodeTranslator(getNodeModel()).getNodeName().getObject();
    }

    public String getDisplayPath() {
        String path;
        try {
            path = getNode().getPath();
        } catch (ItemNotFoundException e) {
            path = getNodeModel().getItemModel().getPath();
        } catch (RepositoryException e) {
            log.error(e.getMessage(), e);
            path = getNodeModel().getItemModel().getPath();
        }

        String[] elements = StringUtils.splitPreserveAllTokens(path, '/');
        for (int i = 0; i < elements.length; i++) {
            elements[i] = NodeNameCodec.decode(elements[i]);
        }
        return StringUtils.join(elements, '/');
    }

    public JcrNodeModel getBrowseModel() {
        JcrItemModel itemModel = getNodeModel().getItemModel();
        while (itemModel != null && !itemModel.exists()) {
            itemModel = itemModel.getParentModel();
        }
        if (itemModel == null) {
            return null;
        }
        return new JcrNodeModel(itemModel);
    }

    @Override
    protected void processEvents(IObservationContext context, Iterator<? extends IEvent> events) {
    }

    private static IModel<Node> resolveModel(String docPath) throws RepositoryException {
        Node node = JcrUtils.getNodeIfExists(docPath, UserSession.get().getJcrSession());
        if (node != null) {
            node = findFirstBrowsableAncestor(node);
            return new JcrNodeModel(node);
        }
        return new JcrNodeModel(docPath);
    }

    private static Node findFirstBrowsableAncestor(Node node) throws RepositoryException {
        while (!isBrowsableNode(node)) {
            node = node.getParent();
        }
        return node;
    }

    private static boolean isBrowsableNode(final Node node) throws RepositoryException {
        return node.isNodeType(HippoNodeType.NT_HANDLE) || node.isNodeType("hippostd:folder") || node.getPath().equals("/");
    }

}
