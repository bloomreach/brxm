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
package org.hippoecm.hst.core.search;

import javax.jcr.Session;

import org.hippoecm.hst.content.beans.manager.ObjectConverter;
import org.hippoecm.hst.content.beans.query.HstCtxWhereClauseComputerImpl;
import org.hippoecm.hst.content.beans.query.HstQueryManager;
import org.hippoecm.hst.content.beans.query.HstQueryManagerImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HstQueryManagerFactoryImpl implements HstQueryManagerFactory{

    private static final Logger log = LoggerFactory.getLogger(HstQueryManagerFactoryImpl.class);
    
    private volatile org.hippoecm.hst.content.beans.query.HstCtxWhereClauseComputer hstCtxWhereClauseComputer;
    
    public org.hippoecm.hst.content.beans.query.HstCtxWhereClauseComputer getHstCtxWhereClauseComputer() {
        if(this.hstCtxWhereClauseComputer == null) {
            // if hstCtxWhereClauseComputer not set through dependency injection, we use the default impl
            synchronized(this){
                if(this.hstCtxWhereClauseComputer == null) {
                    this.hstCtxWhereClauseComputer = new HstCtxWhereClauseComputerImpl();
                }
            }
        }
        return this.hstCtxWhereClauseComputer;
    }

    public void setHstCtxWhereClauseComputer(org.hippoecm.hst.content.beans.query.HstCtxWhereClauseComputer hstCtxWhereClauseComputer) {
        this.hstCtxWhereClauseComputer = hstCtxWhereClauseComputer;
    }

    @Override
    public HstQueryManager createQueryManager(Session session, ObjectConverter objectConverter) {
        HstQueryManager mngr = new HstQueryManagerImpl(session, objectConverter, this.getHstCtxWhereClauseComputer());
        return mngr;
    }

}
