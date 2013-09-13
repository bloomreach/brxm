/*
 * Copyright 2013 Hippo B.V. (http://www.onehippo.com)
 */

package org.onehippo.cms7.essentials.dashboard.recorder;

import org.onehippo.cms7.essentials.dashboard.ctx.PluginContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @version "$Id: JcrRecorder.java 171655 2013-07-25 10:57:48Z mmilicevic $"
 */
public class JcrRecorder implements Recorder {

    private static Logger log = LoggerFactory.getLogger(JcrRecorder.class);

    public static final String RECORDER_NODE = "installer_log";

    final PluginContext context;


    public JcrRecorder(final PluginContext context) {
        this.context = context;
    }



    @Override
    public Instruction record(final Instruction instruction) {
        log.debug("### RECORDING INSTRUCTION : {}", instruction);
        return instruction;
    }


}
