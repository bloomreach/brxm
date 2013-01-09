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
package org.hippoecm.hst.jaxrs.model.content;

import javax.jcr.RepositoryException;
import javax.xml.bind.annotation.XmlRootElement;

import org.hippoecm.hst.content.beans.standard.HippoFacetNavigationBean;

/**
 * HippoFacetNavigationRepresentation
 * @version $Id$
 */
@XmlRootElement(name = "facetnavigation")
public class HippoFacetNavigationRepresentation extends HippoFolderRepresentation {
    
    private static final long serialVersionUID = 1L;
    
    private long count;
    
    public HippoFacetNavigationRepresentation() {
        super();
    }
    
    public HippoFacetNavigationRepresentation represent(HippoFacetNavigationBean hippoFacetNavigationBean) throws RepositoryException {
        super.represent(hippoFacetNavigationBean);
        this.count = hippoFacetNavigationBean.getCount();
        return this;
    }

    public long getCount() {
        return count;
    }

    public void setCount(long count) {
        this.count = count;
    }
    
}
