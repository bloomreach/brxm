package org.hippoecm.frontend.service.restproxy;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.net.URLEncoder;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.jcr.Credentials;
import javax.jcr.SimpleCredentials;
import javax.security.auth.Subject;
import javax.ws.rs.core.MediaType;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpException;
import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.lang.NotImplementedException;
import org.apache.cxf.common.util.StringUtils;
import org.apache.cxf.jaxrs.client.JAXRSClientFactory;
import org.apache.cxf.jaxrs.client.WebClient;
import org.codehaus.jackson.JsonParseException;
import org.codehaus.jackson.JsonParser;
import org.codehaus.jackson.JsonProcessingException;
import org.codehaus.jackson.JsonToken;
import org.codehaus.jackson.Version;
import org.codehaus.jackson.jaxrs.JacksonJaxbJsonProvider;
import org.codehaus.jackson.map.DeserializationContext;
import org.codehaus.jackson.map.JsonDeserializer;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.map.TypeDeserializer;
import org.codehaus.jackson.map.module.SimpleModule;
import org.hippoecm.frontend.plugin.IPluginContext;
import org.hippoecm.frontend.plugin.Plugin;
import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.hippoecm.frontend.service.IRestProxyService;
import org.hippoecm.frontend.session.PluginUserSession;
import org.hippoecm.frontend.session.UserSession;
import org.onehippo.sso.CredentialCipher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Creates proxies for HST REST services. Plugin configuration properties:
 * <ul>
 * <li>'rest.uri': the base URI of the HST REST service to use (default: 'http://localhost:8080/site/_cmsrest')</li>
 * <li>'service.id': the ID to register this service under (default: 'IHstRestService')</li>
 * </ul>
 */
public class RestProxyServicePlugin extends Plugin implements IRestProxyService {

    private static final Logger log = LoggerFactory.getLogger(IRestProxyService.class);

    private static final String ERROR_MESSAGE_NULL_SUBJECT_IS_PASSED = "null subject has been passed which is not acceptable as an argument!";
    private static final String ERROR_MESSAGE_SUBJECT_HAS_NO_CREDENTIALS = "Subject has no credentials attached with it!";
    private static final String CMSREST_CREDENTIALS_HEADER = "X-CMSREST-CREDENTIALS";
    // This is really bad workaround but it is used only for the time being
    private static final String CREDENTIAL_CIPHER_KEY = "ENC_DEC_KEY";
    public static final String CONFIG_REST_URI = "rest.uri";
    public static final String CONFIG_SERVICE_ID = "service.id";
    public static final String DEFAULT_SERVICE_ID = IRestProxyService.class.getName();
    public static final String PING_SERVICE_URI = "ping.service.uri";
    public static final String PING_SERVICE_TIMEOUT = "ping.service.timeout";

    private static final long serialVersionUID = 1L;
    private static final List<?> PROVIDERS;

    static {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.enableDefaultTyping();
        objectMapper.enableDefaultTypingAsProperty(ObjectMapper.DefaultTyping.OBJECT_AND_NON_CONCRETE, "@class");
        SimpleModule cmsRestJacksonJsonModule = new SimpleModule("CmsRestJacksonJsonModule", new Version(2, 23, 02, "SNAPSHOT"));
        cmsRestJacksonJsonModule.addDeserializer(Annotation.class, new AnnotationJsonDeserializer());
        objectMapper.registerModule(cmsRestJacksonJsonModule);
        JacksonJaxbJsonProvider jjjProvider = new JacksonJaxbJsonProvider();
        jjjProvider.setMapper(objectMapper);
        PROVIDERS = Collections.singletonList(jjjProvider);
    }

    private boolean siteIsAlive;
    private final String restUri;

