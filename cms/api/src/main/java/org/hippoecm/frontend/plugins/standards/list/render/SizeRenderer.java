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

import org.hippoecm.frontend.plugins.standards.util.ByteSizeFormatter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.Property;
import javax.jcr.RepositoryException;

public class SizeRenderer extends PropertyRenderer<String> {
    private static final long serialVersionUID = 1L;


    static final Logger log = LoggerFactory.getLogger(SizeRenderer.class);

    ByteSizeFormatter formatter = new ByteSizeFormatter(1);

    public SizeRenderer(String prop) {
        super(prop);
    }

    public SizeRenderer(String prop, String relPath) {
        super(prop, relPath);
    }

    @Override
    protected String getValue(Property p) throws RepositoryException {
        return formatter.format(p.getLength());
    }

}
