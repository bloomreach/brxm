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
package org.hippoecm.repository.servicing;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.Value;
import javax.jcr.nodetype.PropertyDefinition;

public class FacetContentUtilities {
    public static void build(Session session, String[] contents) throws RepositoryException {
        Node node = null;
        for (int i=0; i<contents.length; i+=2) {
            if (contents[i].startsWith("/")) {
                String path = contents[i].substring(1);
                node = session.getRootNode();
                if (path.contains("/")) {
                    node = node.getNode(path.substring(0,path.lastIndexOf("/")));
                    path = path.substring(path.lastIndexOf("/")+1);
                }
                node = node.addNode(path, contents[i+1]);
            } else {
                PropertyDefinition propDef = null;
                PropertyDefinition[] propDefs = node.getPrimaryNodeType().getPropertyDefinitions();
                for (int propidx=0; propidx<propDefs.length; propidx++)
                    if(propDefs[propidx].getName().equals(contents[i])) {
                        propDef = propDefs[propidx];
                        break;
                    }
                if (propDef != null && propDef.isMultiple()) {
                    Value[] values;
                    if (node.hasProperty(contents[i])) {
                        values = node.getProperty(contents[i]).getValues();
                        Value[] newValues = new Value[values.length+1];
                        System.arraycopy(values,0,newValues,0,values.length);
                        values = newValues;
                    } else {
                        values = new Value[1];
                    }
                    values[values.length-1] = session.getValueFactory().createValue(contents[i+1]);
                    node.setProperty(contents[i], values);
                } else {
                    node.setProperty(contents[i], contents[i+1]);
                }
            }
        }
    }
}
