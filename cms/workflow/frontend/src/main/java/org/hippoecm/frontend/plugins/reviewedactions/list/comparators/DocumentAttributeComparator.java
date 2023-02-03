/*
 *  Copyright 2010-2023 Bloomreach
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
package org.hippoecm.frontend.plugins.reviewedactions.list.comparators;

import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.frontend.plugins.reviewedactions.list.resolvers.StateIconAttributes;
import org.hippoecm.frontend.plugins.standards.list.comparators.NodeComparator;

public abstract class DocumentAttributeComparator extends NodeComparator {
    private static final long serialVersionUID = 1L;

    @Override
    public int compare(JcrNodeModel o1, JcrNodeModel o2) {
        return compare(new StateIconAttributes(o1), new StateIconAttributes(o2));
    }

    protected abstract int compare(StateIconAttributes stateIconAttributes, StateIconAttributes stateIconAttributes1);

}
