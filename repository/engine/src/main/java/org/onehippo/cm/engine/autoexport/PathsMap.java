package org.onehippo.cm.engine.autoexport;

import java.util.Collections;
import java.util.Iterator;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

public class PathsMap implements Iterable<String> {

    private TreeSet<String> paths = new TreeSet<>();
    private SortedSet<String> unmodifiablePaths = Collections.unmodifiableSortedSet(paths);

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

    public SortedSet<String> getPaths() {
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
