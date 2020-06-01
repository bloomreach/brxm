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
package org.hippoecm.hst.content.beans.query;

import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;

import org.hippoecm.hst.content.beans.manager.ObjectConverter;
import org.hippoecm.hst.content.beans.standard.HippoBeanIterator;
import org.hippoecm.hst.content.beans.standard.HippoBeanIteratorImpl;
import org.hippoecm.repository.api.HippoNodeIterator;
import org.slf4j.LoggerFactory;

public class HstQueryResultImpl implements HstQueryResult {

    private static final org.slf4j.Logger log = LoggerFactory.getLogger(HstQueryResultImpl.class);
    
    private javax.jcr.query.QueryResult queryResult;
    private ObjectConverter objectConverter;

    // jackrabbit can return a size of -1 if unknown, and higher otherwise. That is why we start with -2 if not set yet
    private int totalSize = -2;
    private int size = -2;
    

    public HstQueryResultImpl(ObjectConverter objectConverter, javax.jcr.query.QueryResult queryResult) {
        this.objectConverter = objectConverter;
        this.queryResult = queryResult;
    }

    public HippoBeanIterator getHippoBeans() {
        try {
            return new HippoBeanIteratorImpl(this.objectConverter, this.queryResult.getNodes());
        } catch (RepositoryException e) {
            log.error("RepositoryException. Return null. {}", e);
            return null;
        }
    }
    
    public int getTotalSize() {
        if(totalSize != -2) {
            return totalSize;
        }
        try {
            NodeIterator iterator = queryResult.getNodes();
            if(iterator instanceof HippoNodeIterator) {
                totalSize = (int)((HippoNodeIterator)iterator).getTotalSize();
                if(totalSize == -1) {
                    log.error("getTotalSize returned -1 for query. Should not be possible. Fallback to normal getSize()");
                    totalSize = getSize();
                } else {
                    log.debug("getTotalSize call returned '{}' hits", totalSize);
                }
            } else {
                log.debug("The getTotalSize method only works properly in embedded repository mode. Fallback to normal getSize()");
                totalSize = getSize();
            }
        } catch (RepositoryException e) {
            log.error("RepositoryException. Return 0. {}", e);
            return 0;
        }
        return totalSize;
    }
    
    public int getSize() {
        if(size != -2) {
            return size;
        }
        try {
            size = (int) queryResult.getNodes().getSize();
        } catch (RepositoryException e) {
            log.error("RepositoryException. Return 0. {}", e);
            return 0;
        }
        return size;
    }

}
