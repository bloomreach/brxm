package org.onehippo.cms7.essentials.rest.model;

import java.io.Serializable;

import javax.xml.bind.annotation.XmlRootElement;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @version "$Id$"
 */
@XmlRootElement(name = "vendor")
public class VendorRestful implements Restful {

    private static final long serialVersionUID = 1L;
    private static Logger log = LoggerFactory.getLogger(VendorRestful.class);
    private String url;
    private String name;
    private String logo;
    private String introduction;
    private String content;


    public String getUrl() {
        return url;
    }

    public void setUrl(final String url) {
        this.url = url;
    }

    public String getName() {
        return name;
    }

    public void setName(final String name) {
        this.name = name;
    }

    public String getLogo() {
        return logo;
    }

    public void setLogo(final String logo) {
        this.logo = logo;
    }

    public String getIntroduction() {
        return introduction;
    }

    public void setIntroduction(final String introduction) {
        this.introduction = introduction;
    }

    public String getContent() {
        return content;
    }

    public void setContent(final String content) {
        this.content = content;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("VendorRestful{");
        sb.append("name='").append(name).append('\'');
        sb.append(", url='").append(url).append('\'');
        sb.append('}');
        return sb.toString();
    }


}
