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
package org.hippoecm.frontend.plugins.gallery.columns.compare;

import javax.jcr.Property;
import javax.jcr.RepositoryException;

public class MimeTypeComparator extends PropertyComparator {
    private static final long serialVersionUID = 1L;

    public MimeTypeComparator(String prop) {
        super(prop);
    }

    public MimeTypeComparator(String prop, String relPath) {
        super(prop, relPath);
    }

    @Override
    protected int compare(Property p1, Property p2) {
        try {
            String mime1 = p1.getString();
            String mime2 = p2.getString();
            return String.CASE_INSENSITIVE_ORDER.compare(mime1, mime2);
        } catch (RepositoryException e) {
        }
        return 0;
    }
}
