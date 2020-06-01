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
package org.hippoecm.editor.type;

import javax.jcr.Session;

/**
 * JCR type store using Wicket. The JCR session is retrieved from the Wicket session, and the creates types
 * are validated when Wicket is run in development mode.
 */
public class PlainJcrTypeStore extends AbstractJcrTypeStore {

    private static final long serialVersionUID = 1L;

    private Session session;

    public PlainJcrTypeStore(Session session) {
        this.session = session;
    }

    // Plain JCR implementations of abstract base class methods

    protected Session getJcrSession() {
        return session;
    }

}
