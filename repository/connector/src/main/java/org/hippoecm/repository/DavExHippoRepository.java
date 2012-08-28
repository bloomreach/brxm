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
package org.hippoecm.repository;

import java.net.MalformedURLException;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;

import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.apache.jackrabbit.jcr2dav.Jcr2davRepositoryFactory;
import org.apache.jackrabbit.spi2davex.Spi2davexRepositoryServiceFactory;

class DavExHippoRepository extends HippoRepositoryImpl {

    @SuppressWarnings("unchecked")
    public DavExHippoRepository(String location) throws MalformedURLException, RepositoryException {
        Jcr2davRepositoryFactory factory = new Jcr2davRepositoryFactory();
        Map params = new HashMap();
        params.put(Spi2davexRepositoryServiceFactory.PARAM_REPOSITORY_URI, location);
        repository = factory.getRepository(params);
    }

    public static HippoRepository create(String location) throws MalformedURLException, RepositoryException {
        return new DavExHippoRepository(location);
    }

    public boolean stateThresholdExceeded(Session session, EnumSet<SessionStateThresholdEnum> interests) {
        return false; // FIXME: unimplemented
    }
}
