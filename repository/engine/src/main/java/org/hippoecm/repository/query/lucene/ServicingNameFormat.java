/*
 *  Copyright 2008-2013 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.repository.query.lucene;

import org.apache.jackrabbit.core.query.lucene.NamespaceMappings;
import org.apache.jackrabbit.spi.Name;
import org.apache.jackrabbit.spi.Path.Element;
import org.apache.jackrabbit.spi.commons.conversion.IllegalNameException;
import org.apache.jackrabbit.spi.commons.name.PathFactoryImpl;

public class ServicingNameFormat {

    private ServicingNameFormat() {
        // private constructor: only static methods
    }

    public static String getInternalFacetName(Name nodeName, NamespaceMappings nsMappings) throws IllegalNameException {
        String internalName = nsMappings.translateName(nodeName);
        int idx = internalName.indexOf(':');
        internalName = internalName.substring(0,idx+1) + ServicingFieldNames.HIPPO_FACET + internalName.substring(idx+1);
        return internalName;
    }

    public static String getInternalFacetName(String facet) {
        String internalName = facet;
        int idx = internalName.indexOf(':');
        internalName = internalName.substring(0,idx+1) + ServicingFieldNames.HIPPO_FACET + internalName.substring(idx+1);
        return internalName;
    }
    
    public static String getInteralPropertyPathName(NamespaceMappings nsMappings, String propertyPath) throws IllegalNameException{
        try {
            StringBuffer internalName = new StringBuffer();
            Element[] pathElements = PathFactoryImpl.getInstance().create(propertyPath).getElements();
            internalName.append(nsMappings.translateName(pathElements[pathElements.length - 1].getName()));
            for (int i = 0; i < pathElements.length - 1; i++) {
                internalName.append("/");
                internalName.append(nsMappings.translateName(pathElements[i].getName()));
            } 
            String name = new String(internalName);
            if(name.indexOf("$") > -1) {
                // $ used for date properties to define something like year, month
                int pos = name.indexOf("$");
                name = name.substring(0, pos) + ServicingFieldNames.DATE_NUMBER_DELIMITER + name.substring(pos+1);
            }
            return name;
        } catch (IllegalArgumentException e) {
            throw new IllegalNameException("Error creating internl property path name for '"+ propertyPath +"'", e);
        }
    }
}
