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
package org.hippoecm.hst.pagecomposer.composer;

import java.util.HashMap;
import java.util.Map;

import org.hippoecm.hst.composer.ComposerInfo;

public class ComposerInfoImpl implements ComposerInfo {


    private static final String ENABLED = "true";
    
    private Map<String, String> mountInComposerModeMap = new HashMap<String, String>();
    
    @Override
    public boolean isInComposerMode(String mountIdentifier) {
        String inComposerMode = mountInComposerModeMap.get(mountIdentifier);
        if(inComposerMode == null) {
            return false;
        }
        return ENABLED.equals(inComposerMode);
    }

    /**
     * 
     * @param mountIdentifier
     * @return the new boolean value for inComposerMode
     */
    public boolean toggleInComposerMode(String mountIdentifier) {
        String inComposerMode = mountInComposerModeMap.get(mountIdentifier);
        if(inComposerMode == null) {
            mountInComposerModeMap.put(mountIdentifier, ENABLED);
           
        } else {
            mountInComposerModeMap.remove(mountIdentifier);
        }
        return isInComposerMode(mountIdentifier);
    }
}
