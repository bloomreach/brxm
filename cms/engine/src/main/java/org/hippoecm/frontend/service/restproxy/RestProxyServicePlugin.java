package org.hippoecm.frontend.service.restproxy;

import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import javax.jcr.Credentials;
import javax.jcr.SimpleCredentials;
import javax.security.auth.Subject;
import javax.ws.rs.core.MediaType;

import org.apache.cxf.common.util.StringUtils;
import org.apache.cxf.jaxrs.client.JAXRSClientFactory;
import org.apache.cxf.jaxrs.client.WebClient;
import org.codehaus.jackson.jaxrs.JacksonJaxbJsonProvider;
import org.hippoecm.frontend.plugin.IPluginContext;
import org.hippoecm.frontend.plugin.Plugin;
import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.hippoecm.frontend.service.IRestProxyService;
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
    private static final String PARAM_ERROR_MESSAGE_ERROR_WHILE_CREATING_PROXY = "Subject has no credentials attached with it! - %s : %s : %s";
    private static final String CMSREST_CREDENTIALS_HEADER = "X-CMSREST-CREDENTIALS";
    // COMMENT - MNour: This is really bad workaround but it is used only for the time being
    private static final String CREDENTIAL_CIPHER_KEY = "ENC_DEC_KEY";
    public static final String CONFIG_REST_URI = "rest.uri";
    public static final String CONFIG_SERVICE_ID = "service.id";
    public static final String DEFAULT_SERVICE_ID = IRestProxyService.class.getName();

    private static final long serialVersionUID = 1L;
    private static final List<?> PROVIDERS = Collections.singletonList(new JacksonJaxbJsonProvider());

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
    }

    @Override
    public <T> T createRestProxy(final Class<T> restServiceApiClass) {
    	return JAXRSClientFactory.create(restUri, restServiceApiClass, PROVIDERS);
    }

	@Override
	public <T> T createRestProxy(Class<T> restServiceApiClass, Subject subject) {
		T clientProxy = JAXRSClientFactory.create(restUri, restServiceApiClass, PROVIDERS);

		try {
			// The accept method is called to solve an issue as the REST call was sent with 'text/plain' as an accept header
			// which caused problems matching with the relevant JAXRS resource
			WebClient.client(clientProxy).header(CMSREST_CREDENTIALS_HEADER, getEncryptedCredentials(subject)).accept(MediaType.APPLICATION_JSON);
			return clientProxy;
		} catch (Exception ex) {
			if (log.isErrorEnabled()) {
				log.error(String.format(PARAM_ERROR_MESSAGE_ERROR_WHILE_CREATING_PROXY, ex.getClass().getName(), ex.getMessage(), ex));
			}

			return null;
		}
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
