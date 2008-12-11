package org.hippoecm.hst.core.filters.domain;

public interface RepositoryMapping extends Comparable<RepositoryMapping>{

    public boolean match(String uri);

    public String getPrefix();

    public String getPath();
    
    public String getContentPath();
    
    public String getHstConfigPath();
    
    public Domain getDomain();
    
    public DomainMapping getDomainMapping();

}