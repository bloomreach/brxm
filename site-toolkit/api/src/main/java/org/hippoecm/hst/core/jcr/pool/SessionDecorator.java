package org.hippoecm.hst.core.jcr.pool;

import javax.jcr.Session;

public interface SessionDecorator
{
    public Session decorate(Session session);
}
