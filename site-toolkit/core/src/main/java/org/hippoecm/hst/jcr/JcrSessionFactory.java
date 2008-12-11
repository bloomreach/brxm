package org.hippoecm.hst.jcr;

import javax.jcr.LoginException;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.SimpleCredentials;
import javax.servlet.FilterConfig;

import org.hippoecm.hst.core.HSTConfiguration;
import org.hippoecm.hst.core.filters.domain.DomainMappingFilter;
import org.hippoecm.repository.HippoRepository;
import org.hippoecm.repository.HippoRepositoryFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class JcrSessionFactory {

    private FilterConfig filterConfig; 
    private static final Logger log = LoggerFactory.getLogger(DomainMappingFilter.class);
    
    public JcrSessionFactory (FilterConfig filterConfig) {
        this.filterConfig = filterConfig;
    }
    
    public Session getSession() {
        String repositoryLocation = HSTConfiguration.get(filterConfig.getServletContext(), HSTConfiguration.KEY_REPOSITORY_ADRESS);
        String username = HSTConfiguration.get(filterConfig.getServletContext(), HSTConfiguration.KEY_REPOSITORY_USERNAME);
        String password = HSTConfiguration.get(filterConfig.getServletContext(), HSTConfiguration.KEY_REPOSITORY_PASSWORD);
        
        SimpleCredentials smplCred = new SimpleCredentials(username, (password != null ? password.toCharArray() : null));
        Session session = null;
        HippoRepositoryFactory.setDefaultRepository(repositoryLocation);
        try {
            HippoRepository repository = HippoRepositoryFactory.getHippoRepository();
            session = repository.login(smplCred);
        } catch (LoginException e) {
            throw new JcrConnectionException("Cannot login with credentials");
        } catch (RepositoryException e) {
            log.error("Problem while obtaining a session from the repository for registering the event listeners");
            throw new JcrConnectionException("Failed to initialize repository");
        }
        return session;
    }
}
