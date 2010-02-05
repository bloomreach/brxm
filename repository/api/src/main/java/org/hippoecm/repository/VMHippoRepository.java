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

import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;

import javax.jcr.RepositoryException;

/**
 * DO NOT USE THIS CLASS!
 * This class is NOT part of the API.
 */
public class VMHippoRepository {
    @SuppressWarnings("unused")
    private final static String SVN_ID = "$Id$";

    private static Map<String,HippoRepository> repositories;

    private VMHippoRepository() {
    }

    static {
        Map<String,HippoRepository> map = new LinkedHashMap<String,HippoRepository>();
        repositories = Collections.synchronizedMap(map);
    }

    /** DO NOT USE THIS METHOD!  This class and all its methods are NOT part of the API. */
    public static HippoRepository create(String location) throws RepositoryException {
        HippoRepository hippoRepository = null;
        if (location != null && location.startsWith("vm://")) {
            location = location.substring("vm://".length());
        }
        if (location == null || location.equals("")) {
            Iterator<HippoRepository> iter = repositories.values().iterator();
            if (iter.hasNext()) {
                hippoRepository = iter.next();
            }
        } else {
            hippoRepository = repositories.get(location);
        }
        if (hippoRepository != null) {
            return hippoRepository;
        } else {
            throw new RepositoryException("No repository found at: vm://" + location);
        }
    }

    /** DO NOT USE THIS METHOD!  This class and all its methods are NOT part of the API. */
    public static void register(String location, HippoRepository repository) {
        repositories.put(location, repository);
    }
}
