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
package org.hippoecm.frontend.plugins.tagging.editor;

import java.util.LinkedHashSet;

import javax.jcr.RepositoryException;
import javax.jcr.Value;
import javax.jcr.ValueFormatException;
import javax.jcr.lock.LockException;
import javax.jcr.nodetype.ConstraintViolationException;
import javax.jcr.version.VersionException;

import org.apache.wicket.model.Model;
import org.hippoecm.frontend.model.properties.JcrPropertyModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TagsModel extends Model {
    @SuppressWarnings("unused")
    private static final String SVN_ID = "$Id$";

    private static final long serialVersionUID = 1L;

    static final Logger log = LoggerFactory.getLogger(TagsModel.class);

    private JcrPropertyModel propertyModel;
    private LinkedHashSet<String> tags;

    static final String TAG_SEPERATOR = ",";

    public TagsModel(JcrPropertyModel propertyModel) {
        this.propertyModel = propertyModel;
        tags = parsePropertyModel(this.propertyModel);
        super.setObject(buildModelObject(tags));
    }

    private String buildModelObject(LinkedHashSet<String> set) {
        StringBuffer buffer = new StringBuffer();
        for (String tag : set) {
            buffer.append(tag);
            buffer.append(TAG_SEPERATOR);
            buffer.append(" ");
        }
        return buffer.toString();
    }

    private String[] buildPropertyModel(String tags) {
        if (tags == null || tags.equals("")) {
            return new String[] {};
        } else {
            String[] tagsArray = tags.split(TAG_SEPERATOR);
            LinkedHashSet<String> set = new LinkedHashSet<String>();
            for (int i = 0; i < tagsArray.length; i++) {
                set.add(tagsArray[i].trim());
            }
            return set.toArray(new String[0]);
        }
    }

    private LinkedHashSet<String> parsePropertyModel(JcrPropertyModel model) {
        LinkedHashSet<String> set = new LinkedHashSet<String>();
        // the property does not exist so don't even try
        if (model.getProperty() == null) {
            return set;
        }
        try {
            for (Value val : model.getProperty().getValues()) {
                String tag = val.getString();
                set.add(tag.trim());
            }
        } catch (ValueFormatException e) {
            log.error("Unsupported value in the JcrPropertyModel.", e);
        } catch (RepositoryException e) {
            log.error("Repository error", e);
        }
        return set;
    }

    public void addTag(String tag) {
        if (!tag.trim().equals("")) {
            log.debug("Adding tag: " + tag);
            tags.add(tag.trim());
            saveProperty(tags.toArray(new String[0]));
            super.setObject(buildModelObject(tags));
        }
    }

    private void saveProperty(String[] tags) {
        try {
            propertyModel.getProperty().setValue(tags);
        } catch (ValueFormatException e) {
            log.error("Failed trying to save insane tag.", e);
        } catch (VersionException e) {
            log.error("Versioning error.", e);
        } catch (LockException e) {
            log.error("Locking error.", e);
        } catch (ConstraintViolationException e) {
            log.error("Constraint violation", e);
        } catch (RepositoryException e) {
            log.error("Repository error.", e);
        }
    }

    /**
     * Sets the string object with comma separated tags but also updates
     * the hippostd:tags property of the document with an updated String array
     */
    public void setObject(final Object object) {
        log.debug("setObject");
        super.setObject(object);

        saveProperty(buildPropertyModel((String) object));
        tags = parsePropertyModel(propertyModel);
    }

}