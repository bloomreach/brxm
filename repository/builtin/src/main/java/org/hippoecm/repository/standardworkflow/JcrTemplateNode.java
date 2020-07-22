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
package org.hippoecm.repository.standardworkflow;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.jcr.Value;

public class JcrTemplateNode {

    private String nodeName;
    private String primaryNodeType;

    private Set<String> mixinNames = new HashSet<>();
    private Map<String, Value> singleValuedProperties = new HashMap<>();
    private Map<String, Value[]> multiValuedProperties = new HashMap<>();
    private List<JcrTemplateNode> children = new ArrayList<>();


    /**
     * Seals the jcrTemplateNode, aka make it immutable including all children
     * @param jcrTemplateNode
     */
    public static void seal(JcrTemplateNode jcrTemplateNode) {
        jcrTemplateNode.mixinNames = Collections.unmodifiableSet(jcrTemplateNode.mixinNames);
        jcrTemplateNode.singleValuedProperties = Collections.unmodifiableMap(jcrTemplateNode.singleValuedProperties);
        // ideally the 'Value[]' arrays would also be immutable
        jcrTemplateNode.multiValuedProperties = Collections.unmodifiableMap(jcrTemplateNode.multiValuedProperties);
        jcrTemplateNode.children = Collections.unmodifiableList(jcrTemplateNode.children);

        for (JcrTemplateNode child : jcrTemplateNode.children) {
            seal(child);
        }
    }

    public JcrTemplateNode() {}

    public String getNodeName() {
        return nodeName;
    }

    public String getPrimaryNodeType() {
        return primaryNodeType;
    }

    public Set<String> getMixinNames() {
        return mixinNames;
    }

    public Map<String, Value> getSingleValuedProperties() {
        return singleValuedProperties;
    }

    public Map<String, Value[]> getMultiValuedProperties() {
        return multiValuedProperties;
    }

    public List<JcrTemplateNode> getChildren() {
        return children;
    }

    /**
     * @param mixinName
     * @return this instance
     */
    public JcrTemplateNode addMixinName(final String mixinName) {
        mixinNames.add(mixinName);
        return this;
    }

    /**
     *
     * @param propertyName
     * @param value
     * @return this instance
     */
    public JcrTemplateNode addSingleValuedProperty(final String propertyName, final Value value) {
        singleValuedProperties.put(propertyName, value);
        return this;
    }

    /**
     * @param propertyName
     * @param values
     * @return this instance
     */
    public JcrTemplateNode addMultiValuedProperty(final String propertyName, final Value[] values) {
        multiValuedProperties.put(propertyName, values);
        return this;
    }

    /**
     *
     * @param nodeName
     * @param primaryNodeType
     * @return return the JcrTemplateNode for the newly created Child
     */
    public JcrTemplateNode addChild(final String nodeName, final String primaryNodeType) {
        JcrTemplateNode child = new JcrTemplateNode();
        child.nodeName = nodeName;
        child.primaryNodeType = primaryNodeType;
        children.add(child);
        return child;
    }

}
