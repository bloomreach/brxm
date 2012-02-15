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
package org.hippoecm.hst.site.container;

import org.springframework.jmx.export.MBeanExporter;

/**
 * UnregisterableMBeanExporter
 * <P>
 * This class overrides the protected <CODE>unregisterBeans()</CODE> method
 * to enable to invoke the method from HST-2 Container.
 * HST-2 Container should unregister the existing MBeans first before
 * initializing a new component manager because the old component manager
 * can be destroyed after the new component manager is created, so
 * the old component manager can trigger removals of the new MBeans which
 * are just registered by the new component manager.
 * </P>
 * @version $Id$
 */
public class UnregisterableMBeanExporter extends MBeanExporter {

    public UnregisterableMBeanExporter() {
        super();
    }

    @Override
    public void unregisterBeans() {
        super.unregisterBeans();
    }
}
