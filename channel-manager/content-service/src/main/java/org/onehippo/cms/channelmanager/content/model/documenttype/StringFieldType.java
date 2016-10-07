/*
 * Copyright 2016 Hippo B.V. (http://www.onehippo.com)
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

package org.onehippo.cms.channelmanager.content.model.documenttype;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Value;

public class StringFieldType extends FieldType {

    public StringFieldType() {
        this.setType(Type.STRING);
    }

    public Optional<Object> readFrom(final Node node) {
        final String property = getId();
        try {
            if (node.hasProperty(property)) {
                if (isStoredAsMultiValueProperty()) {
                    final List<String> values = new ArrayList<>();
                    for (Value v : node.getProperty(property).getValues()) {
                        values.add(v.getString());
                    }
                    if (!values.isEmpty()) {
                        return Optional.of(values);
                    }
                } else {
                    return Optional.of(node.getProperty(property).getString());
                }
            }
        } catch (RepositoryException e) {
            // add a debug message? we should never get here.
        }
        return Optional.empty();
    }
}
