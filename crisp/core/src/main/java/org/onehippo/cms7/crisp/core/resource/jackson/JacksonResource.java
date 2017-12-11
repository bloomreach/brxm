/*
 *  Copyright 2017 Hippo B.V. (http://www.onehippo.com)
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
package org.onehippo.cms7.crisp.core.resource.jackson;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.onehippo.cms7.crisp.api.resource.AbstractResource;
import org.onehippo.cms7.crisp.api.resource.Resource;
import org.onehippo.cms7.crisp.api.resource.ResourceCollection;
import org.onehippo.cms7.crisp.api.resource.ResourceException;
import org.onehippo.cms7.crisp.api.resource.ValueMap;
import org.onehippo.cms7.crisp.core.resource.DefaultValueMap;
import org.onehippo.cms7.crisp.core.resource.EmptyValueMap;
import org.onehippo.cms7.crisp.core.resource.ListResourceCollection;
import org.onehippo.cms7.crisp.core.resource.util.ResourceCollectionUtils;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

public class JacksonResource extends AbstractResource {

    private static final long serialVersionUID = 1L;

    private final JsonNode jsonNode;
    private ValueMap internalValueMap;
    private List<Resource> internalAllChildren;

    public JacksonResource(JsonNode jsonNode) {
        super(jsonNode.getNodeType().toString());
        this.jsonNode = jsonNode;
    }

    public JacksonResource(JsonNode jsonNode, String name) {
        super(jsonNode.getNodeType().toString(), name);
        this.jsonNode = jsonNode;
    }

    public JacksonResource(JacksonResource parent, JsonNode jsonNode, String name) {
        super(parent, jsonNode.getNodeType().toString(), name);
        this.jsonNode = jsonNode;
    }

    @Override
    public boolean isArray() {
        return jsonNode.isArray();
    }

    @Override
    public boolean isAnyChildContained() {
        final int size = jsonNode.size();

        if (jsonNode.isContainerNode() && size == 0) {
            return false;
        }

        if (jsonNode.isObject()) {
            return JacksonUtils.hasAnyContainerNodeField(jsonNode);
        }

        return size > 0;
    }

    @Override
    public long getChildCount() {
        final List<Resource> allChildren = getInternalAllChildren();
        return allChildren.size();
    }

    @Override
    public ResourceCollection getChildren(long offset, long limit) {
        final List<Resource> allChildren = getInternalAllChildren();
        return new ListResourceCollection(ResourceCollectionUtils.createSubList(allChildren, offset, limit));
    }

    @Override
    public ValueMap getMetadata() {
        return EmptyValueMap.getInstance();
    }

    @Override
    public ValueMap getValueMap() {
        return ((DefaultValueMap) getInternalValueMap()).toUnmodifiable();
    }

    @Override
    public boolean equals(Object o) {
        if (o == this) {
            return true;
        }

        if (o == null) {
            return false;
        }

        if (!(o instanceof JacksonResource)) {
            return false;
        }

        return Objects.equals(jsonNode, ((JacksonResource) o).jsonNode);
    }

    @Override
    public int hashCode() {
        return jsonNode.hashCode();
    }

    public String toJsonString(ObjectMapper mapper) {
        try {
            return mapper.writeValueAsString(jsonNode);
        } catch (JsonProcessingException e) {
            throw new ResourceException("JSON processing error.", e);
        }
    }

    JsonNode getJsonNode() {
        return jsonNode;
    }

    private ValueMap getInternalValueMap() {
        if (internalValueMap == null) {
            ValueMap tempValueMap = new DefaultValueMap();

            Map.Entry<String, JsonNode> entry;
            String fieldName;
            JsonNode fieldJsonNode;
            Object fieldValue;

            for (Iterator<Map.Entry<String, JsonNode>> it = jsonNode.fields(); it.hasNext();) {
                entry = it.next();
                fieldName = entry.getKey();
                fieldJsonNode = entry.getValue();

                if (fieldJsonNode.isContainerNode()) {
                    fieldValue = toChildFieldJacksonResource(fieldJsonNode, fieldName);
                } else {
                    fieldValue = JacksonUtils.getJsonScalaValue(fieldJsonNode);
                }

                tempValueMap.put(fieldName, fieldValue);
            }

            internalValueMap = tempValueMap;
        }

        return internalValueMap;
    }

    private List<Resource> getInternalAllChildren() {
        if (internalAllChildren == null) {
            List<Resource> list = new LinkedList<>();

            if (jsonNode.isObject()) {
                Map.Entry<String, JsonNode> entry;
                String name;
                JsonNode value;

                for (Iterator<Map.Entry<String, JsonNode>> it = jsonNode.fields(); it.hasNext();) {
                    entry = it.next();
                    name = entry.getKey();
                    value = entry.getValue();

                    if (value.isContainerNode()) {
                        list.add(toChildFieldJacksonResource(value, name));
                    }
                }
            } else if (jsonNode.isContainerNode()) {
                JsonNode value;
                int index = 0;

                for (Iterator<JsonNode> it = jsonNode.elements(); it.hasNext();) {
                    value = it.next();

                    if (value.isContainerNode()) {
                        list.add(this.toChildIndexedJacksonResource(value, ++index));
                    }
                }
            }

            internalAllChildren = list;
        }

        return internalAllChildren;
    }

    private JacksonResource toChildFieldJacksonResource(JsonNode jsonNode, String fieldName) {
        return new JacksonResource(this, jsonNode, fieldName);
    }

    private JacksonResource toChildIndexedJacksonResource(JsonNode jsonNode, int index) {
        return new JacksonResource(this, jsonNode, "[" + index + "]");
    }

}
