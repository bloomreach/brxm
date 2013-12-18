package org.onehippo.cms7.essentials.rest.model.contentblocks;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlRootElement;

import org.onehippo.cms7.essentials.rest.model.KeyValueRestful;
import org.onehippo.cms7.essentials.rest.model.Restful;
import org.onehippo.cms7.essentials.rest.model.RestfulList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @version "$Id$"
 */
@XmlRootElement(name = "items")
public class DocumentTypes extends KeyValueRestful implements Restful {

    private static final long serialVersionUID = 1L;

    private RestfulList<KeyValueRestful> providers;

    public DocumentTypes() {
    }

    public DocumentTypes(final String key, final String value, final RestfulList<KeyValueRestful> providers) {
        super(key, value);
        this.providers = providers;
    }

    @XmlElement(name = "providers")
    public RestfulList<KeyValueRestful> getProviders() {
        return providers;
    }

    public void setProviders(final RestfulList<KeyValueRestful> providers) {
        this.providers = providers;
    }
}