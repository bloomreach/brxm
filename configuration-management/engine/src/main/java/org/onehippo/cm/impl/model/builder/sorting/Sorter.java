/*
 *  Copyright 2016-2017 Hippo B.V. (http://www.onehippo.com)
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
package org.onehippo.cm.impl.model.builder.sorting;

import java.util.Collection;
import java.util.Comparator;
import java.util.Map;
import java.util.SortedSet;
import java.util.TreeSet;

import org.onehippo.cm.api.model.Orderable;

public class Sorter<T extends Orderable> {

    public SortedSet<T> sort(final Map<String, T> map) {
        return sort(map.values());
    }

    public SortedSet<T> sort(final Collection<T> list) {
        SortedSet<T> sortedOrderables = new TreeSet<>(new Comparator<T>() {
            @Override
            public int compare(final T orderable1, final T orderable2) {
                if (orderable1.equals(orderable2)) {
                    return 0;
                }
                if (orderable1.getAfter().isEmpty()) {
                    if (orderable2.getAfter().isEmpty()) {
                        return orderable1.getName().compareTo(orderable2.getName());
                    } else {
                        return -1;
                    }
                }
                if (orderable2.getAfter().isEmpty()) {
                    return 1;
                }
                boolean orderable1DependsOnConfig2 = false;
                boolean orderable2DependsOnConfig1 = false;
                for (String dependsOn : orderable1.getAfter()) {
                    if (orderable2.getName().equals(dependsOn)) {
                        // orderable1 depends on orderable2 : Now exclude circular dependency
                        orderable1DependsOnConfig2 = true;
                    }
                }
                for (String dependsOn : orderable2.getAfter()) {
                    if (orderable1.getName().equals(dependsOn)) {
                        // orderable2 depends on orderable1
                        orderable2DependsOnConfig1 = true;
                    }
                }
                if (orderable2DependsOnConfig1) {
                    return -1;
                }
                if (orderable1DependsOnConfig2) {
                    return 1;
                }
                return orderable1.getName().compareTo(orderable2.getName());
            }


        });
        sortedOrderables.addAll(list);
        return sortedOrderables;
    }

}
