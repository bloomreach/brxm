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
package org.hippoecm.repository.sample;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.Node;
import org.hippoecm.repository.api.HippoNodeType;
import org.hippoecm.repository.HippoRepository;
import org.hippoecm.repository.HippoRepositoryFactory;

public class Initialize
{

    protected final Logger log = LoggerFactory.getLogger(Initialize.class);

    HippoRepository repository;

    private Initialize() throws RepositoryException {
        repository = HippoRepositoryFactory.getHippoRepository();
    }

    private Initialize(String location) throws RepositoryException {
        repository = HippoRepositoryFactory.getHippoRepository(location);
    }

    private void initializeRepository() throws RepositoryException {
        Session session = repository.login("dummy", "dummy".toCharArray());
        Node node = session.getRootNode();
        if(!node.hasNode("configuration"))
            node = node.addNode(HippoNodeType.CONFIGURATION_PATH,HippoNodeType.NT_CONFIGURATION);
        else
            node = node.getNode(HippoNodeType.CONFIGURATION_PATH);
        if(!node.hasNode("initialize"))
            node = node.addNode(HippoNodeType.INITIALIZE_PATH,HippoNodeType.NT_INITIALIZEFOLDER);
        else
            node = node.getNode(HippoNodeType.INITIALIZE_PATH);
        node = node.addNode("newsmodel",HippoNodeType.NT_INITIALIZEITEM);
        node.setProperty(HippoNodeType.HIPPO_NODETYPESRESOURCE,"newsmodel.cnd");
        node.setProperty(HippoNodeType.HIPPO_CONTENTRESOURCE,"navigation.xml");
        session.logout();
    }

    public static void main(String[] args) {
        Initialize bootstrap = null;
        try {
            String location = args.length>0 ? args[0] : "rmi://localhost:1099/hipporepository";
            if(location != null)
                bootstrap = new Initialize(location);
            else
                bootstrap = new Initialize();
            bootstrap.initializeRepository();
        } catch(RepositoryException ex) {
            System.err.println(ex.getClass().getName()+": "+ex.getMessage());
            ex.printStackTrace(System.err);
        }
    }
}
