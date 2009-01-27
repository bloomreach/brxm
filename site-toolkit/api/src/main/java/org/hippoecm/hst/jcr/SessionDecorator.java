package org.hippoecm.hst.jcr;

import javax.jcr.Session;

public interface SessionDecorator
{
    public Session decorate(Session session);
}
