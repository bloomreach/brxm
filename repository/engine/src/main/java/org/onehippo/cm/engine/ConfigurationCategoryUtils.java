/*
 *  Copyright 2019 Hippo B.V. (http://www.onehippo.com)
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
package org.onehippo.cm.engine;

import javax.jcr.Property;
import javax.jcr.RepositoryException;

import org.hippoecm.repository.api.HippoNodeType;
import org.onehippo.cm.model.ConfigurationModel;
import org.onehippo.cm.model.path.JcrPaths;
import org.onehippo.cm.model.tree.ConfigurationProperty;

import static org.apache.jackrabbit.JcrConstants.JCR_PRIMARYTYPE;
import static org.hippoecm.repository.api.HippoNodeType.HIPPO_GROUPS;
import static org.hippoecm.repository.api.HippoNodeType.HIPPO_USERROLE;
import static org.hippoecm.repository.api.HippoNodeType.HIPPO_USERROLES;
import static org.hippoecm.repository.api.HippoNodeType.HIPPO_USERS;
import static org.hippoecm.repository.api.HippoNodeType.NT_AUTHROLE;
import static org.hippoecm.repository.api.HippoNodeType.NT_EXTERNALGROUP;
import static org.hippoecm.repository.api.HippoNodeType.NT_EXTERNALUSER;
import static org.hippoecm.repository.api.HippoNodeType.NT_GROUP;
import static org.hippoecm.repository.api.HippoNodeType.NT_USER;

public class ConfigurationCategoryUtils {

    /**
     * Test if the provided property is special security configuration property for which its ConfigurationCategory
     * may need overriding handling, depending on the context: as CONFIG to allow detection of (runtime) changes, and as
     * SYSTEM when exporting those changes.
     * <p>
     *     This applies to the following security configuration properties, of nodes which themselves are (or must be)
     *     of category CONFIG:
     *     <ul>
     *         <li>
     *             Node type {@link HippoNodeType#NT_AUTHROLE}
     *             <ul>
     *                 <li>property {@link HippoNodeType#HIPPO_USERROLE}</li>
     *                 <li>property {@link HippoNodeType#HIPPO_GROUPS}</li>
     *                 <li>property {@link HippoNodeType#HIPPO_USERS}</li>
     *             </ul>
     *         </li>
     *         <li>
     *             Property {@link HippoNodeType#HIPPO_USERROLES} of:
     *             <ul>
     *                 <li>Node type {@link HippoNodeType#NT_GROUP}</li>
     *                 <li>Node type {@link HippoNodeType#NT_EXTERNALGROUP}</li>
     *                 <li>Node type {@link HippoNodeType#NT_USER}</li>
     *                 <li>Node type {@link HippoNodeType#NT_EXTERNALUSER}</li>
     *             </ul>
     *         </li>
     *
     *     </ul>
     * </p>
     * <p>Note: the node type check is done on the <em>name</em> of the property parent node its primary type, so only
     * applies to the above listed node types explicitly.</p>
     * @param property the jcr property to test
     * @return true if the property matches one the above conditions
     * @throws RepositoryException in case of an unexpected error
     */
    public static boolean isOverridingCategoryForSecurityProperty(final Property property) throws RepositoryException {
        return isOverridingCategoryForSecurityProperty(property.getParent().getPrimaryNodeType().getName(), property.getName());
    }

    /**
     * Test if the provided property path is for a special security configuration property for which its ConfigurationCategory
     * may need overriding handling, depending on the context: as CONFIG to allow detection of (runtime) changes, and as
     * SYSTEM when exporting those changes.
     * <p>
     *     This applies to the following security configuration properties, of nodes which themselves are (or must be)
     *     of category CONFIG:
     *     <ul>
     *         <li>
     *             Node type {@link HippoNodeType#NT_AUTHROLE}
     *             <ul>
     *                 <li>property {@link HippoNodeType#HIPPO_USERROLE}</li>
     *                 <li>property {@link HippoNodeType#HIPPO_GROUPS}</li>
     *                 <li>property {@link HippoNodeType#HIPPO_USERS}</li>
     *             </ul>
     *         </li>
     *         <li>
     *             Property {@link HippoNodeType#HIPPO_USERROLES} of:
     *             <ul>
     *                 <li>Node type {@link HippoNodeType#NT_GROUP}</li>
     *                 <li>Node type {@link HippoNodeType#NT_EXTERNALGROUP}</li>
     *                 <li>Node type {@link HippoNodeType#NT_USER}</li>
     *                 <li>Node type {@link HippoNodeType#NT_EXTERNALUSER}</li>
     *             </ul>
     *         </li>
     *
     *     </ul>
     * </p>
     * <p>Note: the node type check is done against the primary type of the DefinitionNode parent of the property, so
     * only applies to the above listed node types explicitly.</p>
     * @param model the configuration model
     * @param propertyPath the absolute path of the property in the configuration model
     * @return true if the property matches one the above conditions
     * @throws RepositoryException in case of an unexpected error
     */
    public static boolean isOverridingCategoryForSecurityProperty(final ConfigurationModel model, final String propertyPath) {
        final ConfigurationProperty property = model.resolveProperty(JcrPaths.getPath(propertyPath));
        if (property != null) {
            final ConfigurationProperty primaryType = property.getParent().getProperty(JCR_PRIMARYTYPE);
            return primaryType != null &&
                    isOverridingCategoryForSecurityProperty(primaryType.getValue().getString(), property.getName());
        }
        return false;
    }

    /**
     * Test if the provided property name is for a special security configuration property for which its ConfigurationCategory
     * may need overriding handling, depending on the context: as CONFIG to allow detection of (runtime) changes, and as
     * SYSTEM when exporting those changes.
     * <p>
     *     This applies to the following security configuration properties, of nodes which themselves are (or must be)
     *     of category CONFIG:
     *     <ul>
     *         <li>
     *             Node type {@link HippoNodeType#NT_AUTHROLE}
     *             <ul>
     *                 <li>property {@link HippoNodeType#HIPPO_USERROLE}</li>
     *                 <li>property {@link HippoNodeType#HIPPO_GROUPS}</li>
     *                 <li>property {@link HippoNodeType#HIPPO_USERS}</li>
     *             </ul>
     *         </li>
     *         <li>
     *             Property {@link HippoNodeType#HIPPO_USERROLES} of:
     *             <ul>
     *                 <li>Node type {@link HippoNodeType#NT_GROUP}</li>
     *                 <li>Node type {@link HippoNodeType#NT_EXTERNALGROUP}</li>
     *                 <li>Node type {@link HippoNodeType#NT_USER}</li>
     *                 <li>Node type {@link HippoNodeType#NT_EXTERNALUSER}</li>
     *             </ul>
     *         </li>
     *
     *     </ul>
     * </p>
     * @param nodeType the primary type of the property node to test
     * @param propertyName the name of the property to test
     * @return true if the property matches one the above conditions
     * @throws RepositoryException in case of an unexpected error
     */
    public static boolean isOverridingCategoryForSecurityProperty(final String nodeType, final String propertyName) {
        if (NT_AUTHROLE.equals(nodeType)) {
            return HIPPO_USERROLE.equals(propertyName) || HIPPO_GROUPS.equals(propertyName) || HIPPO_USERS.equals(propertyName);
        }
        else {
            return HIPPO_USERROLES.equals(propertyName) &&
                    (NT_GROUP.equals(nodeType) || NT_EXTERNALGROUP.equals(nodeType) ||
                            NT_USER.equals(nodeType) || NT_EXTERNALUSER.equals(nodeType));
        }
    }
}
