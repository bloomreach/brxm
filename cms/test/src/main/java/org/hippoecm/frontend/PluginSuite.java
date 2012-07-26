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
package org.hippoecm.frontend;

import org.hippoecm.repository.HippoRepository;
import org.hippoecm.repository.HippoRepositoryFactory;
import org.junit.internal.runners.InitializationError;
import org.junit.runner.notification.RunNotifier;
import org.junit.runners.Suite;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.RepositoryException;
import java.io.File;

public class PluginSuite extends Suite {

    static final Logger log = LoggerFactory.getLogger(PluginSuite.class);
    
    /**
     * called when the @RunWith annotation is specified on a test class.
     * Such a class is ignored; it must be explicitly added to a 
     */
    public PluginSuite(Class<?> klass) throws InitializationError {
        super(klass);
    }

    static private void clear() {
        String[] files = new String[] { ".lock", "repository", "version", "workspaces" };
        for (int i = 0; i < files.length; i++) {
            File file = new File(files[i]);
            delete(file);
        }
    }

    static private void delete(File path) {
        if (path.exists()) {
            if (path.isDirectory()) {
                File[] files = path.listFiles();
                for (int i = 0; i < files.length; i++)
                    delete(files[i]);
            }
            path.delete();
        }
    }

    @Override
    public void run(final RunNotifier notifier) {
        clear();

        HippoRepository server = null;
        try {
            server = HippoRepositoryFactory.getHippoRepository();
            RepositoryTest.setRepository(server);

            super.run(notifier);

        } catch (RepositoryException ex) {
            System.err.println(ex.getClass().getName() + ": " + ex.getMessage());
            ex.printStackTrace(System.err);
        } finally {
            if (server != null) {
                server.close();
            }

            clear();
        }
    }
}
