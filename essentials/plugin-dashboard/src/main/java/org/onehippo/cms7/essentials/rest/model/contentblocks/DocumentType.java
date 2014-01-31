package org.onehippo.cms7.essentials.rest.model.contentblocks;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

import org.onehippo.cms7.essentials.rest.model.KeyValueRestful;
import org.onehippo.cms7.essentials.rest.model.Restful;
import org.onehippo.cms7.essentials.rest.model.RestfulList;

/**
 * @version "$Id$"
 */
@XmlRootElement(name = "documentType")
public class DocumentType extends KeyValueRestful implements Restful {

    private static final long serialVersionUID = 1L;

    private RestfulList<KeyValueRestful> providers;

    public DocumentType() {
    }

    public DocumentType(final String key, final String value, final RestfulList<KeyValueRestful> providers) {
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