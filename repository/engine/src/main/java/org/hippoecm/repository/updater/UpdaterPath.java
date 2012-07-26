/*
 *  Copyright 2010 Hippo.
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
package org.hippoecm.repository.updater;

import java.util.Iterator;
import java.util.NoSuchElementException;

/**
 * Path used during upgrade.  Assures that items are processed in the correct order.
 */
public class UpdaterPath implements Comparable<UpdaterPath> {

    private String[] names;
    private Integer[] indices;
    private String string;

    public UpdaterPath(String[] elements) {
        this(elements, elements.length+1);
    }

    public UpdaterPath(String[] pathNames, Integer[] pathIndices, int endIndex) {
        StringBuffer sb = new StringBuffer();
        names = new String[endIndex+1];
        indices = new Integer[endIndex+1];
        for (int i = 0; i <= endIndex; i++) {
            if (i > 0) {
                sb.append("/");
            }
            sb.append(pathNames[i]);
            if (pathIndices[i] > 1) {
                sb.append("[");
                sb.append(pathIndices[i]);
                sb.append("]");
            }
            names[i] = pathNames[i];
            indices[i] = pathIndices[i];
        }
        this.string = new String(sb);
    }

    public UpdaterPath(String[] elements, int endIndex) {
        StringBuffer sb = new StringBuffer();
        names = new String[endIndex+1];
        indices = new Integer[endIndex+1];
        for (int i = 0; i <= endIndex; i++) {
            String element = elements[i];
            if (i > 0) {
                sb.append("/");
            }
            sb.append(element);
            if (element.contains("[")) {
                names[i] = element.substring(0, element.indexOf('['));
                indices[i] = Integer.parseInt(element.substring(element.indexOf('[') + 1, element.indexOf(']')));
            } else {
                names[i] = element;
                indices[i] = 0;
            }
        }
        this.string = new String(sb);
    }

    public UpdaterPath(String path) {
        this.string = path;

        String[] elements = path.split("/");
        names = new String[elements.length];
        indices = new Integer[elements.length];
        for (int i = 0; i < elements.length; i++) {
            String element = elements[i];
            if (element.contains("[")) {
                names[i] = element.substring(0, element.indexOf('['));
                indices[i] = Integer.parseInt(element.substring(element.indexOf('[') + 1, element.indexOf(']')));
            } else {
                names[i] = element;
                indices[i] = 0;
            }
        }
    }

    public UpdaterPath documentVariantPath() {
        for(int i=0; i<names.length-1; i++) {
            if(names[i].equals(names[i+1])) {
                return new UpdaterPath(names, indices, i+1);
            }
        }
        return null;
    }

    public Iterator<UpdaterPath> ancestors() {
        return new Iterator() {
            int idx = 1;
            public boolean hasNext() {
                return idx < names.length-1;
            }
            public UpdaterPath next() {
                if (hasNext()) {
                    return new UpdaterPath(names, indices, idx++);
                } else {
                    throw new NoSuchElementException();
                }
            }
            public void remove() {
                throw new UnsupportedOperationException();
            }
        };
    }

    public boolean isDescendentOrSelf(UpdaterPath ancestor) {
        if(ancestor.names.length > names.length)
            return false;
        for(int i=0; i<ancestor.names.length; i++) {
            if(!ancestor.names[i].equals(names[i]) || ancestor.indices[i] != indices[i]) {
                return false;
            }
        }
        return true;
    }

    public int compareTo(UpdaterPath that) {
        UpdaterPath one = this;
        UpdaterPath two = that;
        boolean reverse = false;
        if (that.names.length < names.length) {
            one = that;
            two = this;
            reverse = true;
        }
        
        int result = 0;
        for (int i = 0; i < one.names.length; i++) {
            result = one.names[i].compareTo(two.names[i]);
            if (result != 0) {
                break;
            }
            result = one.indices[i].compareTo(two.indices[i]);
            if (result != 0) {
                break;
            }
        }
        if (result == 0) {
            if (two.names.length > one.names.length) {
                result = -1;
            }
        }
        if (reverse) {
            return -result;
        }
        return result;
    }

    // warning: part of interface
    @Override
    public String toString() {
        return string;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (obj instanceof UpdaterPath) {
            UpdaterPath that = (UpdaterPath) obj;
            return that.string.equals(this.string);
        }
        return false;
    }

    @Override
    public int hashCode() {
        return string.hashCode() ^ 137;
    }
}
