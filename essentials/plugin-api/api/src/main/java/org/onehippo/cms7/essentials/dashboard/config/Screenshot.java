/*
 * Copyright 2013 Hippo B.V. (http://www.onehippo.com)
 */

package org.onehippo.cms7.essentials.dashboard.config;

import java.io.Serializable;

import javax.xml.bind.annotation.XmlTransient;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @version "$Id: Screenshot.java 171742 2013-07-25 16:39:14Z mmilicevic $"
 */
@XmlTransient
public interface Screenshot extends Serializable {


    String getPath();

    void setPath(String path);
}
