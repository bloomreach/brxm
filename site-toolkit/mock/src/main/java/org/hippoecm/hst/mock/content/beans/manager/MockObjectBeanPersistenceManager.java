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
package org.hippoecm.hst.mock.content.beans.manager;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

import javax.jcr.Session;

import org.hippoecm.hst.content.beans.ContentNodeBinder;
import org.hippoecm.hst.content.beans.ObjectBeanPersistenceException;
import org.hippoecm.hst.content.beans.manager.ObjectBeanPersistenceManager;

/**
 * Simple in-memory implementation for <CODE>MockObjectBeanPersistenceManager</CODE> interface.
 * <P>
 * This implementation assumes that a content object has 'path' bean property to indicate
 * from which absolute path the content object is originated.
 * So, it will use <CODE>getPath()</CODE> method to read the original absolute path to update
 * or remove the object.
 * </P>
 * <P>
 * <P>
 * The absolute content paths and objects mapped to the paths are stored in maps.
 * To store an object to maps, it uses Java Serialization.
 * Therefore, the object used with this implementation should implement 
 * <CODE>java.io.Serializable</CODE>, <CODE>public boolean equals(Object object);</CODE>
 * and <CODE>public int hashCode();</CODE> properly.
 * </P>
 * <P>
 * By the way, this mock implementation does not have any knowledge on how to create a new content
 * based on its node type.
 * </P>
 * 
 * @version $Id: MockObjectBeanPersistenceManager.java 20882 2009-11-26 10:21:54Z aschrijvers $
 */
public class MockObjectBeanPersistenceManager implements ObjectBeanPersistenceManager {
    
    private Map<String, Object> pathToObjectMap = new HashMap<String, Object>();
    private Map<Object, String> objectToPathMap = new HashMap<Object, String>();
    
    public Object getObject(String absPath) throws ObjectBeanPersistenceException {
        Object object = pathToObjectMap.get(absPath);
        
        if (object != null) {
            return getSerializedCopy(object);
        }
        
        return null;
    }
    
    public Object getObjectByUuid(String uuid) throws ObjectBeanPersistenceException {
        for (Object object : objectToPathMap.keySet()) {
            if (uuid.equals(getUuidProperty(object))) {
                return object;
            }
        }
        return null;
    }

    public synchronized void setObject(String absPath, Object object) throws ObjectBeanPersistenceException {
        if (absPath == null) {
            throw new ObjectBeanPersistenceException("The absolute path is null.");
        }

        if (object != null) {
            pathToObjectMap.put(absPath, object);
            objectToPathMap.put(object, absPath);
        } else {
            Object removed = pathToObjectMap.remove(absPath);
            if (removed != null) {
                objectToPathMap.remove(removed);
            }
        }
    }
    
    public void create(String absPath, String nodeTypeName, String name) throws ObjectBeanPersistenceException {
        createAndReturn(absPath, nodeTypeName, name, false);
    }

    public void create(String absPath, String nodeTypeName, String name, boolean autoCreateFolders) throws ObjectBeanPersistenceException {
        createAndReturn(absPath, nodeTypeName, name, autoCreateFolders);
    }

    public String createAndReturn(String absPath, String nodeTypeName, String name, boolean autoCreateFolders) throws ObjectBeanPersistenceException {
        // do nothing... use setObject for mocking.
        return null;
    }
    
    public synchronized void update(Object content) throws ObjectBeanPersistenceException {
        String path = getPathProperty(content);
        if (path != null) {
            setObject(path, content);
        }
    }

    public void update(Object content, ContentNodeBinder customBinder) throws ObjectBeanPersistenceException {
        String path = getPathProperty(content);
        if (path != null) {
            if (customBinder != null) {
                // do nothing...
            }
            setObject(path, content);
        }
    }
    
    public void remove(Object content) throws ObjectBeanPersistenceException {
        String path = getPathProperty(content);
        if (path != null) {
            setObject(path, null);
        }
    }

    public void save() throws ObjectBeanPersistenceException {
    }

    public void refresh() throws ObjectBeanPersistenceException {
        refresh(false);
    }
    
    public void refresh(boolean keepChanges) throws ObjectBeanPersistenceException {
    }
    
    protected String getPathProperty(Object object) {
        String path = null;
        
        try {
            Method getPathMethod = object.getClass().getMethod("getPath", null);
            path = (String) getPathMethod.invoke(object, null);
        } catch (Exception e) {
            e.printStackTrace();
        }
        
        if (path == null) {
            path = objectToPathMap.get(object);
        }
        
        return path;
    }
    
    protected String getUuidProperty(Object object) {
        String uuid = null;
        
        try {
            Method getPathMethod = object.getClass().getMethod("getUuid", null);
            uuid = (String) getPathMethod.invoke(object, null);
        } catch (Exception e) {
            e.printStackTrace();
        }
        
        return uuid;
    }
    
    protected Object getSerializedCopy(Object object) throws ObjectBeanPersistenceException {
        return bytesToObject(objectToBytes(object));
    }
    
    protected byte [] objectToBytes(Object object) throws ObjectBeanPersistenceException {
        if (!(object instanceof Serializable)) {
            throw new ObjectBeanPersistenceException("Object is not serializable.");            
        }
        
        byte [] bytes = null;
        
        ByteArrayOutputStream baos = null;
        ObjectOutputStream oos = null;
        
        try {
            baos = new ByteArrayOutputStream(128);
            oos = new ObjectOutputStream(baos);
            oos.writeObject(object);
            oos.flush();
            bytes = baos.toByteArray();
        } catch (Exception e) {
            throw new ObjectBeanPersistenceException(e);
        } finally {
            if (oos != null) try { oos.close(); } catch (Exception ce) { }
            if (baos != null) try { baos.close(); } catch (Exception ce) { }
        }
        
        return bytes;
    }
    
    protected Object bytesToObject(byte [] bytes) throws ObjectBeanPersistenceException {
        Object object = null;
        
        ByteArrayInputStream bais = null;
        ObjectInputStream ois = null;
        
        try {
            bais = new ByteArrayInputStream(bytes);
            ois = new ObjectInputStream(bais);
            object = ois.readObject();
        } catch (Exception e) {
            throw new ObjectBeanPersistenceException(e);
        } finally {
            if (ois != null) try { ois.close(); } catch (Exception ce) { }
            if (bais != null) try { bais.close(); } catch (Exception ce) { }
        }
        
        return object;
    }

    public Session getSession() {
        return null;
    }

}