    public RestProxyServicePlugin(IPluginContext context, IPluginConfig config) {
        super(context, config);

        restUri = config.getString(CONFIG_REST_URI);
        if (StringUtils.isEmpty(restUri)) {
            throw new IllegalStateException("No REST service URI configured. Please set the plugin configuration property '"
                    + CONFIG_REST_URI + "'");
        }

        log.debug("Using REST uri '{}'", restUri);

        final String serviceId = config.getString(CONFIG_SERVICE_ID, DEFAULT_SERVICE_ID);
        log.debug("Registering this service under id '{}'", serviceId);
        context.registerService(this, serviceId);

        // Make sure that the final ping servuce URL is normalized, no additional '/'
        String pingServiceUriString = (restUri + config.getString(PING_SERVICE_URI, "/sites/#areAlive")).replaceAll("^://", "/");
        log.debug("Using Ping REST uri '{}'", pingServiceUriString);

        // Check whether the site is up and running or not
        try {
            // Make sure that it is URL encoded correctly, except for the '/' and ':' characters
            pingServiceUriString = URLEncoder.encode(pingServiceUriString, Charset.defaultCharset().name()).replaceAll(
                    "%2F", "/").replaceAll("%3A", ":");

            final HttpClient httpClient = new HttpClient();
            // Set the timeout for the HTTP connection in milliseconds, if the configuration parameter is missing or not set
            // use default value of 1 second
            httpClient.getParams().setParameter("http.socket.timeout", config.getAsInteger(PING_SERVICE_TIMEOUT, 1000));
            final int responceCode = httpClient.executeMethod(new GetMethod(pingServiceUriString));

            siteIsAlive = (responceCode == HttpStatus.SC_OK);
        } catch (HttpException httpex) {
            if (log.isDebugEnabled()) {
                log.warn("Error while pinging site using URI " + pingServiceUriString, httpex);
            }

            siteIsAlive = false;
        } catch (UnsupportedEncodingException usence) {
            if (log.isDebugEnabled()) {
                log.warn("Error while pinging site using URI " + pingServiceUriString, usence);
            }

            siteIsAlive = false;
        } catch (IOException ioe) {
            if (log.isDebugEnabled()) {
                log.warn("Error while pinging site using URI " + pingServiceUriString, ioe);
            }

            siteIsAlive = false;
        }

    }

    @Override
    public <T> T createRestProxy(final Class<T> restServiceApiClass) {
        // Check whether the site is up and running or not
        if (!siteIsAlive) {
            log.warn("It appears that the site is down. Please check with your environment adminstrator!");
            return null;
        }

        return JAXRSClientFactory.create(restUri, restServiceApiClass, PROVIDERS);
    }

    @Override
    public <T> T createSecureRestProxy(Class<T> restServiceApiClass) {
        // Check whether the site is up and running or not
        if (!siteIsAlive) {
            log.warn("It appears that the site is down. Please check with your environment adminstrator!");
            return null;
        }

        T clientProxy = JAXRSClientFactory.create(restUri, restServiceApiClass, PROVIDERS);

        Subject subject = getSubject();
        // The accept method is called to solve an issue as the REST call was sent with 'text/plain' as an accept header
        // which caused problems matching with the relevant JAXRS resource
        WebClient.client(clientProxy).header(CMSREST_CREDENTIALS_HEADER, getEncryptedCredentials(subject)).accept(MediaType.APPLICATION_JSON);
        return clientProxy;
    }

    protected Subject getSubject() {
        PluginUserSession session = (PluginUserSession) UserSession.get();

        Credentials credentials = session.getCredentials();
        Subject subject = new Subject();

        subject.getPrivateCredentials().add(credentials);
        subject.setReadOnly();
        return subject;
    }

    protected String getEncryptedCredentials(Subject subject) throws IllegalArgumentException {
        if (subject == null) {
            throw new IllegalArgumentException(ERROR_MESSAGE_NULL_SUBJECT_IS_PASSED);
        }

        Set<Object> credentials = subject.getPrivateCredentials();

        if ( (credentials == null) || (credentials.isEmpty()) ) {
            throw new IllegalArgumentException(ERROR_MESSAGE_SUBJECT_HAS_NO_CREDENTIALS);
        }

        Iterator<Object> credentialsIterator = credentials.iterator();
        SimpleCredentials subjectCredentials = (SimpleCredentials) credentialsIterator.next();

        CredentialCipher credentialCipher = CredentialCipher.getInstance();
        return credentialCipher.getEncryptedString(CREDENTIAL_CIPHER_KEY, subjectCredentials);
    }
    
