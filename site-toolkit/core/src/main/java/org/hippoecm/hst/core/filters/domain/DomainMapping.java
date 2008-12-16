package org.hippoecm.hst.core.filters.domain;


public interface DomainMapping {
    
    void init() throws DomainMappingException;
    boolean isInitialized();
    
    void setInitialized(boolean initialized);
    
    public Domain match(String serverName);
    
    public RepositoryMapping getRepositoryMapping(String repositoryPath, Domain currentDomain);
    
    public Domain getPrimaryDomain();
    
    /**
     * This is a shortcut test to know whether a node is below the gallery or assets folder
     * 
     * @param path
     * @return true when the path is below the repository 'gallery' or 'assets' folder
     */
    public boolean isBinary(String path);

    public String getServletContextPath();
    
    public boolean isServletContextPathInUrl();
    
    public void setServletContextPath(String servletContextPath);
    
    public void setServletContextPathInUrl(boolean isServletContextPathInUrl);
   
    public void setScheme(String scheme);
    
    public String getScheme();
    
    public void setPortInUrl(boolean isPortInUrl);

    public boolean isPortInUrl();
    
    public void setPort(int portNumber);
    
    public int getPort();
    
}
