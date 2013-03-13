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
package org.hippoecm.hst.core.container;

import org.hippoecm.hst.container.valves.AbstractOrderableValve;

/**
 * DelegatingOrderableValve
 * <P>
 * An {@link OrderableValve} implementation which delegates to the internal valve.
 * This implementation is useful if you want to wrap a non-orderable valve component
 * with specifying the name, before and after properties.
 * </P>
 */
public class DelegatingOrderableValve extends AbstractOrderableValve {

    private Valve delegatee;

    public DelegatingOrderableValve(Valve delegatee) {
        this.delegatee = delegatee;
    }

    @Override
    public void invoke(ValveContext context) throws ContainerException {
        delegatee.invoke(context);
    }

    @Override
    public void initialize() throws ContainerException {
        delegatee.initialize();
    }

    @Override
    public void destroy() {
        delegatee.destroy();
    }

}
