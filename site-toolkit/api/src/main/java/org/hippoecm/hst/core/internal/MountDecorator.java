/*
 *  Copyright 2011-2013 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.hst.core.internal;

import org.hippoecm.hst.configuration.hosting.Mount;
import org.hippoecm.hst.configuration.internal.ContextualizableMount;

/**
 *  This is an INTERNAL USAGE ONLY API. Clients should not cast to these interfaces as they should never be used from client code
 * 
 * <p>A mount decorator can decorate a {@link ContextualizableMount} to act like a preview {@link Mount}. If the {@link ContextualizableMount} is already a preview, 
 * it returns the {@link ContextualizableMount} as is. If the is. If the {@link ContextualizableMount} it must decorate is a live {@link Mount}, then it returns a preview version of it</p>
 *
 */
public interface MountDecorator {

    /**
     * This method decorates the <code>mount</code> to a preview {@link Mount}. If the <code>mount</code> in the argument is already a preview mount according {@link Mount#isPreview()},
     * then the <code>mount</code> from the argument is returned without decorating it. 
     * @param mount the mount to decorate
     * @return the decorated {@link Mount}
     */
    ContextualizableMount decorateMountAsPreview(ContextualizableMount mount);
}
