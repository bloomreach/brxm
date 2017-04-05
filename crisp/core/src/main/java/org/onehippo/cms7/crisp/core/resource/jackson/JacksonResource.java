package org.onehippo.cms7.crisp.core.resource.jackson;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.apache.commons.collections.Transformer;
import org.onehippo.cms7.crisp.api.resource.AbstractResource;
import org.onehippo.cms7.crisp.api.resource.Resource;
import org.onehippo.cms7.crisp.api.resource.ResourceException;
import org.onehippo.cms7.crisp.api.resource.ValueMap;
import org.onehippo.cms7.crisp.core.resource.EmptyValueMap;
import org.onehippo.cms7.crisp.core.resource.LazyValueMap;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

public class JacksonResource extends AbstractResource {

    private final JsonNode jsonNode;
    private ValueMap valueMap;

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
    public boolean hasChildren() {
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
    public Iterator<Resource> listChildren() {
        if (jsonNode.isObject()) {
            return new JsonFieldChildResourceIterator(this, jsonNode);
        } else {
            return new JsonElementChildResourceIterator(this, jsonNode);
        }
    }

    @Override
    public Iterable<Resource> getChildren() {
        return new JsonNodeIterable(this);
    }

    @Override
    public ValueMap getMetadata() {
        return EmptyValueMap.getInstance();
    }

    @Override
    public ValueMap getValueMap() {
        if (valueMap == null) {
            valueMap = LazyValueMap.decorate(new HashMap<String, Object>(), new Transformer() {
                @Override
                public Object transform(Object input) {
                    if (jsonNode.has((String) input)) {
                        final JsonNode field = jsonNode.get((String) input);
                        if (field.isContainerNode()) {
                            return toChildFieldJacksonResource(field, (String) input);
                        } else {
                            Object scalaValue = JacksonUtils.getJsonScalaValue(field);
                            return scalaValue;
                        }
                    }
                    return null;
                }
            });
        }

        return valueMap;
    }

    protected String toJsonString(ObjectMapper mapper) {
        try {
            return mapper.writeValueAsString(jsonNode);
        } catch (JsonProcessingException e) {
            throw new ResourceException("JSON processing error.", e);
        }
    }

    protected JacksonResource toChildFieldJacksonResource(JsonNode jsonNode, String fieldName) {
        return new JacksonResource(this, jsonNode, fieldName);
    }

    protected JacksonResource toChildIndexedJacksonResource(JsonNode jsonNode, int index) {
        return new JacksonResource(this, jsonNode, "[" + index + "]");
    }

    private static class JsonNodeIterable implements Iterable<Resource> {

        private final JacksonResource base;

        private JsonNodeIterable(final JacksonResource base) {
            this.base = base;
        }

        @Override
        public Iterator<Resource> iterator() {
            return base.listChildren();
        }

    }

    private static class JsonFieldChildResourceIterator implements Iterator<Resource> {

        private final JacksonResource base;
        private final Iterator<Map.Entry<String, JsonNode>> fieldIterator;

        private JsonFieldChildResourceIterator(final JacksonResource base, final JsonNode jsonNode) {
            this.base = base;
            this.fieldIterator = jsonNode.fields();
        }

        @Override
        public boolean hasNext() {
            return fieldIterator.hasNext();
        }

        @Override
        public Resource next() {
            final Map.Entry<String, JsonNode> entry = fieldIterator.next();
            final String fieldName = entry.getKey();
            final JsonNode fieldNode = entry.getValue();
            return base.toChildFieldJacksonResource(fieldNode, fieldName);
        }

    }

    private static class JsonElementChildResourceIterator implements Iterator<Resource> {

        private final JacksonResource base;
        private final Iterator<JsonNode> elements;
        private int index;

        private JsonElementChildResourceIterator(final JacksonResource base, final JsonNode jsonNode) {
            this.base = base;
            elements = jsonNode.elements();
        }

        @Override
        public boolean hasNext() {
            return elements.hasNext();
        }

        @Override
        public Resource next() {
            JsonNode jsonNode = elements.next();
            return base.toChildIndexedJacksonResource(jsonNode, ++index);
        }

    }

}
