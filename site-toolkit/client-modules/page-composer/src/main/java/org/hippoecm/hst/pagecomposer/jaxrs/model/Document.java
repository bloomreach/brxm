package org.hippoecm.hst.pagecomposer.jaxrs.model;

import javax.xml.bind.annotation.XmlRootElement;

/**
 * Created by IntelliJ IDEA. User: vijaykiran Date: 5/1/11 Time: 2:41 PM To change this template use File | Settings |
 * File Templates.
 */
@XmlRootElement(name = "document")
public class Document {
    private String path;

    public Document(final String path) {
        this.path = path;
    }

    public String getPath() {
        return path;
    }

    public void setPath(final String path) {
        this.path = path;
    }
}
