/*
 * Copyright 2009-2014 Hippo B.V. (http://www.onehippo.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.onehippo.taxonomy.demo.components;

import org.hippoecm.hst.component.support.bean.BaseHstComponent;
import org.hippoecm.hst.core.component.HstComponentException;
import org.hippoecm.hst.core.component.HstRequest;
import org.hippoecm.hst.core.component.HstResponse;
import org.hippoecm.hst.site.HstServices;
import org.onehippo.taxonomy.api.TaxonomyManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TaxonomyTreeComponent extends BaseHstComponent {

    public static final Logger log = LoggerFactory.getLogger(TaxonomyTreeComponent.class);

    @Override
    public void doBeforeRender(HstRequest request, HstResponse response) throws HstComponentException {
        super.doBeforeRender(request, response);
        
        // get current pathInfo to have a baseurl (pathInfo is without contextpath and without servletpath)
        String baseUrl = request.getPathInfo();
        
        String taxonomyRoot = getComponentParameter("root");
        
        log.debug("locating taxonomy from root: {}", taxonomyRoot);

        TaxonomyManager taxonomyManager = HstServices.getComponentManager().getComponent(TaxonomyManager.class.getName());
       
        if (taxonomyRoot == null || taxonomyManager.getTaxonomies().getTaxonomy(taxonomyRoot) == null) {
            request.setAttribute("error", "Cannot find taxonomy " + taxonomyRoot);
        } else {
            request.setAttribute("taxonomy", taxonomyManager.getTaxonomies().getTaxonomy(taxonomyRoot));
            request.setAttribute("baseUrl", baseUrl);
        }
    }
}