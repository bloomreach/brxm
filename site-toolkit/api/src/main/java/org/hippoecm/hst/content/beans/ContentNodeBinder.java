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
package org.hippoecm.hst.content.beans;

import javax.jcr.Node;


/**
 * <CODE>ContentNodeBinder</CODE> is the interface for custom mapping content POJO object to JCR node.
 * <P>
 * An implementation of this interface should be able to set property values of the content node or its child nodes.
 * </P>
 * 
 * @version $Id$
 */
public interface ContentNodeBinder {
    
    /**
     * Does custom binding from content POJO object to JCR node.
     * @param content content POJO object, which can be retrieved by an OCM solution 
     * @param node a main target JCR node which can have some properties or child nodes.
     * @return returns true if the binding makes changes
     * @throws ContentNodeBindingException
     */
    boolean bind(Object content, Node node) throws ContentNodeBindingException;
    
}
