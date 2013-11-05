/*
 * Copyright 2013 Hippo B.V. (http://www.onehippo.com)
 */

package org.onehippo.cms7.essentials.dashboard.config;

import java.io.Serializable;

import javax.xml.bind.annotation.XmlTransient;

/**
 * @version "$Id: Asset.java 171785 2013-07-26 09:37:29Z mmilicevic $"
 */
@XmlTransient
public interface Asset extends Serializable {

    String getMimeType();

    void setMimeType(String mimeType);

    String getId();

    void setId(String id);

    String getUrl();

    void setUrl(String url);

    String getData();

    void setData(String data);

}
