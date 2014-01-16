package org.onehippo.cms7.essentials.rest.client;

import javax.ws.rs.core.MediaType;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;

import org.apache.cxf.jaxrs.client.WebClient;
import org.onehippo.cms7.essentials.rest.model.PluginRestful;
import org.onehippo.cms7.essentials.rest.model.RestfulList;
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

    public RestClient(String baseResourceUri) {
        this.baseResourceUri = baseResourceUri;
    }

    public String getPluginList(){
        final WebClient client = WebClient.create(baseResourceUri);
        return client.accept(MediaType.WILDCARD).get(String.class);
    }


    @SuppressWarnings("unchecked")
    public RestfulList<PluginRestful> getPlugins() {

        // TODO use rest client
        if (true) {
            try {
                final JAXBContext context = JAXBContext.newInstance(RestfulList.class);
                final Unmarshaller unmarshaller = context.createUnmarshaller();
                return (RestfulList<PluginRestful>) unmarshaller.unmarshal(getClass().getResourceAsStream("/rest.xml"));
            } catch (JAXBException e) {
                log.error("Error parsing XML", e);
            }

        } else {
            final WebClient client = WebClient.create(baseResourceUri);
            return client.path("plugins").accept(MediaType.APPLICATION_XML).get(RestfulList.class);
        }
        return null;
    }


}
