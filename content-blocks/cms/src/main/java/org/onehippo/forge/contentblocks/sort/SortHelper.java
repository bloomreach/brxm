/*
 * Copyright 2010-2020 Hippo B.V. (http://www.onehippo.com)
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

import java.io.Serializable;
import java.util.List;

import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.hippoecm.frontend.types.IFieldDescriptor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SortHelper implements Serializable {

    private static final Logger log = LoggerFactory.getLogger(SortHelper.class);

    public static final String DEFAULT_COMPARATOR_CLASS = DefaultAlphabeticalComparator.class.getName();

    private IContentBlockComparator contentBlockComparator;

    public void sort(final List<IFieldDescriptor> values, final IPluginConfig config) {
        if (values == null) {
            return;
        }

        final String comparatorClass = config.getString("sortComparator");
        if (comparatorClass == null) {
            return;
        }

        // instantiate only once
        if (contentBlockComparator != null) {
            return;
        }

        if (comparatorClass.equals(DEFAULT_COMPARATOR_CLASS)) {
            contentBlockComparator = new DefaultAlphabeticalComparator();
        } else {
            // load class dynamically and instantiate
            try {
                final Class<?> clazz = Class.forName(comparatorClass);
                final Object instance = clazz.newInstance();
                if (instance instanceof IContentBlockComparator) {
                    contentBlockComparator = (IContentBlockComparator) instance;
                } else {
                    log.error("Configured class {} does not implement IContentBlockComparator, using NO comparator",
                            comparatorClass);
                }
            } catch (final ClassNotFoundException e) {
                log.error("ClassNotFoundException for configured class {}, using NO comparator", comparatorClass);
            } catch (final InstantiationException e) {
                log.error("InstantiationException for configured class {}, using NO comparator", comparatorClass);
            } catch (final IllegalAccessException e) {
                log.error("IllegalAccessException for configured class {}, using NO comparator", comparatorClass);
            }
        }

        if (contentBlockComparator != null) {
            contentBlockComparator.setConfig(config);
            values.sort(contentBlockComparator);
        }
    }
}
