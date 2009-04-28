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

import javax.jcr.Node;
import javax.jcr.Property;
import javax.jcr.RepositoryException;

import org.hippoecm.repository.ext.UpdaterContext;
import org.hippoecm.repository.ext.UpdaterItemVisitor;
import org.hippoecm.repository.ext.UpdaterModule;

public class M13Process extends M13 implements UpdaterModule {
    @SuppressWarnings("unused")
    private final static String SVN_ID = "$Id$";

    public M13Process() {
    }

    public void register(final UpdaterContext context) {
        context.registerName("m13-process");
        context.registerStartTag("m13-pre2");
        context.registerEndTag("m13-pre3");
        context.registerVisitor(new UpdaterItemVisitor.Default() {
            @Override
            public void leaving(Node node, int level) throws RepositoryException {
                if(node.isNodeType("hippostd:fixeddirectory")) {
                    context.setPrimaryNodeType(node, rename("hippostd:folder"));
                }
            }
            @Override
            public void entering(Property prop, int level) throws RepositoryException {
            }
        });
    }
}
