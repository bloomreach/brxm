package org.hippoecm.hst.core.domain;

import javax.jcr.Item;
import javax.jcr.ItemNotFoundException;
import javax.jcr.Node;
import javax.jcr.PathNotFoundException;
import javax.jcr.Repository;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.hippoecm.hst.core.domain.Domain;
import org.hippoecm.hst.core.domain.DomainMapping;
import org.hippoecm.hst.core.domain.RepositoryMapping;
import org.hippoecm.hst.core.domain.RepositoryMappingException;
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
    private boolean template;
    private Repository repository;

    public RepositoryMappingImpl(Repository repository, DomainMapping domainMapping, Domain domain, String prefix, String path) throws RepositoryMappingException{
        this.repository = repository;
        this.prefix = prefix;
        this.path = path;
        this.depth = prefix.split("/").length;
        this.domain = domain;
        this.domainMapping = domainMapping;
        
        if(prefix.contains(PATH_WILDCARD) || path.contains(PATH_WILDCARD)) {
            if(!(prefix.contains(PATH_WILDCARD) && path.contains(PATH_WILDCARD))) {
                throw new RepositoryMappingException("If the prefix or the path in the hst:repositorypath ends with a *, then both have to end with a *. Ignore this mapping");
            }
            if(prefix.equals(PATH_WILDCARD) || path.equals(PATH_WILDCARD)) {
                throw new RepositoryMappingException("The hst:repositorypath is not allowed to have a prefix or path only consisting of '"+PATH_WILDCARD+"'. Skipping mapping");
            }
            log.debug("Repository mapping '{}' for domain '{}' functions as a template", prefix+":"+path, domain.getPattern());
            this.template = true;
        } else {
            this.template = false;
            init();
        }
        
        
    }
    
    private void init() throws RepositoryMappingException {
        Session session = null;
        
        try {
            session = repository.login();
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
        finally
        {
            if (session != null)
                session.logout();
        }
    }

    /* (non-Javadoc)
     * @see org.hippoecm.hst.core.filters.domain.RepositoryMapping#match(java.lang.String)
     */
    public boolean match(String uri) {
        if(uri == null) {
            return false;
        }
        if(uri.startsWith(prefix)) {
            return true;
        }
        
        if(isTemplate()) {
           if(prefix.contains(PATH_WILDCARD) && !prefix.equals(PATH_WILDCARD)) {
               if(uri.startsWith(prefix.substring(0, prefix.indexOf(PATH_WILDCARD)-1))) {
                   // the template matches
                   log.debug("The repository mapping '{}' matches '{}' as a template. A non template Repository Mapping will be created", prefix+":"+path, uri);
                   return true;
               }
           }
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
            } else if(this.depth == repositoryMappingImpl.depth) {
                if(this.isTemplate()) {
                    return 1;
                } else if(repositoryMappingImpl.isTemplate()){
                    return -1;
                }
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
   
    public boolean isTemplate() {
        return template;
    }
    
    /* (non-Javadoc)
     * @see org.hippoecm.hst.core.filters.domain.RepositoryMapping#getMapping()
     */
    public String getPath() {
        return path;
    }

    public int getDepth() {
        return depth;
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
