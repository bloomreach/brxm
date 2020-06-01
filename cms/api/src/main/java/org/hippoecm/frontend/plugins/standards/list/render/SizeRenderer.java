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
package org.hippoecm.frontend.plugins.standards.list.render;

import javax.jcr.Property;
import javax.jcr.RepositoryException;

import org.hippoecm.frontend.plugins.standards.util.ByteSizeFormatter;

public class SizeRenderer extends PropertyRenderer<String> {

    private final ByteSizeFormatter formatter = new ByteSizeFormatter(1);

    public SizeRenderer(final String prop) {
        super(prop);
    }

    public SizeRenderer(final String prop, final String relPath) {
        super(prop, relPath);
    }

    @Override
    protected String getValue(final Property p) throws RepositoryException {
        return formatter.format(p.getLength());
    }

}
