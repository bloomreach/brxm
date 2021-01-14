/*
 * Copyright 2020 Bloomreach
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
 *
 */
package org.hippoecm.hst.pagecomposer.jaxrs.services.component.state.util;

import java.util.Calendar;

import javax.jcr.Node;
import javax.jcr.Property;
import javax.jcr.RepositoryException;
import javax.jcr.Value;

import org.hippoecm.repository.util.JcrUtils;

public final class ScheduledRequest {

    private final Calendar scheduledDate;
    private final String type;
    private final String identifier;

    public ScheduledRequest(final Node requestNode) throws RepositoryException {
        scheduledDate = JcrUtils.getDateProperty(requestNode, "hipposched:triggers/default/hipposched:nextFireTime", null);
        type = getAttributeValue(requestNode, "hipposched:methodName", "unknown");
        identifier = requestNode.getIdentifier();
    }

    public String getType() {
        return type;
    }

    public Calendar getScheduledDate() {
        return scheduledDate;
    }

    public String getIdentifier() {
        return identifier;
    }

    private static String getAttributeValue(final Node node, final String attributeName, final String defaultValue) throws RepositoryException {
        final Property attributeNamesProperty = JcrUtils.getPropertyIfExists(node, "hipposched:attributeNames");
        if (attributeNamesProperty == null) {
            return defaultValue;
        }

        final Property attributeValuesProperty = JcrUtils.getPropertyIfExists(node, "hipposched:attributeValues");
        if (attributeValuesProperty == null) {
            return defaultValue;
        }

        final Value[] names = attributeNamesProperty.getValues();
        final Value[] values = attributeValuesProperty.getValues();

        if (names.length != values.length) {
            return defaultValue;
        }

        for (int i = 0; i < names.length; i++) {
            final String name = names[i].getString();
            if (name.equals(attributeName)) {
                return values[i].getString();
            }
        }

        return defaultValue;
    }
}
