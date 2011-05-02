/*
 *  Copyright 2011 Hippo.
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
package org.hippoecm.repository.export;

import java.io.File;

import javax.jcr.Session;

abstract class ResourceInstruction extends AbstractInstruction {
    @SuppressWarnings("unused")
    private final static String SVN_ID = "$Id: ";

    /** the resource */
    final File file;
    /** dirty flag */
    volatile boolean changed = false;

    ResourceInstruction(String name, Double sequence, File file) {
        super(name, sequence);
        this.file = file;
    }

    boolean hasChanged() {
        return changed;
    }

    abstract void export(Session session);

    void delete() {
        file.delete();
    }

    void nodeAdded(String path) {
        changed = true;
    }

    boolean nodeRemoved(String path) {
        changed = true;
        return false;
    }

    void propertyAdded(String path) {
        changed = true;
    }

    void propertyChanged(String path) {
        changed = true;
    }

    void propertyRemoved(String path) {
        changed = true;
    }
}
