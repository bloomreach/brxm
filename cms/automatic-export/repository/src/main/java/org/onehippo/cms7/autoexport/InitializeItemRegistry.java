/*
 *  Copyright 2011 Hippo.
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
package org.onehippo.cms7.autoexport;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.jcr.observation.Event;


class InitializeItemRegistry {
    
    private final Map<String, Collection<InitializeItem>> initializeItemsByContextPath = new HashMap<String, Collection<InitializeItem>>();
    private final Map<String, InitializeItem> initializeItemsByNamespace = new HashMap<String, InitializeItem>();
    private final Map<String, InitializeItem> initializeItemsByPrefix = new HashMap<String, InitializeItem>();
    
    
    Collection<InitializeItem> getInitializeItemsByPath(String path, int eventType) {
        if (path.startsWith("/jcr:system/jcr:nodeTypes")) {
            // path = /jcr:system/jcr:nodeTypes/example:doctype/jcr:propertyDefinition
            // prefix = example
            String relPath = path.substring("/jcr:system/jcr:nodeTypes/".length());
            int offset = relPath.indexOf('/');
            String nodeTypeRoot = (offset == -1) ? relPath : relPath.substring(0, offset);
            int indexOfColon = nodeTypeRoot.indexOf(':');
            String prefix = (indexOfColon == -1) ? nodeTypeRoot : nodeTypeRoot.substring(0, indexOfColon);
            InitializeItem item = getInitializeItemByNamespacePrefix(prefix);
            return item == null ? null : Arrays.asList(item);
        }
        return getInitializeItemByContextPath(path, eventType);
    }

    /**
     * Gets all initialize items that have context paths that start with the supplied <code>path</code>
     */
    List<InitializeItem> getDescendentInitializeItems(String path) {
        String prefixPath = path + "/";
        List<InitializeItem> result = new ArrayList<InitializeItem>();
        for (Collection<InitializeItem> items : initializeItemsByContextPath.values()) {
            for (InitializeItem item : items) {
                if (item.getContextPath().startsWith(prefixPath)) {
                    result.add(item);
                }
            }
        }
        return result;
    }

    /**
     * Gets the initialize items with the longest matching context path or null if
     * a new one needs to be created.
     */
    private Collection<InitializeItem> getInitializeItemByContextPath(String path, int eventType) {
        Collection<InitializeItem> result = null;
        String resultContextPath = null;
        // first try an exact match
        Collection<InitializeItem> items = initializeItemsByContextPath.get(path);
        if (items != null) {
            result = items;
            resultContextPath = path;
        }
        if (result == null) {
            // gather the items with the longest matching context paths
            for (Map.Entry<String, Collection<InitializeItem>> entry : initializeItemsByContextPath.entrySet()) {
                String contextPath = entry.getKey();
                items = entry.getValue();
                if (path.startsWith(contextPath + "/")) {
                    if (result == null) {
                        result = new ArrayList<InitializeItem>();
                    }
                    if (resultContextPath != null && contextPath.length() > resultContextPath.length()) {
                        // we have longer matching items than previously found
                        result.clear();
                    }
                    if (resultContextPath == null || contextPath.length() >= resultContextPath.length()) {
                        result.addAll(items);
                        resultContextPath = contextPath;
                    }
                }
            }
        }
        // check if item conforms to location hierarchy
        if (result != null && eventType == Event.NODE_ADDED) {
            assert resultContextPath != null;
            // If the context node of the result is not the one that is required, return null:
            // a new instruction needs to be created.
            String requiredContextPath = LocationMapper.contextNodeForPath(path, true);
            if (requiredContextPath != null && !requiredContextPath.equals(resultContextPath)) {
                // but we allow longer context paths if they already exist
                if (requiredContextPath.length() > resultContextPath.length()) {
                    result = null;
                }
            }
        }
        return result;
    }
    
    /**
     * Gets the initialize item by namespace uri. If an initialize item was registered
     * with uri http://example.com/1.0 and we request the initialize item for http://example.com/2.0
     * the former will count as a match and is returned.
     */
    InitializeItem getInitializeItemByNamespace(String namespace) {
        InitializeItem result = initializeItemsByNamespace.get(namespace);
        if (result == null) {
            int offset = namespace.lastIndexOf('/');
            // namespace = http://example.com/1.0
            // namespaceRoot = http://example.com
            // versionString = 1.0
            String namespaceRoot = offset == -1 ? namespace : namespace.substring(0, offset);
            // TODO: check if versionString and versionStringCompare are indeed version strings
//            String versionString = offset == -1 ? null : namespace.substring(offset+1);
            for (String key : initializeItemsByNamespace.keySet()) {
                offset = key.lastIndexOf('/');
                String namespaceRootCompare = offset == -1 ? key : key.substring(0, offset);
//                String versionStringCompare = offset == -1 ? null : key.substring(offset+1);
                if (namespaceRoot.equals(namespaceRootCompare)) {
                    return initializeItemsByNamespace.get(key);
                }
            }
        }
        return result;
    }
    
    InitializeItem getInitializeItemByNamespacePrefix(String prefix) {
        return initializeItemsByPrefix.get(prefix);
    }
    
    boolean addInitializeItem(InitializeItem item) {
        boolean added = false;
        if (item.getContextPath() != null) {
            Collection<InitializeItem> items = initializeItemsByContextPath.get(item.getContextPath());
            if (items == null) {
                items = new ArrayList<InitializeItem>(2);
                initializeItemsByContextPath.put(item.getContextPath(), items);
            }
            items.add(item);
            added = true;
        }
        if (item.getNamespace() != null) {
            initializeItemsByNamespace.put(item.getNamespace(), item);
            added = true;
        }
        if (item.getNodeTypesResource() != null) {
            initializeItemsByPrefix.put(ExportUtils.prefixFromName(item.getName()), item);
            added = true;
        }
        return added;
    }
    
    boolean removeInitializeItem(InitializeItem item) {
        boolean removed = false;
        if (item.getContextPath() != null) {
            Collection<InitializeItem> items = initializeItemsByContextPath.remove(item.getContextPath());
            if (items != null) {
                removed = items.remove(item);
                if (items.isEmpty()) {
                    initializeItemsByContextPath.remove(item.getContextPath());
                }
            }
        }
        if (item.getNamespace() != null) {
            removed = initializeItemsByNamespace.remove(item.getNamespace()) != null;
        }
        if (item.getNodeTypesResource() != null) {
            removed = initializeItemsByPrefix.remove(ExportUtils.prefixFromName(item.getName())) != null;
        }
        return removed;
    }
    
    boolean replaceInitializeItem(InitializeItem oldItem, InitializeItem newItem) {
        if (removeInitializeItem(oldItem)) {
            return addInitializeItem(newItem);
        }
        return false;
    }

}
