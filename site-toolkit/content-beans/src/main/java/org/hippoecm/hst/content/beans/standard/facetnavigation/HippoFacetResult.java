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
package org.hippoecm.hst.content.beans.standard.facetnavigation;

import org.hippoecm.hst.content.beans.Node;
import org.hippoecm.hst.content.beans.index.Indexable;
import org.hippoecm.hst.content.beans.standard.HippoFolder;
import org.hippoecm.hst.content.beans.standard.HippoResultSetBean;
import org.hippoecm.repository.api.HippoNodeType;

@Indexable(ignore = true)
@Node(jcrType="hippo:facetresult")
public class HippoFacetResult extends HippoFolder implements HippoResultSetBean {

    public Long getCount() {
        return this.getProperty(HippoNodeType.HIPPO_COUNT);
    }

    @Override
    public int getDocumentSize() {
        /*
         * do not get from HippoFolder as that one fetches all docs first which is very inefficient. Use getCount
         */
        return getCount().intValue();
    }
    
    
}
