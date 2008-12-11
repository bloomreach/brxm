package org.hippoecm.hst.core.filters.domain;

public class RepositoryMappingImpl implements RepositoryMapping {

    private static final String DEFAULT_RELATIVE_CONTENT = "content";
    private static final String DEFAULT_RELATIVE_HST_CONFIGURATION = "hst:configuration/hst:configuration";
    private String prefix;
    private String path;
    private int depth;
    private Domain domain;
    private DomainMapping domainMapping;
    
    public RepositoryMappingImpl(DomainMapping domainMapping, Domain domain, String prefix, String path) {
        this.prefix = prefix;
        this.path = path;
        this.depth = prefix.split("/").length;
        this.domain = domain;
        this.domainMapping = domainMapping;
    }
    
    /* (non-Javadoc)
     * @see org.hippoecm.hst.core.filters.domain.RepositoryMapping#match(java.lang.String)
     */
    public boolean match(String uri) {
        if(uri.startsWith(prefix)) {
            return true;
        }
        return false;
    }
    
    public int compareTo(RepositoryMapping repositoryMapping) {
        if(repositoryMapping instanceof RepositoryMappingImpl) {
            RepositoryMappingImpl repositoryMappingImpl = (RepositoryMappingImpl) repositoryMapping;
            if(this.depth > repositoryMappingImpl.depth) {
                return -1;
            } else if(this.depth < repositoryMappingImpl.depth) {
                return 1;
            }
        }
        return 0;
    }

    /* (non-Javadoc)
     * @see org.hippoecm.hst.core.filters.domain.RepositoryMapping#getPrefix()
     */
    public String getPrefix() {
        return prefix;
    }

    /* (non-Javadoc)
     * @see org.hippoecm.hst.core.filters.domain.RepositoryMapping#getMapping()
     */
    public String getPath() {
        return path;
    }

    public Domain getDomain() {
        return domain;
    }

    public DomainMapping getDomainMapping() {
        return this.domainMapping;
    }

    public String getContentPath() {
        return this.getPath()+"/"+DEFAULT_RELATIVE_CONTENT;
    }

    public String getHstConfigPath() {
        return this.getPath()+"/"+DEFAULT_RELATIVE_HST_CONFIGURATION;
    }

}
