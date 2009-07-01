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
package org.hippoecm.hst.persistence;

/**
 * <CODE>ContentPersistenceManager</CODE> is the primary interface for HST-2-Persistence-aware application components.
 * <P>
 * An implementation of this interface should be able to convert content nodes to objects, and vice versa.
 * Also it should have knowledges on how to create, update or remove a content node with node type and absolute path.
 * </P>
 * <P>
 * Some sophisticated JCR repository engine already have their own workflow knowledges on 
 * how to create, update and remove content nodes based on node types.
 * </P>
 * <P>
 * Or, some domain specific content-based application should know the knowledges on their own content models.
 * In this case, they should provide an implementation for this interface.
 * </P>
 * 
 * @version $Id$
 */
public interface ContentPersistenceManager {
    
    /**
     * Returns the content object at the specified absolute path.
     * @param absPath
     * @return an object mapped to the specified absolute path and its primary node type.
     * @throws ContentPersistenceException
     */
    Object getObject(String absPath) throws ContentPersistenceException;
    
    /**
     * Creates a content node with the specified node type at the specified absolute path.
     * @param absPath
     * @param nodeTypeName
     * @param name the content node name
     * @throws ContentPersistenceException
     */
    void create(String absPath, String nodeTypeName, String name) throws ContentPersistenceException;
    
    /**
     * Updates the content node which is mapped to the object.
     * An implementation can provide automatic content binding.
     * @param content
     * @throws ContentPersistenceException
     */
    void update(Object content) throws ContentPersistenceException;
    
    /**
     * Updates the content node which is mapped to the object.
     * With <CODE>customBinder</CODE> parameter, the physical binding content object to content node(s)
     * should be provided by the <CODE>customBinder</CODE> itself.
     * @param content
     * @param customBinder
     * @throws ContentPersistenceException
     */
    void update(Object content, ContentPersistenceBinder customBinder) throws ContentPersistenceException;
    
    /**
     * Removes the content node which is mapped to the object.
     * @param content
     * @throws ContentPersistenceException
     */
    void remove(Object content) throws ContentPersistenceException;
    
    /**
     * Saves all pending changes. 
     * @throws ContentPersistenceException
     */
    void save() throws ContentPersistenceException;
    
    /**
     * Discards all pending changes and resets the current state.
     * @throws ContentPersistenceException
     */
    void reset() throws ContentPersistenceException;
    
}
