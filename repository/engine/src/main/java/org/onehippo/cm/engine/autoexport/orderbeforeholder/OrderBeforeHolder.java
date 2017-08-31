/*
 *  Copyright 2017 Hippo B.V. (http://www.onehippo.com)
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

package org.onehippo.cm.engine.autoexport.orderbeforeholder;

import java.util.List;

import com.google.common.collect.ImmutableList;

import org.onehippo.cm.model.impl.path.JcrPathSegment;
import org.onehippo.cm.model.impl.tree.DefinitionNodeImpl;

public abstract class OrderBeforeHolder implements Comparable {
    public void apply(final ImmutableList<JcrPathSegment> expected, final List<JcrPathSegment> intermediate) {
        final JcrPathSegment myName = getDefinitionNode().getJcrName();

        if (intermediate.size() == 0) {
            intermediate.add(myName);
            return;
        }

        // start by assuming myName must be added at the end of the list
        int position = intermediate.size();
        String orderBefore = null;

        // iterate through the elements in expected from the last to the first
        while (position > 0) {
            final JcrPathSegment candidate = intermediate.get(position - 1);

            // if myName needs to be sorted after candidate, we've found the right place for myName
            if (expected.indexOf(candidate) < expected.indexOf(myName)) {
                break;
            }

            // myName needs to be added at least before candidate
            position--;
            orderBefore = candidate.toString();
        }

        intermediate.add(position, myName);
        setOrderBefore(orderBefore);
    }
    abstract DefinitionNodeImpl getDefinitionNode();
    abstract void setOrderBefore(final String orderBefore);
    public abstract void finish();
}
