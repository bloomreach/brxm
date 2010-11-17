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
    private HstVirtualizer virtualizer;

    public HstQueryResultImpl(ObjectConverter objectConverter, javax.jcr.query.QueryResult queryResult, HstVirtualizer virtualizer) {
        this.objectConverter = objectConverter;
        this.queryResult = queryResult;
        this.virtualizer = virtualizer;
    }

    public HippoBeanIterator getHippoBeans() {
        try {
            return new HippoBeanIteratorImpl(this.objectConverter, this.queryResult.getNodes(), virtualizer);
        } catch (RepositoryException e) {
            log.error("RepositoryException. Return null. {}", e);
            return null;
        }
    }
    
    public int getTotalSize() {
        try {
            NodeIterator iterator = queryResult.getNodes();
            if(iterator instanceof HippoNodeIterator) {
                int total = (int)((HippoNodeIterator)iterator).getTotalSize();
                if(total == -1) {
                    log.warn("getTotalSize returned -1 for query. Should not happen. Fallback to normal getSize()");
                    return getSize();
                } else {
                    log.debug("getTotalSize call returned '{}' hits", total);
                    return total;
                }
            }
            log.debug("The getTotalSize method only works properly in embedded repository mode. Fallback to normal getSize()");
            return getSize();
        } catch (RepositoryException e) {
            log.error("RepositoryException. Return 0. {}", e);
            return 0;
        }
    }
    
    public int getSize() {
        try {
            return (int) queryResult.getNodes().getSize();
        } catch (RepositoryException e) {
            log.error("RepositoryException. Return 0. {}", e);
            return 0;
        }
    }

}
