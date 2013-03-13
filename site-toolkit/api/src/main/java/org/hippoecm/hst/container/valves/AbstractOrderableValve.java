/**
 * Copyright 2013-2013 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.hst.container.valves;

import org.hippoecm.hst.core.container.OrderableValve;

/**
 * AbstractOrderableValve
 * <P>
 * This abstract class determines the valve name and other attributes by reading the properties of {@link OrderableValve}.
 * e.g., {@link OrderableValve#getName()}, {@link OrderableValve#getAfter()}, {@link OrderableValve#getBefore()}, etc.
 * </P>
 */
public abstract class AbstractOrderableValve extends AbstractValve implements OrderableValve {

    private String name;
    private String before;
    private String after;

    @Override
    public String getName() {
        return name;
    }

    /**
     * Sets the valve name
     * @param name
     */
    public void setName(String name) {
        this.name = name;
    }

    @Override
    public String getBefore() {
        return before;
    }

    /**
     * Sets postrequisite valve names that should follow this valve.
     * The <code>before</code> can have multiple valve names, separated by ' ', ',', '\t', '\r' or '\n'
     * @param before
     */
    public void setBefore(String before) {
        this.before = before;
    }

    @Override
    public String getAfter() {
        return after;
    }

    /**
     * Sets prerequisite valve names that should follow this valve.
     * The <code>after</code> can have multiple valve names, separated by ' ', ',', '\t', '\r' or '\n'
     * @param after
     */
    public void setAfter(String after) {
        this.after = after;
    }
}
