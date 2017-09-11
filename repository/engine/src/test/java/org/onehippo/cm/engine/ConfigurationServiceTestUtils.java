/*
 * Copyright 2017 Hippo B.V. (http://www.onehippo.com)
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

package org.onehippo.cm.engine;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.jcr.Node;
import javax.jcr.Property;
import javax.jcr.RepositoryException;

import org.hippoecm.repository.util.NodeIterable;
import org.hippoecm.repository.util.PropertyIterable;
import org.onehippo.cm.model.impl.path.JcrPathSegment;

public class ConfigurationServiceTestUtils {

    public static String createChildNodesString(final Node node) throws RepositoryException {
        final List<JcrPathSegment> names = new ArrayList<>();
        for (Node child : new NodeIterable(node.getNodes())) {
            names.add(JcrPathSegment.get(child));
        }
        if (!node.getPrimaryNodeType().hasOrderableChildNodes()) {
            Collections.sort(names);
        }
        return names.toString();
    }

    public static String createChildPropertiesString(final Node node) throws RepositoryException {
        final List<String> names = new ArrayList<>();
        for (Property property : new PropertyIterable(node.getProperties())) {
            names.add(property.getName());
        }
        Collections.sort(names);
        return names.toString();
    }

}
