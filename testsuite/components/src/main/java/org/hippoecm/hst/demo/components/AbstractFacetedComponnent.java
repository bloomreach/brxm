/*
 *  Copyright 2010-2016 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.hst.demo.components;

import org.hippoecm.hst.component.support.bean.BaseHstComponent;
import org.hippoecm.hst.content.beans.query.HstQuery;
import org.hippoecm.hst.content.beans.query.builder.HstQueryBuilder;
import org.hippoecm.hst.core.component.HstRequest;
import org.hippoecm.hst.util.SearchInputParsingUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.hippoecm.hst.content.beans.query.builder.ConstraintBuilder.constraint;
import static org.hippoecm.hst.content.beans.query.builder.ConstraintBuilder.or;

public class AbstractFacetedComponnent extends BaseHstComponent {

    public static final Logger log = LoggerFactory.getLogger(AbstractFacetedComponnent.class);

    public HstQuery getHstQuery(HstRequest request) {

        String query = this.getPublicRequestParameter(request, "query");
        if (query != null) {
            query = SearchInputParsingUtils.parse(query, false);
        }
        HstQuery hstQuery = null;
        if ((query != null && !"".equals(query))) {
            // there was a free text query. We need to account for this. 
            request.setAttribute("query", query);
            // account for the free text string


            hstQuery = HstQueryBuilder.create(request.getRequestContext().getSiteContentBaseBean())
                    .where(
                            or(
                                    constraint(".").contains(query),
                                    // boost title hits
                                    constraint("demosite:title").contains(query)
                            )
                    )
                    .build();

        }
        return hstQuery;
    }

}