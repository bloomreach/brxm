package org.hippoecm.hst.site.request;

import javax.jcr.Credentials;
import javax.jcr.Repository;

import org.hippoecm.hst.core.request.HstRequestContext;
import org.hippoecm.hst.core.request.HstRequestContextComponent;

public class HstRequestContextComponentImpl implements HstRequestContextComponent {

    protected Repository repository;
    protected Credentials defaultCredentials;

    public HstRequestContextComponentImpl(Repository repository, Credentials defaultCredentials) {
        this.repository = repository;
        this.defaultCredentials = defaultCredentials;
    }

    public HstRequestContext create() {
        return new HstRequestContextImpl(this.repository, this.defaultCredentials);
    }

    public void release(HstRequestContext context) {
    }
}
