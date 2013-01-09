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
package org.hippoecm.hst.core.jcr.pool;

import javax.jcr.Session;

/**
 * Session decorator interface.
 * <CODE>javax.jcr.Session</CODE> instances can be decorated for some reasons.
 * For example, sessions in a session pool can be decorated to override <CODE>logout()</CODE> method
 * to change the default action by returning itself to the pool.
 * Or, in some cases, a session can be decorated to disallow writing actions to make it read-only.
 * 
 * @version $Id$
 */
public interface SessionDecorator
{
    
    /**
     * Decorates the session and returns another decorated session.
     * 
     * @param session
     * @return
     */
    Session decorate(Session session);
    
    /**
     * Decorates the session and returns another decorated session
     * with the user ID used to acquire the session
     * 
     * @param session
     * @param userID
     * @return
     */
    Session decorate(Session session, String userID);
    
}
