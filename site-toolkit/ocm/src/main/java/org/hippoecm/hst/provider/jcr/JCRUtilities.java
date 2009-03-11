/*
 *  Copyright 2008 Hippo.
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
package org.hippoecm.hst.provider.jcr;

import java.lang.reflect.Method;

import javax.jcr.Node;
import javax.jcr.RepositoryException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class JCRUtilities {

    private static final Logger log = LoggerFactory.getLogger(JCRUtilities.class);
    
    public static Node getCanonical(Node n) {
        Method getCanonicalNodeMethod = null;
        
        try {
            getCanonicalNodeMethod = ((Object) n).getClass().getMethod("getCanonicalNodeMethod");
        } catch (Exception e) {
        }
        
        if (getCanonicalNodeMethod != null) {
            try {
                Node canonical = null;
                try {
                    canonical = (Node) getCanonicalNodeMethod.invoke(n, null);
                } catch (Exception e) {
                }
                
                if(canonical == null) {
                    log.debug("Cannot get canonical node for '{}'. This means there is no phyiscal equivalence of the " +
                    		"virtual node. Return null", n.getPath());
                }
                return canonical;
            } catch (RepositoryException e) {
                log.error("Repository exception while fetching canonical node. Return null" , e);
                return null;
            }
        } 
        return n;
    }
}
