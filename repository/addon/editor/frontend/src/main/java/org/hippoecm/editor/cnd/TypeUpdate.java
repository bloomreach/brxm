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
package org.hippoecm.editor.cnd;

import java.io.ObjectStreamException;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;


public class TypeUpdate implements Serializable {
    @SuppressWarnings("unused")
    private final static String SVN_ID = "$Id: TypeUpdate.java 17361 2009-04-21 10:07:17Z bvanhalderen $";

    private static final long serialVersionUID = 1L;

    public String newName;

    public String prototype;

    public Map<FieldIdentifier, FieldIdentifier> renames;

    private Object writeReplace() throws ObjectStreamException {
        Map<String, Object> obj =  new HashMap<String, Object>();
        obj.put("newName", newName);
        obj.put("prototype", prototype);
        obj.put("renames", renames);
        return obj;
    }

}
