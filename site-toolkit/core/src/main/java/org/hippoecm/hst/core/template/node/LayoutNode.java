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
package org.hippoecm.hst.core.template.node;

import java.util.ArrayList;
import java.util.List;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.PathNotFoundException;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.ValueFormatException;

import org.hippoecm.hst.core.template.ContextBase;

public class LayoutNode  extends TemplateNode{   
	public static final String TEMPLATEPAGE_PROPERTY_NAME = "hst:template";
   
    
    public LayoutNode(ContextBase contextBase, Node jcrPageLayoutNode) {
    	super(contextBase, jcrPageLayoutNode);
    }
    
  /*  public List<Node> getSubNodes() {
    	List<Node> subNodes = new ArrayList<Node>();
    	try {
			NodeIterator nodeIter = jcrNode.getNodes();
			while (nodeIter.hasNext()) {
				subNodes.add((Node) nodeIter.next());
			}
		} catch (RepositoryException e) {			
			e.printStackTrace();
			return null;
		}
    	return subNodes;
    } */
    
    public String getTemplatePage() throws PathNotFoundException, ValueFormatException, RepositoryException {
    	return jcrNode.getProperty(TEMPLATEPAGE_PROPERTY_NAME).getString();
    }
    
    public NodeList<LayoutAttributeNode> getSubNodes() throws RepositoryException {
    	return new NodeList(contextBase, jcrNode, LayoutAttributeNode.class);
    }
    
    
    
}
