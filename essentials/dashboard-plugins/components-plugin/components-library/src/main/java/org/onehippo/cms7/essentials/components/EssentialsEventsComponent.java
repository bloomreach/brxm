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
import org.hippoecm.hst.core.component.HstResponse;
import org.hippoecm.hst.core.parameters.ParametersInfo;
import org.hippoecm.repository.util.DateTools;
import org.onehippo.cms7.essentials.components.info.EssentialsEventsComponentInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Strings;

/**
 * HST component used for listing of Event document types
 *
 * @version "$Id$"
 */
@ParametersInfo(type = EssentialsEventsComponentInfo.class)
public class EssentialsEventsComponent extends EssentialsListComponent {

    private static Logger log = LoggerFactory.getLogger(EssentialsEventsComponent.class);

    @Override
    public void doBeforeRender(final HstRequest request, final HstResponse response) {
        final EssentialsEventsComponentInfo paramInfo = getComponentParametersInfo(request);
        final String documentTypes = paramInfo.getDocumentTypes();
        if (Strings.isNullOrEmpty(documentTypes)) {
            log.warn("No events document type(s) are defined.");
            return;
        }
        super.doBeforeRender(request, response);
    }

    @Override
    protected void contributeAndFilters(final List<BaseFilter> filters, final HstRequest request, final HstQuery query) {
        final EssentialsEventsComponentInfo paramInfo = getComponentParametersInfo(request);
        if (paramInfo.getHidePastEvents()) {
            final String dateField = paramInfo.getDocumentDateField();
            if (!Strings.isNullOrEmpty(dateField)) {
                try {
                    final Filter filter = query.createFilter();
                    filter.addGreaterOrEqualThan(dateField, Calendar.getInstance(), DateTools.Resolution.DAY);
                    filters.add(filter);
                } catch (FilterException e) {
                    log.error("Error while creating query filter to hide past events using date field {}", dateField, e);
                }
            }
        }
    }
}

