/*
 * Copyright 2013 Hippo B.V. (http://www.onehippo.com)
 */

package org.onehippo.cms7.essentials.dashboard.model;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlValue;

import org.onehippo.cms7.essentials.dashboard.config.Asset;
import org.onehippo.cms7.essentials.dashboard.utils.EssentialConst;

/**
 * @version "$Id: PluginAsset.java 172296 2013-07-31 09:32:49Z mmilicevic $"
 */
@XmlRootElement(name = "asset", namespace = EssentialConst.URI_ESSENTIALS_PLUGIN)
public class PluginAsset implements Asset {

    private static final long serialVersionUID = 1L;
    private String id;
    private String url;
    private String data;
    private String mimeType = "plain/text";


    public PluginAsset(final String id) {
        this.id = id;

    }

    public PluginAsset(final String id, final String url) {
        this.id = id;
        this.url = url;
    }

    public PluginAsset(final String id, final String url, final String mimeType) {
        this.id = id;
        this.url = url;
        this.mimeType = mimeType;
    }

    public PluginAsset() {
    }

    @Override
    @XmlAttribute(namespace = EssentialConst.URI_ESSENTIALS_PLUGIN)
    public String getMimeType() {
        return mimeType;
    }

    @Override
    public void setMimeType(final String mimeType) {
        this.mimeType = mimeType;
    }

    @XmlValue
    @Override
    public String getData() {
        return data;
    }

    @Override
    public void setData(final String data) {
        this.data = data;
    }

    @XmlAttribute
    @Override
    public String getUrl() {
        return url;
    }

    @Override
    public void setUrl(final String url) {
        this.url = url;
    }

    @XmlAttribute
    @Override
    public String getId() {
        return id;
    }

    @Override
    public void setId(final String id) {
        this.id = id;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("PluginAsset{");
        sb.append("id='").append(id).append('\'');
        sb.append(", url='").append(url).append('\'');
        sb.append(", data='").append(data).append('\'');
        sb.append('}');
        return sb.toString();
    }
}
