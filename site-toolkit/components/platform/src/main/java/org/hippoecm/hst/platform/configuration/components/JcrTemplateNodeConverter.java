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
package org.hippoecm.hst.platform.configuration.components;

import java.util.Arrays;
import java.util.Map;

import javax.jcr.Value;
import javax.jcr.ValueFactory;

import org.apache.jackrabbit.value.StringValue;
import org.apache.jackrabbit.value.ValueFactoryImpl;
import org.hippoecm.hst.configuration.model.HstNode;
import org.hippoecm.repository.standardworkflow.JcrTemplateNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.hippoecm.hst.configuration.HstNodeTypes.MIXINTYPE_HST_XPAGE_MIXIN;
import static org.hippoecm.hst.configuration.HstNodeTypes.NODENAME_HST_XPAGE;
import static org.hippoecm.hst.configuration.HstNodeTypes.NODETYPE_HST_CONTAINERCOMPONENT;
import static org.hippoecm.hst.configuration.HstNodeTypes.NODETYPE_HST_XPAGE;
import static org.hippoecm.hst.configuration.HstNodeTypes.XPAGE_PROPERTY_PAGEREF;
import static org.hippoecm.repository.api.HippoNodeType.HIPPO_IDENTIFIER;

public class JcrTemplateNodeConverter {

    private final static Logger log = LoggerFactory.getLogger(JcrTemplateNodeConverter.class);
    private final static ValueFactory valueFactory = ValueFactoryImpl.getInstance();


    /**
     * @param xpageLayoutHstNode
     * @return immutable JcrTemplateNode
     */
    public static JcrTemplateNode getXPageLaoutAsJcrTemplate(final HstNode xpageLayoutHstNode) {
        if (!NODETYPE_HST_XPAGE.equals(xpageLayoutHstNode.getNodeTypeName())) {
            throw new IllegalArgumentException(String.format("Node '%s' is an invalid XPage Layout", xpageLayoutHstNode.getValueProvider().getPath()));
        }
        // the root will get mixin "hst:xpagemixin" since that is the mixin that needs to be added on the
        // xpage document variant
        final JcrTemplateNode root = new JcrTemplateNode()
                .addMixinName(MIXINTYPE_HST_XPAGE_MIXIN);


        final JcrTemplateNode xpageTemplateNode = root.addChild(NODENAME_HST_XPAGE, NODETYPE_HST_XPAGE)
                .addSingleValuedProperty(XPAGE_PROPERTY_PAGEREF, new StringValue(xpageLayoutHstNode.getName()));

        // iterate through all descendants of 'xpageNode' and whenever a 'container' found, check the hst:qualifier
        // and if present, add a xpage container child with that name and copy the container items as JcrTemplateNode

        addContainers(xpageLayoutHstNode, xpageTemplateNode);

        JcrTemplateNode.seal(root);
        return root;
    }

    private static void addContainers(final HstNode hstNode, final JcrTemplateNode xpageTemplateNode) {
        if (NODETYPE_HST_CONTAINERCOMPONENT.equals(hstNode.getNodeTypeName())) {
            if (!hstNode.getValueProvider().hasProperty(HIPPO_IDENTIFIER)) {
                log.warn("Skip container '{}' from XPage Layout since misses property '{}'", hstNode.getValueProvider().getPath(), HIPPO_IDENTIFIER);
                return;
            }
            final String hippoIdentifier = hstNode.getValueProvider().getString(HIPPO_IDENTIFIER);
            final JcrTemplateNode container = xpageTemplateNode.addChild(hippoIdentifier, NODETYPE_HST_CONTAINERCOMPONENT);
            // that is all we need on the container. Now copy any descendant nodes from the XPage Layout container since these
            // are the prototype items
            for (HstNode child : hstNode.getNodes()) {
                copyDescendants(child, container.addChild(child.getName(), child.getNodeTypeName()));
            }
        } else {
            for (HstNode child : hstNode.getNodes()) {
                addContainers(child, xpageTemplateNode);
            }
        }
    }

    private static void copyDescendants(final HstNode hstNode, final JcrTemplateNode node) {

        // TODO support mixins!
//        for (NodeType nodeType : jcrNode.getMixinNodeTypes()) {
//            node.addMixinName(nodeType.getName());
//        }

        // hst config nodes for components only String and Booleans are used so only need those
        final Map<String, Boolean[]> booleanArrays = hstNode.getValueProvider().getPropertyMap().getBooleanArrays();

        booleanArrays.forEach((propertyName, booleans) -> {
            final Value[] values = Arrays.stream(booleans).map(b -> valueFactory.createValue(b)).toArray(Value[]::new);
            node.addMultiValuedProperty(propertyName, values);
        });

        final Map<String, String[]> stringArrays = hstNode.getValueProvider().getPropertyMap().getStringArrays();

        stringArrays.forEach((propertyName, strings) -> {
            final Value[] values = Arrays.stream(strings).map(s -> valueFactory.createValue(s)).toArray(Value[]::new);
            node.addMultiValuedProperty(propertyName, values);
        });

        final Map<String, String> strings = hstNode.getValueProvider().getPropertyMap().getStrings();

        strings.forEach((propertyName, s) -> node.addSingleValuedProperty(propertyName, valueFactory.createValue(s)));

        final Map<String, Boolean> booleans = hstNode.getValueProvider().getPropertyMap().getBooleans();

        booleans.forEach((propertyName, b) -> node.addSingleValuedProperty(propertyName, valueFactory.createValue(b)));

        for (HstNode child : hstNode.getNodes()) {
            copyDescendants(child, node.addChild(child.getName(), child.getNodeTypeName()));
        }
    }
}
