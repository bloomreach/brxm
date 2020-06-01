/*
 *  Copyright 2008-2015 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.frontend.plugins.standards.list.comparators;

import java.util.Comparator;

import javax.jcr.Node;
import javax.jcr.RepositoryException;

import org.apache.wicket.util.io.IClusterable;
import org.hippoecm.repository.HippoStdNodeType;

public class StateComparator implements Comparator<Node>, IClusterable {

    private static final StateComparator INSTANCE = new StateComparator();

    private StateComparator() {
    }

    public static StateComparator getInstance() {
        return INSTANCE;
    }

    public int compare(final Node n1, final Node n2) {
        try {
            final Node variant1 = n1.getNode(n1.getName());
            final String state1 = getStateSummary(variant1);

            final Node variant2 = n2.getNode(n2.getName());
            final String state2 = getStateSummary(variant2);

            return String.CASE_INSENSITIVE_ORDER.compare(state1, state2);
        } catch (RepositoryException e) {
            return 0;
        }
    }

    private String getStateSummary(final Node variant) throws RepositoryException {
        if (variant.hasProperty(HippoStdNodeType.HIPPOSTD_STATESUMMARY)) {
            return variant.getProperty(HippoStdNodeType.HIPPOSTD_STATESUMMARY).getString();
        }
        return "unknown";
    }

}
