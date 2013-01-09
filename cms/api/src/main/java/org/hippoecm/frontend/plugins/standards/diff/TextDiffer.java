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
package org.hippoecm.frontend.plugins.standards.diff;

import java.io.Serializable;
import java.util.Iterator;
import java.util.List;
import java.util.regex.Pattern;

import org.apache.wicket.util.string.Strings;

public class TextDiffer implements Serializable {

    private static final long serialVersionUID = -112736024904848783L;

    private final static Pattern WHITESPACE = Pattern.compile("[ \t\r\n]");

    private String addedClass = "hippo-diff-added";
    private String removedClass = "hippo-diff-removed";

    public void setAddedClass(String addedClass) {
        this.addedClass = addedClass;
    }

    public String getAddedClass() {
        return addedClass;
    }

    public void setRemovedClass(String removedClass) {
        this.removedClass = removedClass;
    }

    public String getRemovedClass() {
        return removedClass;
    }

    class DiffBuilder {
        StringBuilder result = new StringBuilder();
        final String[] aParts;
        final String[] bParts;
        int aPos = 0;
        int bPos = 0;

        DiffBuilder(String[] aParts, String[] bParts) {
            this.aParts = aParts;
            this.bParts = bParts;
        }

        void addLCSPart(String value) {
            if (aPos != 0 || bPos != 0) {
                result.append(' ');
            }

            boolean removed = addRemoved(value);
            boolean added = addAdded(value, removed);

            aPos++;
            bPos++;
            
            if (removed || added) {
                result.append(' ');
            }
            result.append(Strings.escapeMarkup(value));
        }

        void finish() {
            if (aPos < aParts.length || bPos < bParts.length) {
                if (aPos != 0 || bPos != 0) {
                    result.append(' ');
                }
            }
            boolean removed = false;
            if (aPos < aParts.length) {
                removed = addRemoved(null);
            }
            if (bPos < bParts.length) {
                addAdded(null, removed);
            }
        }

        private boolean addAdded(String value, boolean removed) {
            boolean added = false;
            while (bPos < bParts.length && !bParts[bPos].equals(value)) {
                if (!added) {
                    if (removed) {
                        result.append(' ');
                    }
                    result.append("<span class=\"");
                    result.append(addedClass);
                    result.append("\">");
                    added = true;
                } else {
                    result.append(' ');
                }
                result.append(Strings.escapeMarkup(bParts[bPos]));
                bPos++;
            }
            if (added) {
                result.append("</span>");
            }
            return added;
        }

        private boolean addRemoved(String value) {
            boolean removed = false;
            while (aPos < aParts.length && !aParts[aPos].equals(value)) {
                if (!removed) {
                    result.append("<span class=\"");
                    result.append(removedClass);
                    result.append("\">");
                    removed = true;
                } else {
                    result.append(' ');
                }
                result.append(Strings.escapeMarkup(aParts[aPos]));
                aPos++;
            }
            if (removed) {
                result.append("</span>");
            }
            return removed;
        }

        @Override
        public String toString() {
            finish();
            return result.toString();
        }
    }

    public String diffText(String a, String b) {
        String[] aParts;
        if (a != null) {
            aParts = WHITESPACE.split(a);
        } else {
            aParts = new String[0];
        }

        String[] bParts;
        if (b != null) {
            bParts = WHITESPACE.split(b);
        } else {
            bParts = new String[0];
        }

        List<String> lcs = LCS.getLongestCommonSubsequence(aParts, bParts);
        Iterator<String> iter = lcs.iterator();
        DiffBuilder db = new DiffBuilder(aParts, bParts);
        while (iter.hasNext()) {
            String value = iter.next();
            db.addLCSPart(value);
        }
        return db.toString();
    }

}
