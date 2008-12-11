package org.hippoecm.hst.core.filters.domain;


public interface DomainMapping {
    
    void init() throws DomainMappingException;
    boolean isInitialized();
    void setInitialized(boolean initialized);
    public Domain match(String serverName);
    public Domain getDomainByRepositoryPath(String repositoryPath);
    public Domain getPrimaryDomain();

    public String getServletContextPath();
    public boolean isServletContextPathInUrl();
    
    public void setServletContextPath(String servletContextPath);
    public void setServletContextPathInUrl(boolean isServletContextPathInUrl);
    
}
