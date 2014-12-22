/*
 * Copyright 2013 Hippo B.V. (http://www.onehippo.com)
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

package org.onehippo.cms7.resourcebundle.data;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.jcr.Node;
import javax.jcr.Property;
import javax.jcr.PropertyIterator;
import javax.jcr.RepositoryException;
import javax.jcr.Value;

import org.apache.wicket.model.IDetachable;
import org.hippoecm.frontend.session.UserSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A bundle contains the data structures that represent a bundle document's content, the document is stored in
 * the JCR repository, and referred to by a JCR node.
 */
public class Bundle implements IDetachable {

    private static Logger log = LoggerFactory.getLogger(Bundle.class);

    private static final String NAMESPACE = "resourcebundle:";
    private static final String PROP_KEYS = NAMESPACE + "keys";
    private static final String PROP_DESCRIPTIONS = NAMESPACE + "descriptions";
    static final String PROP_VALUES_PREFIX = NAMESPACE + "messages";
    private static final String PROP_PATTERN_VALUES = PROP_VALUES_PREFIX + "*";

    // properties for accessing backing JCR Node
    // Since Bundle should be serializable (it's referenced directly from the plugin!)
    // the Node is not referenced directly.  Serializing the Node would effectively serialize
    // the whole repository, including other JCR sessions, bundle caches, etcetera.  Big NONO.
    private final String nodeId;
    private transient Node node; // JCR node holding the bundle document

    private final String defaultValueSetName; // display-name to use for default value set
    private List<Resource> resources; // List of resources, sorted by display name
    private List<Resource> resourceByIndex; // List of resources, preserving storage order
    private List<ValueSet> valueSets; // List of value-sets, sorted by key


    public Bundle(Node node, String defaultValueSetName) {
        try {
            this.nodeId = node.getIdentifier();
        } catch (RepositoryException e) {
            throw new RuntimeException("Cannot edit bundle node", e);
        }
        this.node = node;
        this.defaultValueSetName = defaultValueSetName;
        load();
    }

    public String getDefaultValueSetName() {
        return defaultValueSetName;
    }

    public List<Resource> getResources() {
        return resources;
    }

    public List<ValueSet> getValueSets() {
        return valueSets;
    }

    public List<ValueSet> getMutableValueSets() {
        List<ValueSet> result = new ArrayList<ValueSet>();
        for (ValueSet valueSet : valueSets) {
            if (!valueSet.getDisplayName().equals(defaultValueSetName)) {
                result.add(valueSet);
            }
        }
        return result;
    }

    public Resource newResource() {
        Map<String, String> valueMap = new HashMap<String, String>();
        for (ValueSet valueSet : valueSets) {
            valueMap.put(valueSet.getName(), "");
        }
        return new Resource(this, "", "", valueMap);
    }

    public Resource copyResource(Resource original) {
        Map<String, String> valueMap = new HashMap<String, String>(original.getValueMap());
        return new Resource(this, original.getKey(), original.getDescription(), valueMap);
    }

    public void addResource(Resource resource) {
        resourceByIndex.add(resource);
        resources.add(resource);
        Collections.sort(resources);
    }

    public void deleteResource(Resource resource) {
        resourceByIndex.remove(resource);
        resources.remove(resource);
    }

    public int indexOfResource(Resource resource) {
        return resourceByIndex.indexOf(resource);
    }

    public ValueSet newValueSet() {
        return new ValueSet(this, "", defaultValueSetName);
    }

    public void addValueSet(ValueSet valueSet) {
        String name = valueSet.makeName();

        for (Resource resource: resources) {
            resource.setValue(name, "");
        }

        valueSet.setName(name);
        valueSets.add(valueSet);
        Collections.sort(valueSets);
    }

    public void renameValueSet(ValueSet valueSet) {
        String oldName = valueSet.getName();
        String newName = valueSet.makeName();

        for (Resource resource : resources) {
            resource.setValue(newName, resource.deleteValue(oldName));
        }

        valueSet.setName(newName);
        Collections.sort(valueSets);
    }

    public void deleteValueSet(ValueSet valueSet) {
        valueSets.remove(valueSet);

        for (Resource resource : resources) {
            resource.deleteValue(valueSet.getName());
        }
    }

    public String getNameFromDisplayName(String displayName) {
        if (displayName == null) {
            return null;
        }

        if (displayName.equals(defaultValueSetName)) {
            return PROP_VALUES_PREFIX;
        }

        return PROP_VALUES_PREFIX + "_" + displayName;
    }

