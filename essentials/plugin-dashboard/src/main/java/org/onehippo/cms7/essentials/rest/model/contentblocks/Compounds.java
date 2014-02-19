package org.onehippo.cms7.essentials.rest.model.contentblocks;

import javax.xml.bind.annotation.XmlRootElement;

import org.onehippo.cms7.essentials.dashboard.rest.KeyValueRestful;
import org.onehippo.cms7.essentials.dashboard.model.Restful;

/**
 * @version "$Id$"
 */
@XmlRootElement(name = "items")
public class Compounds extends KeyValueRestful implements Restful {

    private static final long serialVersionUID = 1L;

    private String path;

    public Compounds() {
    }

    public Compounds(final String key, final String value, final String path) {
        super(key, value);
        this.path = path;
    }

    public String getPath() {
        return path;
    }

    public void setPath(final String path) {
        this.path = path;
    }
}