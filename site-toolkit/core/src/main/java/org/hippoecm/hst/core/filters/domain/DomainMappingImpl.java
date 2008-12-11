package org.hippoecm.hst.core.filters.domain;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.PathNotFoundException;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.Value;
import javax.servlet.FilterConfig;

import org.hippoecm.hst.jcr.JcrSessionFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;



public class DomainMappingImpl implements DomainMapping{
    
    private static final Logger log = LoggerFactory.getLogger(DomainMapping.class);
    
    private String domainMappingLocation;
    private FilterConfig filterConfig;
    private boolean initialized;
    private Domain[] orderedDomains; 
    private Domain primaryDomain;
    
    private String servletContextPath;
    private boolean isServletContextPathInUrl;
     
  
    public DomainMappingImpl(String domainMappingLocation, FilterConfig filterConfig){
        
        this.domainMappingLocation = domainMappingLocation;
        this.filterConfig = filterConfig;
        
    }

    public void init() throws DomainMappingException{
        Set<Domain> domains = new HashSet<Domain>();
        Session session = null;
        
        // regardless wether initialization succeeds the domain mapping is initialized. If failing, the hst will run without domain mapping
        setInitialized(true);
        
        if(domainMappingLocation == null) {
            throw new DomainMappingException("No domainMappingLocation configured in web.xml");
        }
        
        try {
            session = new JcrSessionFactory(filterConfig).getSession();
            if(domainMappingLocation.startsWith("/")) {
                domainMappingLocation = domainMappingLocation.substring(1);
            }
            if("".equals("domainMappingLocation")) {
                throw new DomainMappingException("domainMappingLocation not allowed to be empty or '/'");
            }
            
            // get the domain mapping node
            try {
                Node domainMappingNode = session.getRootNode().getNode(domainMappingLocation);
                createDomainPatterns(domainMappingNode, domains, "", 1, new ArrayList<String>());
                orderedDomains = domains.toArray(new Domain[domains.size()]);
                Arrays.sort(orderedDomains);
                if(this.primaryDomain == null) {
                    log.warn("There is no primary domain configured which is used as fallback. Set the mixin 'hst:primarydomain' on the domain that is primary");
                }
            } catch (PathNotFoundException e) {
                throw new DomainMappingException("PathNotFoundException for domain mapping node '" + domainMappingLocation + "'" , e.getCause());
            } catch (RepositoryException e) {
                throw new DomainMappingException("RepositoryException for domain mapping node '" + domainMappingLocation + "'" , e.getCause());
            }
            
        } finally {
            if(session!=null) {
                session.logout();
            }
        }
        
        log.info(this.toString());
        
    }

    private void createDomainPatterns(Node domainMappingNode, Set<Domain> domains, String domainPattern, int depth, ArrayList<String> repositorypaths) throws RepositoryException {
        
        NodeIterator mappings = domainMappingNode.getNodes();
        if(depth > 6) {
            log.warn("Reached the maximum depth for domain mapping. Skipping deeper paths");
        }
        
        while(mappings.hasNext()) {
            Node mappingNode = mappings.nextNode();
            if(mappingNode != null) {
                if( !(mappingNode.isNodeType("hst:domainmapping") || mappingNode.isNodeType("hst:redirectmapping"))) {
                    log.warn("Skipping node '{}' because node is not of type 'hst:domainmapping' or 'hst:redirectmapping' " , mappingNode.getPath());
                    continue;
                }
                try {
                    /*
                     * if the node has 'hst:repositorypath' with values, then use these values for the DomainImpl. Otherwise use the ones from the
                     * parent
                     */
                    ArrayList<String> newRepositoryPaths = null;
                    if(mappingNode.hasProperty("hst:repositorypath")) {
                        if(mappingNode.getProperty("hst:repositorypath").getValues().length > 0) {
                            newRepositoryPaths = new ArrayList<String>();
                            Value[] paths = mappingNode.getProperty("hst:repositorypath").getValues();
                            for(Value path : paths) {
                                newRepositoryPaths.add(path.getString());
                            }
                        }
                    }
                    String newPattern = domainPattern;
                    if(!"".equals(newPattern)) {
                        newPattern = "."+newPattern;
                    }
                    newPattern = mappingNode.getName() + newPattern;
                    // only create domain mapping for at least two parts (so '*.com' or 'mysite.com', but not 'com')
                    
                    Domain domain = null;
                    
                    if(mappingNode.isNodeType("hst:redirectmapping")) {
                        if(mappingNode.hasProperty("hst:redirect")) {
                            String redirect = mappingNode.getProperty("hst:redirect").getString();
                            domain = new DomainImpl(newPattern, new String[0], this, redirect);
                        } else {
                            log.warn("Skipping redirect mapping node '{}' because does not have mandatory 'hst:redirect' property", mappingNode.getPath());
                        }
                    } else {
                        if(newRepositoryPaths == null) {
                            domain = new DomainImpl(newPattern, repositorypaths.toArray(new String[repositorypaths.size()]), this, null);
                        } else {
                            domain = new DomainImpl(newPattern, newRepositoryPaths.toArray(new String[newRepositoryPaths.size()]), this, null);
                        }
                    }
                    if(mappingNode.isNodeType("hst:primarydomain")) {
                        if(domain.isExactHost()) { 
                            if(this.primaryDomain == null) {
                                log.debug("setting primary domain --> {}", domain);
                                this.primaryDomain = domain;
                            } else {
                                log.warn("Multiple primary domains configured. Using {} instead of {}", primaryDomain, domain);
                            }
                        } else {
                            log.warn("primary domain cannot contain wildcards. Skipping domain as primary");
                        }
                    }
                    if(domain != null) {
                        domains.add(domain);
                    }
                    createDomainPatterns(mappingNode, domains, newPattern , depth+1 , (newRepositoryPaths==null? repositorypaths:newRepositoryPaths));
          
                } catch(DomainException e){
                    log.warn("DomainException Skipping domain mapping node '{}' : {}", mappingNode.getPath(), e.getMessage());
                }
            }
        }
    }

