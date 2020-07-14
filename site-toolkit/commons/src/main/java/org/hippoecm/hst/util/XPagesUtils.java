/*
 *  Copyright 2020 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.hst.util;

import javax.jcr.Node;
import javax.jcr.Property;
import javax.jcr.RepositoryException;
import javax.jcr.nodetype.NodeType;

import org.apache.jackrabbit.value.StringValue;
import org.hippoecm.repository.standardworkflow.JcrTemplateNode;
import org.hippoecm.repository.util.NodeIterable;
import org.hippoecm.repository.util.PropertyIterable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.hippoecm.hst.configuration.HstNodeTypes.MIXINTYPE_HST_XPAGE_MIXIN;
import static org.hippoecm.hst.configuration.HstNodeTypes.NODENAME_HST_XPAGE;
import static org.hippoecm.hst.configuration.HstNodeTypes.NODETYPE_HST_CONTAINERCOMPONENT;
import static org.hippoecm.hst.configuration.HstNodeTypes.NODETYPE_HST_XPAGE;
import static org.hippoecm.hst.configuration.HstNodeTypes.XPAGE_PROPERTY_PAGEREF;
import static org.hippoecm.repository.api.HippoNodeType.HIPPO_IDENTIFIER;

public class XPagesUtils {

    private final static Logger log = LoggerFactory.getLogger(XPagesUtils.class);

    public static JcrTemplateNode xpageAsJcrTemplate(final String xpageLayoutIdentifier) throws RepositoryException {
        // todo Use hst config user since read access everywhere...
       return null;
    }

    public static JcrTemplateNode xpageAsJcrTemplate(final Node xpageLayoutNode) throws RepositoryException {
        if (!xpageLayoutNode.isNodeType(NODETYPE_HST_XPAGE)) {
            throw new IllegalArgumentException(String.format("Node '%s' is an invalid XPage Layout", xpageLayoutNode.getPath()));
        }
        final JcrTemplateNode root = new JcrTemplateNode()
                .addMixinName(MIXINTYPE_HST_XPAGE_MIXIN);


        final JcrTemplateNode xpageTemplateNode = root.addChild(NODENAME_HST_XPAGE, NODETYPE_HST_XPAGE)
                .addSingleValuedProperty(XPAGE_PROPERTY_PAGEREF, new StringValue(xpageLayoutNode.getName()));

        // iterate through all descendants of 'xpageNode' and whenever a 'container' found, check the hst:qualifier
        // and if present, add a xpage container child with that name and copy the container items as JcrTemplateNode

        addContainers(xpageLayoutNode, xpageTemplateNode);

        return root;
    }

    private static void addContainers(final Node jcrNode, final JcrTemplateNode xpageTemplateNode) throws RepositoryException {
        if (jcrNode.isNodeType(NODETYPE_HST_CONTAINERCOMPONENT)) {
            if (!jcrNode.hasProperty(HIPPO_IDENTIFIER)) {
                log.warn("Skip container '{}' from XPage Layout since misses property '{}'", jcrNode.getPath(), HIPPO_IDENTIFIER);
                return;
            }
            final String hippoIdentifier = jcrNode.getProperty(HIPPO_IDENTIFIER).getString();
            final JcrTemplateNode container = xpageTemplateNode.addChild(hippoIdentifier, NODETYPE_HST_CONTAINERCOMPONENT);
            // that is all we need on the container. Now copy any descendant nodes from the XPage Layout container since these
            // are the prototype items
            for (Node child : new NodeIterable(jcrNode.getNodes())) {
                copyDescendants(child, container.addChild(child.getName(), child.getPrimaryNodeType().getName()));
            }
        } else {
            for (Node child : new NodeIterable(jcrNode.getNodes())) {
                addContainers(child, xpageTemplateNode);
            }
        }
    }

    private static void copyDescendants(final Node jcrNode, final JcrTemplateNode node) throws RepositoryException {

        for (NodeType nodeType : jcrNode.getMixinNodeTypes()) {
            node.addMixinName(nodeType.getName());
        }

        for (Property property : new PropertyIterable(jcrNode.getProperties())) {
            if (property.getDefinition().isProtected()) {
                continue;
            }
            if (property.isMultiple()) {
                node.addMultiValuedProperty(property.getName(), property.getValues());
            } else {
                node.addSingleValuedProperty(property.getName(), property.getValue());
            }
        }

        for (Node child : new NodeIterable(jcrNode.getNodes())) {
            copyDescendants(child, node.addChild(child.getName(), child.getPrimaryNodeType().getName()));
        }
    }
}
