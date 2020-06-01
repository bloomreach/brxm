/*
 * Copyright 2020 Hippo B.V. (http://www.onehippo.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.hippoecm.hst.pagemodelapi.v10;

import java.io.IOException;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.annotation.JsonUnwrapped;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonPointer;
import com.fasterxml.jackson.databind.BeanDescription;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationConfig;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.ser.BeanSerializerModifier;
import com.fasterxml.jackson.databind.ser.ResolvableSerializer;

import org.junit.Test;

import static org.junit.Assert.assertFalse;

public class PageModelApiV10LearningTest {

    final static Class[] PageModelEntities = {Component.class, Product.class, Content.class};

    @Test
    public void page_model_pma_v10_learning_test() throws Exception {

        final ObjectMapper objectMapper = new ObjectMapper().registerModule(new SimpleModule().setSerializerModifier(
                new PageModelSerializerModifier()
        ));

        Component root = new Component("root", "component")
                .addModel("simple", "test")
                .addModel("simple2", "test")
                .addModel("product", new Product("1"))
                .addModel("unknown", new Unknown("1"))
                .addChild(
                        new Component("child1", "container")
                                .addModel("simple", "test1")
                                .addModel("product", new Product("2")))
                .addChild(new Component("child2", "container")
                        .addModel("simple", "test2")
                        .addModel("product", new Product("3"))
                        .addModel("content", new Content("1"))
                        .addChild(new Component("childOfchild2", "container")));

        String s = new ObjectMapper().writerWithDefaultPrettyPrinter().writeValueAsString(root);

        AggregatedPageModel pageModel = new AggregatedPageModel(root);

        final String serialized = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(pageModel);

        JsonNode jsonNodeRoot = objectMapper.readTree(serialized);

        JsonValidationUtil.validateReferences(jsonNodeRoot, jsonNodeRoot);

    }


    class PageModelSerializerModifier extends BeanSerializerModifier {

        public PageModelSerializerModifier() {
        }

        @SuppressWarnings("unchecked")
        @Override
        public JsonSerializer<?> modifySerializer(SerializationConfig config, BeanDescription beanDesc,
                                                  JsonSerializer<?> serializer) {
            return new PageModelSerializer((JsonSerializer<Object>) serializer);
        }

    }

    public static class PageModelSerializer extends JsonSerializer<Object> implements ResolvableSerializer {

        private static ThreadLocal<Object> tlRoot = new ThreadLocal<>();
        private static ThreadLocal<Object> tlFirstEntity = new ThreadLocal<>();
        private static ThreadLocal<ArrayDeque<WrapperEntity>> tlSerializeQueue = new ThreadLocal<>();
        private static ThreadLocal<Object> tlSerializingPageModelEntity = new ThreadLocal<>();


        private JsonSerializer<Object> serializer;

        public PageModelSerializer(final JsonSerializer<Object> serializer) {
            this.serializer = serializer;
        }

        @Override
        public void resolve(final SerializerProvider provider) throws JsonMappingException {
            if (serializer instanceof ResolvableSerializer) {
                ((ResolvableSerializer) serializer).resolve(provider);
            }
        }

        @Override
        public void serialize(final Object object, final JsonGenerator gen, final SerializerProvider serializerProvider) throws IOException {
            if (tlRoot.get() == null) {
                tlRoot.set(object);
                tlFirstEntity.set(Boolean.TRUE);
                tlSerializeQueue.set(new ArrayDeque<>());
            }
            try {
                if (object.getClass().equals(RootReference.class)) {
                    RootReference ref = (RootReference) object;
                    Object pageRoot = ref.getObject();
                    String jsonPointerId = createJsonPointerId();
                    WrapperEntity value = new WrapperEntity(pageRoot, jsonPointerId);
                    serializeBeanReference(gen, jsonPointerId);
                    tlSerializeQueue.get().add(value);
                    return;
                }

                Optional<Class> pmaEntity = Arrays.stream(PageModelEntities).filter(aClass -> object.getClass().equals(aClass)).findFirst();

                if (!pmaEntity.isPresent()) {
                    serializer.serialize(object, gen, serializerProvider);
                    return;
                }

                if (tlSerializingPageModelEntity.get() != null && tlSerializingPageModelEntity.get() != object) {
                    String jsonPointerId = createJsonPointerId();
                    serializeBeanReference(gen, jsonPointerId);
                    tlSerializeQueue.get().add(new WrapperEntity(object, jsonPointerId));
                    return;
                } else {
                    if (tlSerializingPageModelEntity.get() != null) {
                        serializer.serialize(object, gen, serializerProvider);
                    } else {

                        while (!tlSerializeQueue.get().isEmpty()) {

                            WrapperEntity pop = tlSerializeQueue.get().removeFirst();
                            if (tlFirstEntity.get() != null) {
                                gen.writeStartObject();
                                tlFirstEntity.set(null);
                            }
                            gen.writeFieldName(pop.jsonPointer);
                            tlSerializingPageModelEntity.set(pop.object);
                            gen.writeObject(pop.object);
                            tlSerializingPageModelEntity.set(null);

                        }
                    }
                }
            } finally {
                if (tlRoot.get() == object) {
                    // cleanup
                    tlRoot.set(null);
                    tlFirstEntity.set(null);
                    tlSerializeQueue.set(null);
                    tlSerializingPageModelEntity.set(null);
                }
            }

        }


        private void serializeBeanReference(final JsonGenerator gen,
                                            final String jsonPointerId) throws IOException {
            gen.writeStartObject();
            gen.writeStringField(CONTENT_JSON_POINTER_REFERENCE_PROP,
                    "/page/" + jsonPointerId);
            gen.writeEndObject();
        }
    }


    /**
     * JSON property name prefix for a UUID-based identifier.
     */
    private static final String CONTENT_ID_JSON_NAME_PREFIX = "u";

    /**
     * JSON Pointer Reference Property name.
     */
    private static final String CONTENT_JSON_POINTER_REFERENCE_PROP = "$ref";

    static String createJsonPointerId() {
        return createJsonPointerId(UUID.randomUUID().toString());
    }

    static String createJsonPointerId(final String uuid) {
        return new StringBuilder(uuid.length()).append(CONTENT_ID_JSON_NAME_PREFIX).append(uuid.replaceAll("-", ""))
                .toString();
    }

    @JsonPropertyOrder({"name", "type", "children", "models"})
    public final static class Component {

        private String name;
        private String type;
        List<Component> children = new ArrayList<>();
        Map<String, Object> models = new HashMap<>();

        public Component(final String name, final String type) {
            this.name = name;
            this.type = type;
        }

        public String getName() {
            return name;
        }

        public String getType() {
            return type;
        }

        public List<Component> getChildren() {
            return children;
        }

        public Map<String, Object> getModels() {
            return models;
        }

        public Component addChild(final Component child) {
            children.add(child);
            return this;
        }

        public Component addModel(final String name, final Object model) {
            models.put(name, model);
            return this;
        }
    }

    @JsonPropertyOrder({"root", "page"})
    public static class AggregatedPageModel {
        private Object object;

        public AggregatedPageModel(final Object object) {
            this.object = object;
        }

        public Object getPage() {
            return object;
        }

        public RootReference getRoot() {
            return new RootReference(object);
        }
    }

    public static class RootReference {
        private Object object;

        public RootReference(final Object object) {
            this.object = object;
        }

        public Object getObject() {
            return object;
        }
    }

    public static class WrapperEntity {

        private final Object object;
        private final String jsonPointer;

        public WrapperEntity(final Object object, final String jsonPointer) {

            this.object = object;
            this.jsonPointer = jsonPointer;
        }

        public Object getObject() {
            return object;
        }
    }

    public class Product {
        final String id;

        Product(String id) {
            this.id = id;
        }

        public String getId() {
            return id;
        }

        public String getType() {
            return "product";
        }

        @JsonAnyGetter
        public Map<String, String> getValues() {
            Map<String, String> map = new HashMap<>();
            map.put("foo", "Foo");
            map.put("bar", "Bar");
            return map;
        }
    }

    public class Content {
        final String id;

        Content(String id) {
            this.id = id;
        }

        public String getId() {
            return id;
        }

        public String getType() {
            return "product";
        }
    }

    public class Unknown {
        final String id;

        Unknown(String id) {
            this.id = id;
        }

        public String getId() {
            return id;
        }

        public String getType() {
            return "Not a PMA Entity";
        }

        @JsonUnwrapped
        public Name getName() {
            return new Name("john", "doe");
        }
    }

    public class Name {

        private final String john;
        private final String doe;

        public Name(final String john, final String doe) {

            this.john = john;
            this.doe = doe;
        }

        public String getFirstName() {
            return john;
        }

        public String getLastName() {
            return doe;
        }
    }
}
