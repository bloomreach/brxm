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
package org.hippoecm.hst.content.beans.manager;

import javax.jcr.Node;
import javax.jcr.Session;

import org.hippoecm.hst.content.beans.ObjectBeanManagerException;
import org.hippoecm.hst.content.beans.standard.HippoBean;

/**
 * Convert any kind of beans into JCR nodes & properties.
 * <P>
 * This interface mimics Jackrabbit's one, but this is provided
 * to support more lightweight beans in HST.
 * </P>
 *
 * @version $Id$
 */
public interface ObjectConverter
{
	/**
	 * Returns the Primary Node Type for a Node (or a child Node if the Node is a Handle) to be used for Object conversion, 
	 * possibly using a fallback Node Type if no exact match can be determined.
	 * 
	 * @param node to determine the Primary Object Type for
	 * @return Primary Object Type to be used for Object conversion
	 * @throws ObjectBeanManagerException
	 */
    String getPrimaryObjectType(Node node) throws ObjectBeanManagerException;
    
    Object getObject(Session session, String path) throws ObjectBeanManagerException;
    
    Object getObject(Node node) throws ObjectBeanManagerException;

    Object getObject(Node node, String relPath) throws ObjectBeanManagerException;

    Object getObject(String uuid, Session session) throws ObjectBeanManagerException;
    
    Object getObject(String uuid, Node node) throws ObjectBeanManagerException;
    
    /**
     * @param jcrPrimaryNodeType
     * @return the annotated <code>Class</code> for this jcrPrimaryNodeType or <code>null</code> if no annotated class can be found
     */
    Class<? extends HippoBean> getAnnotatedClassFor(String jcrPrimaryNodeType);
    
    String getPrimaryNodeTypeNameFor(Class<? extends HippoBean> hippoBean);
    
}
