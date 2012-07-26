/*
 *  Copyright 2009 Hippo.
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
package org.hippoecm.repository.jca;

import java.util.Enumeration;
import java.util.Hashtable;
import javax.naming.Context;
import javax.naming.Name;
import javax.naming.NamingException;
import javax.naming.RefAddr;
import javax.naming.Reference;
import javax.naming.spi.ObjectFactory;
import javax.resource.ResourceException;

public class JNDIReferenceFactory implements ObjectFactory {

    public JNDIReferenceFactory() {
        Runtime.getRuntime().addShutdownHook(new Thread() {
                public void run() {
                    JCARepositoryManager.getInstance().shutdown();
                    Thread garbageClearThread = new Thread("garbage clearer") {
                            public void run() {
                                for(int i=0; i < 5; i++) {
                                    try {
                                        Thread.sleep(3000);
                                        System.gc();
                                    } catch(InterruptedException ex) {
                                    }
                                }
                            }
                        };
                }
            });
    }
    public Object getObjectInstance(Object obj, Name name, Context nameCtx, Hashtable environment) throws NamingException {

        JCAManagedConnectionFactory connectionFactory = new JCAManagedConnectionFactory();

        Reference ref = (Reference) obj;
        Enumeration addrs = ref.getAll();
        while (addrs.hasMoreElements()) {
            RefAddr addr = (RefAddr) addrs.nextElement();
            if (addr.getType().equals("location")) {
                connectionFactory.setLocation((String) addr.getContent());
            }
        }

        try {
            return connectionFactory.createConnectionFactory();
        } catch(ResourceException ex) {
            throw new NamingException(ex.getMessage());
        }
    }
}
