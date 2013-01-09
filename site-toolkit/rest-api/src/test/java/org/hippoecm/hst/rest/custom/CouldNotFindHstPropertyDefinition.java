/*
 *  Copyright 2012-2013 Hippo B.V. (http://www.onehippo.com)
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

package org.hippoecm.hst.rest.custom;

/**
 * An {@link Exception} class to signal the in-ability of finding a specific HstPropertyDefinition
 */
public class CouldNotFindHstPropertyDefinition extends Exception {

    /**
     * Serial version ID
     */
    private static final long serialVersionUID = 1L;

    /**
     * A constructor with a message
     * 
     * @param message - A message indicating the reason behind throwing an instance of that exception
     */
    public CouldNotFindHstPropertyDefinition(String message) {
        super(message);
    }

}
