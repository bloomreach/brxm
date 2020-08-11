/*
 * Copyright 2011-2019 Hippo B.V. (http://www.onehippo.com)
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *  http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.onehippo.forge.selection.frontend.plugin.sorting;

import java.io.Serializable;

import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.onehippo.forge.selection.frontend.model.ValueList;
import org.onehippo.forge.selection.frontend.plugin.Config;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Helper for sorting a ValueList based on config settings.
 */
public class SortHelper implements Serializable {

    public final static String DEFAULT_COMPARATOR_CLASS = DefaultListItemComparator.class.getName();

    private final static Logger log = LoggerFactory.getLogger(SortHelper.class);
    private IListItemComparator listItemComparator;

    public void sort(ValueList valueList, IPluginConfig config) {

        if (valueList == null) {
            return;
        }

        final String comparatorClass = config.getString(Config.SORT_COMPARATOR);
        if (comparatorClass != null) {

            // instantiate only once
            if (listItemComparator == null) {

                if (comparatorClass.equals(DEFAULT_COMPARATOR_CLASS)) {
                    listItemComparator = new DefaultListItemComparator();
                }
                else {
                    // load class dynamically and instantiate
                    try {
                        Class clazz = Class.forName(comparatorClass);

                        Object instance = clazz.newInstance();
                        if (instance instanceof IListItemComparator) {
                            listItemComparator = (IListItemComparator) instance;
                        }
                        else {
                            log.error("Configured class " + comparatorClass + " does not implement IListItemComparator, using NO comparator");
                        }
                    } catch (ClassNotFoundException e) {
                        log.error("ClassNotFoundException for configured class " + comparatorClass + ", using NO comparator");
                    } catch (InstantiationException e) {
                        log.error("InstantiationException for configured class " + comparatorClass + ", using NO comparator");
                    } catch (IllegalAccessException e) {
                        log.error("IllegalAccessException for configured class " + comparatorClass + ", using NO comparator");
                    }
                }

            }

            if (listItemComparator != null) {
                listItemComparator.setConfig(config);
                valueList.sort(listItemComparator);
            }
        }
    }
}
