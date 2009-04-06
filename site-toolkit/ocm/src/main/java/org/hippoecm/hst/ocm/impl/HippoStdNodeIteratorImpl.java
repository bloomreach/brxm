package org.hippoecm.hst.ocm.impl;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;

import org.apache.jackrabbit.ocm.exception.ObjectContentManagerException;
import org.apache.jackrabbit.ocm.manager.ObjectContentManager;
import org.hippoecm.hst.ocm.HippoStdNode;
import org.hippoecm.hst.ocm.HippoStdNodeIterator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