    public void save() {
        attach();

        String path = "";
        int numResources = resources == null ? 0 : resources.size();
        int numValueSets = valueSets == null ? 0 : valueSets.size();

        try {
            path = node.getPath();

            // remove all value set properties to support deletion
            PropertyIterator valueSetPropertyIterator = node.getProperties(PROP_PATTERN_VALUES);
            while (valueSetPropertyIterator.hasNext()) {
                Property valueSetProperty = valueSetPropertyIterator.nextProperty();
                valueSetProperty.remove();
            }

            String[] keys = new String[numResources];
            String[] descriptions = new String[numResources];
            String[][] values = new String[numValueSets][numResources];

            for (int r = 0; r < keys.length; r++) {
                Resource resource = resourceByIndex.get(r);

                keys[r] = resource.getKey();
                String description = resource.getDescription();
                if (description == null) {
                    description = ""; // null in the array will make JCR compact the multi-value property...
                }
                descriptions[r] = description;
                for (int v = 0; v < numValueSets; v++) {
                    String value = resource.getValue(valueSets.get(v).getName());
                    if (value == null) {
                        value = ""; // null in the array will make JCR compact the multi-value property...
                    }
                    values[v][r] = value;
                }
            }

            node.setProperty(PROP_KEYS, keys);
            node.setProperty(PROP_DESCRIPTIONS, descriptions);
            for (int v = 0; v < numValueSets; v++) {
                node.setProperty(valueSets.get(v).getName(), values[v]);
            }
        } catch (RepositoryException ex) {
            log.error("Error saving bundle document " + path, ex);
        }
    }

    /**
     * Load the bundle from the JCR node, construct data structures.
     */
    protected void load() {
        attach();

        String path = "";
        try {
            path = node.getPath();

            // Load value-sets
            if (valueSets == null) {
                valueSets = new ArrayList<ValueSet>();
            } else {
                valueSets.clear();
            }

            List<Value[]> valuesList = new ArrayList<Value[]>();
            List<String> valueSetNames = new ArrayList<String>();
            PropertyIterator valueSetPropertyIterator = node.getProperties(PROP_PATTERN_VALUES);
            while (valueSetPropertyIterator.hasNext()) {
                Property valueSetProperty = valueSetPropertyIterator.nextProperty();

                ValueSet valueSet = new ValueSet(this, valueSetProperty.getName(), defaultValueSetName);
                valueSets.add(valueSet);

                valuesList.add(valueSetProperty.getValues());
                valueSetNames.add(valueSet.getName());
            }
            Collections.sort(valueSets);

            // Load Resources
            if (resources == null) {
                resources = new ArrayList<Resource>();
            } else {
                resources.clear();
            }
            if (resourceByIndex == null) {
                resourceByIndex = new ArrayList<Resource>();
            } else {
                resourceByIndex.clear();
            }

            if (node.hasProperty(PROP_KEYS)) {
                Value[] keys = node.getProperty(PROP_KEYS).getValues();

                Value[] descriptions = null;
                if (node.hasProperty(PROP_DESCRIPTIONS)) {
                    descriptions = node.getProperty(PROP_DESCRIPTIONS).getValues();
                }

                for (int r = 0; r < keys.length; r++) {
                    String description = (descriptions != null && descriptions.length > r)
                                       ? descriptions[r].getString() : "";

                    // build the map of {valueSetName, value} for this resource
                    Map<String, String> valueMap = new HashMap<String, String>();

                    for (int v = 0; v < valuesList.size(); v++) {
                        Value[] values = valuesList.get(v);
                        String value = (values.length > r) ? values[r].getString() : "";
                        valueMap.put(valueSetNames.get(v), value);
                    }

                    Resource resource = new Resource(this, keys[r].getString(), description, valueMap);
                    resourceByIndex.add(resource);
                    resources.add(resource);
                }
                Collections.sort(resources);
            }
        } catch (RepositoryException ex) {
            log.error("Error loading resource bundle document " + path, ex);
        }
    }

    private void attach() {
        if (node == null) {
            try {
                node = UserSession.get().getJcrSession().getNodeByIdentifier(nodeId);
            } catch (RepositoryException e) {
                throw new RuntimeException("Unable to load bundle node", e);
            }
        }
    }

    @Override
    public void detach() {
        node = null;
    }
}
