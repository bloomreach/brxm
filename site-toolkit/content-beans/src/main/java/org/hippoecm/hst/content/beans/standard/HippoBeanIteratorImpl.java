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
package org.hippoecm.hst.content.beans.standard;

import java.util.concurrent.atomic.AtomicInteger;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;

import org.hippoecm.hst.content.beans.ObjectBeanManagerException;
import org.hippoecm.hst.content.beans.manager.ObjectConverter;
import org.hippoecm.hst.diagnosis.HDC;
import org.slf4j.LoggerFactory;

public class HippoBeanIteratorImpl implements HippoBeanIterator {
    
    private static final org.slf4j.Logger log = LoggerFactory.getLogger(HippoBeanIteratorImpl.class);
    
    
    private ObjectConverter objectConverter;
    private NodeIterator nodeIterator;
    
    public HippoBeanIteratorImpl(ObjectConverter objectConverter, NodeIterator nodeIterator){
        this.objectConverter = objectConverter;
        this.nodeIterator = nodeIterator;
    }
    
    public long getPosition() {
        return nodeIterator.getPosition();
    }

    public long getSize() {
        return nodeIterator.getSize();
    }

    public HippoBean nextHippoBean() {
        Node n = null;
        try {
            n = nodeIterator.nextNode();
            if (HDC.isStarted()) {
                AtomicInteger iterCount = (AtomicInteger) HDC.getCurrentTask().getAttribute("HippoBeanIterationCount");
                if (iterCount == null) {
                    HDC.getCurrentTask().setAttribute("HippoBeanIterationCount", new AtomicInteger(1));
                } else {
                    iterCount.incrementAndGet();
                }
            }
            return (HippoBean)objectConverter.getObject(n);
        } catch (ObjectBeanManagerException  e) {
            String path = getPath(n);
            log.info("ObjectContentManagerException. Return null for '"+path+"'" , e);
        }
        return null;
    }

    private String getPath(Node n) {
        if(n == null) {
            return "";
        }
        try {
            return n.getPath();
        } catch (RepositoryException e) {
            log.error("RepositoryException ", e);
            return "";
        }
    }

    public void skip(int skipNum) {
        nodeIterator.skip(skipNum);
    }

    public boolean hasNext() {
        return nodeIterator.hasNext();
    }

    public void remove() {
        throw new UnsupportedOperationException("remove");
    }

    public HippoBean next() {
        return nextHippoBean();
    }

}
