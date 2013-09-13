/*
 * Copyright 2013 Hippo B.V. (http://www.onehippo.com)
 */

package org.onehippo.cms7.essentials.shared.model;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @version "$Id: Licence.java 157405 2013-03-08 09:15:49Z mmilicevic $"
 */
public class Licence {

    private static Logger log = LoggerFactory.getLogger(Licence.class);

    private String name;

    public String getName() {
        return name;
    }

    public void setName(final String name) {
        this.name = name;
    }
}
