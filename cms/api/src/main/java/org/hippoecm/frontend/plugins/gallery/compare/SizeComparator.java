/*
 *  Copyright 2008-2017 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.frontend.plugins.gallery.compare;

import javax.jcr.Property;
import javax.jcr.RepositoryException;

public class SizeComparator extends PropertyComparator {

    public SizeComparator(final String prop) {
        super(prop);
    }

    public SizeComparator(final String prop, final String relPath) {
        super(prop, relPath);
    }

    @Override
    protected int compare(final Property p1, final Property p2) {
        long size1 = 0;
        long size2 = 0;
        try {
            size1 = p1 == null ? 0 : p1.getLength();
            size2 = p2 == null ? 0 : p2.getLength();
        } catch (final RepositoryException ignored) {
        }

        final long diff = size1 - size2;
        if (diff < 0) {
            return -1;
        } else if (diff > 0) {
            return 1;
        }
        return 0;
    }
}
