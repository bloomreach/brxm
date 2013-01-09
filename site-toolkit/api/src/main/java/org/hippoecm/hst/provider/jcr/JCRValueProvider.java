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
package org.hippoecm.hst.provider.jcr;

import javax.jcr.Node;

import org.hippoecm.hst.provider.ValueProvider;
import org.hippoecm.repository.api.HippoNode;

public interface JCRValueProvider extends ValueProvider{

    /**
     * returns the {@link Node} that was used to create this value provider with or <code>null</code> when the
     * node is already detached
     * @return the jcr node 
     */
    Node getJcrNode();
    
    /**
     * @return the locallized name of the backing jcr node according {@link HippoNode#getLocalizedName()}
     */
    String getLocalizedName();
    
    /**
     * returns the parent {@link Node} of this value provider or <code>null</code> when the node is detached.
     * If the node is the jcr rootNode, <code>null</code> will be returned
     * @return the parent Node or <code>null</code> if the node is null or there is no parent
     */
    Node getParentJcrNode();
    
    /**
     * Method for detaching the jcr node. After calling this method, the jcr node is not available anymore
     */
    void detach();
    
    /**
     * Test whether the jcr node is detached or not
     * @return true if the node is detached
     */
    boolean isDetached();
    
    /** 
     * @param nodeType
     * @return true when the underlying jcr node is of type nodeType
     */
    boolean isNodeType(String nodeType);
    
    /**
     * Flushes all fetched data kept in instance variables
     */
    void flush();

   
}
