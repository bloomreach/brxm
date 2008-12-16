package org.hippoecm.hst.core.filters.domain;

public interface RepositoryMapping extends Comparable<RepositoryMapping>{

    public boolean match(String uri);

    public String getPrefix();

    public String getPath();
    
    /**
     * 
     * @return Repository path to the content start point. Normally, this is a facetselect (filtered mirror) below which for example only published nodes are found.
     */
    public String getContentPath();
    
    /**
     * 
     * @return Canonical repository path of the content start point found by getContentPath()
     */
    public String getCanonicalContentPath();
    
    /**
     * 
     * @return Repository path to the hst:configuration node. Normally, this is a facetselect (filtered mirror) below which for example only published nodes are found.
     */
    public String getHstConfigPath();
    
    public Domain getDomain();
    
    public DomainMapping getDomainMapping();

}