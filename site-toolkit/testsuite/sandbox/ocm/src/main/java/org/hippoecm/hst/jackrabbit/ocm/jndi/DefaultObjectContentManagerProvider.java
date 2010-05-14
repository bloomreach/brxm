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
package org.hippoecm.hst.jackrabbit.ocm.jndi;

import java.util.List;

import javax.jcr.Session;

import org.apache.jackrabbit.ocm.manager.ObjectContentManager;
import org.hippoecm.hst.jackrabbit.ocm.util.OCMUtils;

public class DefaultObjectContentManagerProvider implements ObjectContentManagerProvider {
    private List<Class> annotatedClasses;
    private String[] xmlMappingFiles;

    public DefaultObjectContentManagerProvider(List<Class> annotatedClasses) {
        this.annotatedClasses = annotatedClasses;
    }

    public DefaultObjectContentManagerProvider(String[] xmlMappingFiles) {
        this.xmlMappingFiles = xmlMappingFiles;
    }

    public ObjectContentManager getObjectContentManager(Session session) {
        if (annotatedClasses != null) {
            Class [] annotatedClassArray = new Class [annotatedClasses.size()];
            int index = 0;
            for (Class annotatedClass : annotatedClasses)
            {
                annotatedClassArray[index++] = annotatedClass;
            }
            return OCMUtils.createObjectContentManager(session, annotatedClassArray);
        } else if (xmlMappingFiles != null) {
            return OCMUtils.createObjectContentManager(session, xmlMappingFiles);
        } else {
            throw new IllegalStateException("No configuration found. Annotated class names or xml configurations should be provided.");
        }
    }
}
