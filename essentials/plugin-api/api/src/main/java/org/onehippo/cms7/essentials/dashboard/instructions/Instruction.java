/*
 * Copyright 2013 Hippo B.V. (http://www.onehippo.com)
 */

package org.onehippo.cms7.essentials.dashboard.instructions;

import javax.xml.bind.annotation.XmlTransient;

/**
 * @version "$Id$"
 */
@XmlTransient
public interface Instruction {

    String getMessage();
    void setMessage(String message);

    String getAction();
    void setAction(String action);
}
