package org.hippoecm.hst.core.filters.domain;


public interface Domain extends Comparable<Domain>{ 

    // The DELIMITER is a "." but it needs to be escaped for regexp hence the \\
    public static final String DELIMITER = "\\.";
    public static final String WILDCARD = "_default_";
    
    public DomainMapping getDomainMapping();
    public String getPattern();
    public RepositoryMapping getRepositoryMapping(String ctxStippedUri);
    public RepositoryMapping[] getRepositoryMapping();
    public Domain getDomainByRepositoryPath(String repositoryPath);
    public boolean isExactHost();
    public boolean isRedirect();
    public String getRedirect();
    /**
     * Return whether a host pattern matches or not
     * @param host
     * @return
     */
    public boolean match(String serverName, String[] serverNameParts);
}
