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
package org.hippoecm.repository.standardworkflow;

import java.io.Serializable;
import java.rmi.RemoteException;
import java.util.Map;
import java.util.TreeMap;

import javax.jcr.RepositoryException;
import org.hippoecm.repository.api.MappingException;
import org.hippoecm.repository.api.Workflow;
import org.hippoecm.repository.api.WorkflowException;

public interface TemplateEditorWorkflow extends RepositoryWorkflow, Workflow {
    public final static class TypeUpdate implements Serializable {
        private static final long serialVersionUID = 1L;

        public String newName;

        public String prototype;

        public Map<FieldIdentifier, FieldIdentifier> renames;
    }

    public final static class FieldIdentifier implements Serializable {
        private static final long serialVersionUID = 1L;

        public String path;

        public String type;

        @Override
        public boolean equals(Object object) {
            if (object != null) {
                if (object instanceof FieldIdentifier) {
                    FieldIdentifier id = (FieldIdentifier) object;
                    return id.path.equals(path) && id.type.equals(type);
                }
            }
            return false;
        }

        @Override
        public int hashCode() {
            return (path.hashCode() * type.hashCode()) % 1001;
        }
    }

    public void createNamespace(String prefix, String namespace) throws WorkflowException, MappingException, RepositoryException, RemoteException;

    public void createType(String name) throws WorkflowException, MappingException, RepositoryException, RemoteException;

    public void updateModel(String prefix, String cnd, Map<String, TypeUpdate> updates) throws WorkflowException, MappingException, RepositoryException, RemoteException;

    public void updateModel(String cnd, Map<String, TypeUpdate> updates) throws WorkflowException, MappingException, RepositoryException, RemoteException;
}
