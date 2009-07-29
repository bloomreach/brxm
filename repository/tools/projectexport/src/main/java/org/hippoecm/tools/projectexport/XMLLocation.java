/*
 *  Copyright 2009 Hippo.
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
package org.hippoecm.tools.projectexport;

class XMLLocation implements Comparable<XMLLocation> {
    @SuppressWarnings("unused")
    private final static String SVN_ID = "$Id$";

    private XMLItemType type;
    private String path;
    private boolean afterElement;

    public XMLLocation(XMLItemType type, String path, boolean afterElement) {
        this.type = type;
        this.path = path;
        this.afterElement = afterElement;
    }

    public int compareTo(XMLLocation other) {
        int rtvalue = type.compareTo(other.type);
        if (rtvalue == 0) {
            if (path == null || other.path == null) {
                if (path == null && other.path == null)
                    return 0;
                else if (path == null)
                    return -1;
                else
                    return 1;
            }
            rtvalue = path.compareTo(other.path);
            if (rtvalue == 0) {
                if (afterElement == false) {
                    rtvalue = other.afterElement == false ? 0 : 1;
                } else {
                    rtvalue = other.afterElement == true ? 0 : -1;
                }
            }
        }
        return rtvalue;
    }

    @Override
    public boolean equals(Object o) {
        if (o instanceof XMLLocation) {
            XMLLocation other = (XMLLocation)o;
            return type == other.type && (path == null ? other.path == null : path.equals(other.path)) && afterElement == other.afterElement;
        } else {
            return false;
        }
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 29 * hash + (this.type != null ? this.type.hashCode() : 0);
        hash = 29 * hash + (this.path != null ? this.path.hashCode() : 0);
        hash = 29 * hash + (this.afterElement ? 1 : 0);
        return hash;
    }
}
