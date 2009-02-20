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

import javax.jcr.Node;

import org.hippoecm.hst.provider.ValueProvider;

public interface JCRValueProvider extends ValueProvider{

    public Node getJcrNode();

    /**
     *  the canonical node of the node. If the node is virtual but does not have a physical equivalence, null is returned. If the node
     *  is physical, then the canonical node is the same as returned by getJcrNode()  
     * @return String canonical path
     * 
     */
    public Node getCanonical();
    
    /**
     * @return node path of the caononical or null 
     */
    public String getCanonicalPath();
    

    /**
     * The handle node of the canonical node. This is a convenience node needed frequently for linkrewriting and such
     * @return
     */
    public Node getHandle();
    
    /**
     * @return node path of the handle or null 
     */
    public String getHandlePath();
    
    /**
     * Method for detaching the jcr node. After calling this method, the jcr node is not available anymore
     */
    public void detach();
    
    /**
     * Test whether the jcr node is detached or not
     * @return true if the node is detached
     */
    public boolean isDetached();
    
    /** 
     * @param nodeType
     * @return true when the underlying jcr node is of type nodeType
     */
    public boolean isNodeType(String nodeType);
    
   
}
