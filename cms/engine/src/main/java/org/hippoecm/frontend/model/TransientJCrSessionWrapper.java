/*
 *  Copyright 2014 Hippo B.V. (http://www.onehippo.com)
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
import java.io.Serializable;

import javax.jcr.Session;

/**
 * A simple wrapper class of a transient instance variable <code>session</code>: It's purpose is to make sure the transient
 * jcr session in JcrSessionModel gets logged out on serialization
 */
public class TransientJCrSessionWrapper implements Serializable {

    final transient Session session;

    TransientJCrSessionWrapper(Session session) {
        this.session = session;
    }

    private void writeObject(java.io.ObjectOutputStream out)
         throws IOException {
        if (session != null && session.isLive()) {
            session.logout();
        }
        out.defaultWriteObject();
    }

}
