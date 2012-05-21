package org.hippoecm.frontend.service.restproxy;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.lang.annotation.Annotation;
import java.net.URLEncoder;
import java.nio.charset.Charset;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import javax.jcr.Credentials;
import javax.jcr.SimpleCredentials;
import javax.security.auth.Subject;
import javax.ws.rs.core.MediaType;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpException;
import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.cxf.common.util.StringUtils;
import org.apache.cxf.jaxrs.client.JAXRSClientFactory;
import org.apache.cxf.jaxrs.client.WebClient;
import org.codehaus.jackson.Version;
import org.codehaus.jackson.jaxrs.JacksonJaxbJsonProvider;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.map.module.SimpleModule;
import org.hippoecm.frontend.plugin.IPluginContext;
import org.hippoecm.frontend.plugin.Plugin;
import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.hippoecm.frontend.service.IRestProxyService;
import org.hippoecm.frontend.service.restproxy.custom.AnnotationJsonDeserializer;
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
        // Version here used for Jakson JSON purposes and has nothing to do with Hippo CMS Engine version
        // Just using unknown version as it is of no use to us
        SimpleModule cmsRestJacksonJsonModule = new SimpleModule("CmsRestJacksonJsonModule", Version.unknownVersion());
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

}
