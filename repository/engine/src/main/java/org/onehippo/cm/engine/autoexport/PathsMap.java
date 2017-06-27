/*
 *  Copyright 2017 Hippo B.V. (http://www.onehippo.com)
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
package org.onehippo.cm.engine.autoexport;

import java.util.Collections;
import java.util.Iterator;
import java.util.Set;

import org.apache.commons.collections4.trie.PatriciaTrie;

public class PathsMap implements Iterable<String> {

    // use a PATRICIA Trie, which stores strings efficiently when there are common prefixes
    private Set<String> paths = Collections.newSetFromMap(new PatriciaTrie<>());
    private Set<String> unmodifiablePaths = Collections.unmodifiableSet(paths);

    /**
     * Makes a path start with a / and not end with a /. unless its / itself
     * @param path path to make absolute
     */
    public static String makeAbsolute(final String path) {
        final String proper = path.charAt(0) == '/' ? path : "/" + path;
        for (int index = proper.length()-1; index > -1; index--) {
            if (proper.charAt(index) != '/') {
                return proper.substring(0, index+1);
            }
        }
        return "/";
    }

    public PathsMap() {
    }

    public PathsMap(Set<String> paths)
    {
        for (final String path : paths) {
            add(path);
        }
    }

    public PathsMap(String[] paths)
    {
        for (final String path : paths) {
            add(path);
        }
    }

    public Set<String> getPaths() {
        return unmodifiablePaths;
    }

    public boolean isEmpty() {
        return paths.isEmpty();
    }

    public void clear() {
        paths.clear();
    }

    public void addAll(final Set<String> paths) {
        for (final String path : paths) {
            add(path);
        }
    }

    public void addAll(final PathsMap other) {
        if (other != null) {
            paths.addAll(other.paths);
        }
    }

    public boolean add(final String path) {
        return paths.add(makeAbsolute(path));
    }

    public boolean remove(final String path) {
        return paths.remove(makeAbsolute(path));
    }

    public boolean removeChildren(final String parentPath) {
        boolean childRemoved = false;
        final String childPath = makeAbsolute(parentPath) + "/";
        if ("//".equals(childPath)) {
            boolean hasRoot = paths.remove("/");
            childRemoved = !paths.isEmpty();
            paths.clear();
            if (hasRoot) {
                paths.add("/");
            }
        } else {
            for (Iterator<String> iter = paths.iterator(); iter.hasNext(); ) {
                final String path = iter.next();
                if (path.startsWith(childPath)) {
                    childRemoved = true;
                    iter.remove();
                } else if (path.compareTo(childPath) > 0) {
                    break;
                }
            }
        }
        return childRemoved;
    }

    public boolean matches(final String somePath) {
        if (paths.contains("/")) {
            return true;
        }
        final String properPath = makeAbsolute(somePath);
        if ("/".equals(properPath)) {
            return false;
        }
        for (String path : paths) {
            final String childPath = path+"/";
            if (properPath.equals(path) || properPath.startsWith(childPath)) {
                return true;
            } else if ((childPath).compareTo(properPath) > 0) {
                return false;
            }
        }
        return false;
    }

    @Override
    public Iterator<String> iterator() {
        return paths.iterator();
    }

    @Override
    public String toString() {
        return paths.toString();
    }
}
