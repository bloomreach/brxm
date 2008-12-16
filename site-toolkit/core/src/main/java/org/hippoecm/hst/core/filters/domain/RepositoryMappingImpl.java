package org.hippoecm.hst.core.filters.domain;

import javax.jcr.Item;
import javax.jcr.ItemNotFoundException;
import javax.jcr.Node;
import javax.jcr.PathNotFoundException;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.hippoecm.repository.api.HippoNodeType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RepositoryMappingImpl implements RepositoryMapping {
    
    private static final Logger log = LoggerFactory.getLogger(RepositoryMappingImpl.class);
    
    private static final String DEFAULT_RELATIVE_CONTENT = "content";
    private static final String DEFAULT_RELATIVE_HST_CONFIGURATION = "hst:configuration/hst:configuration";
    private String prefix;
    private String path;
    private String canonicalContentPath;
    private int depth;
    private Domain domain;
    private DomainMapping domainMapping;
    
    public RepositoryMappingImpl(Session session, DomainMapping domainMapping, Domain domain, String prefix, String path) throws RepositoryMappingException{
        this.prefix = prefix;
        this.path = path;
        this.depth = prefix.split("/").length;
        this.domain = domain;
        this.domainMapping = domainMapping;
            
        try {
            Item item = session.getItem(this.getContentPath());
            if(item.isNode()) {
                Node node = (Node)item;
                if(node.isNodeType(HippoNodeType.NT_FACETSELECT)) {
                    String docBaseUuid = node.getProperty(HippoNodeType.HIPPO_DOCBASE).getString();
                    try {
                      Node canonical = session.getNodeByUUID(docBaseUuid); 
                      this.canonicalContentPath = canonical.getPath();
                    } catch (ItemNotFoundException e) {
                      throw new RepositoryMappingException("Cannot create Repository Mapping because cannot find canonical content path '"+this.getHstConfigPath()+"'.", e);
                    }
                } else {
                    throw new RepositoryMappingException("Cannot create Repository Mapping because the content path '"+ this.getContentPath()+"' does not point to a node that is of type 'hippo:facetselect'.");
                }
            } else {
                throw new RepositoryMappingException("Cannot create Repository Mapping because the content path '"+ this.getContentPath()+"' points to a property.");
            }
        } catch (PathNotFoundException e) {
            throw new RepositoryMappingException("PathNotFoundException: Cannot create Repository Mapping because the content path '"+ this.getContentPath()+"' can not be found.", e);
        } catch (RepositoryException e) {
            throw new RepositoryMappingException("RepositoryException: Cannot create Repository Mapping because an exception  happened for '"+ this.getContentPath()+"'.", e);
        }
        
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

    public String getCanonicalContentPath() {
        return this.canonicalContentPath;
    }

}
