package org.hippoecm.hst.core.filters.domain;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.jcr.LoginException;
import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.PathNotFoundException;
import javax.jcr.Repository;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.Value;

import org.apache.commons.collections.map.LRUMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;



public class DomainMappingImpl implements DomainMapping{
    
    private static final Logger log = LoggerFactory.getLogger(DomainMapping.class);
    
    private Repository repository;
    private String domainMappingLocation;
    private boolean initialized;
    private Domain[] orderedDomains; 
    private Domain primaryDomain;
    
    private String servletContextPath;
    private boolean isServletContextPathInUrl;
    private String scheme;
    private boolean isPortInUrl;
    private int port;

    private Map domainsCache = Collections.synchronizedMap(new LRUMap(1000));
    private Map repositoryMappingCache = Collections.synchronizedMap(new LRUMap(2000));
    
    // TODO for now hardcoded paths to binary location. This information is needed to rewrite urls to subsites with another domain
    private String[] binaryLocations = new String[]{"/content/gallery", "/content/assets"};
     
  
    public DomainMappingImpl(Repository repository, String domainMappingLocation){
        this.repository = repository;
        this.domainMappingLocation = domainMappingLocation;
    }

    public void init() throws DomainMappingException{
        Set<Domain> domains = new HashSet<Domain>();
        Session session = null;
        
        // regardless wether initialization succeeds the domain mapping is initialized. If failing, the hst will run without domain mapping
        setInitialized(true);
        

        // default settings
        this.setServletContextPathInUrl(true);
        this.setPortInUrl(true);
        
        if(domainMappingLocation == null) {
            throw new DomainMappingException("No domainMappingLocation configured in web.xml");
        }
        
        try {
            session = this.repository.login();
            if(domainMappingLocation.startsWith("/")) {
                domainMappingLocation = domainMappingLocation.substring(1);
            }
            if("".equals("domainMappingLocation")) {
                throw new DomainMappingException("domainMappingLocation not allowed to be empty or '/'");
            }
            
            // get the domain mapping node
            try {
                Node domainMappingNode = session.getRootNode().getNode(domainMappingLocation);
                
                if(domainMappingNode.hasProperty("hst:showport")) {
                    this.setPortInUrl(domainMappingNode.getProperty("hst:showport").getBoolean());
                }
                if(domainMappingNode.hasProperty("hst:showcontextpath")) {
                    this.setServletContextPathInUrl(domainMappingNode.getProperty("hst:showcontextpath").getBoolean());
                }
                
                if(domainMappingNode.hasProperty("hst:port")) {
                    this.setPort((int)domainMappingNode.getProperty("hst:port").getLong());
                }
                long start = System.currentTimeMillis();
                createDomainPatterns(domainMappingNode, domains, "", 1, new ArrayList<String>());
                
                log.debug("Creating all domain patterns took {} ms.", System.currentTimeMillis()-start);
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
            
        }
        catch (LoginException e)
        {
            throw new DomainMappingException("Failed to retrieve session.", e);
        }
        catch (RepositoryException e)
        {
            throw new DomainMappingException("Failed to retrieve session.", e);
        } 
        finally {
            if(session!=null) {
                session.logout();
            }
        }
        
        log.info(this.toString());
        
    }

    private void createDomainPatterns(Node domainMappingNode, Set<Domain> domains, String domainPattern, int depth, ArrayList<String> repositorypaths) throws RepositoryException {
        
        NodeIterator mappings = domainMappingNode.getNodes();
        if(depth > 8) {
            log.warn("Reached the maximum depth for domain mapping. Skipping deeper paths");
        }
        int i = 0;
        while(mappings.hasNext()) {
            Node mappingNode = mappings.nextNode();
            if(mappingNode != null) {
                long start = System.currentTimeMillis();
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
                            domain = new DomainImpl(repository, newPattern, new String[0], this, redirect);
                        } else {
                            log.warn("Skipping redirect mapping node '{}' because does not have mandatory 'hst:redirect' property", mappingNode.getPath());
                        }
                    } else {
                        if(newRepositoryPaths == null) {
                            domain = new DomainImpl(repository, newPattern, repositorypaths.toArray(new String[repositorypaths.size()]), this, null);
                        } else {
                            domain = new DomainImpl(repository, newPattern, newRepositoryPaths.toArray(new String[newRepositoryPaths.size()]), this, null);
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
                log.debug("Creating domain took {} ms", System.currentTimeMillis() - start);
            }
        }
    }

    public Domain match(String serverName){
        Object d = domainsCache.get(serverName);
        if(d != null && d instanceof Domain)
        {
            return (Domain)d;
        }
        long start = System.currentTimeMillis();
        if(serverName == null) {
            log.warn("Cannot match serverName because is null");
            return null;
        }
        log.debug("Trying to find a matching domain for '{}'", serverName);
        
        // do the lowercase and split outside the loop for performance.
        String lCaseName = serverName.toLowerCase();
        String[] splittedServerName = serverName.split(Domain.DELIMITER);
        if(orderedDomains!=null){
          for(Domain domain : orderedDomains) {
            if(domain.match(lCaseName, splittedServerName)) {
                log.info("found matching domain for '{}' --> '{}'", serverName, domain.getPattern());
                domainsCache.put(serverName, domain);
                log.debug("Matching serverName {} to domain {} took " +(System.currentTimeMillis() - start)+ " ms.", serverName, domain.getPattern());
                return domain;
            }
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

    public boolean isBinary(String path) {
        if(path == null) {
            return false;
        }
        for(String binaryLoc : binaryLocations) {
            if(path.startsWith(binaryLoc)) {
                return true;
            }
        }
        return false;
    }
    
    /**
     * Method that returns you the best matching repository mapping for a repository path. For example, /preview/mysite repository path
     * can occur in multiple domains. First, the current domain is checked. If the current domain does not have a RepositoryMapping with this
     * repository path, the entire DomainMapping will be scanned, and 'the best' repository mapping is returned. Only domains with an exact host and prefix plus path, thus no 
     * wildcards are taken into account. The best match is computed by how well the current domain pattern matches. For each segment that matches,
     * the score is +1. So, www.mysite.com and subsite.mysite.com have 2 segments in common so a score of 2. By equal score, the domain with 
     * the most segments is chosen.
     * 
     */
    
    public RepositoryMapping getRepositoryMapping(String repositoryPath, RepositoryMapping currentRepositoryMapping) {
        if(repositoryPath == null) {
            log.warn("Cannot find a Domain belonging to repositoryPath which is null");
            return null;
        }
        Domain currentDomain = currentRepositoryMapping.getDomain();
        String cacheKey = currentDomain.hashCode() + "_"+ repositoryPath;
        Object cached = repositoryMappingCache.get(cacheKey);
        if(cached != null && cached instanceof RepositoryMapping) {
            return (RepositoryMapping)cached;
        }
        if(currentDomain != null) {
            for(RepositoryMapping repositoryMapping : currentDomain.getRepositoryMappings()) {
                if(repositoryMapping.getCanonicalContentPath()!=null && repositoryPath.startsWith(repositoryMapping.getCanonicalContentPath())) {
                    log.debug("The current Domain also has a mapping for repository path '{}'. Returning the matching repository mapping from the current domain", repositoryPath);
                    repositoryMappingCache.put(cacheKey, repositoryMapping);
                    return repositoryMapping;
                }
            }
            log.debug("The current Domain does not have a mapping for repository path '{}'. Searching for a matching domain in all Domains", repositoryPath);
        }
        
        Set<RepositoryMapping> matchingRepositoryMapping =  new HashSet<RepositoryMapping>();
        int bestMatchDepth = 0;
        for(Domain domain : orderedDomains) {
            // we are not interested in domains with a wildcard in them because we can only rewrite to exact hosts
            if(domain.isExactHost() && !domain.isRedirect()) {
                // first collect all repositoryMappings that match and have an 'equal' matching depth.
                for(RepositoryMapping repositoryMapping : domain.getRepositoryMappings()) {
                    if(!repositoryMapping.isTemplate() && repositoryPath.startsWith(repositoryMapping.getCanonicalContentPath())) {
                        int crDepth = repositoryMapping.getCanonicalContentPath().split("/").length;
                        if(crDepth > bestMatchDepth) {
                            bestMatchDepth = crDepth;
                            // restart collecting best mappings when a deeper match is found
                            matchingRepositoryMapping.clear();
                        }
                        if(crDepth == bestMatchDepth) {
                            log.debug("found a domain for repository path '{}' --> '{}' ", repositoryPath, domain.getPattern());
                            matchingRepositoryMapping.add(repositoryMapping);
                        } else {
                            log.debug("found a domain '{}' which matches but already found a domain with a more specific repository path", domain.getPattern());
                        }
                    }
                }
            }
        }
        if(!matchingRepositoryMapping.isEmpty()) {
            
            // find the repository mapping that belongs to the domain that best resembles the current domain. 
            RepositoryMapping bestRepositoryMapping = null;
            int bestScore = 0;
            for(RepositoryMapping repositoryMapping : matchingRepositoryMapping)  {
                if(bestRepositoryMapping == null) {
                    bestRepositoryMapping = repositoryMapping;
                }
                int score = compare(repositoryMapping, currentRepositoryMapping);
                if(score > bestScore) {
                    bestScore = score;
                    bestRepositoryMapping = repositoryMapping;
                }
            }
            repositoryMappingCache.put(cacheKey, bestRepositoryMapping);
            log.info("For repositorypath {} and domain: {} \nwe found a the following best mapping: " , repositoryPath, currentDomain);
            log.info("Domain {} \nRepository mapping: {}" , bestRepositoryMapping.getDomain(), bestRepositoryMapping.getPrefix()+":"+bestRepositoryMapping.getPath());
            return bestRepositoryMapping;
        }
        log.warn("No repository mapping with a host without wildcards can be found for '{}'", repositoryPath);
        return null;
    }
    
    
    /*
     * compute how much repositoryMapping 1 looks like repositoryMapping 2. For each 'segment match (starting at the end) in  the pattern' the score increases by 100. The total number of segments in 
     * pattern 1 is added to the score to favor a deeper pattern over a less deep pattern.
     * 
     * Furthermore, in the score the path is matched between the current repository mapping. This is to make sure, that a link in for example the preview of subsite A link
     * to the preview of subsite B, and not 
     */
    private int compare(RepositoryMapping repositoryMapping1, RepositoryMapping repositoryMapping2) {
        String[] segments1 = repositoryMapping1.getDomain().getPattern().split(Domain.DELIMITER);
        String[] segments2 = repositoryMapping2.getDomain().getPattern().split(Domain.DELIMITER);
        
        int score = segments1.length;

        int seg1length = segments1.length;
        int seg2length = segments2.length;
        
        // case one
        if(seg1length == seg2length) {
            int offset = seg1length -1;
            while(offset > -1 && segments1[offset].equals(segments2[offset])) {
                offset--;
                score += 100;
            }
        }
        
     // case two
        if(seg1length < seg2length) {
            int offset = seg1length -1;
            int diff = seg2length - seg1length;
            while(offset > -1 && segments1[offset].equals(segments2[offset+diff])) {
                offset--;
                score += 100;
            }
        }
        
     // case three
        if(seg1length > seg2length) {
            int offset = seg2length -1;
            int diff = seg1length - seg2length;
            while(offset > -1 && segments1[offset+diff].equals(segments2[offset])) {
                offset--;
                score += 100;
            }
        }

        String[] paths1 = repositoryMapping1.getPath().split("/");
        String[] paths2 = repositoryMapping2.getPath().split("/");
        
        int i = 0;
        while(i < paths1.length && i < paths2.length) {
            i++;
            if(paths1[i].equals(paths2[i])) {
                score += 100;
            } else {
                // stop further comparison
                break;
            }
        }
        
        return score;
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
    
    public String getScheme() {
        return scheme;
    }

    public void setScheme(String scheme) {
        this.scheme = scheme;
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
                for(RepositoryMapping repoMapping : domain.getRepositoryMappings()) {
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

    public boolean isPortInUrl() {
        return isPortInUrl;
    }

    public void setPortInUrl(boolean isPortInUrl) {
        this.isPortInUrl = isPortInUrl;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

 


   
}
