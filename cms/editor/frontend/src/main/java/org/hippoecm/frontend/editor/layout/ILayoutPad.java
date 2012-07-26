/*
 *  Copyright 2009 Hippo.
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
package org.hippoecm.frontend.editor.layout;

import java.util.List;

import org.apache.wicket.IClusterable;

/**
 * A layout pad is available as a service at the extension point where the
 * child render service registers.  It allows the child to reposition itself.
 */
public interface ILayoutPad extends IClusterable {

    /**
     * Orientation of a list pad
     */
    enum Orientation {
        HORIZONTAL, VERTICAL
    }

    /**
     * Name of the pad. 
     */
    String getName();

    /**
     * Does the layout pad represent a list.
     * 
     * @return true if the pad is a list
     */
    boolean isList();

    /**
     * Orientation of the list.
     * 
     * @return the {@link Orientation} of the list.  If the pad does not represent
     *          a list, null.
     */
    Orientation getOrientation();

    /**
     * @return the names of transitions that are available at the current location.
     */
    List<String> getTransitions();

    /**
     * @return the transition with the given name.
     */
    ILayoutTransition getTransition(String name);

}
