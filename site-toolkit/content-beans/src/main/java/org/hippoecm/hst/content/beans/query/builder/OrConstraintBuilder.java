/*
 *  Copyright 2016 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.hst.content.beans.query.builder;

import javax.jcr.Session;

import org.hippoecm.hst.content.beans.query.exceptions.FilterException;
import org.hippoecm.hst.content.beans.query.filter.Filter;
import org.hippoecm.hst.content.beans.query.filter.FilterImpl;

class OrConstraintBuilder extends ConstraintBuilderAdapter {

    private ConstraintBuilder[] constraintBuilders;

    protected OrConstraintBuilder(final ConstraintBuilder... constraintBuilders) {
        super();
        this.constraintBuilders = constraintBuilders;
    }

    @Override
    protected Filter doBuild(final HstQueryBuilder queryBuilder, final Session session) throws FilterException {
        Filter filter = new FilterImpl(session, queryBuilder.defaultResolution());

        if (constraintBuilders != null) {
            for (ConstraintBuilder constraintBuilder : constraintBuilders) {
                final Filter nestedFilter = constraintBuilder.build(queryBuilder, session);
                if (nestedFilter != null) {
                    filter.addOrFilter(nestedFilter);
                }
            }
        }

        return filter;
    }
}
