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
package org.hippoecm.repository.jackrabbit.xml;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.Value;

import org.apache.jackrabbit.core.SessionImpl;
import org.apache.jackrabbit.spi.Name;
import org.apache.jackrabbit.spi.NameFactory;
import org.apache.jackrabbit.spi.Path;
import org.apache.jackrabbit.spi.commons.name.NameFactoryImpl;

public class Reference {

    @SuppressWarnings("unused")
    private final static String SVN_ID = "$Id$";

    private static final NameFactory FACTORY = NameFactoryImpl.getInstance();
    
    /** this implementation requires a property that can be set on a parent node.  Because this
     * node isn't actually persisted, there will be no constraint violation, but this property
     * may not clash with any property in the parent node. (FIXME)
     */
    static final String PROPERTY_STRING = "hippo:pathreference";
    static final Name PROPERTY_NAME = FACTORY.create("http://www.hippoecm.org/nt/1.0", "pathreference");
    
    /**
     * sv:value
     */

    /** '*' is not valid in property name, but can of course be used in value */
    final static String SEPARATOR = "*";

    /** indicate whether original reference property was a multi valued property */
    final static String MULTI_VALUE = "m";

    /** indicate whether original reference property was a single valued property */
    final static String SINGLE_VALUE = "s";

    private boolean isMulti;

    private String propName;
    
    private String basePath;

    private String[] paths;

    private String[] uuids;
    
    private String pathString = null;


    //-------------------------------------------------------------< Instantiators >
    Reference(String basePath, String uuidString, String propName) {
        setBasePath(basePath);
        setPropertyName(propName);
        this.uuids = new String[1];
        this.paths = new String[1];
        this.uuids[0] = uuidString;
        this.isMulti = false;
    }

    Reference(String basePath, Value uuidVal, String propName) throws RepositoryException {
        setBasePath(basePath);
        setPropertyName(propName);
        this.uuids = new String[1];
        this.paths = new String[1];
        this.uuids[0] = uuidVal.getString();
        this.isMulti = false;
    }

    Reference(String basePath, Value[] uuidVals, String propName) throws RepositoryException {
        setBasePath(basePath);
        setPropertyName(propName);
        this.uuids = new String[uuidVals.length];
        this.paths = new String[uuidVals.length];
        for (int i = 0; i < uuidVals.length; i++) {
            this.uuids[i] = uuidVals[i].getString();
        }
        this.isMulti = true;
    }
    
    Reference(String pathString) throws RepositoryException {
        this.pathString = pathString;
        parsePathString();
    }

    //-------------------------------------------------------------< Getters & Setters >
    String getPropertyName() {
        return propName;
    }
    String getBasePath() {
        return basePath;
    }
    String[] getUUIDs() {
        return uuids;
    }
    String[] getPaths() {
        return paths;
    }
    boolean isMulti() {
        return isMulti;
    }
    void setPropertyName(String name) {
        propName = name;
    }
    void setBasePath(String path) {
        basePath = path;
    }
    

    //-------------------------------------------------------------< Reference path parsers >
    /**
     * Create path string uuid 
     * @param session
     * @return
     * @throws RepositoryException
     */
    String createPathString(Session session) throws RepositoryException {
        if (pathString != null) {
            return pathString;
        }
        resolvePaths(session);
        StringBuffer buf = new StringBuffer();
        if (isMulti) {
            buf.append(MULTI_VALUE);
        } else {
            buf.append(SINGLE_VALUE);
        }
            
        buf.append(SEPARATOR);
        buf.append(propName);
        for (String p : paths) {
            buf.append(SEPARATOR);
            buf.append(p);
        }
        pathString = buf.toString();
        return pathString;
    }
    

    // format ([MULTI_VALUE|SINGLE_VALUE]+SEPARATOR+propName+SEPARATOR+path)
    void parsePathString() throws RepositoryException {
        isMulti = false;
        
        String[] parts = pathString.split("\\" + SEPARATOR);
        
        if (parts.length < 3) {
            throw new RepositoryException("Invalid pathreference string: " + pathString);
        }
        
        if (parts[0].length() != 1) {
            throw new RepositoryException("Invalid pathreference string, first part too long: " + pathString);
        }
        
        if (MULTI_VALUE.equals(parts[0])) {
            isMulti = true;
        } else if (SINGLE_VALUE.equals(parts[0])) {
            isMulti = false;
        } else {
            throw new RepositoryException("Invalid pathreference string, first part not valid: " + pathString);
        }
        
        if (pathString.startsWith(MULTI_VALUE)) {
            isMulti = true;
        } else if (pathString.startsWith(SINGLE_VALUE)) {
            isMulti = false;
        } else {
            throw new RepositoryException("Invalid pathString format: " + pathString);
        }

        propName = parts[1];
        paths = new String[parts.length - 2];
        uuids = new String[parts.length - 2];
        for (int i = 2; i < parts.length; i++) {
            paths[i-2] = parts[i];
        }
    }

    //-------------------------------------------------------------< Resolvers >
    void resolveUUIDs(SessionImpl sessionImpl) throws RepositoryException {
        for (int i = 0; i < paths.length; i++) {
            try {
                String path = paths[i];
                if (!path.startsWith("/")) {
                    path = getBasePath() + "/" + path;
                }
                Path p = sessionImpl.getQPath(path).getNormalizedPath();
                if (!p.isAbsolute()) {
                    throw new RepositoryException("not an absolute path: " + path);
                } else {
                    uuids[i] = sessionImpl.getItemManager().getNode(p).getUUID();
                }
            } catch (RepositoryException e) {
                uuids[i] = null;
            }
        }
    }

    /**
     * Resolve uuids to paths. Make paths to uuids inside the export relative.
     * @param session
     * @throws RepositoryException
     */
    void resolvePaths(Session session) throws RepositoryException {
        Node base = session.getRootNode().getNode(getBasePath().substring(1)); // remove starting slash
        Node parent;
        if (base.getDepth() == 0) {
            parent = base;
        } else {
            parent = base.getParent();
        }
        int len = parent.getPath().length() + 1; // +1 is trailing slash
        String path;
        for (int i = 0; i < uuids.length; i++) {
            path = session.getNodeByUUID(uuids[i]).getPath();
            if (path.startsWith(getBasePath())) {
                paths[i] = path.substring(len);
            } else {
                paths[i] = path;
            }
        }
    }

}
