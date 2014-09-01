/*
 * Copyright 2014 Hippo B.V. (http://www.onehippo.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.onehippo.cms7.essentials.dashboard.event;

import java.io.Serializable;

/**
 * @version "$Id$"
 */
public interface PluginEvent extends Serializable {

    /**
     * in case of change events, indicates if plugin itself can undo changes
     *
     * @return true if change event and change can be reverted
     */
    boolean canUndo();

    /**
     *
     */
    void setCanUndo(boolean canUndo);

    /**
     * Human readable message
     *
     * @return message which can be displayed to users
     */
    String getMessage();

    /**
     * Location where to display event message
     *
     * @return DisplayLocation enum
     */
    DisplayLocation getDisplayLocation();

}
