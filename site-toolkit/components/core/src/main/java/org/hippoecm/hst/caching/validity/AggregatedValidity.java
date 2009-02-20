/*
 *  Copyright 2008 Hippo.
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
package org.hippoecm.hst.caching.validity;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

public final class AggregatedValidity implements SourceValidity {
    private static final long serialVersionUID = 1L;

    final ArrayList<SourceValidity> m_list = new ArrayList<SourceValidity>();

    public void add(final SourceValidity validity) {
        m_list.add(validity);
    }

    public String toString() {
        final StringBuffer sb = new StringBuffer("SourceValidity ");
        for (Iterator<SourceValidity> i = m_list.iterator(); i.hasNext();) {
            sb.append(i.next());
            if (i.hasNext())
                sb.append(':');
        }
        return sb.toString();
    }

    public List<SourceValidity> getValidities() {
        return Collections.unmodifiableList(m_list);
    }

    SourceValidity getValidity(final int index) {
        return (SourceValidity) m_list.get(index);
    }

    public int isValid() {
        for (Iterator<SourceValidity> i = m_list.iterator(); i.hasNext();) {
            final int v = ((SourceValidity) i.next()).isValid();
            if (v < 1) {
                return v;
            }
        }
        return 1;
    }

    public int isValid(final SourceValidity validity) {
        if (validity instanceof AggregatedValidity) {
            final AggregatedValidity other = (AggregatedValidity) validity;
            final List<SourceValidity> otherList = other.m_list;
            if (m_list.size() != otherList.size()) {
                return -1;
            }

            for (Iterator<SourceValidity> i = m_list.iterator(), j = otherList.iterator(); i.hasNext();) {
                final SourceValidity srcA = (SourceValidity) i.next();
                final SourceValidity srcB = (SourceValidity) j.next();
                int result = srcA.isValid();
                if (result == -1) {
                    return -1;
                }
                if (result == 0) {
                    result = srcA.isValid(srcB);
                    if (result < 1) {
                        return result;
                    }
                }
            }
            return 1;
        }
        return -1;
    }

}
