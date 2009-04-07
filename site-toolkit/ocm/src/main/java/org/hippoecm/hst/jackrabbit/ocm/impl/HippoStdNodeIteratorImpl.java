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
package org.hippoecm.hst.jackrabbit.ocm.impl;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;

import org.apache.jackrabbit.ocm.exception.ObjectContentManagerException;
import org.apache.jackrabbit.ocm.manager.ObjectContentManager;
import org.hippoecm.hst.jackrabbit.ocm.HippoStdNode;
import org.hippoecm.hst.jackrabbit.ocm.HippoStdNodeIterator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Deprecated
public class HippoStdNodeIteratorImpl implements HippoStdNodeIterator{

    private static Logger log = LoggerFactory.getLogger(HippoStdNodeIteratorImpl.class);
    
    private NodeIterator nodeIterator; 
    private ObjectContentManager ocm;
    
    public HippoStdNodeIteratorImpl(ObjectContentManager ocm, NodeIterator nodeIterator) {
       this.ocm = ocm;
       this.nodeIterator = nodeIterator;
    }

    public long getPosition() {
        return nodeIterator.getPosition();
    }

    public long getSize() {
        return nodeIterator.getSize();
    }

    public HippoStdNode nextHippoStdNode() {
        try {
            Node n = nodeIterator.nextNode();
            if(n != null) {
                return (HippoStdNode)ocm.getObject(n.getPath());
            } else {
                log.warn("Node in node iterator is null. Cannot return a HippoStdNode");
            }
        } catch (ObjectContentManagerException e) {
            log.warn("ObjectContentManagerException. Return null : {}" , e);
        } catch (RepositoryException e) {
            log.warn("RepositoryException. Return null: {}" , e);
        }
        return null;
    }

    public void skip(int skipNum) {
        nodeIterator.skip((long)skipNum);
    }

    public boolean hasNext() {
        return nodeIterator.hasNext();
    }

    public Object next() {
        return nextHippoStdNode();
    }

    public void remove() {
        throw new UnsupportedOperationException("remove");
    }

}
