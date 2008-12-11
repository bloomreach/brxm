package org.hippoecm.hst.core.filters.domain;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;

import org.hippoecm.hst.core.mapping.UrlUtilities;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DomainMappingRequestWrapper  extends HttpServletRequestWrapper{

    private static final Logger log = LoggerFactory.getLogger(DomainMappingRequestWrapper.class);
    
    private RepositoryMapping repositoryMapping;
    private String transformerUri;
    
    public DomainMappingRequestWrapper(HttpServletRequest request, RepositoryMapping repositoryMapping) {
        super(request);
        this.repositoryMapping = repositoryMapping;
    }


    @Override
    public String getRequestURI() {
        if(transformerUri != null) {
            // within one requests, only go ones through the transformer below
            return transformerUri;
        }
        if(repositoryMapping == null ) {
            log.warn("No repository mapping found for request uri '{}'. Try to process request without mapping", super.getRequestURI());
            return super.getRequestURI();
        } else {
            
            String uri = super.getRequestURI();
            uri = uri.substring(this.getContextPath().length());
            // replace the prefix with the repository path in the mapping
            if(repositoryMapping.getPrefix() != null ) {
                uri = uri.substring(repositoryMapping.getPrefix().length());
            }
            /*
             * we forward the url without the context path and without the repository prefix.
             * On the hstRequestContext we have the RepositoryMapping object available
             */  
            uri = UrlUtilities.decodeUrl(uri);
            log.debug("wrapped request uri to internal uri '{}' --> '{}'", super.getRequestURI(), uri);
            transformerUri = uri;
            return transformerUri;   
        }
        
    }
  
}
