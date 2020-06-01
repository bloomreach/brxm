/*
 *  Copyright 2008-2013 Hippo B.V. (http://www.onehippo.com)
 * 
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 * 
 *       http://www.apache.org/licenses/LICENSE-2.0
 * 
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package org.hippoecm.repository.security.domain;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.hippoecm.repository.security.ManagerContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The domain class holds all the {@link Domain}s and is used
 * the read the complete domain configuration from the JCR repository
 */
public class Domains {

    /** SVN id placeholder */

    /**
     * The system/root session
     */
    private Session session;

    /**
     * The path from the root containing the domains
     */
    private String domainsPath;

    /**
     * Is the class initialized
     */
    private boolean initialized = false;

    /**
     * The current groups
     */
    private final Set<Domain> domains = new HashSet<Domain>();

    /**
     * Logger
     */
    private final Logger log = LoggerFactory.getLogger(this.getClass());

    //------------------------< Public Methods >--------------------------//
    /**
     * Initialize all domains from the node in the repository. On initialization
     * all domains are initialized from the JCR configuration.
     * @param node the node holding the configuration
     * @throws RepositoryException
     */
    public void init(ManagerContext context) {
        this.session = context.getSession();
        this.domainsPath = context.getPath();
        loadDomains();
        initialized = true;
    }

    /**
     * Get the set of all the domains in the repository
     * @return a set with all the domains
     * @see Domain
     */
    public Set<Domain> getDomains() {
        if (!initialized) {
            throw new IllegalStateException("Not initialized.");
        }
        return Collections.unmodifiableSet(domains);
    }

    /**
     * Get the set domains that a user has a role inas defined
     * in one of the AuthRoles of the domain
     * @param userId the id of the user
     * @return the set with domains
     */
    public Set<Domain> getDomainsForUser(String userId) {
        if (!initialized) {
            throw new IllegalStateException("Not initialized.");
        }
        log.debug("Looking for domains for user: {}", userId);
        Set<Domain> userDomains = new HashSet<Domain>();
        for (Domain domain : domains) {
            for (AuthRole ar : domain.getAuthRoles()) {
                if (ar.hasUser(userId)) {
                    log.debug("Found domain {} for user {}", domain.getName(), userId);
                    userDomains.add(domain);
                }
            }
        }
        return Collections.unmodifiableSet(userDomains);
    }

    /**
     * Get the set domains that a group has a role in as defined
     * in one of the AuthRoles of the domain
     * @param userId the id of the user
     * @return the set with domains
     */
    public Set<Domain> getDomainsForGroup(String groupId) {
        if (!initialized) {
            throw new IllegalStateException("Not initialized.");
        }
        log.debug("Looking for domains for group: {}", groupId);
        Set<Domain> groupDomains = new HashSet<Domain>();
        for (Domain domain : domains) {
            for (AuthRole ar : domain.getAuthRoles()) {
                if (ar.hasGroup(groupId)) {
                    log.debug("Found domain {} for group {}", domain.getName(), groupId);
                    groupDomains.add(domain);
                }
            }
        }
        return Collections.unmodifiableSet(groupDomains);
    }

    //------------------------< Private Helper methods >--------------------------//
    /**
     * Load all domains from the config node and create all domain rules, facet rules and
     * auth roles.
     */
    private void loadDomains() {
        log.debug("Searching for domains node: {}", domainsPath);
        try {
            Node domainsNode = session.getRootNode().getNode(domainsPath);
            log.debug("Found domains node: {}", domainsPath);
            NodeIterator domainIter = domainsNode.getNodes();
            Node domainNode;
            Domain domain;
            while (domainIter.hasNext()) {
                domainNode = domainIter.nextNode();
                try {
                    domain = new Domain(domainNode);
                    domains.add(domain);
                    log.debug("Added domain: {}", domain.getName());
                    log.trace("Added domain: {}", domain);
                } catch (RepositoryException e) {
                    log.warn("Unable to parse domain: " + domainNode.getPath(), e);
                }
            }
        } catch (RepositoryException e) {
            log.warn("Exception while parsing groups from path: {}", domainsPath);
        }
    }
}
