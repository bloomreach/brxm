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
package org.hippoecm.hst.pagemodelapi.v10.core.container;

import org.hippoecm.hst.core.container.AbstractBaseOrderableValve;
import org.hippoecm.hst.core.container.ContainerException;
import org.hippoecm.hst.core.container.ValveContext;

public class ConditionalSupportedValve extends AbstractBaseOrderableValve {
    private boolean supported;

    @Override
    public void invoke(final ValveContext context) throws ContainerException {
        if (!supported) {
            throw new ContainerException("V1.0 is not supported in current version");
        }
        context.invokeNext();
    }

    public void setSupported(final boolean supported) {
        this.supported = supported;
    }
}
