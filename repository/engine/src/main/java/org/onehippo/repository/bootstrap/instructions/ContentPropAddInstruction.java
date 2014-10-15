/*
 * Copyright 2014 Hippo B.V. (http://www.onehippo.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.onehippo.repository.bootstrap.instructions;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.jcr.Property;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.Value;

import org.onehippo.repository.bootstrap.InitializeInstruction;
import org.onehippo.repository.bootstrap.InitializeItem;
import org.onehippo.repository.bootstrap.PostStartupTask;

import static org.hippoecm.repository.api.HippoNodeType.HIPPO_CONTENTPROPADD;
import static org.onehippo.repository.bootstrap.util.BootstrapConstants.log;

public class ContentPropAddInstruction extends InitializeInstruction {

    public ContentPropAddInstruction(final InitializeItem item, final Session session) {
        super(item, session);
    }

    @Override
    protected String getName() {
        return HIPPO_CONTENTPROPADD;
    }

    @Override
    public PostStartupTask execute() throws RepositoryException {
        final Property contentAddProperty = item.getContentPropAddProperty();
        final String contentRoot = item.getContentRoot();
        final Property property = session.getProperty(contentRoot);
        if (property.isMultiple()) {
            final List<Value> values = new ArrayList<>(Arrays.asList(property.getValues()));
            values.addAll(Arrays.asList(contentAddProperty.getValues()));
            property.setValue(values.toArray(new Value[values.size()]));
        } else {
            log.warn("Invalid content prop add item {}: Cannot add multiple values to a single valued property", item.getName());
        }
        return null;
    }
}
