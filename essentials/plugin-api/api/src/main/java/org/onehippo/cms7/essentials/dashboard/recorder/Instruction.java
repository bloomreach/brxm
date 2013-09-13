/*
 * Copyright 2013 Hippo B.V. (http://www.onehippo.com)
 */

package org.onehippo.cms7.essentials.dashboard.recorder;

/**
 * @version "$Id: Instruction.java 171648 2013-07-25 09:40:01Z mmilicevic $"
 */
public interface Instruction {

    /**
     * Instruction description
     *
     * @return instruction description
     */
    //String description();

    /**
     * Returns type of instruction
     *
     * @return type of instruction
     */
    //InstructionType getType();

    /**
     * Executes an instruction.
     *
     * @return returns itself
     */
    Instruction record();

    /**
     * rollbacks an instruction.
     * <p><strong>NOTE:</strong> some instructions cannot be rolled back</p>
     *
     * @return returns itself
     */
    //Instruction rollback();
}
