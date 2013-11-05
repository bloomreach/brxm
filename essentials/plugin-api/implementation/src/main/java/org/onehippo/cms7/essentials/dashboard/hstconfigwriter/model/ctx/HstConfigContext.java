/*
 * Copyright 2013 Hippo B.V. (http://www.onehippo.com)
 */

package org.onehippo.cms7.essentials.dashboard.hstconfigwriter.model.ctx;

/**
 * @version "$Id: HstConfigContext.java 171483 2013-07-24 09:26:52Z mmilicevic $"
 */
public class HstConfigContext implements Context {


    private boolean debug = true;
    private boolean merge;

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("HstConfigContext{");
        sb.append("debug=").append(debug);
        sb.append(", merge=").append(merge);
        sb.append('}');
        return sb.toString();
    }

    @Override
    public boolean isDebug() {
        return debug;
    }

    @Override
    public void setDebug(final boolean debug) {
        this.debug = debug;
    }

    @Override
    public boolean isMerge() {
        return merge;
    }

    @Override
    public void setMerge(final boolean merge) {
        this.merge = merge;
    }
}
