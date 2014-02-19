package org.onehippo.cms7.essentials.rest.model.contentblocks;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

import org.codehaus.jackson.annotate.JsonSubTypes;
import org.onehippo.cms7.essentials.dashboard.rest.KeyValueRestful;
import org.onehippo.cms7.essentials.dashboard.model.Restful;
import org.onehippo.cms7.essentials.rest.model.RestList;


/**
 * @version "$Id$"
 */
@XmlRootElement(name = "documentType")
public class DocumentType extends KeyValueRestful implements Restful {

    private static final long serialVersionUID = 1L;

    public DocumentType() {
    }

    private RestList<KeyValueRestful> providers;

    public DocumentType(final String key, final String value, final RestList<KeyValueRestful> providers) {
        super(key, value);
        this.providers = providers;
    }

    @XmlElement(name = "providers")
    @JsonSubTypes({
            @JsonSubTypes.Type(value = KeyValueRestful.class, name = "keyvalue")})
    public RestList<KeyValueRestful> getProviders() {
        return providers;
    }

    public void setProviders(final RestList<KeyValueRestful> providers) {
        this.providers = providers;
    }
}