    // Would you take a look at the serialization of HstPropertyDefinition
    private static final class AnnotationJsonDeserializer extends JsonDeserializer<Annotation> {

        @SuppressWarnings("unchecked")
        @Override
        public Annotation deserialize(JsonParser jsonPasrer, DeserializationContext deserContext) throws IOException,
                JsonProcessingException {

            Annotation annotation = null;
            String annotationTypeName = null;
            Class<? extends Annotation> annotationClass = null;
            Map<String, Object> annotationAttributes = null;

            while(jsonPasrer.nextToken() != JsonToken.END_OBJECT) {
                // Read the '@class' field name
                jsonPasrer.nextToken();
                // Now read the '@class' field value
                annotationTypeName = jsonPasrer.getText();

                try {
                    annotationClass = (Class<? extends Annotation>) Class.forName(annotationTypeName);
                    annotationAttributes = new HashMap<String, Object>(annotationClass.getDeclaredMethods().length);
                    while (jsonPasrer.nextToken() != JsonToken.END_OBJECT) {
                        final String fieldName = jsonPasrer.getCurrentName();
                        final Method annotationAttribute = annotationClass.getDeclaredMethod(fieldName, new Class<?>[] {});
                        annotationAttributes.put(fieldName, deserializeAnnotationAttribute(annotationClass, annotationAttribute, jsonPasrer));
                    }
                    // Annotation deserialization is done here
                    break;
                } catch (ClassNotFoundException cnfe) {
                    throw new AnnotationProcessingException(String.format("Error while processing annotation: %s", annotationTypeName), cnfe);
                } catch (SecurityException se) {
                    throw new AnnotationProcessingException(String.format("Error while processing annotation: %s", annotationTypeName), se);
                } catch (NoSuchMethodException nsme) {
                    throw new AnnotationProcessingException(String.format("Error while processing annotation: %s", annotationTypeName), nsme);
                }
            }

            annotation = (Annotation) Proxy.newProxyInstance(Thread.currentThread().getContextClassLoader(),
                    new Class<?>[] { annotationClass }, new AnnotationProxyInvocationHandler(annotationClass, annotationAttributes));

            return annotation;
        }

        @Override
        public Object deserializeWithType(JsonParser jp, DeserializationContext ctxt, TypeDeserializer typeDeserializer)
                throws IOException, JsonProcessingException {

            return deserialize(jp, ctxt);
        }

    }

