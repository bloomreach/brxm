package org.hippoecm.repository;

import java.net.URL;

import javax.jcr.Session;

import org.apache.jackrabbit.classloader.DynamicRepositoryClassLoader;
import org.hippoecm.repository.api.HippoNodeType;

public class HippoRepositoryClassLoader extends DynamicRepositoryClassLoader {

    /** dynamic loading class path
     * Jars should be put into /hippo:plugins/&lt;plugin&gt;/&lt;jar&gt;
     */
    private static final String JARS = HippoNodeType.PLUGIN_PATH + "/*/*/jcr:data";
    private static final String[] handles = { JARS };

    public HippoRepositoryClassLoader(Session session) {
        // This is the preferred method of instantiation, with automatic (on-demand)
        // unpacking of newly inserted jars.  However, this fails because of JR bug #???.
        super(session, handles, HippoRepositoryClassLoader.class.getClassLoader());
    }

    // Used by RMI to construct codebase for the classes that have been loaded.
    // The "jcr" protocol is not generally available and would have to be 
    // registered globally in a J2EE environment.  Assume that the client is using
    // a Hippo classloader, i.e. uses the same path in the repository.
    @Override
    public URL[] getURLs() {
        return null;
    }
}
