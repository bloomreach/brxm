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
package org.hippoecm.repository.upgrade;

import javax.jcr.NamespaceRegistry;
import javax.jcr.Node;
import javax.jcr.RepositoryException;

import org.apache.jackrabbit.core.NamespaceRegistryImpl;

import org.hippoecm.repository.ext.UpdaterContext;
import org.hippoecm.repository.ext.UpdaterItemVisitor;
import org.hippoecm.repository.ext.UpdaterModule;

public class M13Switch extends M13 implements UpdaterModule {
    @SuppressWarnings("unused")
    private final static String SVN_ID = "$Id$";

    public M13Switch() {
    }

    public void register(final UpdaterContext context) {
        context.registerName("m13-switch");
        context.registerStartTag("m13-pre1");
        context.registerEndTag("m13-pre2");
        context.registerVisitor(new UpdaterItemVisitor.Default() {
            @Override
            public void visit(Node node) throws RepositoryException {
                    NamespaceRegistry nsReg = node.getSession().getWorkspace().getNamespaceRegistry();
                    for(NamespaceMapping mapping : mappings) {
                        ((NamespaceRegistryImpl)nsReg).externalRemap(mapping.prefix, mapping.prefix+"_"+mapping.oldVersion, mapping.oldNamespaceURI);
                        ((NamespaceRegistryImpl)nsReg).externalRemap(mapping.prefix+"_"+mapping.oldVersion, mapping.prefix, mapping.newNamespaceURI);
                    }
            }
        });
    }
}
