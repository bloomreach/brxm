package org.onehippo.cms7.essentials.rest.model;

import java.io.Serializable;

import javax.xml.bind.annotation.XmlRootElement;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @version "$Id$"
 */
@XmlRootElement(name = "item")
public class PluginRestful implements Restful {

    private static final long serialVersionUID = 1L;
    private static Logger log = LoggerFactory.getLogger(PluginRestful.class);
    private VendorRestful vendor;
    private DependencyRestful dependency;
    private String title;
    private String introduction;

    public VendorRestful getVendor() {
        return vendor;
    }

    public void setVendor(final VendorRestful vendor) {
        this.vendor = vendor;
    }


    public String getTitle() {
        return title;
    }

    public void setTitle(final String title) {
        this.title = title;
    }

    public String getIntroduction() {
        return introduction;
    }

    public void setIntroduction(final String introduction) {
        this.introduction = introduction;
    }

    public DependencyRestful getDependency() {
        return dependency;
    }

    public void setDependency(final DependencyRestful dependency) {
        this.dependency = dependency;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("PluginRestful{");
        sb.append("vendor=").append(vendor);
        sb.append(", title='").append(title).append('\'');
        sb.append(", introduction='").append(introduction).append('\'');
        sb.append('}');
        return sb.toString();
    }
}
