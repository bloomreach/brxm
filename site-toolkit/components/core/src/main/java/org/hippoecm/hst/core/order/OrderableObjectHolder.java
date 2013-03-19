/**
 * Copyright 2013 Hippo B.V. (http://www.onehippo.com)
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *         http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.hippoecm.hst.core.order;

/**
 * Used by an {@link org.hippoecm.hst.core.order.ObjectOrderer} to organize
 * a single object and its pre- and post-requisites.
 */
class OrderableObjectHolder<T> {

    private String name;
    private T object;
    private String prerequisites;
    private String postrequisites;

    OrderableObjectHolder(T object, String name, String prerequisites, String postrequisites) {
        this.object = object;
        this.name = name;
        this.prerequisites = prerequisites;
        this.postrequisites = postrequisites;
    }

    public String getName() {
        return name;
    }

    public T getObject() {
        return object;
    }

    public String getPrerequisites() {
        return prerequisites;
    }

    public String getPostrequisites() {
        return postrequisites;
    }

    @Override
    public String toString() {
        return new StringBuilder(80)
        .append(super.toString())
        .append(" { name:'" + name + "',")
        .append("prerequisites: '" + prerequisites + "',")
        .append("postrequisites: '" + postrequisites + "',")
        .append("object: " + object + " }")
        .toString();
    }
}
