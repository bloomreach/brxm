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
package org.hippoecm.frontend.service;

import org.apache.wicket.IClusterable;

/**
 * Interface to intercept editor lifecycle events.
 * Register instances of this type at the IEditor instance.
 */
public interface IEditorFilter extends IClusterable {

    /**
     * Pre-close lifecycle callback.  When the close() operation is cancelled
     * (null is returned), the IEditor instance will throw an EditorException to
     * notify the client.
     * @return Context object that will be passed to postClose
     *         When null, the stop() operation is cancelled.
     */
    Object preClose();

    /**
     * Post-close lifecycle callback.
     * @param object
     */
    void postClose(Object object);

}