    private static final Object deserializeAnnotationAttribute(Class<? extends Annotation> annotationClass, Method annotationAttribute,
            JsonParser jsonPasrer) throws JsonParseException, IOException {

        jsonPasrer.nextToken();
        if (annotationAttribute.getReturnType() == byte.class || annotationAttribute.getReturnType() == Byte.class) {
            return Byte.valueOf(jsonPasrer.getNumberValue().byteValue());
        } else if (annotationAttribute.getReturnType() == short.class || annotationAttribute.getReturnType() == Short.class) {
            return Short.valueOf(jsonPasrer.getNumberValue().shortValue());            
        } else if (annotationAttribute.getReturnType() == int.class || annotationAttribute.getReturnType() == Integer.class) {
            return Integer.valueOf(jsonPasrer.getNumberValue().intValue());            
        } else if (annotationAttribute.getReturnType() == long.class || annotationAttribute.getReturnType() == Long.class) {
            return Long.valueOf(jsonPasrer.getNumberValue().longValue());            
        } else if (annotationAttribute.getReturnType() == float.class || annotationAttribute.getReturnType() == Float.class) {
            return Float.valueOf(jsonPasrer.getNumberValue().floatValue());            
        } else if (annotationAttribute.getReturnType() == double.class || annotationAttribute.getReturnType() == Double.class) {
            return Double.valueOf(jsonPasrer.getNumberValue().doubleValue());            
        } else if (annotationAttribute.getReturnType() == double.class || annotationAttribute.getReturnType() == Double.class) {
            return Double.valueOf(jsonPasrer.getNumberValue().doubleValue());            
        } else if (annotationAttribute.getReturnType() == boolean.class || annotationAttribute.getReturnType() == Boolean.class ) {
            return Boolean.valueOf(jsonPasrer.getBooleanValue());
        } else if (annotationAttribute.getReturnType() == char.class || annotationAttribute.getReturnType() == Character.class ) {
            return Character.valueOf(jsonPasrer.getTextCharacters()[0]);
        } else if (annotationAttribute.getReturnType() == String.class) {
            return jsonPasrer.getText();
        } else if (annotationAttribute.getReturnType() == byte[].class) {
            return deserializeBytePrimitiveArrayAnnotationAttribute(jsonPasrer);
        } else if (annotationAttribute.getReturnType() == Byte[].class) {
            return deserializeByteArrayAnnotationAttribute(jsonPasrer);
        } else if (annotationAttribute.getReturnType() == short[].class) {
            return deserializeShortPrimitiveArrayAnnotationAttribute(jsonPasrer);
        } else if (annotationAttribute.getReturnType() == Short[].class) {
            return deserializeShortArrayAnnotationAttribute(jsonPasrer);
        } else if (annotationAttribute.getReturnType() == int[].class) {
            return deserializeIntegerPrimitiveArrayAnnotationAttribute(jsonPasrer);
        } else if (annotationAttribute.getReturnType() == Short[].class) {
            return deserializeIntegerArrayAnnotationAttribute(jsonPasrer);
        } else if (annotationAttribute.getReturnType() == long[].class) {
            return deserializeLongPrimitiveArrayAnnotationAttribute(jsonPasrer);
        } else if (annotationAttribute.getReturnType() == Long[].class) {
            return deserializeLongArrayAnnotationAttribute(jsonPasrer);
        } else if (annotationAttribute.getReturnType() == float[].class) {
            return deserializeFloatPrimitiveArrayAnnotationAttribute(jsonPasrer);
        } else if (annotationAttribute.getReturnType() == Float[].class) {
            return deserializeFloatArrayAnnotationAttribute(jsonPasrer);
        } else if (annotationAttribute.getReturnType() == double[].class) {
            return deserializeDoublePrimitiveArrayAnnotationAttribute(jsonPasrer);
        } else if (annotationAttribute.getReturnType() == Double[].class) {
            return deserializeDoubleArrayAnnotationAttribute(jsonPasrer);
        } else if (annotationAttribute.getReturnType() == boolean[].class) {
            return deserializeBooleanPrimitiveArrayAnnotationAttribute(jsonPasrer);
        } else if (annotationAttribute.getReturnType() == Boolean[].class) {
            return deserializeBooleanArrayAnnotationAttribute(jsonPasrer);
        } else if (annotationAttribute.getReturnType() == char[].class) {
            return deserializeCharacterPrimitiveArrayAnnotationAttribute(jsonPasrer);
        } else if (annotationAttribute.getReturnType() == Character[].class) {
            return deserializeCharacterArrayAnnotationAttribute(jsonPasrer);
        } else if (annotationAttribute.getReturnType() == String[].class) {
            return deserializeStringArrayAnnotationAttribute(jsonPasrer);
        } else {
            throw new IllegalArgumentException(String.format("Unrecognized attribute value type %s for annotation %s",
                    annotationAttribute.getReturnType().getName(), annotationClass.getName()));
        }

    }

    private static final byte[] deserializeBytePrimitiveArrayAnnotationAttribute(JsonParser jsonPasrer) throws JsonParseException, IOException {
        Byte[] byteArray = deserializeByteArrayAnnotationAttribute(jsonPasrer);
        byte[] returnValue = new byte[byteArray.length];

        for (int index = 0; index < byteArray.length; index++) {
            returnValue[index] = byteArray[index];
        }

        return returnValue;
    }

