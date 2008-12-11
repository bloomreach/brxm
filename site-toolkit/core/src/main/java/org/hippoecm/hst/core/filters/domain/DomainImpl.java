package org.hippoecm.hst.core.filters.domain;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DomainImpl implements Domain {
    
    private static final Logger log = LoggerFactory.getLogger(Domain.class);
  
    private DomainMapping domainMapping;
    private String pattern;
    private String[] segments;
    private RepositoryMapping[] repositoryMappings;
    private boolean exactHost;
    private String redirect;

    public DomainImpl(String pattern, String[] repositoryPaths, DomainMapping domainMapping, String redirect) throws DomainException{
        if(pattern == null || "".equals(pattern)) {
            throw new DomainException("Not allowed to have an empty pattern.");
        }
        this.domainMapping = domainMapping;
        this.segments = pattern.split(DELIMITER);
        this.pattern = pattern;
        this.exactHost = !pattern.contains(Domain.WILDCARD);
        this.redirect = redirect;
        this.repositoryMappings = createRepositoryMappings(repositoryPaths);
    }

    
    private RepositoryMapping[] createRepositoryMappings(String[] repositoryPaths) {
        List<RepositoryMapping> repoMappings = new ArrayList<RepositoryMapping>();
        for(String repositoryPath : repositoryPaths) {
            int index = repositoryPath.indexOf(":");
            if(index > -1) {
                repoMappings.add(new RepositoryMappingImpl(domainMapping,this, repositoryPath.substring(0,index), repositoryPath.substring(index+1, repositoryPath.length())));
            } else {
                repoMappings.add(new RepositoryMappingImpl(domainMapping, this, "",repositoryPath));
            }
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
            log.debug("Found an exact host match for {}  --> {} ", pattern , serverName );
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
                   log.debug("Match: ServerName '{}' does match pattern '{}'", serverName, pattern);
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
                return repositoryMapping;
            }
        }
        return null;
    }

    public RepositoryMapping[] getRepositoryMapping() {
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


    /**
     * Method that return you the best matching domain for a repository path. For example, /preview/mysite repository path
     * can belong to multiple domains. First, the current domain is checked. If the current domain does not have a RepositoryMapping with this
     * repository path, the entire DomainMapping will be scanned, and the best (domain with most specific serverName) is returned.
     * 
     */
    public Domain getDomainByRepositoryPath(String repositoryPath) {
        if(repositoryPath == null) {
            log.warn("Cannot find a Domain belonging to repositoryPath which is null");
            return null;
        }
        for(RepositoryMapping repositoryMapping : this.repositoryMappings) {
            if(repositoryPath.equals(repositoryMapping.getPath())) {
                log.debug("The current Domain also has a mapping for repository path '{}'. Returning current domain", repositoryPath);
                return this;
            }
        }
        log.debug("The current Domain does not have a mapping for repository path '{}'. Searching for a matching domain in all Domains", repositoryPath);
        return this.domainMapping.getDomainByRepositoryPath(repositoryPath);
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
            for(RepositoryMapping repoMapping : getRepositoryMapping()) {
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
