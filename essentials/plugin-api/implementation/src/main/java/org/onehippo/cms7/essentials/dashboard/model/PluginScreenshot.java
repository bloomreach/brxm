package org.onehippo.cms7.essentials.dashboard.model;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

import org.onehippo.cms7.essentials.dashboard.config.Screenshot;
import org.onehippo.cms7.essentials.dashboard.utils.EssentialConst;

/**
 * @version "$Id: PluginScreenshot.java 171742 2013-07-25 16:39:14Z mmilicevic $"
 */
@XmlRootElement(name = "screenshot", namespace = EssentialConst.URI_ESSENTIALS_PLUGIN)
public class PluginScreenshot implements Screenshot {

    private static final long serialVersionUID = 1L;
    private String path;

    public PluginScreenshot() {
    }

    public PluginScreenshot(final String path) {
        this.path = path;
    }

    @XmlElement(namespace = EssentialConst.URI_ESSENTIALS_PLUGIN)
    @Override
    public String getPath() {
        return path;
    }

    @Override
    public void setPath(final String path) {
        this.path = path;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("PluginScreenshot{");
        sb.append("path='").append(path).append('\'');
        sb.append('}');
        return sb.toString();
    }
}