    private static final Byte[] deserializeByteArrayAnnotationAttribute(JsonParser jsonPasrer) throws JsonParseException, IOException {
        List<Byte> byteArray = new ArrayList<Byte>();

        while(jsonPasrer.nextToken() != JsonToken.END_ARRAY) {
            byteArray.add(jsonPasrer.getByteValue());
        }

        return byteArray.toArray(new Byte[] {});
    }

    private static final short[] deserializeShortPrimitiveArrayAnnotationAttribute(JsonParser jsonPasrer) throws JsonParseException, IOException {
        Short[] shortArray = deserializeShortArrayAnnotationAttribute(jsonPasrer);
        short[] returnValue = new short[shortArray.length];

        for (int index = 0; index < shortArray.length; index++) {
            returnValue[index] = shortArray[index];
        }

        return returnValue;
    }

    private static final Short[] deserializeShortArrayAnnotationAttribute(JsonParser jsonPasrer) throws JsonParseException, IOException {
        List<Short> integerArray = new ArrayList<Short>();

        while(jsonPasrer.nextToken() != JsonToken.END_ARRAY) {
            integerArray.add(jsonPasrer.getShortValue());
        }

        return integerArray.toArray(new Short[] {});
    }

    private static final int[] deserializeIntegerPrimitiveArrayAnnotationAttribute(JsonParser jsonPasrer) throws JsonParseException, IOException {
        Integer[] integerArray = deserializeIntegerArrayAnnotationAttribute(jsonPasrer);
        int[] returnValue = new int[integerArray.length];

        for (int index = 0; index < integerArray.length; index++) {
            returnValue[index] = integerArray[index];
        }

        return returnValue;
    }

    private static final Integer[] deserializeIntegerArrayAnnotationAttribute(JsonParser jsonPasrer) throws JsonParseException, IOException {
        List<Integer> integerArray = new ArrayList<Integer>();

        while(jsonPasrer.nextToken() != JsonToken.END_ARRAY) {
            integerArray.add(jsonPasrer.getIntValue());
        }

        return integerArray.toArray(new Integer[] {});
    }

    private static final long[] deserializeLongPrimitiveArrayAnnotationAttribute(JsonParser jsonPasrer) throws JsonParseException, IOException {
        Long[] longArray = deserializeLongArrayAnnotationAttribute(jsonPasrer);
        long[] returnValue = new long[longArray.length];

        for (int index = 0; index < longArray.length; index++) {
            returnValue[index] = longArray[index];
        }

        return returnValue;
    }

    private static final Long[] deserializeLongArrayAnnotationAttribute(JsonParser jsonPasrer) throws JsonParseException, IOException {
        List<Long> longArray = new ArrayList<Long>();

        while(jsonPasrer.nextToken() != JsonToken.END_ARRAY) {
            longArray.add(jsonPasrer.getLongValue());
        }

        return longArray.toArray(new Long[] {});
    }

    private static final float[] deserializeFloatPrimitiveArrayAnnotationAttribute(JsonParser jsonPasrer) throws JsonParseException, IOException {
        Float[] floatArray = deserializeFloatArrayAnnotationAttribute(jsonPasrer);
        float[] returnValue = new float[floatArray.length];

        for (int index = 0; index < floatArray.length; index++) {
            returnValue[index] = floatArray[index];
        }

        return returnValue;
    }

    private static final Float[] deserializeFloatArrayAnnotationAttribute(JsonParser jsonPasrer) throws JsonParseException, IOException {
        List<Float> floatArray = new ArrayList<Float>();

        while(jsonPasrer.nextToken() != JsonToken.END_ARRAY) {
            floatArray.add(jsonPasrer.getFloatValue());
        }

        return floatArray.toArray(new Float[] {});
    }

    private static final double[] deserializeDoublePrimitiveArrayAnnotationAttribute(JsonParser jsonPasrer) throws JsonParseException, IOException {
        Double[] doubleArray = deserializeDoubleArrayAnnotationAttribute(jsonPasrer);
        double[] returnValue = new double[doubleArray.length];

        for (int index = 0; index < doubleArray.length; index++) {
            returnValue[index] = doubleArray[index];
        }

        return returnValue;
    }

