/*
 *  Copyright 2012 Hippo.
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
package org.hippoecm.repository.deriveddata;

import javax.jcr.Node;
import javax.jcr.RepositoryException;

class PropertyReference {
    private final Node propDef;

    PropertyReference(final Node propDef) {
        this.propDef = propDef;
    }

    PropertyReferenceType getType() throws RepositoryException {
        if (propDef.isNodeType("hipposys:builtinpropertyreference")) {
            return PropertyReferenceType.BUILTIN;
        } else if (propDef.isNodeType("hipposys:relativepropertyreference")) {
            return PropertyReferenceType.RELATIVE;
        } else if (propDef.isNodeType("hipposys:resolvepropertyreference")) {
            return PropertyReferenceType.RESOLVE;
        }
        return PropertyReferenceType.UNKNOWN;
    }

    String getName() throws RepositoryException {
        return propDef.getName();
    }

    String getRelativePath() throws RepositoryException {
        return propDef.getProperty("hipposys:relPath").getString();
    }

    String getMethod() throws RepositoryException {
        return propDef.getProperty("hipposys:method").getString();
    }
}
