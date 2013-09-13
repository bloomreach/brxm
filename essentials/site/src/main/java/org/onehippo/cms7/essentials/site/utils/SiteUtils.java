/*
 * Copyright 2013 Hippo B.V. (http://www.onehippo.com)
 */

package org.onehippo.cms7.essentials.site.utils;

import java.util.UUID;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @version "$Id: SiteUtils.java 157405 2013-03-08 09:15:49Z mmilicevic $"
 */
public final class SiteUtils {

    private static Logger log = LoggerFactory.getLogger(SiteUtils.class);

    public static String  createPluginId(){
        return String.format("id%s", StringUtils.remove(UUID.randomUUID().toString(), '-').substring(16));
    }

    private SiteUtils() {
    }
}
