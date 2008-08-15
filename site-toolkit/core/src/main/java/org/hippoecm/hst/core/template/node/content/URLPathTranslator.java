package org.hippoecm.hst.core.template.node.content;

import javax.jcr.Session;

public interface URLPathTranslator {

    public String documentPathToURL(Session jcrSession, String documentPath);
}
