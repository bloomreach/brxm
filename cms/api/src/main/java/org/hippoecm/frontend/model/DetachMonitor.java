/*
 *  Copyright 2009-2013 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.frontend.model;

import java.io.IOException;
import java.io.ObjectOutputStream;

import org.apache.wicket.Application;
import org.apache.wicket.RuntimeConfigurationType;
import org.apache.wicket.model.IDetachable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DetachMonitor implements IDetachable {

    private static final long serialVersionUID = 1L;

    static final Logger log = LoggerFactory.getLogger(DetachMonitor.class);

    static final int ATTACHED = 0x00000001;

    private transient int flags = 0;

    public void attach() {
        flags |= ATTACHED;
    }

    public boolean isAttached() {
        return (flags & ATTACHED) != 0;
    }

    public void detach() {
        flags &= 0xFFFFFFFF ^ ATTACHED;
    }

    private void writeObject(ObjectOutputStream output) throws IOException {
        if ((flags & ATTACHED) != 0) {
            // TODO: walk the stack to identify owner
            log.warn("Undetached DetachMonitor");
            if (RuntimeConfigurationType.DEPLOYMENT.equals(Application.get().getConfigurationType())) {
                detach();
            }
        }
        output.defaultWriteObject();
    }
}
