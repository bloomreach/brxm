package org.hippoecm.hst.content.beans.standard;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @version "$Id$"
 */
public class MockHippoResource extends HippoResource {

    @SuppressWarnings("unused")
    private static Logger log = LoggerFactory.getLogger(MockHippoResource.class);

    private long length;

    public MockHippoResource(final long length) {
        this.length = length;
    }

    public long getLength() {
        return length;
    }

    public void setLength(final long length) {
        this.length = length;
    }
}

