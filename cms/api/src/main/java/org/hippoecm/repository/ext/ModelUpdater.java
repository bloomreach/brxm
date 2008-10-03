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
package org.hippoecm.repository.ext;

import java.rmi.Remote;
import java.util.Date;

import java.util.Iterator;
import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Value;

import javax.jcr.ValueFactory;
import javax.jcr.nodetype.NodeDefinition;
import javax.jcr.nodetype.NodeType;
import javax.jcr.nodetype.PropertyDefinition;
import org.hippoecm.repository.api.Workflow;

public abstract class ModelUpdater implements Remote, Workflow
{
    @SuppressWarnings("unused")
    private final static String SVN_ID = "$Id$";

    public ModelUpdater() {
    }

    public final void update(Node node, Node target) throws RepositoryException {
        update(node);
    }
    public abstract void update(Node node) throws RepositoryException;
}
