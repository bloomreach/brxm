/*
 *  Copyright 2010-2016 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.repository;

public class OrderBy {

    private String name;
    private boolean descending;
    private String orderFunction;

    public OrderBy(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public boolean isDescending() {
        return descending;
    }

    public void setDescending(final boolean descending) {
        this.descending = descending;
    }

    public void setOrderFunction(final String orderFunction) {
        this.orderFunction = orderFunction;
    }

    public String getOrderFunction() {
        return orderFunction;
    }
}
