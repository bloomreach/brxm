package org.onehippo.cms7.essentials.dashboard.model.hst;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @version "$Id$"
 */
public class TemplateExistsException extends Exception {

    private static Logger log = LoggerFactory.getLogger(TemplateExistsException.class);

    public TemplateExistsException(final String s) {
        super(s);
    }
}
