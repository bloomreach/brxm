/*
 *  Copyright 2008 Hippo.
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
package org.hippoecm.frontend.i18n;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.jcr.Node;
import javax.jcr.Property;
import javax.jcr.RepositoryException;
import javax.jcr.Value;

import org.apache.wicket.model.IModel;
import org.hippoecm.frontend.model.properties.JcrPropertyModel;
import org.hippoecm.frontend.model.properties.JcrPropertyValueModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LocalizationNodeWrapper implements Comparable<LocalizationNodeWrapper> {
    private static final long serialVersionUID = 1L;

    final static Logger log = LoggerFactory.getLogger(LocalizationNodeWrapper.class);

    private Node node;
    private Map<String, String> keys;
    private Set<String> matches;

    LocalizationNodeWrapper(Node node, Map<String, String> keys) {
        this.node = node;
        this.keys = keys;

        matches = new HashSet<String>();
        try {
            for (Map.Entry<String, String> entry : keys.entrySet()) {
                if (node.hasProperty(entry.getKey())) {
                    String value = node.getProperty(entry.getKey()).getString();
                    if (value.equals(entry.getValue())) {
                        matches.add(entry.getKey());
                    }
                }
            }
        } catch(RepositoryException ex) {
            log.error(ex.getMessage());
        }
    }

    public IModel getModel() throws RepositoryException {
        Property property = node.getProperty("hippo:message");
        Value value = property.getValue();
        return new JcrPropertyValueModel(-1, value, new JcrPropertyModel(property));
    }

    public int compareTo(LocalizationNodeWrapper that) {
        for (Map.Entry<String, String> entry : keys.entrySet()) {
            boolean matchA = matches.contains(entry.getKey());
            boolean matchB = that.matches.contains(entry.getKey());
            if (matchA && !matchB) {
                return -1;
            } else if (!matchA && matchB) {
                return 1;
            }
        }
        return 0;
    }

}
