/*
 *  Copyright 2008-2013 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.repository.reviewedactions;

import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.jcr.RepositoryException;
import javax.jcr.Value;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DerivedDataFunction extends org.hippoecm.repository.ext.DerivedDataFunction {

    static final Logger log = LoggerFactory.getLogger(DerivedDataFunction.class);

    private Set<String> getStringValues(Value[] values) throws RepositoryException {
        if (values == null) {
            return Collections.emptySet();
        }

        final Set<String> set = new HashSet<>(values.length);
        for (Value value : values) {
            set.add(value.getString());
        }
        return set;
    }

    private Date getDateValue(Value[] values) throws RepositoryException {
        if (values == null || values.length == 0) {
            return null;
        }
        return values[0].getDate().getTime();
    }

    public Map<String, Value[]> compute(Map<String, Value[]> parameters) {
        String stateSummary = "unknown";
        try {
            Set<String> unpublishedAvailability = getStringValues(parameters.get("unpublished"));
            Set<String> publishedAvailability = getStringValues(parameters.get("published"));

            final Date publishedLastModified = getDateValue(parameters.get("publishedLastModified"));
            final Date unPublishedLastModified = getDateValue(parameters.get("unpublishedLastModified"));

            if (publishedAvailability.size() == 0) {
                stateSummary = "new";
            } else {
                if (unpublishedAvailability.size() != 0 && !equals(publishedLastModified, unPublishedLastModified)) {
                    stateSummary = "changed";
                } else {
                    stateSummary = "live";
                }
            }
        } catch (RepositoryException e) {
            log.error("Unable to determine state summary", e);
        }
        parameters.put("summary", new Value[]{getValueFactory().createValue(stateSummary)});
        return parameters;
    }

    private boolean equals(final Date date1, final Date date2) {
        if (date1 == null && date2 == null) {
            return false;
        }
        return date1 != null && date1.equals(date2);
    }
}
