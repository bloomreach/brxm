package org.hippoecm.hst.core.domain;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.jcr.Repository;
import javax.jcr.Session;
import javax.servlet.FilterConfig;

import org.hippoecm.hst.core.domain.Domain;
import org.hippoecm.hst.core.domain.DomainException;
import org.hippoecm.hst.core.domain.DomainMapping;
import org.hippoecm.hst.core.domain.RepositoryMapping;
import org.hippoecm.hst.core.domain.RepositoryMappingException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DomainImpl implements Domain {
    
    private static final Logger log = LoggerFactory.getLogger(DomainImpl.class);

    private Repository repository;
    private DomainMapping domainMapping;
    private String pattern;
    private String[] segments;
    private RepositoryMapping[] repositoryMappings;
    private boolean exactHost;
    private String redirect;

    public DomainImpl(Repository repository, String pattern, String[] repositoryPaths, DomainMapping domainMapping, String redirect) throws DomainException{
        if(pattern == null || "".equals(pattern)) {
            throw new DomainException("Not allowed to have an empty pattern. Skipping domain");
        }
        this.repository = repository;
        this.domainMapping = domainMapping;
        this.segments = pattern.split(DELIMITER);
        this.pattern = pattern;
        this.exactHost = !pattern.contains(Domain.WILDCARD);
        this.redirect = redirect;
        this.repositoryMappings = createRepositoryMappings(repositoryPaths);
    }

    
    private RepositoryMapping[] createRepositoryMappings(String[] repositoryPaths) throws DomainException{
        List<RepositoryMapping> repoMappings = new ArrayList<RepositoryMapping>();
        for(String repositoryPath : repositoryPaths) {
            int index = repositoryPath.indexOf(":");
            try {
                if(index > -1) {
                    repoMappings.add(new RepositoryMappingImpl(repository, domainMapping,this, repositoryPath.substring(0,index), repositoryPath.substring(index+1, repositoryPath.length())));
                } else {
                    repoMappings.add(new RepositoryMappingImpl(repository, domainMapping, this, "",repositoryPath));
                }
            } catch (RepositoryMappingException e) {
               log.warn("Skipping Repository Mapping for pattern '{}' because {}", pattern, e.getMessage());
            }
        }
        if(repoMappings.isEmpty() && redirect == null) {
            throw new DomainException("Skipping domain with pattern '"+this.pattern+"' because failed to initialize at least one repository mapping and the Domain is not a redirect.");
        }
        RepositoryMapping[] repoMappingsArr = repoMappings.toArray(new RepositoryMapping[repoMappings.size()]);
        Arrays.sort(repoMappingsArr);
        return repoMappingsArr;
    }

    public DomainMapping getDomainMapping() {
        return this.domainMapping;
    }
 
    
    public boolean match(String serverName, String[] serverNameParts) {
        
        if(serverNameParts.length < segments.length) {
            // can never match
            log.debug("ServerName '{}' does not match pattern '{}'", serverName, pattern);
            return false;
        }
        
        if (pattern.equals(serverName)) {
            log.info("Found an exact host match for {}  --> {} ", pattern , serverName );
            return true;
        }
        
        if(serverNameParts.length == segments.length) {
            for(int i = serverNameParts.length-1; i >=0; i--) {
                if(serverNameParts[i].equals(segments[i]) || segments[i].equals(Domain.WILDCARD)){
                    continue;
                } else {
                    log.debug("ServerName '{}' does not match pattern '{}'", serverName, pattern);
                    return false;
                }
            }
            // if we get here, there was a match
            return true;
        } else if (serverNameParts.length > segments.length) {
            // we shouldn't count to 0 for i, but untill there are no segments left to test AND the last segment must be the WILDCARD
            int testUntill = serverNameParts.length - segments.length;
            for(int i = serverNameParts.length-1; i >=testUntill; i--) {
                if(i == testUntill && segments[i-testUntill].equals(Domain.WILDCARD)) {
                   log.info("Match: ServerName '{}' does match pattern '{}'", serverName, pattern);
                   return true;
                }
                else if(serverNameParts[i].equals(segments[i-testUntill]) || segments[i-testUntill].equals(Domain.WILDCARD)){
                    continue;
                } else {
                    log.debug("ServerName '{}' does not match pattern '{}'", serverName, pattern);
                    return false;
                }
            }
        }
        log.debug("ServerName '{}' does not match pattern '{}'", serverName, pattern);
        return false;
    }


    public RepositoryMapping getRepositoryMapping(String ctxStrippedUri) {
        for(RepositoryMapping repositoryMapping : repositoryMappings) {
            if(repositoryMapping.match(ctxStrippedUri)) {
                // return the first match because we have sorted the mappings already
                
                // when the repository mapping is a 'template', create an actual repository mapping out of the template.
                if(repositoryMapping.isTemplate()) {
                    int prefixDepth = repositoryMapping.getDepth();
                    String[] segments = ctxStrippedUri.split("/");
                    if(segments.length < prefixDepth) {
                        log.warn("Cannot create repository mapping from template");
                        continue;
                    }
                    String prefix = repositoryMapping.getPrefix().substring(0,repositoryMapping.getPrefix().indexOf(RepositoryMapping.PATH_WILDCARD))+segments[prefixDepth - 1];
                    String path = repositoryMapping.getPath().substring(0,repositoryMapping.getPath().indexOf(RepositoryMapping.PATH_WILDCARD))+segments[prefixDepth - 1];
                    
                    RepositoryMapping repositoryMappingFromTemplate = null;
                    try {
                        repositoryMappingFromTemplate = new RepositoryMappingImpl(repository, this.domainMapping, this, prefix, path);
                        
                        // if succesfully created, let's add it to the existing repository mappings of this domain.
                        List<RepositoryMapping> repoMappingsList =  new ArrayList<RepositoryMapping>(Arrays.asList(this.repositoryMappings));
                        repoMappingsList.add(repositoryMappingFromTemplate);
                        this.repositoryMappings = repoMappingsList.toArray(new RepositoryMapping[repoMappingsList.size()]);      
                        Arrays.sort(repositoryMappings);
                        log.debug("Added new repositoryMapping to domain. Domain now is: {}", this);
                        return repositoryMappingFromTemplate;
                    } catch (RepositoryMappingException e) {
                        log.warn("Failed to create repository mapping from template: {}", e.getMessage());
                    }
                } else {
                    return repositoryMapping;
                }
            }
        }
        return null;
    }

    public RepositoryMapping[] getRepositoryMappings() {
        return this.repositoryMappings;
    }


    public String getPattern() {
        return this.pattern;
    }

    public int compareTo(Domain domain) {
        if(domain instanceof DomainImpl) {
            DomainImpl domainImpl = (DomainImpl)domain;
            if(this.segments.length > domainImpl.segments.length) {
                return -1;
            } else if(this.segments.length < domainImpl.segments.length){
                return +1;
            } else {
                return whoHasTheMostWildCards(this, domainImpl, 0);
            }
        }
        return 0;
    }

    private int whoHasTheMostWildCards(DomainImpl domainImpl, DomainImpl domainImpl2, int position) {
        if(Domain.WILDCARD.equals(domainImpl.segments[position])) {
            if(Domain.WILDCARD.equals(domainImpl2.segments[position])) {
                if(position == segments.length-1) {
                    return 0;
                } 
                // both have a wildcard as last segment. Check the one before
                whoHasTheMostWildCards(domainImpl, domainImpl2, ++position);
            } else {
                return +1;
            }
        } else if(Domain.WILDCARD.equals(domainImpl2.segments[position])) {
            return -1;
        }
        return 0;
    }

    public boolean isExactHost() {
       return this.exactHost;
    }


    public String getRedirect() {
        return this.redirect;
    }


    public boolean isRedirect() {
        return this.redirect != null ;
    }


    @Override
    public String toString() {
        StringBuffer stringRepr = new StringBuffer();
        stringRepr.append("\n-----Domain-----");
        stringRepr.append("\n").append(super.toString());
        stringRepr.append("\n\t").append(pattern.replaceAll("_default_", "*"));
        if(this.isRedirect()) {
            stringRepr.append("\n\t\t redirect --> \t").append(this.redirect);
        } else {
            for(RepositoryMapping repoMapping : getRepositoryMappings()) {
                stringRepr.append("\n\t\t ").append(repoMapping.getPath());
                if(repoMapping.getPrefix() != null && !"".equals(repoMapping.getPrefix())) {
                    stringRepr.append("\t (").append(repoMapping.getPrefix()).append(")");
                }
            }
        }
        stringRepr.append("\n-----End Domain-----");
        return stringRepr.toString();
    }
    
    

}
