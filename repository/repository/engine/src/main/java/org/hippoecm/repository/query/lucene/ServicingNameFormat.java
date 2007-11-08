/*
 * Copyright 2007 Hippo
 *
 * Licensed under the Apache License, Version 2.0 (the  "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.hippoecm.repository.query.lucene;

import javax.jcr.NamespaceException;

import org.apache.jackrabbit.conversion.IllegalNameException;
import org.apache.jackrabbit.conversion.NameResolver;
import org.apache.jackrabbit.conversion.ParsingNameResolver;
import org.apache.jackrabbit.core.query.lucene.NamespaceMappings;
import org.apache.jackrabbit.name.NameFactoryImpl;
import org.apache.jackrabbit.spi.Name;

public class ServicingNameFormat {
    private ServicingNameFormat(){
        // private constructor: only static methods
    }

    public static String getInternalFacetName(Name nodeName, NamespaceMappings nsMappings) throws IllegalNameException {
    	String internalName = nsMappings.translatePropertyName(nodeName);
        int idx = internalName.indexOf(':');
        internalName = internalName.substring(0, idx + 1)
                + ServicingFieldNames.HIPPO_FACET + internalName.substring(idx + 1);
        return internalName;
    }
    
    public static String getInternalFacetName(String facet, NamespaceMappings nsMappings) throws IllegalNameException, NamespaceException {
    	
    	Name nodeName = NameFactoryImpl.getInstance().create("", facet);
        String internalName = nsMappings.translatePropertyName(nodeName);
        //String internalName = NameFormat.format(nodeName,nsMappings);
        int idx = internalName.indexOf(':');
        internalName = internalName.substring(0, idx + 1)
                + ServicingFieldNames.HIPPO_FACET + internalName.substring(idx + 1);
        return internalName;
    }
    
    
}
