/*
 * Copyright 2013 Hippo B.V. (http://www.onehippo.com)
 */

package org.onehippo.cms7.essentials.dashboard.recorder;

import javax.jcr.Node;
import javax.jcr.Session;

import org.onehippo.cms7.essentials.dashboard.PluginConfigService;
import org.onehippo.cms7.essentials.dashboard.ctx.PluginContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @version "$Id: JcrInstruction.java 171782 2013-07-26 09:32:16Z mmilicevic $"
 */
public class JcrInstruction implements Instruction {

    private static Logger log = LoggerFactory.getLogger(JcrInstruction.class);

    @Override
    public Instruction record() {

        log.debug("*** recording:  {}", this);
        return this;
    }


}
