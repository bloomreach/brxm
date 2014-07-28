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
package org.hippoecm.frontend.session;

import java.lang.ref.ReferenceQueue;
import java.lang.ref.WeakReference;

import javax.jcr.Session;

import org.apache.wicket.model.IModel;

/**
 * Maintains a reference to a JCR session model, based on a Wicket session.
 * When the Wicket session is collected as garbage, the JCR session model
 * is detached.
 */
class JcrSessionReference extends WeakReference<UserSession> {

    static final ReferenceQueue<UserSession> refQueue = new ReferenceQueue<UserSession>();

    private IModel<Session> jcrSessionModel;

    JcrSessionReference(UserSession referent, IModel<Session> jcrSessionModel) {
        super(referent, refQueue);
        this.jcrSessionModel = jcrSessionModel;
    }

    IModel<Session> getJcrSessionModel() {
        return jcrSessionModel;
    }

    static void cleanup() {
        JcrSessionReference ref;
        while ((ref = (JcrSessionReference) refQueue.poll()) != null) {
            if (ref.jcrSessionModel != null) {
                ref.jcrSessionModel.detach();
            }
        }
    }

}
