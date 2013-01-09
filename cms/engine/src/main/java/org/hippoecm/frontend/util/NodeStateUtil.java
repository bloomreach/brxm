/*
 *  Copyright 2011-2013 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.frontend.util;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class NodeStateUtil {

    public static List<String> added(Collection<String> old, Collection<String> nu) {
        List<String> added = null;
        for (String s : nu) {
            if (!old.contains(s)) {
                if (added == null) {
                    added = new ArrayList<String>(nu.size());
                }
                added.add(s);
            }
        }
        return added;
    }
    
    public static List<String> removed(Collection<String> old, Collection<String> nu) {
        List<String> removed = null;
        for (String s : old) {
            if (!nu.contains(s)) {
                if (removed == null) {
                    removed = new ArrayList<String>(old.size());
                }
                removed.add(s);
            }
        }
        return removed;
    }
    
    public static List<String> moved(List<String> old, List<String> nu, Collection<String> added, Collection<String> removed) {
        List<String> moved = null;
        // first we construct the new list with added nodes removed and removed nodes added
        // so that the difference with the old list is just moved nodes
        List<String> compare = null;
        if (added != null) {
            for (String child : added) {
                if (compare == null) {
                    compare = new ArrayList<String>(nu);
                }
                compare.remove(child);
            }
        }
        if (removed != null) {
            for (String child : removed) {
                if (compare == null) {
                    compare = new ArrayList<String>(nu);
                }
                compare.add(old.indexOf(child), child);
            }
        }
        if (compare == null) {
            compare = nu;
        }
        // now we can determine if any nodes were moved
        for (String child : old) {
            if (old.indexOf(child) != compare.indexOf(child)) {
                if (moved == null) {
                    moved = new ArrayList<String>(old.size());
                }
                moved.add(child);
            }
        }
        return moved;
    }

}
