package org.onehippo.cms7.essentials.dashboard.hstconfigwriter.model.ctx;

/**
 * @version "$Id: Context.java 171483 2013-07-24 09:26:52Z mmilicevic $"
 */
public interface Context {

    boolean isDebug();

    void setDebug(boolean debug);

    boolean isMerge();

    void setMerge(boolean merge);
}
