package org.hippoecm.hst.core.mapping;

import javax.jcr.Session;

import org.hippoecm.hst.core.filters.base.HstRequestContext;
import org.hippoecm.hst.core.filters.domain.RepositoryMapping;

public interface URLMappingManager {
    
    public URLMapping getUrlMapping(HstRequestContext hstRequestContext) throws URLMappingException;
    
    public URLMapping getUrlMapping(RepositoryMapping repositoryMapping, URLMappingManager urlMappingManager, Session jcrSession) throws URLMappingException;
}