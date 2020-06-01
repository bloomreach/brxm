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
package org.onehippo.forge.ecmtagging.editor;

import org.apache.commons.lang.StringUtils;
import org.apache.wicket.model.Model;
import org.apache.wicket.util.string.Strings;
import org.hippoecm.frontend.model.JcrItemModel;
import org.hippoecm.frontend.model.properties.JcrPropertyModel;
import org.onehippo.forge.ecmtagging.TaggingNodeType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.Node;
import javax.jcr.Property;
import javax.jcr.RepositoryException;
import javax.jcr.Value;
import javax.jcr.ValueFormatException;
import javax.jcr.lock.LockException;
import javax.jcr.nodetype.ConstraintViolationException;
import javax.jcr.version.VersionException;
import java.io.Serializable;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * This model represents a tag. It translates the tags from the Value[] to a 
 * String needed by the frontend and translates back if the frontend is updated
 * by the user.
 * 
 * Other than the name might suggest, this is not a wrapper of the Tag class.
 * 
 * @version $Id$
 *
 */
public class TagsModel extends Model {


    private static final long serialVersionUID = 1L;

    static final Logger log = LoggerFactory.getLogger(TagsModel.class);
    public static final String[] EMPTY_ARRAY = new String[]{};

    private JcrPropertyModel propertyModel;
    private LinkedHashSet<String> tags;
    private boolean toLowerCase = false;

    static final String TAG_SEPARATOR = ",";

    public TagsModel(JcrPropertyModel propertyModel) {
        this.propertyModel = propertyModel;
    }
    
    public TagsModel(String relPath) {
        this(new JcrPropertyModel(relPath));
    }

    public Set<String> getTags() {
        if (tags == null) {
            load();
        }
        return tags;
    }

    private String buildModelObject(LinkedHashSet<String> set) {
        StringBuilder buffer = new StringBuilder();
        for (String tag : set) {
            if (StringUtils.isBlank(tag)) {
                continue;
            }
            buffer.append(tag);
            buffer.append(TAG_SEPARATOR);
            buffer.append(' ');
        }
        return buffer.toString();
    }

    private String[] buildPropertyModel(String tags) {
        if (StringUtils.isBlank(tags)) {
            return EMPTY_ARRAY;
        }
        String[] tagsArray = tags.split(TAG_SEPARATOR);
        LinkedHashSet<String> set = new LinkedHashSet<String>();
        for (String tag : tagsArray) {
            if (StringUtils.isNotBlank(tag)) {
                set.add(tag.trim());
            }
        }
        return set.toArray(new String[set.size()]);
    }

    private LinkedHashSet<String> parsePropertyModel(JcrPropertyModel model) {
        LinkedHashSet<String> set = new LinkedHashSet<String>();
        // the property does not exist so don't even try
        if (model.getProperty() == null) {
            return set;
        }
        try {
            for (Value val : model.getProperty().getValues()) {
                set.add(val.getString());
            }
        } catch (ValueFormatException e) {
            log.error("Unsupported value in the JcrPropertyModel.", e);
        } catch (RepositoryException e) {
            log.error("Repository error", e);
        }
        return set;
    }

    /**
     * Parse new tag values
     * 
     * @param tag
     * @return
     */
    protected String parseTag(String tag) {
        if (tag != null) {
            tag = tag.trim();
            if (toLowerCase) {
                tag = tag.toLowerCase();
            }
        }
        return tag;
    }

    public void addTag(String tag) {
        if (tags == null) {
            load();
        }
        if (!Strings.isEmpty(tag.trim())) {
            log.debug("Adding tag: {}",  tag);
            tags.add(parseTag(tag));
            saveProperty(tags.toArray(new String[tags.size()]));
            super.setObject(buildModelObject(tags));
        }
    }

    private void saveProperty(String[] tags) {
        try {
            Property property = propertyModel.getProperty();
            if (property != null) {
                property.setValue(tags);
            } else {
                log.debug("Property for tags does not exist yet");
                JcrItemModel nodeModel = propertyModel.getItemModel().getParentModel();
                Node node = (Node) nodeModel.getObject();
                if (node == null) {
                    throw new IllegalStateException("Node to set tags property is null");
                }
                node.setProperty(TaggingNodeType.PROP_TAGS, tags);
                propertyModel.detach();
            }
        } catch (ValueFormatException e) {
            log.error("Failed trying to save invalid tag value.", e);
        } catch (VersionException e) {
            log.error("Versioning error while trying to save tag.", e);
        } catch (LockException e) {
            log.error("Locking error while trying to save tag.", e);
        } catch (ConstraintViolationException e) {
            log.error("Constraint violation while trying to save tag", e);
        } catch (RepositoryException e) {
            log.error("Repository error while trying to save tag.", e);
        }
    }

    /**
     * Sets the string object with comma separated tags but also updates
     * the hippostd:tags property of the document with an updated String array
     */
    public void setObject(final Object object) {
        if (tags == null) {
            load();
        }
        log.debug("setObject");

        saveProperty(buildPropertyModel((String)object));
        tags = parsePropertyModel(propertyModel);
    }
    
    @Override
    public Serializable getObject() {
        if (tags == null) {
            load();
        }
        return super.getObject();
    }

    protected void load() {
        tags = parsePropertyModel(this.propertyModel);
        super.setObject(buildModelObject(tags));
    }
    
    public void detach(){
        super.detach();
        propertyModel.detach();
        tags = null;
    }

    public void setToLowerCase(boolean toLowerCase) {
        this.toLowerCase = toLowerCase;
    }

    public boolean isToLowerCase() {
        return toLowerCase;
    }

}
