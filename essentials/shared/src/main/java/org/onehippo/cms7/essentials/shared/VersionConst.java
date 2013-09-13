/*
 * Copyright 2013 Hippo B.V. (http://www.onehippo.com)
 */

package org.onehippo.cms7.essentials.shared;

import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @version "$Id: VersionConst.java 157405 2013-03-08 09:15:49Z mmilicevic $"
 */
public final class VersionConst {

    private static Logger log = LoggerFactory.getLogger(VersionConst.class);
    public static final Pattern VERSION_PATTERN = Pattern.compile("^[\\d]{1,}\\.");

}
