/*
 *  Copyright 2008-2011 Hippo.
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

import java.util.LinkedHashMap;
import java.util.Map;

import javax.jcr.RepositoryException;

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

    public class Child implements Comparable<Child> {
        Name name;
        ViewNodeId nodeId;

        public Child(Name name, ViewNodeId viewNodeId) {
            this.name = name;
            this.nodeId = viewNodeId;
        }

        public Name getKey() {
            return name;
        }

        public ViewNodeId getValue() {
            return ViewNodeId.this;
        }

        public int compareTo(Child o) {
            if (o == null) {
                throw new NullPointerException();
            }
            if (o.equals(this)) {
                return 0;
            }
            if (order != null) {
                HippoVirtualProvider provider = getProvider();
                try {
                    for (Map.Entry<Name, String> entry : order.entrySet()) {
                        Name facet = entry.getKey();
                        String value = entry.getValue();

                        int thisFacetValueIndex = -1;
                        String[] thisFacetValues = provider.getProperty(getCanonicalId(), facet, null);
                        if (thisFacetValues != null) {
                            for (int i = 0; i < thisFacetValues.length; i++) {
                                if (thisFacetValues[i].equals(value)) {
                                    thisFacetValueIndex = i;
                                    break;
                                }
                            }
                        }

                        int otherFacetValueIndex = -1;
                        String[] otherFacetValues = provider.getProperty(o.getValue().getCanonicalId(), facet, null);
                        if (otherFacetValues != null) {
                            for (int i = 0; i < otherFacetValues.length; i++) {
                                if (otherFacetValues[i].equals(value)) {
                                    otherFacetValueIndex = i;
                                    break;
                                }
                            }
                        }

                        if (thisFacetValueIndex != -1 && otherFacetValueIndex == -1) {
                            return -1;
                        } else if (thisFacetValueIndex == -1 && otherFacetValueIndex != -1) {
                            return 1;
                        } else if (value == null || value.equals("") || value.equals("*")) {
                            if (thisFacetValues[thisFacetValueIndex].compareTo(otherFacetValues[otherFacetValueIndex]) != 0) {
                                return thisFacetValues[thisFacetValueIndex]
                                        .compareTo(otherFacetValues[otherFacetValueIndex]);
                            }
                        }
                    }
                } catch (RepositoryException ex) {
                }

            }
            // document nodes are always ordered before anything else
            // (we make use of the fact that document nodes always have the same name as their handles)
            if (nodeId.parentName != null && nodeId.parentName.equals(name)) {
                return -1;
            }
            if (o.nodeId.parentName != null && o.nodeId.parentName.equals(o.name)) {
                return 1;
            }

            // never return 0 (See Comparable api)
            return -1;
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
}
