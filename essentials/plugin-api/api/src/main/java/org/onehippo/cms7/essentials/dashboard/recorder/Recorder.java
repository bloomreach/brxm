/*
 * Copyright 2013 Hippo B.V. (http://www.onehippo.com)
 */

package org.onehippo.cms7.essentials.dashboard.recorder;

/**
 * @version "$Id: Recorder.java 171648 2013-07-25 09:40:01Z mmilicevic $"
 */
public interface Recorder {

    Instruction record(Instruction instruction);
}
