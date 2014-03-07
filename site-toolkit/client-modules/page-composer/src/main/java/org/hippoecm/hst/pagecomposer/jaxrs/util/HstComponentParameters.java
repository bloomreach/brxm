/*
 *  Copyright 2012-2013 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.hst.pagecomposer.jaxrs.util;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Value;

import org.apache.commons.lang.StringUtils;
import org.hippoecm.hst.configuration.HstNodeTypes;
import org.hippoecm.hst.container.RequestContextProvider;
import org.hippoecm.hst.core.container.ContainerConstants;
import org.hippoecm.hst.pagecomposer.jaxrs.services.helpers.ContainerItemHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Utility class to retrieve and modify HST component parameters of a node. Empty prefixes are returned as the
 * string {@link org.hippoecm.hst.core.container.ContainerConstants#DEFAULT_PARAMETER_PREFIX}, and that prefix is saved again as an empty string.
 */
public class HstComponentParameters {

    private static final Logger log = LoggerFactory.getLogger(HstComponentParameters.class);

    private final Node node;
    private final ContainerItemHelper containerItemHelper;
    private final Map<String, Map<String, String>> prefixedParameters;

    public HstComponentParameters(final Node node, final ContainerItemHelper containerItemHelper) throws RepositoryException {
        this.node = node;
        this.containerItemHelper = containerItemHelper;
        prefixedParameters = new HashMap<String, Map<String, String>>();
        initialize();
    }

    private void initialize() throws RepositoryException {
        if (!nodeHasParameterNamesAndValues()) {
            return;
        }
        if (nodeHasPrefixes()) {
            readPrefixesAndParameters();
        } else {
            readDefaultParameters();
        }
    }

    private void readPrefixesAndParameters() throws RepositoryException {
        Value[] prefixes = node.getProperty(HstNodeTypes.COMPONENT_PROPERTY_PARAMETER_NAME_PREFIXES).getValues();
        Value[] names = node.getProperty(HstNodeTypes.GENERAL_PROPERTY_PARAMETER_NAMES).getValues();
        Value[] values = node.getProperty(HstNodeTypes.GENERAL_PROPERTY_PARAMETER_VALUES).getValues();

        if (!(prefixes.length == names.length && prefixes.length == values.length)) {
            log.warn("Parameter names, values and prefixes are are not all of equal length for '{}'", node.getPath());
            throw new IllegalStateException(HstNodeTypes.COMPONENT_PROPERTY_PARAMETER_NAME_PREFIXES + ", "
                    + HstNodeTypes.GENERAL_PROPERTY_PARAMETER_NAMES + " and "
                    + HstNodeTypes.GENERAL_PROPERTY_PARAMETER_VALUES + " properties do not have the same number of values");
        }

        for (int i = 0; i < names.length; i++) {
            String prefix = prefixOrDefault(prefixes[i].getString());
            setValue(prefix, names[i].getString(), values[i].getString());
        }
    }

    private void readDefaultParameters() throws RepositoryException {
        Value[] names = node.getProperty(HstNodeTypes.GENERAL_PROPERTY_PARAMETER_NAMES).getValues();
        Value[] values = node.getProperty(HstNodeTypes.GENERAL_PROPERTY_PARAMETER_VALUES).getValues();

        if (names.length != values.length) {
            log.warn("Parameter names and values are not of equal length for '{}'", node.getPath());
            throw new IllegalStateException(HstNodeTypes.GENERAL_PROPERTY_PARAMETER_NAMES + " and "
                    + HstNodeTypes.GENERAL_PROPERTY_PARAMETER_VALUES
                    + " properties do not have the same number of values");
        }

        for (int i = 0; i < names.length; i++) {
            setValue(ContainerConstants.DEFAULT_PARAMETER_PREFIX, names[i].getString(), values[i].getString());
        }
    }

    private static String prefixOrDefault(final String prefix) {
        return StringUtils.isEmpty(prefix) ? ContainerConstants.DEFAULT_PARAMETER_PREFIX : prefix;
    }

    private boolean nodeHasPrefixes() throws RepositoryException {
        return node.hasProperty(HstNodeTypes.COMPONENT_PROPERTY_PARAMETER_NAME_PREFIXES);
    }

    private boolean nodeHasParameterNamesAndValues() throws RepositoryException {
        return node.hasProperty(HstNodeTypes.GENERAL_PROPERTY_PARAMETER_NAMES) && node.hasProperty(HstNodeTypes.GENERAL_PROPERTY_PARAMETER_VALUES);
    }

    public static boolean isDefaultPrefix(final String prefix) {
        return StringUtils.isEmpty(prefix) || ContainerConstants.DEFAULT_PARAMETER_PREFIX.equals(prefix);
    }

    public boolean hasPrefix(final String prefix) {
        return prefixedParameters.containsKey(prefixOrDefault(prefix));
    }

    public Set<String> getPrefixes() {
        return new HashSet<String>(prefixedParameters.keySet());
    }

