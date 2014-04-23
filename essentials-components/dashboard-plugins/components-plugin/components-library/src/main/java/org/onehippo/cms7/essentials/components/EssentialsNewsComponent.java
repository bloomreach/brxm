/*
 * Copyright 2014 Hippo B.V. (http://www.onehippo.com)
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

package org.onehippo.cms7.essentials.components;

import java.util.Calendar;
import java.util.List;

import org.hippoecm.hst.content.beans.query.HstQuery;
import org.hippoecm.hst.content.beans.query.exceptions.FilterException;
import org.hippoecm.hst.content.beans.query.filter.BaseFilter;
import org.hippoecm.hst.content.beans.query.filter.Filter;
import org.hippoecm.hst.core.component.HstRequest;
import org.hippoecm.hst.core.parameters.ParametersInfo;
import org.hippoecm.repository.util.DateTools;
import org.onehippo.cms7.essentials.components.info.EssentialsNewsComponentInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Strings;

/**
 * HST component used for listing of News document types
 *
 * @version "$Id$"
 */
@ParametersInfo(type = EssentialsNewsComponentInfo.class)
public class EssentialsNewsComponent extends EssentialsListComponent {

    private static Logger log = LoggerFactory.getLogger(EssentialsNewsComponent.class);

    @Override
    protected void contributeAndFilters(final List<BaseFilter> filters, final HstRequest request, final HstQuery query) {
        final EssentialsNewsComponentInfo paramInfo = getComponentParametersInfo(request);
        if (paramInfo.getHideFutureItems()) {
            final String documentDateField = paramInfo.getDocumentDateField();
            if (!Strings.isNullOrEmpty(documentDateField)) {
                try {
                    Filter filter = query.createFilter();
                    filter.addLessOrEqualThan(documentDateField, Calendar.getInstance(), DateTools.Resolution.DAY);
                    filters.add(filter);
                } catch (FilterException e) {
                    log.error("An exception occurred while trying to create a query filter for hiding future items: {}", e);
                }
            }
        }
    }
}
