package org.hippoecm.hst.core.template.node.content;

import javax.jcr.Session;

public interface SourceRewriter {

    /**
     * Search content for hrefs/src's and replace them with a translated value
     */
    public String replace(final Session jcrSession, String content);

}