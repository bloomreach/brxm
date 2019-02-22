/*
 * Copyright 2010-2014 Hippo B.V. (http://www.onehippo.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.onehippo.forge.contentblocks.sort;

import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.hippoecm.frontend.types.IFieldDescriptor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @version "$Id$"
 */
public class DefaultAlphabeticalComparator implements IContentBlockComparator {

    private static Logger log = LoggerFactory.getLogger(DefaultAlphabeticalComparator.class);

    private IPluginConfig config;

    @Override
    public int compare(final IFieldDescriptor o1, final IFieldDescriptor o2) {
        final String type1 = o1.getTypeDescriptor().getType();
        final String type2 = o2.getTypeDescriptor().getType();
        if (config != null && config.containsKey("order") && config.getString("order").startsWith("desc")) {
            return type2.compareTo(type1);
        } else {
            return type1.compareTo(type2);
        }

    }

    @Override
    public void setConfig(final IPluginConfig config) {
        this.config = config;
    }
}
