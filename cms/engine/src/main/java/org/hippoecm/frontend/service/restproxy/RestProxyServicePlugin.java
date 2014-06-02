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

import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import javax.jcr.Credentials;
import javax.jcr.SimpleCredentials;
import javax.security.auth.Subject;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.core.MediaType;

import org.apache.cxf.common.util.StringUtils;
import org.apache.cxf.jaxrs.client.JAXRSClientFactory;
import org.apache.cxf.jaxrs.client.WebClient;
import org.apache.wicket.request.cycle.RequestCycle;
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
    private static final String CMSREST_CMSHOST_HEADER = "X-CMSREST-CMSHOST";
    // This is really bad workaround but it is used only for the time being
    private static final String CREDENTIAL_CIPHER_KEY = "ENC_DEC_KEY";
    public static final String CONFIG_REST_URI = "rest.uri";
    public static final String CONFIG_CONTEXT_PATH = "context.path";
    public static final String CONFIG_SERVICE_ID = "service.id";
    public static final String DEFAULT_SERVICE_ID = IRestProxyService.class.getName();

    private static final long serialVersionUID = 1L;
    private static final JacksonJaxbJsonProvider defaultJJJProvider;

    private final String restUri;
    private final String contextPath;

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


    public RestProxyServicePlugin(IPluginContext context, IPluginConfig config) {
        super(context, config);

        restUri = config.getString(CONFIG_REST_URI);
        if (StringUtils.isEmpty(restUri)) {
            throw new IllegalStateException("No REST service URI configured. Please set the plugin configuration property '"
                    + CONFIG_REST_URI + "'");
        }
        log.info("Using REST uri '{}'", restUri);

        contextPath = config.getString(CONFIG_CONTEXT_PATH);
        if (contextPath != null) {
            if (isValidContextPath(contextPath)) {
               log.info("Configured contextPath for REST service with uri '{}' is '{}'", restUri, contextPath);
            } else {
                throw new IllegalStateException("Invalid context path configured for restUri '"
                        + restUri + "'. Property '"+CONFIG_CONTEXT_PATH+"' must be either missing, empty, or " +
                        "start with a '/' and no more other '/' but it was '"+contextPath+"'");
            }
        } else {
            log.info("No context path set. #getContextPath will return null");
        }


        final String serviceId = config.getString(CONFIG_SERVICE_ID, DEFAULT_SERVICE_ID);
        log.info("Registering this service under id '{}'", serviceId);
        context.registerService(this, serviceId);

    }

    @Override
    public String getContextPath() {
        return contextPath;
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
        T clientProxy = JAXRSClientFactory.create(restUri, restServiceApiClass, getProviders(additionalProviders));

        Subject subject = getSubject();
        // The accept method is called to solve an issue as the REST call was sent with 'text/plain' as an accept header
        // which caused problems matching with the relevant JAXRS resource
        WebClient.client(clientProxy)
                .header(CMSREST_CREDENTIALS_HEADER, getEncryptedCredentials(subject))
                .header(CMSREST_CMSHOST_HEADER, getFarthestRequestHost())
                .accept(MediaType.WILDCARD_TYPE);

        // TODO set a timeout?
        //WebClient.getConfig(WebClient.client(clientProxy)).getHttpConduit().getClient().setAsyncExecuteTimeout(60000);
        // WebClient.getConfig(WebClient.client(clientProxy)).setSynchronousTimeout(60000);
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

    protected String getFarthestRequestHost() {
        final HttpServletRequest request = (HttpServletRequest) RequestCycle.get().getRequest().getContainerRequest();
        String host = request.getHeader("X-Forwarded-Host");

        if (host != null) {
            String [] hosts = host.split(",");
            return hosts[0].trim();
        }
        host = request.getHeader("Host");
        if (host != null && !"".equals(host)) {
            return host;
        }
        // should never happen : HTTP/1.0 based browser clients are unlikely to login in the cms :)
        int serverPort = request.getServerPort();
        if (serverPort == 80 || serverPort == 443 || serverPort <= 0) {
            host = request.getServerName();
        } else {
            host = request.getServerName() + ":" + serverPort;
        }
        return host;
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

    protected List<Object> getProviders(final List<Object> additionalProviders) {
        List<Object> providers = new ArrayList<Object>();
        providers.add(defaultJJJProvider);
        if (additionalProviders != null) {
            providers.addAll(additionalProviders);
        }
        return providers;
    }

    private static boolean isValidContextPath(String path) {
        if (path == null) {
            // we allow context path to be null which means can be used to be
            // context path agnostic
            return true;
        }
        if (path.equals("")) {
            return true;
        }
        if (!path.startsWith("/")) {
            return false;
        }
        if (path.substring(1).contains("/")) {
            return false;
        }
        return true;
    }

}
