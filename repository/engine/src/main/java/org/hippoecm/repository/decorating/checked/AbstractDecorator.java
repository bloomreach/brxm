/*
 *  Copyright 2008 Hippo.
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
package org.hippoecm.repository.decorating.checked;

import javax.jcr.RepositoryException;
import javax.jcr.Session;

/**
 * Provides fields to common objects used by any decorator:
 * <ul>
 * <li><code>DecoratorFactory</code>: the decorator factory in use</li>
 * <li><code>Session</code>: the decorated session which was used to create
 * this decorator</li>
 * </ul>
 */
public abstract class AbstractDecorator {
    @SuppressWarnings("unused")
    private static final String SVN_ID = "$Id$";

    /**
     * The decorator factory. Used to decorate returned objects.
     */
    protected final DecoratorFactory factory;

    /**
     * The decorated session to which the returned objects belong.
     */
    protected final SessionDecorator session;

    /**
     * The undecorated session to which the returned objects belong.
     */
    private Session upstreamSession;

    /**
     * Constructs an abstract decorator.
     *
     * @param factory decorator factory
     * @param session decorated session
     */
    protected AbstractDecorator(DecoratorFactory factory, SessionDecorator session) {
        this.factory = factory;
        this.session = session;
        this.upstreamSession = session.session;
    }

    protected void check() throws RepositoryException {
        if(!upstreamSession.isLive()) {
            session.check();
            upstreamSession = session.session;
            repair(upstreamSession);
        }
    }
    protected abstract void repair(Session upstreamSession) throws RepositoryException;
}
