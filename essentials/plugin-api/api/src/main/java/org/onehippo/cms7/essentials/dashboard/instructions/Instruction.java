/*
 * Copyright 2013 Hippo B.V. (http://www.onehippo.com)
 */

package org.onehippo.cms7.essentials.dashboard.instructions;

import java.util.Map;

import javax.xml.bind.annotation.XmlTransient;

import org.onehippo.cms7.essentials.dashboard.ctx.PluginContext;

/**
 * @version "$Id$"
 */
@XmlTransient
public interface Instruction {

    String getMessage();

    void setMessage(String message);

    String getAction();

    void setAction(String action);

    InstructionStatus process(PluginContext context, InstructionStatus previousStatus);

    void processPlaceholders(final Map<String, Object> data);
}
