/*
 * Copyright 2013 Hippo B.V. (http://www.onehippo.com)
 */

package org.onehippo.cms7.essentials.dashboard.recorder;

/**
 * * @version "$Id: InstallerInstruction.java 171647 2013-07-25 09:39:57Z mmilicevic $"
 */
public enum InstallerInstruction implements InstructionType {

    NODE_WRITE("Node instruction", "Writing a JCR node");

    private final String description;
    private final String type;


    private InstallerInstruction(final String type, final String description) {
        this.description = description;
        this.type = type;
    }

    @Override
    public String description() {
        return description;
    }

    @Override
    public String type() {
        return type;
    }
}