    public Domain match(String serverName){
        if(serverName == null) {
            log.warn("Cannot match serverName because is null");
            return null;
        }
        log.debug("Trying to find a matching domain for '{}'", serverName);
        
        for(Domain domain : orderedDomains) {
            if(domain.match(serverName.toLowerCase(), serverName.split(Domain.DELIMITER))) {
                log.debug("found matching domain for '{}' --> '{}'", serverName, domain.getPattern());
                return domain;
            }
        } 
        return null;
    }
    
    public boolean isInitialized() {
        return initialized;
    }

    public void setInitialized(boolean initialized) {
        this.initialized = initialized;
    }

    public Domain getDomainByRepositoryPath(String repositoryPath) {
        for(Domain domain : orderedDomains) {
            // we are not interested in domains with a wildcard in them
            if(domain.isExactHost() && !domain.isRedirect()) {
                for(RepositoryMapping repositoryMapping : domain.getRepositoryMapping()) {
                    if(repositoryPath.equals(repositoryMapping.getPath())) {
                        log.debug("found a domain for repository path '{}' --> '{}' ", repositoryPath, domain.getPattern());
                        return domain;
                    }
                }
            }
        }
        return null;
    }
    
    public Domain getPrimaryDomain() {
        return primaryDomain;
    }
    
    public String getServletContextPath() {        
        return servletContextPath;
    }

    public void setServletContextPath(String servletContextPath) {
        this.servletContextPath = servletContextPath;
    }
    
    public boolean isServletContextPathInUrl() {
        
        return isServletContextPathInUrl;
    }

    public void setServletContextPathInUrl(boolean isServletContextPathInUrl) {
        this.isServletContextPathInUrl = isServletContextPathInUrl;
    }

    @Override
    public String toString() {
        StringBuffer stringRepresentation = new StringBuffer();
        stringRepresentation.append("\n-------Domain Mapping--------");
        stringRepresentation.append("\n").append(super.toString());
        for(Domain domain : orderedDomains) {
            stringRepresentation.append("\n\t").append(domain.getPattern().replaceAll("_default_", "*"));
            if(domain.isRedirect()) {
                stringRepresentation.append("\n\t\t redirect --> \t").append(domain.getRedirect());
            } else {
                for(RepositoryMapping repoMapping : domain.getRepositoryMapping()) {
                    stringRepresentation.append("\n\t\t ").append(repoMapping.getPath());
                    if(repoMapping.getPrefix() != null && !"".equals(repoMapping.getPrefix())) {
                        stringRepresentation.append("\t (").append(repoMapping.getPrefix()).append(")");
                    }
                }
            }
        }
        stringRepresentation.append("\n-------End Domain Mapping--------");
        return stringRepresentation.toString();
    }


   
}
