/*
 * Copyright 2009-2017 Hippo B.V. (http://www.onehippo.com)
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

import java.util.ArrayList;
import java.util.List;

import org.hippoecm.hst.component.support.bean.BaseHstComponent;
import org.hippoecm.hst.content.beans.query.HstQuery;
import org.hippoecm.hst.content.beans.query.HstQueryResult;
import org.hippoecm.hst.content.beans.query.builder.HstQueryBuilder;
import org.hippoecm.hst.content.beans.query.exceptions.QueryException;
import org.hippoecm.hst.content.beans.standard.HippoBean;
import org.hippoecm.hst.content.beans.standard.HippoBeanIterator;
import org.hippoecm.hst.core.component.HstComponentException;
import org.hippoecm.hst.core.component.HstRequest;
import org.hippoecm.hst.core.component.HstResponse;
import org.hippoecm.hst.core.parameters.ParametersInfo;
import org.hippoecm.hst.site.HstServices;
import org.onehippo.taxonomy.api.Category;
import org.onehippo.taxonomy.api.Taxonomy;
import org.onehippo.taxonomy.api.TaxonomyManager;
import org.onehippo.taxonomy.api.TaxonomyNodeTypes;
import org.onehippo.taxonomy.demo.beans.TextPage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.hippoecm.hst.content.beans.query.builder.ConstraintBuilder.constraint;

@ParametersInfo(type = TaxonomyCategoryResultInfo.class)
public class TaxonomyCategoryResult extends BaseHstComponent {

    private static final Logger log = LoggerFactory.getLogger(TaxonomyCategoryResult.class);

    @Override
    public void doBeforeRender(HstRequest request, HstResponse response) throws HstComponentException {
        super.doBeforeRender(request, response);

        final TaxonomyManager taxonomyManager = HstServices.getComponentManager().getComponent(TaxonomyManager.class.getName());
        final TaxonomyCategoryResultInfo componentParametersInfo = getComponentParametersInfo(request);
        final String root = componentParametersInfo.getRoot();
        final String relativePath = componentParametersInfo.getPath();

        if (root == null || taxonomyManager.getTaxonomies().getTaxonomy(root) == null) {
            request.setAttribute("error", "Cannot find taxonomy " + root);
        } else {
            final Taxonomy taxonomy = taxonomyManager.getTaxonomies().getTaxonomy(root);
            if (relativePath == null || taxonomy.getCategory(relativePath) == null) {
                request.setAttribute("error", "Cannot find taxonomy term for relative path: " + relativePath);
            } else {
                final Category category = taxonomy.getCategory(relativePath);
                final String key = category.getKey();
                final HippoBean baseBean = request.getRequestContext().getSiteContentBaseBean();
                try {

                    HstQuery query = HstQueryBuilder.create(baseBean)
                            .ofTypes(TextPage.class)
                            .where(constraint(TaxonomyNodeTypes.HIPPOTAXONOMY_KEYS).contains(key))
                            .build();

                    final HstQueryResult queryResult = query.execute();
                    final HippoBeanIterator beans = queryResult.getHippoBeans();
                    final List<TextPage> documents = new ArrayList<>();
                    while (beans.hasNext()) {
                        TextPage bean = (TextPage) beans.nextHippoBean();
                        // in case document was depublished / removed in the meantime...
                        if (bean != null) {
                            documents.add(bean);
                        }
                    }
                    request.setAttribute("category", category);
                    request.setAttribute("documents", documents);
                } catch (QueryException e) {
                    log.error("Invalid query", e);
                }
            }
        }
    }
}