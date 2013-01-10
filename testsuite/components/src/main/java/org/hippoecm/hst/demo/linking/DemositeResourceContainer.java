/*
 *  Copyright 2010-2013 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.hst.demo.linking;

import javax.jcr.Item;
import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.PathNotFoundException;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.hippoecm.hst.configuration.hosting.Mount;
import org.hippoecm.hst.core.linking.AbstractResourceContainer;
import org.hippoecm.repository.api.HippoNodeType;

/**
 * 
 * This ResourceContainer is meant as an example about how you can implement your own resource container.  
 * 
 * This is to show that you can add your own resource containers, that know *how* to rewrite a resource to some binaries link
 */
public class DemositeResourceContainer extends AbstractResourceContainer {

    // This container works on all nodes that are of (sub)type "demosite:basedocument" 
    public String getNodeType() {
        return "demosite:basedocument";
    }

    @Override
    public String resolveToPathInfo(Node resourceContainerNode, Node resourceNode, Mount mount) {
        try {
            if(resourceNode.getDefinition().allowsSameNameSiblings()) {
                // there can be multiple ones
                return super.resolveToPathInfo(resourceContainerNode, resourceNode, mount) + "/["+resourceNode.getIndex()+"]";
            } else {
                // there can be only one 
                return super.resolveToPathInfo(resourceContainerNode, resourceNode, mount);
            }
        } catch (RepositoryException e) {
           
        }
        return super.resolveToPathInfo(resourceContainerNode, resourceNode, mount);
    }

    public Node resolveToResourceNode(Session session, String pathInfo) {    
        String actualPath = pathInfo;
        String[] elems = actualPath.split("/");
        int index = -1;
        if(elems[elems.length - 1].startsWith("[") && elems[elems.length - 1].endsWith("]") ) {
          // same name sibblings allowed
            actualPath = actualPath.substring(0, actualPath.lastIndexOf("/"));
            String indexStr = elems[elems.length - 1].substring(1, elems[elems.length - 1].length() - 1);
            try {
                index = Integer.parseInt(indexStr);
            } catch (NumberFormatException e) {
                // invalid index
            }
        } 
        
        try {
            Item item = session.getItem(actualPath);
            Node node = (Node)item;
            if(node.isNodeType(HippoNodeType.NT_HANDLE)) {
                try {
                    node = node.getNode(node.getName());
                } catch(PathNotFoundException e) {
                    // not found, return null
                    return null;
                }
            }
            
            // remember, jcr index is 1 based, not 0 based
            if (node.isNodeType(getNodeType())) {
                // we are of (sub)type "demosite:basedocument"
                if(index == -1 || index == 1) {
                    return node.getNode(this.getPrimaryItem());
                } else {
                    NodeIterator it = node.getNodes(this.getPrimaryItem());
                    if(it.getSize() > (index - 1)) {
                        it.skip(index -1 );
                        return it.nextNode();
                    }
                }
            }
        } catch (PathNotFoundException e) {
           // not found
        } catch (RepositoryException e) {
           // not found
        }
        // did not find the resource. Return null, and let the default resource containers try it
        return null;
    }
    
}
