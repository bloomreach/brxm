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
package org.onehippo.repository.xml;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.Value;

import org.apache.jackrabbit.spi.Name;
import org.apache.jackrabbit.spi.Path;
import org.hippoecm.repository.jackrabbit.InternalHippoSession;

public class Reference {


    /** this implementation requires a property that can be set on a parent node.  Because this
     * node isn't actually persisted, there will be no constraint violation, but this property
     * may not clash with any property in the parent node. (FIXME)
     */
    static final String REFERENCE_SUFFIX = "___pathreference";

    private final boolean isMulti;
    private String propName;
    private Name name;
    private String basePath;
    private final String[] paths;
    private final String[] uuids;

    //-------------------------------------------------------------< Instantiators >
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

    Reference(Name name, Value[] paths, boolean isMulti) throws RepositoryException {
        this.name = name;
        this.isMulti = isMulti;
        this.uuids = new String[paths.length];
        this.paths = new String[paths.length];
        for (int i = 0; i < paths.length; i++) {
            this.paths[i] = paths[i].getString();
        }
    }

    //-------------------------------------------------------------< Getters & Setters >
    String getPropertyName() {
        return propName;
    }
    Name getName() {
        return name;
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


    Value[] getPathValues(Session session) throws RepositoryException  {
        Value[] vals = new Value[paths.length];
        resolvePaths(session);
        for (int i = 0; i < paths.length; i++) {
            vals[i] =  session.getValueFactory().createValue(paths[i]);
        }
        return vals;
    }



    //-------------------------------------------------------------< Resolvers >
    void resolveUUIDs(InternalHippoSession sessionImpl) {
        for (int i = 0; i < paths.length; i++) {
            try {
                String path = paths[i];
                if (!path.startsWith("/")) {
                    path = basePath.equals("/") ? basePath + path : basePath + "/" + path;
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
    private void resolvePaths(Session session) throws RepositoryException {
        Node base;
        if (getBasePath().startsWith("/")) {
            base = session.getRootNode().getNode(getBasePath().substring(1)); // remove starting slash
        } else {
            base = session.getRootNode().getNode(getBasePath()); // remove starting slash
        }
        Node parent;
        if (base.getDepth() == 0) {
            parent = base;
        } else {
            parent = base.getParent();
        }
        int len = 0;
        if (parent.getDepth() == 0) {
            // subnode of root node
            len = parent.getPath().length();
        } else {
            len = parent.getPath().length() + 1; // +1 is trailing slash
        }
        String path;
        for (int i = 0; i < uuids.length; i++) {
            path = session.getNodeByIdentifier(uuids[i]).getPath();
            if (path.startsWith(getBasePath())) {
                paths[i] = path.substring(len);
            } else {
                paths[i] = path;
            }
        }
    }

}
