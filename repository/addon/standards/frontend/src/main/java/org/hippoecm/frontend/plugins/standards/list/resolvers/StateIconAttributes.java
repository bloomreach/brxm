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
package org.hippoecm.frontend.plugins.standards.list.resolvers;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.nodetype.NodeType;
import javax.jcr.nodetype.NodeTypeManager;

import org.apache.wicket.model.IDetachable;
import org.apache.wicket.model.IModel;
import org.hippoecm.frontend.i18n.types.TypeTranslator;
import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.frontend.model.nodetypes.JcrNodeTypeModel;
import org.hippoecm.frontend.model.properties.JcrPropertyModel;
import org.hippoecm.frontend.model.properties.JcrPropertyValueModel;
import org.hippoecm.repository.HippoStdNodeType;
import org.hippoecm.repository.api.HippoNodeType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Standard attributes of a hippostd:publishable document.  Figures out what css classes
 * should be used to represent the state.  Can be used with handles, documents and (document)
 * versions.
 */
public class StateIconAttributes implements IDetachable {
    @SuppressWarnings("unused")
    private final static String SVN_ID = "$Id$";

    private static final long serialVersionUID = 1L;

    static final Logger log = LoggerFactory.getLogger(StateIconAttributes.class);

    private JcrNodeModel nodeModel;
    private transient String cssClass;
    private transient String summary;
    private transient boolean loaded = false;

    public StateIconAttributes(JcrNodeModel nodeModel) {
        this.nodeModel = nodeModel;
    }

    public String getSummary() {
        load();
        return summary;
    }

    public String getCssClass() {
        load();
        return cssClass;
    }

    public void detach() {
        loaded = false;
        summary = null;
        cssClass = null;
        nodeModel.detach();
    }

    void load() {
        if (!loaded) {
            try {
                Node node = nodeModel.getNode();
                if (node != null) {
                    Node document = null;
                    NodeType primaryType = null;
                    if (node.isNodeType(HippoNodeType.NT_HANDLE)) {
                        document = node.getNode(node.getName());
                        primaryType = document.getPrimaryNodeType();
                    } else if (node.isNodeType(HippoNodeType.NT_DOCUMENT)) {
                        document = node;
                        primaryType = document.getPrimaryNodeType();
                    } else if (node.isNodeType("nt:version")) {
                        Node frozen = node.getNode("jcr:frozenNode");
                        String primary = frozen.getProperty("jcr:frozenPrimaryType").getString();
                        NodeTypeManager ntMgr = frozen.getSession().getWorkspace().getNodeTypeManager();
                        primaryType = ntMgr.getNodeType(primary);
                        if (primaryType.isNodeType(HippoNodeType.NT_DOCUMENT)) {
                            document = frozen;
                        }
                    }
                    if (document != null) {
                        if (primaryType.isNodeType(HippoStdNodeType.NT_PUBLISHABLESUMMARY)
                                || document.isNodeType(HippoStdNodeType.NT_PUBLISHABLESUMMARY)) {
                            cssClass = StateIconAttributeModifier.PREFIX
                                    + document.getProperty(HippoStdNodeType.HIPPOSTD_STATESUMMARY).getString()
                                    + StateIconAttributeModifier.SUFFIX;
                            IModel stateModel = new JcrPropertyValueModel(new JcrPropertyModel(document
                                    .getProperty(HippoStdNodeType.HIPPOSTD_STATESUMMARY)));
                            summary = (String) new TypeTranslator(new JcrNodeTypeModel(
                                    HippoStdNodeType.NT_PUBLISHABLESUMMARY)).getValueName(
                                    HippoStdNodeType.HIPPOSTD_STATESUMMARY, stateModel).getObject();
                        }
                    }
                }
            } catch (RepositoryException ex) {
                log.error("Unable to obtain state properties", ex);
            }
            loaded = true;
        }
    }
}
