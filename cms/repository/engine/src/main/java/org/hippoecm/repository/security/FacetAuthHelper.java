/*
 * Copyright 2007 Hippo
 *
 * Licensed under the Apache License, Version 2.0 (the  "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.hippoecm.repository.security;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import javax.jcr.NamespaceException;
import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.Value;

import org.apache.jackrabbit.spi.Name;
import org.apache.jackrabbit.spi.commons.name.NameFactoryImpl;
import org.hippoecm.repository.api.HippoNodeType;
import org.hippoecm.repository.security.principals.FacetAuthPrincipal;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Help class for resolving facet auth principals from jcr nodes
 */
public class FacetAuthHelper {

    private static final Logger log = LoggerFactory.getLogger(FacetAuthHelper.class);

    /**
     * Get the set of FacetAuthPrincipals from a path containing the facet auth nodes
     * @param facetAuthPath
     * @return Set a set of FacetAuthPrincipals
     * @throws RepositoryException
     */
    public static Set<FacetAuthPrincipal> getFacetAuths(Node facetAuthPath) throws RepositoryException {
        NodeIterator nodeIter = facetAuthPath.getNodes();
        Set<FacetAuthPrincipal> principals = new HashSet<FacetAuthPrincipal>();
        while (nodeIter.hasNext()) {
            Node fa = (Node) nodeIter.next();
            Long permissions = fa.getProperty(HippoNodeType.HIPPO_PERMISSIONS).getLong();
            String facet = fa.getProperty(HippoNodeType.HIPPO_FACET).getString();
            Value[] facetValues = fa.getProperty(HippoNodeType.HIPPO_VALUES).getValues();
            String[] values = new String[facetValues.length];
            for (int i = 0; i < facetValues.length; i++) {
                values[i] = facetValues[i].getString();
            }
            principals.add(new FacetAuthPrincipal(resolveName(facetAuthPath.getSession(), facet), values, permissions));
        }
        return Collections.unmodifiableSet(principals);
    }
    
    /**
     * Get the Name from the node type string short notation
     * @param session The session
     * @param nodeTypeName The node type name
     * @return the name
     */
    private static Name resolveName(Session session, String nodeTypeName) {
        Name name;
        name = NameFactoryImpl.getInstance().create("", nodeTypeName);
        int i = nodeTypeName.indexOf(":");
        if (i > 0) {
            try {
                name = NameFactoryImpl.getInstance().create(session.getNamespaceURI(nodeTypeName.substring(0, i)),
                        nodeTypeName.substring(i + 1));
            } catch (RepositoryException e) {
                log.warn("Unable to find URI for namespace: " + nodeTypeName.substring(0, i));
            }
        }
        return name;
    }
    
}
