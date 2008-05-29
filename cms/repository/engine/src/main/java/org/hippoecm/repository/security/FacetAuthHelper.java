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

import javax.jcr.NamespaceException;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.apache.jackrabbit.spi.Name;
import org.apache.jackrabbit.spi.NameFactory;
import org.apache.jackrabbit.spi.commons.conversion.IllegalNameException;
import org.apache.jackrabbit.spi.commons.conversion.NameParser;
import org.apache.jackrabbit.spi.commons.name.NameFactoryImpl;
import org.apache.jackrabbit.spi.commons.namespace.NamespaceResolver;
import org.apache.jackrabbit.spi.commons.namespace.SessionNamespaceResolver;

/**
 * Help class for resolving facet auth principals from jcr nodes
 */
public class FacetAuthHelper {

    /** SVN id placeholder */
    @SuppressWarnings("unused")
    private final static String SVN_ID = "$Id$";

    /** The wildcard to match everything */
    public final static String WILDCARD = "*";

    /**
     * NameFactory for create Names
     */
    private static final NameFactory FACTORY = NameFactoryImpl.getInstance();
    
    /**
     * Get the Name from the node type string short notation
     * @param session The session
     * @param jcrName The node type name
     * @return the name
     */
    public static Name getNameFromJCRName(Session session, String jcrName) throws IllegalNameException, NamespaceException {
        return getNameFromJCRName(new SessionNamespaceResolver(session), jcrName);
    }
    
    public static Name getNameFromJCRName(NamespaceResolver nsRes, String jcrName) throws IllegalNameException, NamespaceException {
        return NameParser.parse(jcrName, nsRes, FACTORY);
    }
    
    public static String getJCRNameFromName(Session session, Name name) throws NamespaceException {
        return getJCRNameFromName(new SessionNamespaceResolver(session), name);
    }
    
    public static String getJCRNameFromName(NamespaceResolver nsRes, Name name) throws NamespaceException {
        String uri = name.getNamespaceURI();
        if (nsRes.getPrefix(uri).length() == 0) {
            return name.getLocalName();
        } else {
            return nsRes.getPrefix(uri) + ":" + name.getLocalName();
        }
    }
}