    private static final Double[] deserializeDoubleArrayAnnotationAttribute(JsonParser jsonPasrer) throws JsonParseException, IOException {
        List<Double> doubleArray = new ArrayList<Double>();

        while(jsonPasrer.nextToken() != JsonToken.END_ARRAY) {
            doubleArray.add(jsonPasrer.getDoubleValue());
        }

        return doubleArray.toArray(new Double[] {});
    }

    private static final boolean[] deserializeBooleanPrimitiveArrayAnnotationAttribute(JsonParser jsonPasrer) throws JsonParseException, IOException {
        Boolean[] booleanArray = deserializeBooleanArrayAnnotationAttribute(jsonPasrer);
        boolean[] returnArray = new boolean[booleanArray.length];

        for (int index = 0; index < booleanArray.length; index++) {
            returnArray[index] = booleanArray[index];
        }

        return returnArray;
    }

    private static final Boolean[] deserializeBooleanArrayAnnotationAttribute(JsonParser jsonPasrer) throws JsonParseException, IOException {
        List<Boolean> booleanArray = new ArrayList<Boolean>();

        while(jsonPasrer.nextToken() != JsonToken.END_ARRAY) {
            booleanArray.add(jsonPasrer.getBooleanValue());
        }

        return booleanArray.toArray(new Boolean[] {});
    }

    private static final char[] deserializeCharacterPrimitiveArrayAnnotationAttribute(JsonParser jsonPasrer) throws JsonParseException, IOException {
        Character[] characterArray = deserializeCharacterArrayAnnotationAttribute(jsonPasrer);
        char[] returnValue = new char[characterArray.length];

        for (int index = 0; index < characterArray.length; index++) {
            returnValue[index] = characterArray[index];
        }
        return returnValue;
    }

    private static final Character[] deserializeCharacterArrayAnnotationAttribute(JsonParser jsonPasrer) throws JsonParseException, IOException {
        List<Character> characterArray = new ArrayList<Character>();

        while(jsonPasrer.nextToken() != JsonToken.END_ARRAY) {
            characterArray.add(jsonPasrer.getTextCharacters()[0]);
        }

        return characterArray.toArray(new Character[] {});
    }

    private static final String[] deserializeStringArrayAnnotationAttribute(JsonParser jsonPasrer) throws JsonParseException, IOException {
        List<String> stringArray = new ArrayList<String>();

        while(jsonPasrer.nextToken() != JsonToken.END_ARRAY) {
            stringArray.add(jsonPasrer.getText());
        }

        return stringArray.toArray(new String[] {});
    }

    private static final class AnnotationProxyInvocationHandler implements InvocationHandler {

        private final Class<? extends Annotation> annotationClass;
        private final Map<String, ?> annotationAttributes;

        public AnnotationProxyInvocationHandler(Class<? extends Annotation> annotationTypeName, Map<String, ?> annotationAttributes) {
            this.annotationClass = annotationTypeName;
            this.annotationAttributes = annotationAttributes;
        }

        @Override
        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
            Object returnValue = this.annotationAttributes.get(method.getName());

            if (returnValue != null) {
                return returnValue;
            } else if (method.getName().equals("toString")) {
                return handleToString();
            } else if (method.getName().equals("annotationType")) {
                return this.annotationClass;
            } else {
                throw new NotImplementedException(String.format("This method can not be handled for : %s", this.handleToString()));
            }
        }

        @SuppressWarnings("unused")
        protected AnnotationProxyInvocationHandler() {
            annotationClass = null;
            annotationAttributes = null;
        }

        protected String handleToString() {
            String superString = super.toString();

            return String.format("%s : %s", superString, this.annotationClass.getName());
        }

    }

    @SuppressWarnings("serial")
    private static final class AnnotationProcessingException extends JsonProcessingException {

        protected AnnotationProcessingException(String msg, Throwable rootCause) {
            super(msg, rootCause);
        }

    }

}
