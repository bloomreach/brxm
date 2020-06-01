/*
 * Copyright 2016 Hippo B.V. (http://www.onehippo.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.onehippo.repository.bootstrap;

import java.util.Comparator;

import javax.jcr.RepositoryException;

import static org.apache.commons.lang.StringUtils.isEmpty;
import static org.onehippo.repository.bootstrap.util.BootstrapConstants.log;

class InitializeItemComparator implements Comparator<InitializeItem> {
    
    @Override
    public int compare(final InitializeItem i1, final InitializeItem i2) {
        try {
            int result = compareNamespace(i1, i2);
            if (result != 0) {
                return result;
            }
            result = compareNodeTypes(i1, i2);
            if (result != 0) {
                return result;
            }
            return compareItem(i1, i2);
        } catch (RepositoryException e) {
            log.error("Error comparing initialize items", e);
        }
        return 0;
    }
    
    private int compareNodeTypes(final InitializeItem i1, final InitializeItem i2) throws RepositoryException {
        final String cnds1 = i1.getNodetypesResource();
        final String cnds2 = i2.getNodetypesResource();
        if (isEmpty(cnds1) && !isEmpty(cnds2)) {
            return 1;
        }
        if (!isEmpty(cnds1) && isEmpty(cnds2)) {
            return -1;
        }
        if (!isEmpty(cnds1) && !isEmpty(cnds2)) {
            return compareItem(i1, i2);
        }
        return 0;
    }

    private int compareNamespace(final InitializeItem i1, final InitializeItem i2) throws RepositoryException {
        final String n1 = i1.getNamespace();
        final String n2 = i2.getNamespace();
        if (isEmpty(n1) && !isEmpty(n2)) {
            return 1;
        }
        if (isEmpty(n2) && !isEmpty(n1)) {
            return -1;
        }
        if (!isEmpty(n1) && !isEmpty(n2)) {
            return compareItem(i1, i2);
        }
        return 0;
    }

    private int compareItem(final InitializeItem i1, final InitializeItem i2) throws RepositoryException {
        final int result = compareSequence(i1, i2);
        if (result != 0) {
            return result;
        }
        if (i1.isDeltaMerge()) {
            if (!i2.isDeltaMerge()) {
                // not delta-merge > delta-merge
                return 1;
            }
        }
        else {
            if (i2.isDeltaMerge()) {
                // delta-merge < not delta-merge
                return -1;
            }
            // i1 and i2 both not a delta-merge
            if (i2.isDownstreamItem(i1)) {
                return -1;
            }
            if ( i1.isDownstreamItem(i2)) {
                return 1;
            }
        }
        return i1.getName().compareTo(i2.getName());
    }

    private int compareSequence(final InitializeItem i1, final InitializeItem i2) throws RepositoryException {
        final Double s1 = i1.getSequence();
        final Double s2 = i2.getSequence();
        return s1.compareTo(s2);
    }
    
}