    public boolean hasParameter(final String prefix, final String name) {
        final Map<String, String> parameters = prefixedParameters.get(prefixOrDefault(prefix));
        if (parameters == null) {
            return false;
        }
        return parameters.containsKey(name);
    }

    public boolean hasDefaultParameter(final String name) {
        return hasParameter(ContainerConstants.DEFAULT_PARAMETER_PREFIX, name);
    }

    public String getValue(final String prefix, final String name) {
        final Map<String, String> parameters = prefixedParameters.get(prefixOrDefault(prefix));
        if (parameters == null) {
            return null;
        }
        return parameters.get(name);
    }

    public String getDefaultValue(final String name) {
        return getValue(ContainerConstants.DEFAULT_PARAMETER_PREFIX, name);
    }

    public void setValue(final String prefix, final String name, final String value) {
        final String prefixOrDefault = prefixOrDefault(prefix);
        Map<String, String> parameters = prefixedParameters.get(prefixOrDefault);
        if (parameters == null) {
            parameters = new HashMap<String, String>();
            prefixedParameters.put(prefixOrDefault, parameters);
        }
        parameters.put(name, value);
    }

    public void renamePrefix(String oldPrefix, String newPrefix) throws IllegalArgumentException {
        if (isDefaultPrefix(oldPrefix)) {
            throw new IllegalArgumentException("Cannot rename default prefix '" + oldPrefix + "'");
        }
        if (isDefaultPrefix(newPrefix)) {
            throw new IllegalArgumentException("Cannot rename prefix '" + oldPrefix + "' to default prefix '" + newPrefix + "'");
        }
        Map<String, String> parameters = prefixedParameters.remove(oldPrefix);
        prefixedParameters.put(newPrefix, parameters);
    }

    public boolean removePrefix(final String prefix) {
        if (!isDefaultPrefix(prefix)) {
            Map<String, String> removedParameters = prefixedParameters.remove(prefix);
            return removedParameters != null;
        }
        return false;
    }

    public void save(long versionStamp) throws RepositoryException, IllegalStateException {
        setNodeChanges();
        if (RequestContextProvider.get() == null) {
            node.getSession().save();
        } else {
            if (!node.isNodeType(HstNodeTypes.NODETYPE_HST_CONTAINERITEMCOMPONENT)) {
                throw new IllegalStateException("Node to be saved must be of type '"+HstNodeTypes.NODETYPE_HST_CONTAINERITEMCOMPONENT+"' but " +
                        "was of type '"+node.getPrimaryNodeType().getName()+"'. Skip save");
            }
            containerItemHelper.acquireLock(node, versionStamp);
            HstConfigurationUtils.persistChanges(node.getSession());
        }
    }

    private void setNodeChanges() throws RepositoryException {
        List<String> prefixes = new ArrayList<String>();
        List<String> names = new ArrayList<String>();
        List<String> values = new ArrayList<String>();

        boolean addPrefixes = false;

        for (Map.Entry<String, Map<String, String>> entry : prefixedParameters.entrySet()) {
            String prefix = entry.getKey();
            String savePrefix = prefix.equals(ContainerConstants.DEFAULT_PARAMETER_PREFIX) ? "" : prefix;

            for (Map.Entry<String, String> f : entry.getValue().entrySet()) {
                String name = f.getKey();
                String value = f.getValue();

                if (!savePrefix.isEmpty()) {
                    // only add the HstNodeTypes.COMPONENT_PROPERTY_PARAMETER_NAME_PREFIXES property
                    // if there are actually prefixed name/value pairs
                    addPrefixes = true;
                }
                prefixes.add(savePrefix);
                names.add(name);
                values.add(value);
            }
        }

        if (addPrefixes) {
            node.setProperty(HstNodeTypes.COMPONENT_PROPERTY_PARAMETER_NAME_PREFIXES, prefixes.toArray(new String[prefixes.size()]));
        } else if (node.hasProperty(HstNodeTypes.COMPONENT_PROPERTY_PARAMETER_NAME_PREFIXES)) {
            node.getProperty(HstNodeTypes.COMPONENT_PROPERTY_PARAMETER_NAME_PREFIXES).remove();
        }
        if (names.isEmpty()) {
            if (node.hasProperty(HstNodeTypes.GENERAL_PROPERTY_PARAMETER_NAMES)) {
                node.getProperty(HstNodeTypes.GENERAL_PROPERTY_PARAMETER_NAMES).remove();
            }
            assert values.isEmpty();
            if (node.hasProperty(HstNodeTypes.GENERAL_PROPERTY_PARAMETER_VALUES)) {
                node.getProperty(HstNodeTypes.GENERAL_PROPERTY_PARAMETER_VALUES).remove();
            }
        } else {
            node.setProperty(HstNodeTypes.GENERAL_PROPERTY_PARAMETER_NAMES, names.toArray(new String[names.size()]));
            node.setProperty(HstNodeTypes.GENERAL_PROPERTY_PARAMETER_VALUES, values.toArray(new String[values.size()]));
        }
    }

}
