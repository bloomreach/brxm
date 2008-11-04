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
package org.hippoecm.repository.reviewedactions;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Value;

import org.hippoecm.repository.api.HippoNodeType;
import org.hippoecm.repository.ext.UpdaterContext;
import org.hippoecm.repository.ext.UpdaterItemVisitor;
import org.hippoecm.repository.ext.UpdaterModule;

public class Conversion implements UpdaterModule {
    @SuppressWarnings("unused")
    private final static String SVN_ID = "$Id$";

    public void register(final UpdaterContext context) {
        context.registerName(Conversion.class.getName());
        context.registerAfter("m8-begin");
        context.registerAfter("m8-namespace");
        context.registerAfter("m8-end");
        context.registerStartTag("m7");
        context.registerEndTag("m8-reviewedactions");
        context.registerVisitor(new UpdaterItemVisitor.NodeTypeVisitor("hippostd:publishable") {
            @Override
            protected void entering(Node node, int level) throws RepositoryException {
                if (node.getDepth() == 0) {
                    return;
                }
                Node handle = node.getParent();
                if (!handle.isNodeType(HippoNodeType.NT_HANDLE)) {
                    return;
                }
                Value[] values;
                if (handle.hasProperty(HippoNodeType.HIPPO_DISCRIMINATOR)) {
                    Value[] oldValues = handle.getProperty(HippoNodeType.HIPPO_DISCRIMINATOR).getValues();
                    for (int i = 0; i < oldValues.length; i++) {
                        if (oldValues[i].getString().equals(HippoNodeType.HIPPO_DISCRIMINATOR)) {
                            return;
                        }
                    }
                    values = new Value[oldValues.length];
                    System.arraycopy(oldValues, 0, values, 0, oldValues.length);
                } else {
                    values = new Value[1];
                }
                values[values.length - 1] = node.getSession().getValueFactory().createValue("hippostd:publishable");
                handle.setProperty(HippoNodeType.HIPPO_DISCRIMINATOR, values);
            }
        });
    }
}
