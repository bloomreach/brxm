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
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.jcr.RepositoryException;
import javax.jcr.Value;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DerivedDataFunction extends org.hippoecm.repository.ext.DerivedDataFunction {

    static final Logger log = LoggerFactory.getLogger(DerivedDataFunction.class);

    private Set<String> getValues(Value[] values) throws RepositoryException {
        if (values == null) {
            return Collections.emptySet();
        }

        HashSet<String> set = new HashSet<String>(2);
        for (Value value : values) {
            set.add(value.getString());
        }
        return set;
    }

    public Map<String, Value[]> compute(Map<String, Value[]> parameters) {
        String stateSummary = "unknown";
        try {
            Set<String> unpublishedAvailability = getValues(parameters.get("unpublished"));
            Set<String> publishedAvailability = getValues(parameters.get("published"));

            if (unpublishedAvailability.size() == 0) {
                if (publishedAvailability.size() > 0) {
                    stateSummary = "live";
                } else {
                    stateSummary = "new";
                }
            } else {
                if (publishedAvailability.size() > 0) {
                    stateSummary = "changed";
                } else {
                    stateSummary = "new";
                }
            }
        } catch (RepositoryException e) {
            log.error("Unable to determine state summary", e);
        }
        parameters.put("summary", new Value[]{getValueFactory().createValue(stateSummary)});
        return parameters;
    }
}
