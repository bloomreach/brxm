/*
 * Copyright 2013 Hippo B.V. (http://www.onehippo.com)
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.hippoecm.frontend.model;

import javax.jcr.LoginException;
import javax.jcr.Session;

/**
 * A tuple class used as a multi-return-value holding the {@link Session} and the {@link LoginException}
 * if something happened and went wrong while login to obtain that {@link Session}
 */
public class SessionTuple<X extends Session, Y extends LoginException> {

    public final X session;
    public final Y exception;

    public SessionTuple(X session, Y exception) {
        this.session = session;
        this.exception = exception;
    }

}
