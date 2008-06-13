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
package org.hippoecm.frontend.session;

import javax.jcr.Session;

import org.apache.jackrabbit.classloader.DynamicRepositoryClassLoader;
import org.apache.jackrabbit.classloader.RepositoryClassLoader;

import org.hippoecm.repository.api.HippoNodeType;

public class SessionClassLoader extends RepositoryClassLoader {
    @SuppressWarnings("unused")
    private final static String SVN_ID = "$Id$";

    /**
     * Dynamic loading class path
     * Jars should be put into /hippo:plugins/&lt;plugin&gt;/&lt;jar&gt;
     */
    private static final String JARS = HippoNodeType.PLUGIN_PATH + "/*/*/jcr:data";
    private static final String[] handles = { JARS };

    public SessionClassLoader(Session session) {
        /* FIXME:
         * This is the preferred method of instantiation, with automatic (on-demand)
         * unpacking of newly inserted jars.  However, this fails because of JR bug #???.
         */
        super(session, handles, SessionClassLoader.class.getClassLoader());
    }
}
