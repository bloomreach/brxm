/*
 * Copyright 2017 Hippo B.V. (http://www.onehippo.com)
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

package org.onehippo.cm.impl.model.builder.sorting;

import java.util.Comparator;

import org.onehippo.cm.api.model.Orderable;

public class OrderableComparator<T extends Orderable> implements Comparator<T> {
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
        if (dependsOn(orderable1, orderable2)) {
            // if orderable1 depends on orderable2, orderable1 should go past orderable2 and be considered "greater"
            return 1;
        }
        if (dependsOn(orderable2, orderable1)) {
            // if orderable2 depends on orderable1, orderable1 should go before orderable2 and be considered "smaller"
            return -1;
        }
        return orderable1.getName().compareTo(orderable2.getName());
    }

    private boolean dependsOn(Orderable orderable1, Orderable orderable2) {
        for (String dependsOn : orderable1.getAfter()) {
            if (orderable2.getName().equals(dependsOn)) {
                return true;
            }
        }
        return false;
    }
}
