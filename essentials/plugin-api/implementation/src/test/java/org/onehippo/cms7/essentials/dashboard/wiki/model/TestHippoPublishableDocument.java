package org.onehippo.cms7.essentials.dashboard.wiki.model;

import org.jcrom.annotations.JcrParentNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @version "$Id$"
 */
abstract public class TestHippoPublishableDocument extends TestHippoDocument {

    private static Logger log = LoggerFactory.getLogger(TestHippoPublishableDocument.class);

    @JcrParentNode
    private TestHippoHandle handle;

    public TestHippoHandle getHandle() {
        return handle;
    }

    public void setHandle(final TestHippoHandle handle) {
        this.handle = handle;
    }
}
