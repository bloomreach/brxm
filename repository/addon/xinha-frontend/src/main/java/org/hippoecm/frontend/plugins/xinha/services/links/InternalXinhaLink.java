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

package org.hippoecm.frontend.plugins.xinha.services.links;

import java.util.Map;

import javax.jcr.Item;
import javax.jcr.Node;
import javax.jcr.PathNotFoundException;
import javax.jcr.RepositoryException;

import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.frontend.plugins.xinha.XinhaUtil;
import org.hippoecm.repository.api.HippoNodeType;

public class InternalXinhaLink extends XinhaLink {
    private static final long serialVersionUID = 1L;

    @SuppressWarnings("unused")
    private final static String SVN_ID = "$Id$";
    
    public InternalXinhaLink(Map<String, String> values, JcrNodeModel parentModel) {
        super(values, parentModel);
    }

    @Override
    protected JcrNodeModel createInitialModel(JcrNodeModel parentModel) {
        if (parentModel == null) {
            return null;
        }
        String relPath = getHref();
        if (relPath != null && !"".equals(relPath)) {
            relPath = XinhaUtil.decode(relPath);
            try {
                Node node = parentModel.getNode();
                if (node.hasNode(relPath)) {
                    Node linkNode = node.getNode(relPath);
                    if (linkNode.isNodeType(HippoNodeType.NT_FACETSELECT)) {
                        String uuid = linkNode.getProperty(HippoNodeType.HIPPO_DOCBASE).getValue().getString();
                        Item item = node.getSession().getNodeByUUID(uuid);
                        if (item != null) {
                            return new JcrNodeModel(item.getPath());
                        }
                    }
                }
            } catch (PathNotFoundException e) {
                log.error("Error finding facet node for relative path " + relPath, e);
            } catch (RepositoryException e) {
                log.error("Error finding facet node for relative path " + relPath, e);
            }
        }
        return null;
    }
}
