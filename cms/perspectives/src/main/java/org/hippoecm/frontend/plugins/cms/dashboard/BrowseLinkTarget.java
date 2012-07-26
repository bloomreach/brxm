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
package org.hippoecm.frontend.plugins.cms.dashboard;

import java.util.Iterator;

import javax.jcr.ItemNotFoundException;
import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Value;

import org.apache.commons.lang.StringUtils;
import org.hippoecm.frontend.i18n.model.NodeTranslator;
import org.hippoecm.frontend.model.JcrItemModel;
import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.frontend.model.event.IEvent;
import org.hippoecm.frontend.model.event.IObservationContext;
import org.hippoecm.frontend.model.ocm.JcrObject;
import org.hippoecm.repository.api.HippoNodeType;
import org.hippoecm.repository.api.NodeNameCodec;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BrowseLinkTarget extends JcrObject {

    private static final long serialVersionUID = 1L;

    static final Logger log = LoggerFactory.getLogger(BrowseLinkTarget.class);

    public BrowseLinkTarget(String path) {
        super(resolvePath(path));
    }

    public String getName() {
        return (String) new NodeTranslator(getNodeModel()).getNodeName().getObject();
    }

    public String getDisplayPath() {
        String path;
        try {
            Node node = getNode();
            path = node.getPath();
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
        // not implemented
    }

    static JcrNodeModel resolvePath(String docPath) {
        JcrNodeModel model = new JcrNodeModel(docPath);
        Node node = model.getNode();
        if (node != null) {
            try {
                while (!node.isNodeType(HippoNodeType.NT_HANDLE) && !node.isNodeType("hippostd:folder")
                        && !node.isNodeType("nt:version") && !node.getPath().equals("/")) {
                    node = node.getParent();
                }
                if (node.isNodeType("nt:version")) {
                    Node frozen = node.getNode("jcr:frozenNode");
                    Node result = node.getSession().getRootNode();
                    // use hippo:paths to find handle; then use the matching variant 
                    if (frozen.hasProperty(HippoNodeType.HIPPO_PATHS)) {
                        Value[] paths = frozen.getProperty(HippoNodeType.HIPPO_PATHS).getValues();
                        if (paths.length > 1) {
                            String handleUuid = paths[1].getString();
                            result = node.getSession().getNodeByUUID(handleUuid);
                        }
                    }
                    node = result;
                }
                return new JcrNodeModel(node);
            } catch (RepositoryException e) {
                log.error(e.getMessage(), e);
            }
        }
        return model;
    }

}
