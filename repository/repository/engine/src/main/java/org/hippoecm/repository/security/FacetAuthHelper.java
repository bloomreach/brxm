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

import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.apache.jackrabbit.spi.Name;
import org.apache.jackrabbit.spi.commons.name.NameFactoryImpl;

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
     * Get the Name from the node type string short notation
     * @param session The session
     * @param nodeTypeName The node type name
     * @return the name
     */
    public static Name resolveName(Session session, String nodeTypeName) throws RepositoryException {
        Name name;
        int i = nodeTypeName.indexOf(":");
        if (i > 0) {
            name = NameFactoryImpl.getInstance().create(session.getNamespaceURI(nodeTypeName.substring(0, i)),
                    nodeTypeName.substring(i + 1));
        } else {
            name = NameFactoryImpl.getInstance().create("", nodeTypeName);
        }
        return name;
    }
    
}
