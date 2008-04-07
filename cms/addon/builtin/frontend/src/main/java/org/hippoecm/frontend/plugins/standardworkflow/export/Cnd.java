/*
 * Copyright 2008 Hippo
 *
 * Licensed under the Apache License, Version 2.0 (the  "License");
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
package org.hippoecm.frontend.plugins.standardworkflow.export;

import javax.jcr.RepositoryException;

import org.apache.wicket.Application;
import org.apache.wicket.util.value.ValueMap;
import org.hippoecm.frontend.Main;
import org.hippoecm.frontend.model.JcrSessionModel;
import org.hippoecm.repository.HippoRepository;
import org.hippoecm.repository.HippoRepositoryFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Cnd {

    private static final Logger log = LoggerFactory.getLogger(Cnd.class);

    public static String SYSTEMUSER_ID = "admin";
    public static String SYSTEMUSER_PASSWORD = "admin";

    public static void main(final String[] args) {
        if (args.length < 1) {
            System.out.println("usage: Cnd <namespace> [<repository location>]");
            return;
        }

        // ugly, but it works.  Application.get() is used to get a reference to a repository.
        // We have to provide a Main subclass to intercept this.
        Application.set(new Main() {
            @Override
            public HippoRepository getRepository() {
                try {
                    if (args.length > 1) {
                        return HippoRepositoryFactory.getHippoRepository(args[1]);
                    } else {
                        return HippoRepositoryFactory.getHippoRepository();
                    }
                } catch (RepositoryException ex) {
                    log.error(ex.getMessage());
                }
                return null;
            }
        });

        ValueMap credentials = new ValueMap();
        credentials.put("username", SYSTEMUSER_ID);
        credentials.put("password", SYSTEMUSER_PASSWORD);
        JcrSessionModel jcrSession = new JcrSessionModel(credentials);

        CndSerializer serializer = new CndSerializer(jcrSession,  args[0]);
        log.debug(serializer.getOutput());
    }

}
