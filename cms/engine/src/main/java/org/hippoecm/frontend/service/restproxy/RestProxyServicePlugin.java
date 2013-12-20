/*
 * Copyright 2011-2013 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.frontend.service.restproxy;

import java.io.IOException;
import java.lang.annotation.Annotation;
import java.net.URLEncoder;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import javax.jcr.Credentials;
import javax.jcr.SimpleCredentials;
import javax.security.auth.Subject;
import javax.ws.rs.core.MediaType;

import com.google.common.base.Optional;

import org.apache.cxf.common.util.StringUtils;
import org.apache.cxf.jaxrs.client.JAXRSClientFactory;
import org.apache.cxf.jaxrs.client.WebClient;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.params.ClientPNames;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.conn.PoolingClientConnectionManager;
import org.apache.http.params.CoreConnectionPNames;
import org.apache.http.protocol.BasicHttpContext;
import org.apache.http.protocol.HttpContext;
import org.codehaus.jackson.Version;
import org.codehaus.jackson.jaxrs.JacksonJaxbJsonProvider;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.map.module.SimpleModule;
import org.hippoecm.frontend.plugin.IPluginContext;
import org.hippoecm.frontend.plugin.Plugin;
import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.hippoecm.frontend.service.IRestProxyService;
import org.hippoecm.frontend.service.restproxy.custom.json.deserializers.AnnotationJsonDeserializer;
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

    private static final String CMSREST_CREDENTIALS_HEADER = "X-CMSREST-CREDENTIALS";
    // This is really bad workaround but it is used only for the time being
    private static final String CREDENTIAL_CIPHER_KEY = "ENC_DEC_KEY";
    public static final String CONFIG_REST_URI = "rest.uri";
    public static final String CONFIG_SERVICE_ID = "service.id";
    public static final String DEFAULT_SERVICE_ID = IRestProxyService.class.getName();
    public static final String PING_SERVICE_URI = "ping.service.uri";

    private static final long serialVersionUID = 1L;
    private static final JacksonJaxbJsonProvider defaultJJJProvider;

    private final static int PING_SERVLET_TIMEOUT = 1000;
    private final static int MAX_CONNECTIONS = 50;
    private final String pingServiceUri;
    private Optional<Boolean> siteIsAlive;
    private final String restUri;

    static {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.enableDefaultTyping();
        objectMapper.enableDefaultTypingAsProperty(ObjectMapper.DefaultTyping.OBJECT_AND_NON_CONCRETE, "@class");
        // Version here used for Jackson JSON purposes and has nothing to do with Hippo CMS Engine version
        // Just using unknown version as it is of no use to us
        SimpleModule cmsRestJacksonJsonModule = new SimpleModule("CmsRestJacksonJsonModule", Version.unknownVersion());
        cmsRestJacksonJsonModule.addDeserializer(Annotation.class, new AnnotationJsonDeserializer());
        objectMapper.registerModule(cmsRestJacksonJsonModule);
        defaultJJJProvider = new JacksonJaxbJsonProvider();
        defaultJJJProvider.setMapper(objectMapper);
    }

    protected static HttpClient httpClient = null;
    static {
        PoolingClientConnectionManager mgr = new PoolingClientConnectionManager();
        mgr.setDefaultMaxPerRoute(MAX_CONNECTIONS);
        mgr.setMaxTotal(MAX_CONNECTIONS);
        httpClient = new DefaultHttpClient(mgr);
        // @see http://hc.apache.org/httpcomponents-client-ga/tutorial/html/connmgmt.html#d5e399
        httpClient.getParams().setIntParameter(CoreConnectionPNames.SO_TIMEOUT, PING_SERVLET_TIMEOUT);
        httpClient.getParams().setIntParameter(CoreConnectionPNames.CONNECTION_TIMEOUT, PING_SERVLET_TIMEOUT);
        httpClient.getParams().setBooleanParameter(CoreConnectionPNames.STALE_CONNECTION_CHECK, false);
        httpClient.getParams().setBooleanParameter(ClientPNames.HANDLE_AUTHENTICATION, false);
    }

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

        pingServiceUri = (restUri + config.getString(PING_SERVICE_URI, "/sites/_isAlive")).replaceAll("^://", "/");
        log.debug("Using ping REST uri '{}'", pingServiceUri);

    }

    @Override
    public <T> T createRestProxy(final Class<T> restServiceApiClass) {
        return createRestProxy(restServiceApiClass, null);
    }

    @Override
    public <T> T createSecureRestProxy(Class<T> restServiceApiClass) {
        return createSecureRestProxy(restServiceApiClass, null);
    }

    /**
     * Creates a proxy to a REST service based on the provided class
     * <p/>
     * <p/>
     * This version takes addition list of providers to configure the client proxy with </P>
     *
     * @param restServiceApiClass the class representing the REST service API.
     * @param <T>                 the generic type of the REST service API class.
     * @param additionalProviders {@link java.util.List} of additional providers to configure client proxies with
     * @return a proxy to the REST service represented by the given class, or null if no proxy could be created.
     */
    @Override
    public <T> T createRestProxy(final Class<T> restServiceApiClass, final List<Object> additionalProviders) {
        if (siteIsAlive == null) {
            checkSiteIsAlive(pingServiceUri);
        }
        if (!siteIsAlive.get()) {
            log.info("It appears that the site might be down. Pinging site one more time!");
            checkSiteIsAlive(pingServiceUri);
            if (!siteIsAlive.get()) {
                log.warn("It appears that site is still down. Please check with your administrator!");
                return null;
            } else {
                log.info("Site is up and running.");
            }
        }

        return JAXRSClientFactory.create(restUri, restServiceApiClass, getProviders(additionalProviders));
    }

    /**
     * Creates a proxy to a REST service based on the provided class and security {@link javax.security.auth.Subject} A
     * security {@link javax.security.auth.Subject} which indicates that the caller wants a security context to be
     * propagated with the REST call
     * <p/>
     * <p/>
     * This version takes addition list of providers to configure the client proxy with </P>
     *
     * @param restServiceApiClass the class representing the REST service API.
     * @param <T>                 the generic type of the REST service API class.
     * @param additionalProviders {@link java.util.List} of additional providers to configure client proxies with
     * @return a proxy to the REST service represented by the given class, or null if no proxy could be created.
     */
    @Override
    public <T> T createSecureRestProxy(final Class<T> restServiceApiClass, final List<Object> additionalProviders) {
        if (siteIsAlive == null) {
            checkSiteIsAlive(pingServiceUri);
        }
        if (!siteIsAlive.get()) {
            log.info("It appears that the site might be down. Pinging site one more time!");
            checkSiteIsAlive(pingServiceUri);
            if (!siteIsAlive.get()) {
                log.warn("It appears that site is still down. Please check with your administrator!");
                return null;
            } else {
                log.info("Site is up and running.");
            }
        }

        T clientProxy = JAXRSClientFactory.create(restUri, restServiceApiClass, getProviders(additionalProviders));

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
            throw new IllegalArgumentException("Null subject has been passed which is not acceptable as an argument!");
        }

        Set<Object> credentials = subject.getPrivateCredentials();

        if ( (credentials == null) || (credentials.isEmpty()) ) {
            throw new IllegalArgumentException("Subject has no credentials attached with it!");
        }

        Iterator<Object> credentialsIterator = credentials.iterator();
        SimpleCredentials subjectCredentials = (SimpleCredentials) credentialsIterator.next();

        CredentialCipher credentialCipher = CredentialCipher.getInstance();
        return credentialCipher.getEncryptedString(CREDENTIAL_CIPHER_KEY, subjectCredentials);
    }

    protected void checkSiteIsAlive(final String pingServiceUri) {
        String normalizedPingServiceUri = "";

        // Check whether the site is up and running or not
        HttpGet httpGet = null;

        try {
            // Make sure that it is URL encoded correctly, except for the '/' and ':' characters
            normalizedPingServiceUri = URLEncoder.encode(pingServiceUri, Charset.defaultCharset().name())
                    .replaceAll("%2F", "/").replaceAll("%3A", ":");

            // Set the timeout for the HTTP connection in milliseconds, if the configuration parameter is missing or not set
            // use default value of 1 second
            httpGet = new HttpGet(normalizedPingServiceUri);
            // @see http://hc.apache.org/httpcomponents-client-ga/tutorial/html/connmgmt.html#d5e639
            final HttpContext httpContext = new BasicHttpContext();
            final HttpResponse httpResponse = httpClient.execute(httpGet, httpContext);
            boolean ok = (httpResponse.getStatusLine().getStatusCode() == HttpStatus.SC_OK);
            if (!ok) {
                log.warn("The response status ('{}') is not okay from the pinging site service URI, '{}'.", httpResponse.getStatusLine(), normalizedPingServiceUri);
            }
            siteIsAlive = Optional.of(new Boolean(ok));
        } catch (IOException e) {
            if (log.isDebugEnabled()) {
                log.warn("Error while pinging site using URI " + normalizedPingServiceUri, e);
            } else {
                log.warn("Error while pinging site using URI {} - {}", normalizedPingServiceUri, e.toString());
            }
            siteIsAlive = Optional.of(Boolean.FALSE);
        } finally {
            if ((httpGet != null) && (!httpGet.isAborted())) {
                httpGet.reset();
            }
        }
    }

    protected List<Object> getProviders(final List<Object> additionalProviders) {
        List<Object> providers = new ArrayList<Object>();
        providers.add(defaultJJJProvider);
        if (additionalProviders != null) {
            providers.addAll(additionalProviders);
        }
        return providers;
    }

}
