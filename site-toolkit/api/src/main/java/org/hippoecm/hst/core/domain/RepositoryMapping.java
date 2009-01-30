package org.hippoecm.hst.core.domain;

public interface RepositoryMapping extends Comparable<RepositoryMapping>{
    public static final String PATH_WILDCARD = "*";
    
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
    
    /**
     * When the repository mapping is a template, it functions to create 'real' repository mappings. When the hst:repositorypath contains
     * a wildcard (*), the repository mapping is handles as a template. Only after a match to a wildcard, the actual repository mapping is created and
     * added to the DomainMapping
     * 
     * @return boolean whether this repository Mapping functions as a RepositoryMapping template 
     */
    
    public boolean isTemplate();
    
    public int getDepth();
    
    public Domain getDomain();
    
    public DomainMapping getDomainMapping();

}