/*
 *  Copyright 2008-2013 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.repository.dataprovider;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.Map;

import javax.jcr.RepositoryException;

import org.apache.commons.lang.StringUtils;
import org.apache.jackrabbit.core.id.NodeId;
import org.apache.jackrabbit.spi.Name;

public final class ViewNodeId extends MirrorNodeId implements IFilterNodeId {

    private static final long serialVersionUID = 1L;

    /* The following fields MUST be immutable
     */
    public final boolean singledView;
    public final LinkedHashMap<Name, String> view;
    public final LinkedHashMap<Name, String> order;
    public final Name parentName;

    public ViewNodeId(HippoVirtualProvider provider, NodeId parent, Name parentName, NodeId upstream, StateProviderContext context,
            Name name, LinkedHashMap<Name, String> view, LinkedHashMap<Name, String> order, boolean singledView) {
        super(provider, parent, context, name, upstream);
        this.view = view;
        this.order = order;
        this.singledView = singledView;
        this.parentName = parentName;
    }

    public class Child {
        Name name;

        public Child(Name name) {
            this.name = name;
        }

        public Name getKey() {
            return name;
        }

        public ViewNodeId getValue() {
            return ViewNodeId.this;
        }

    }

    public LinkedHashMap<Name, String> getOrder() {
        if (this.order != null) {
            return new LinkedHashMap<Name, String>(this.order);
        }
        return null;
    }

    public LinkedHashMap<Name, String> getView() {
        if (this.view != null) {
            return new LinkedHashMap<Name, String>(this.view);
        }
        return null;
    }

    public boolean isSingledView() {
        return this.singledView;
    }

    public class ChildComparator implements Comparator<Child> {
        @Override
        public int compare(final Child o1, final Child o2) {
            if (o1 == null || o2 == null) {
                throw new NullPointerException();
            }
            if (o2.equals(o1)) {
                return 0;
            }
            if (order != null) {
                HippoVirtualProvider provider = getProvider();
                try {
                    for (Map.Entry<Name, String> entry : order.entrySet()) {
                        Name facet = entry.getKey();
                        String value = entry.getValue();

                        int thisFacetValueIndex = -1;
                        String[] thisFacetValues = provider.getProperty(o1.getValue().getCanonicalId(), facet, null);
                        if (thisFacetValues != null) {
                            for (int i = 0; i < thisFacetValues.length; i++) {
                                if (thisFacetValues[i].equals(value)) {
                                    thisFacetValueIndex = i;
                                    break;
                                }
                            }
                        }

                        int otherFacetValueIndex = -1;
                        String[] otherFacetValues = provider.getProperty(o2.getValue().getCanonicalId(), facet, null);
                        if (otherFacetValues != null) {
                            for (int i = 0; i < otherFacetValues.length; i++) {
                                if (otherFacetValues[i].equals(value)) {
                                    otherFacetValueIndex = i;
                                    break;
                                }
                            }
                        }

                        if (thisFacetValueIndex == -1 && otherFacetValueIndex == -1) {
                            break;
                        }
                        if (thisFacetValueIndex != -1 && otherFacetValueIndex == -1) {
                            return -1;
                        } else if (thisFacetValueIndex == -1 && otherFacetValueIndex != -1) {
                            return 1;
                        } else if (StringUtils.isEmpty(value) || value.equals("*")) {
                            if (thisFacetValues[thisFacetValueIndex].compareTo(otherFacetValues[otherFacetValueIndex]) != 0) {
                                return thisFacetValues[thisFacetValueIndex]
                                        .compareTo(otherFacetValues[otherFacetValueIndex]);
                            }
                        }
                    }
                } catch (RepositoryException ignored) {
                }

            }

            final Name name1 = o1.name;
            final Name name2 = o2.name;
            if (name1.equals(name2)) {
                return 0;
            }

            final Name parentName1 = o1.getValue().parentName;
            final Name parentName2 = o2.getValue().parentName;
            if (parentName1 == null && parentName2 == null) {
                return 0;
            }

            // document nodes are always ordered before anything else
            // (we make use of the fact that document nodes always have the same name as their handles)
            if ((parentName1 != null && parentName1.equals(name1)) && !(parentName2 != null && parentName2.equals(name2))) {
                return -1;
            }
            if ((parentName2 != null && parentName2.equals(name2)) && !(parentName1 != null && parentName1.equals(name1))) {
                return 1;
            }

            return 0;
        }
    }
}
