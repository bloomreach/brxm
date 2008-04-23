/*
 * Copyright 2007 Hippo
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
package org.hippoecm.repository.standardworkflow;

import java.rmi.RemoteException;
import java.util.Map;

import javax.jcr.RepositoryException;

import org.hippoecm.repository.api.MappingException;
import org.hippoecm.repository.api.Workflow;
import org.hippoecm.repository.api.WorkflowException;

public interface RemodelWorkflow extends Workflow {

    final public static String VERSION_DRAFT = "draft";
    final public static String VERSION_CURRENT = "current";
    final public static String VERSION_ERROR = "error";
    final public static String VERSION_OLD = "old";

    /**
     * Instruct the repository to apply the new node definition overriding the
     * earlier node definition.
     */
    public String[] remodel(String cnd, Map<String, TypeUpdate> updates) throws WorkflowException, MappingException,
            RepositoryException, RemoteException;

    /**
     * convert a single node
     */
    public void convert(String namespace, Map<String, TypeUpdate> updates) throws WorkflowException, MappingException,
            RepositoryException, RemoteException;

    /**
     * create a new namespace
     */
    public void createNamespace(String prefix, String namespace) throws WorkflowException, MappingException,
            RepositoryException, RemoteException;

    /**
     * Create a new node type definition
     * FIXME: also create a template and return its path
     */
    public void createType(String name) throws WorkflowException, MappingException, RepositoryException,
            RemoteException;

    public abstract class TypeUpdate {

        public String newName;

        abstract public Map<FieldIdentifier, FieldIdentifier> getRenames();
    }

    public class FieldIdentifier {

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
}
