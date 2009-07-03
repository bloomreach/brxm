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
package org.hippoecm.hst.mock;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

import org.hippoecm.hst.persistence.ContentNodeBinder;
import org.hippoecm.hst.persistence.ContentPersistenceException;
import org.hippoecm.hst.persistence.ContentPersistenceManager;

/**
 * Simple in-memory implementation for <CODE>ContentPersistenceManager</CODE> interface.
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
 * @version $Id$
 */
public class MockContentPersistenceManager implements ContentPersistenceManager {
    
    protected Map<String, Object> pathToObjectMap = new HashMap<String, Object>();
    protected Map<Object, String> objectToPathMap = new HashMap<Object, String>();
    
    public Object getObject(String absPath) throws ContentPersistenceException {
        Object object = pathToObjectMap.get(absPath);
        
        if (object != null) {
            return getSerializedCopy(object);
        }
        
        return null;
    }

    public synchronized void setObject(String absPath, Object object) throws ContentPersistenceException {
        if (absPath == null) {
            throw new ContentPersistenceException("The absolute path is null.");
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
    
    public void create(String absPath, String nodeTypeName, String name) throws ContentPersistenceException {
        create(absPath, nodeTypeName, name, false);
    }

    public void create(String absPath, String nodeTypeName, String name, boolean autoCreateFolders) throws ContentPersistenceException {
        // do nothing... use setObject for mocking.
    }
    
    public synchronized void update(Object content) throws ContentPersistenceException {
        String path = getPathProperty(content);
        if (path != null) {
            setObject(path, content);
        }
    }

    public void update(Object content, ContentNodeBinder customBinder) throws ContentPersistenceException {
        String path = getPathProperty(content);
        if (path != null) {
            if (customBinder != null) {
                // do nothing...
            }
            setObject(path, content);
        }
    }
    
    public void remove(Object content) throws ContentPersistenceException {
        String path = getPathProperty(content);
        if (path != null) {
            setObject(path, null);
        }
    }

    public void save() throws ContentPersistenceException {
    }

    public void reset() throws ContentPersistenceException {
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
    
    protected Object getSerializedCopy(Object object) throws ContentPersistenceException {
        return bytesToObject(objectToBytes(object));
    }
    
    protected byte [] objectToBytes(Object object) throws ContentPersistenceException {
        if (!(object instanceof Serializable)) {
            throw new ContentPersistenceException("Object is not serializable.");            
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
            throw new ContentPersistenceException(e);
        } finally {
            if (oos != null) try { oos.close(); } catch (Exception ce) { }
            if (baos != null) try { baos.close(); } catch (Exception ce) { }
        }
        
        return bytes;
    }
    
    protected Object bytesToObject(byte [] bytes) throws ContentPersistenceException {
        Object object = null;
        
        ByteArrayInputStream bais = null;
        ObjectInputStream ois = null;
        
        try {
            bais = new ByteArrayInputStream(bytes);
            ois = new ObjectInputStream(bais);
            object = ois.readObject();
        } catch (Exception e) {
            throw new ContentPersistenceException(e);
        } finally {
            if (ois != null) try { ois.close(); } catch (Exception ce) { }
            if (bais != null) try { bais.close(); } catch (Exception ce) { }
        }
        
        return object;
    }

}
