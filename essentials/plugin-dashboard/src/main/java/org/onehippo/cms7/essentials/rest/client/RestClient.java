package org.onehippo.cms7.essentials.rest.client;

import javax.ws.rs.core.MediaType;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;

import org.apache.cxf.jaxrs.client.WebClient;
import org.apache.cxf.transport.http.HTTPConduit;
import org.onehippo.cms7.essentials.dashboard.rest.RestfulList;
import org.onehippo.cms7.essentials.rest.model.PluginRestful;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * @version "$Id: RestClient.java 174875 2013-08-23 13:57:46Z mmilicevic $"
 */
public class RestClient {

    private static Logger log = LoggerFactory.getLogger(RestClient.class);

    /**
     * e.g. http://localhost:8080/site/restapi
     */
    private final String baseResourceUri;
    private long receiveTimeout = 2000;
    private long connectionTimeout = 2500;

    public RestClient(String baseResourceUri) {
        this.baseResourceUri = baseResourceUri;
    }

    public RestClient(String baseResourceUri, long receiveTimeout, long connectionTimeout) {
        this.baseResourceUri = baseResourceUri;
        this.connectionTimeout = connectionTimeout;
        this.receiveTimeout = receiveTimeout;
    }

    public String getPluginList() {
        final WebClient client = WebClient.create(baseResourceUri);
        setTimeouts(client, connectionTimeout, receiveTimeout);
        return client.accept(MediaType.WILDCARD).get(String.class);
    }

    private void setTimeouts(final WebClient client, final long connectionTimeout, final long receiveTimeout) {
        HTTPConduit conduit = WebClient.getConfig(client).getHttpConduit();
        if (receiveTimeout != 0) {
            conduit.getClient().setReceiveTimeout(receiveTimeout);
        }
        if (connectionTimeout != 0) {
            conduit.getClient().setConnectionTimeout(connectionTimeout);
        }
    }


    @SuppressWarnings("unchecked")
    public RestfulList<PluginRestful> getPlugins() {
        // TODO use rest client
        if (isEnabled()) {
            try {
                final JAXBContext context = JAXBContext.newInstance(RestfulList.class);
                final Unmarshaller unmarshaller = context.createUnmarshaller();
                return (RestfulList<PluginRestful>) unmarshaller.unmarshal(getClass().getResourceAsStream("/rest.xml"));
            } catch (JAXBException e) {
                log.error("Error parsing XML", e);
            }

        } else {

            final WebClient client = WebClient.create(baseResourceUri);
            setTimeouts(client, connectionTimeout, receiveTimeout);
            return client.path("plugins").accept(MediaType.APPLICATION_XML).get(RestfulList.class);
        }
        return null;
    }

    private boolean isEnabled() {
        return true;
    }


}
