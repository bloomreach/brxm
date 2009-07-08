/*
 *  Copyright 2008 Hippo.
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

package org.hippoecm.hst.plugins.frontend.util;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Value;

import org.hippoecm.frontend.model.JcrNodeModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class JcrUtilities {

    static final Logger log = LoggerFactory.getLogger(JcrUtilities.class);

    /**
     * Rename the node in the JcrNodeModel.
     * 
     * @param model
     * @param newName
     * @return A JcrNodeModel of the new node or the old model if names were equal
     */

    public static JcrNodeModel rename(JcrNodeModel model, String newName) {
        String oldPath = model.getItemModel().getPath();
        String newPath = model.getParentModel().getItemModel().getPath() + "/" + newName;
        if (!oldPath.equals(newPath)) {
            try {
                model.getNode().getSession().move(oldPath, newPath);
                return new JcrNodeModel(newPath);
            } catch (RepositoryException e) {
                log.error("Error renaming node to " + newName, e);
            }
        }
        return model;
    }

    public static void setProperty(JcrNodeModel model, String property, String value) {
        try {
            model.getNode().setProperty(property, value);
        } catch (RepositoryException e) {
            log.error("Error saving property " + property + " with value " + value, e);
        }
    }

    public static String getProperty(JcrNodeModel model, String property) {
        try {
            Node node = model.getNode();
            if (node.hasProperty(property)) {
                return node.getProperty(property).getString();
            }
        } catch (RepositoryException e) {
            log.error("Failed to retrieve value of property " + property, e);
        }
        return null;
    }

    public static boolean hasProperty(JcrNodeModel model, String property) {
        try {
            return model.getNode().hasProperty(property);
        } catch (RepositoryException e) {
            log.error("Failed to check if property " + property + " exists", e);
        }
        return false;
    }

    public static void updateProperty(JcrNodeModel model, String property, String value) {
        if (value != null) {
            if (hasProperty(model, property) && getProperty(model, property).equals(value)) {
                return; //value the same
            }
            setProperty(model, property, value);
        }
    }

    public static List<String> getMultiValueProperty(JcrNodeModel model, String property) {
        try {
            Node node = model.getNode();
            if (node.hasProperty(property)) {
                List<String> values = new ArrayList<String>();
                for (Value v : node.getProperty(property).getValues()) {
                    values.add(v.getString());
                }
                return values;
            }
        } catch (RepositoryException e) {
            log.error("Failed to retrieve value of property " + property, e);
        }
        return null;
    }

    public static void setMultiValueProperty(JcrNodeModel model, String property, List<String> values) {
        try {
            model.getNode().setProperty(property, values.toArray(new String[] {}));
        } catch (RepositoryException e) {
            log.error("Error saving list of Strings in property " + property, e);
        }
    }

    public static void updateMultiValueProperty(JcrNodeModel model, String property, List<String> value) {
        if (value != null) {
            if (hasProperty(model, property) && getMultiValueProperty(model, property).equals(value)) {
                return; //value the same
            }
            setMultiValueProperty(model, property, value);
        }
    }

    public static void updateProperty(JcrNodeModel model, String property, InputStream value) {
        try {
            model.getNode().setProperty(property, value);
        } catch (RepositoryException e) {
            log.error("Error saving property {0} of type InputStream", new Object[] { property });
        }
    }

    public static void updateProperty(JcrNodeModel model, String property, long value) {
        try {
            model.getNode().setProperty(property, value);
        } catch (RepositoryException e) {
            log.error("Error saving property {0} of type Long", new Object[] { property });
        }
    }
}
