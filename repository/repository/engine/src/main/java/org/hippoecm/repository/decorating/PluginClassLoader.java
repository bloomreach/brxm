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
package org.hippoecm.repository.decorating;

import java.net.URL;

import javax.jcr.Session;

import org.apache.jackrabbit.classloader.RepositoryClassLoader;
import org.hippoecm.repository.api.HippoNodeType;

public class PluginClassLoader extends RepositoryClassLoader {
    @SuppressWarnings("unused")
    private final static String SVN_ID = "$Id$";

    /**
     * Dynamic loading class path
     * Jars should be put into /hippo:plugins/&lt;plugin&gt;/&lt;jar&gt;
     */
    private static final String JARS = HippoNodeType.PLUGIN_PATH + "/*/*/jcr:data";
    private static final String[] handles = { JARS };

    public PluginClassLoader(Session session) {
        super(session, handles, PluginClassLoader.class.getClassLoader());
    }

    /**
     * Used by RMI to construct codebase for the classes that have been loaded.
     * The "jcr" protocol is not generally available and would have to be
     * registered globally in a J2EE environment.  Assume that the client is using
     * a Hippo classloader, i.e. uses the same path in the repository.
     *
     * @returns an empty array of URLs.
     */
    @Override
    public URL[] getURLs() {
        return null;
    }
}
