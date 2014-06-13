/*
 *  Copyright 2010-2013 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.upgrade;

import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.TreeSet;

class Item extends TreeMap<String, Value[]> implements Comparable<Item> {

    private static final long serialVersionUID = 1L;

    String name;
    private String parentPath;
    SortedMap<String, Set<Item>> children = new TreeMap<String, Set<Item>>();

    Item(String name) {
        this.name = name;
    }

    protected void setParentPath(String parentPath) {
        this.parentPath = parentPath;
    }

    public String getPath() {
        return (parentPath == null ? "" : parentPath) + (name == null ? "" : "/" + name);
    }

    void add(Item child) {
        if (!children.containsKey(child.name)) {
            children.put(child.name, new TreeSet<Item>());
        }
        child.setParentPath(getPath());
        children.get(child.name).add(child);
    }

    public int compareTo(Item o) {
        if (o == this) {
            return 0;
        }
        Iterator<Map.Entry<String, Value[]>> thisIter = entrySet().iterator();
        Iterator<Map.Entry<String, Value[]>> thatIter = o.entrySet().iterator();
        while (thisIter.hasNext()) {
            if (!thatIter.hasNext()) {
                return 1;
            }
            Map.Entry<String, Value[]> thisEntry = thisIter.next();
            Map.Entry<String, Value[]> thatEntry = thatIter.next();
            int keyCmp = thisEntry.getKey().compareTo(thatEntry.getKey());
            if (keyCmp != 0) {
                return keyCmp;
            }
            Value[] thisValue = thisEntry.getValue();
            Value[] thatValue = thatEntry.getValue();
            if (thisValue.length != thatValue.length) {
                return thisValue.length < thatValue.length ? -1 : 1;
            }
            for (int i = 0; i < thisValue.length; i++) {
                int valueCmp = thisValue[i].compareTo(thatValue[i]);
                if (valueCmp != 0) {
                    return valueCmp;
                }
            }
        }
        if (thatIter.hasNext()) {
            return -1;
        }
        return 0;
    }
}
