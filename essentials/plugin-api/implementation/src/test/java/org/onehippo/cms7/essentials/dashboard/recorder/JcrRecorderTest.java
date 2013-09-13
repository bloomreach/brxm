/*
 * Copyright 2013 Hippo B.V. (http://www.onehippo.com)
 */

package org.onehippo.cms7.essentials.dashboard.recorder;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @version "$Id: JcrRecorderTest.java 171648 2013-07-25 09:40:01Z mmilicevic $"
 */
public class JcrRecorderTest {

    private static Logger log = LoggerFactory.getLogger(JcrRecorderTest.class);

    @Test
    public void testRecord() throws Exception {
        final JcrInstruction instruction = new JcrInstruction();
        instruction.record();


    }
}
