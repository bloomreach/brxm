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

import java.io.Serializable;
import java.util.Collections;
import java.util.List;

import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.hippoecm.frontend.types.IFieldDescriptor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @version "$Id$"
 */
public class SortHelper implements Serializable {

    public final static String DEFAULT_COMPARATOR_CLASS = DefaultAlphabeticalComparator.class.getName();

    private final static Logger log = LoggerFactory.getLogger(SortHelper.class);
    private IContentBlockComparator contentBlockComparator;

    public void sort(List<IFieldDescriptor> values, IPluginConfig config) {
        if (values == null) {
            return;
        }

        final String comparatorClass = config.getString("sortComparator");
        if (comparatorClass != null) {

            // instantiate only once
            if (contentBlockComparator == null) {

                if (comparatorClass.equals(DEFAULT_COMPARATOR_CLASS)) {
                    contentBlockComparator = new DefaultAlphabeticalComparator();
                } else {
                    // load class dynamically and instantiate
                    try {
                        Class clazz = Class.forName(comparatorClass);

                        Object instance = clazz.newInstance();
                        if (instance instanceof IContentBlockComparator) {
                            contentBlockComparator = (IContentBlockComparator) instance;
                        } else {
                            log.error("Configured class " + comparatorClass + " does not implement IContentBlockComparator, using NO comparator");
                        }
                    } catch (ClassNotFoundException e) {
                        log.error("ClassNotFoundException for configured class " + comparatorClass + ", using NO comparator");
                    } catch (InstantiationException e) {
                        log.error("InstantiationException for configured class " + comparatorClass + ", using NO comparator");
                    } catch (IllegalAccessException e) {
                        log.error("IllegalAccessException for configured class " + comparatorClass + ", using NO comparator");
                    }
                }

                if (contentBlockComparator != null) {
                    contentBlockComparator.setConfig(config);
                    Collections.sort(values, contentBlockComparator);
                }
            }
        }
    }

}
