/*
 *  Copyright 2020 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.frontend.editor.plugins.field;

import java.util.LinkedHashSet;
import java.util.Set;

import org.apache.wicket.util.io.IClusterable;

public class CollapsedItems implements IClusterable {

    private final Set<Integer> collapsedItems = new LinkedHashSet<>();

    public boolean contains(final int index) {
        return collapsedItems.contains(index);
    }

    public void set(final int index, final boolean collapsed) {
        if (collapsed) {
            collapsedItems.add(index);
        } else {
            collapsedItems.remove(index);
        }
    }

    public void clear(final int index, final int total) {
        process(index, -1, total, true);
    }

    public void update(final int from, final int to, final int total) {
        process(from, to, total, false);
    }

    private void process(final int from, int to, final int total, final boolean remove) {
        if (to == -1) { // to the bottom
            to = total - 1;
        }

        final boolean isCollapsed = collapsedItems.remove(from);
        int current = from;
        while (current != to) {
            final int previous = current;
            current = from > to ? current - 1 : current + 1;
            if (collapsedItems.contains(current)) {
                collapsedItems.add(previous);
                collapsedItems.remove(current);
            }
        }

        if (isCollapsed && !remove) {
            collapsedItems.add(to);
        }
    }
}
