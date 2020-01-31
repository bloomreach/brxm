package org.hippoecm.hst.pagemodelapi.v10;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Stack;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.BeanDescription;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationConfig;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.ser.BeanSerializerModifier;

import org.junit.Test;

public class PageModelApiV10LearningTest {

    private ThreadLocal<Stack<WrapperEntity>> serializeStack = new ThreadLocal<Stack<WrapperEntity>>();
    private ThreadLocal<Object> alreadySerializingPageModelEntity = new ThreadLocal<Object>();
    private ThreadLocal<Object> poppedSerializingPageModelEntity = new ThreadLocal<Object>();
    private ThreadLocal<WrapperEntity> pageRootEntity = new ThreadLocal<WrapperEntity>();

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

        alreadySerializingPageModelEntity.set(null);
        serializeStack.set(new Stack<>());

        AggregatedPageModel ard = new AggregatedPageModel(root);

        System.out.println(objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(ard));

    }


    class PageModelSerializerModifier extends BeanSerializerModifier {

        public PageModelSerializerModifier() {
        }

        @SuppressWarnings("unchecked")
        @Override
        public JsonSerializer<?> modifySerializer(SerializationConfig config, BeanDescription beanDesc,
                                                  JsonSerializer<?> serializer) {
            return new ComponentSerializer((JsonSerializer<Object>) serializer);
        }

    }

    public class ComponentSerializer extends JsonSerializer<Object> {

        private JsonSerializer<Object> serializer;

        public ComponentSerializer(final JsonSerializer<Object> serializer) {
            this.serializer = serializer;
        }

        private boolean test = true;

        @Override
        public void serialize(final Object object, final JsonGenerator gen, final SerializerProvider serializerProvider) throws IOException {

            if (object.getClass().equals(RootReference.class)) {
                RootReference ref = (RootReference) object;
                Object pageRoot = ref.getObject();
                String jsonPointerId = createJsonPointerId();
                WrapperEntity value = new WrapperEntity(pageRoot, jsonPointerId);
                pageRootEntity.set(value);
                serializeBeanReference(gen, jsonPointerId);
                serializeStack.get().add(value);
                return;
            }

            Optional<Class> first = Arrays.stream(PageModelEntities).filter(aClass -> object.getClass().equals(aClass)).findFirst();

            if (!first.isPresent()) {
                serializer.serialize(object, gen, serializerProvider);
                return;
            }


            if (alreadySerializingPageModelEntity.get() != null ||
                    (poppedSerializingPageModelEntity.get() != null && poppedSerializingPageModelEntity.get() != object)) {
                String jsonPointerId = createJsonPointerId();
                serializeBeanReference(gen, jsonPointerId);
                serializeStack.get().add(new WrapperEntity(object, jsonPointerId));
                return;
            } else {
                if (poppedSerializingPageModelEntity.get() != null) {
                    serializer.serialize(object, gen, serializerProvider);
                } else {

                    while (!serializeStack.get().isEmpty()) {

                        WrapperEntity pop = serializeStack.get().remove(0);
                        if (test) {
                            gen.writeStartObject();
                            test = false;
                        }
                        gen.writeFieldName(pop.jsonPointer);
                        poppedSerializingPageModelEntity.set(pop.object);
                        gen.writeObject(pop.object);
                        poppedSerializingPageModelEntity.set(null);

                    }
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
       return createJsonPointerId(UUID.randomUUID().toString()) ;
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

        public String getName() {
            return "Not a PMA Entity";
        }
    }
}
