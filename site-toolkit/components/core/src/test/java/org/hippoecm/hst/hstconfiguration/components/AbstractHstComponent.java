package org.hippoecm.hst.hstconfiguration.components;

import javax.jcr.LoginException;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.servlet.ServletConfig;

import org.hippoecm.hst.core.component.HstComponent;
import org.hippoecm.hst.core.component.HstComponentException;
import org.hippoecm.hst.core.component.HstRequest;
import org.hippoecm.hst.core.component.HstResponse;
import org.hippoecm.hst.core.request.HstRequestContext;

public abstract class AbstractHstComponent implements HstComponent {

    public void init(ServletConfig servletConfig) throws HstComponentException {
    }

    public void destroy() throws HstComponentException {
    }

    public void doAction(HstRequest request, HstResponse response) throws HstComponentException {
    }

    public void doBeforeRender(HstRequest request, HstResponse response) throws HstComponentException {
    }

    public void doBeforeServeResource(HstRequest request, HstResponse response) throws HstComponentException {
    }

    protected Session getSession(HstRequest request) throws HstComponentException {
        HstRequestContext requestContext = request.getRequestContext();
        Session session = null;
        
        try {
            session = requestContext.getSession();
        } catch (LoginException e) {
            throw new HstComponentException(e);
        } catch (RepositoryException e) {
            throw new HstComponentException(e);
        }
        
        return session;
    }
}
