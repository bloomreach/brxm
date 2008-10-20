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
package org.hippoecm.frontend.plugins.tagging.providers;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.Property;
import javax.jcr.RepositoryException;
import javax.jcr.Value;
import javax.jcr.query.Query;
import javax.jcr.query.QueryManager;

import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.frontend.plugins.tagging.Tag;
import org.hippoecm.frontend.plugins.tagging.TagCollection;
import org.hippoecm.frontend.plugins.tagging.editor.TagsPlugin;

public class AllTagsProvider implements ITagsProvider {
    @SuppressWarnings("unused")
    private final static String SVN_ID = "$Id$";

    public TagCollection getTags(JcrNodeModel nodeModel) {
        TagCollection tags = new TagCollection();
        try {
            for (NodeIterator ni = query(nodeModel); ni.hasNext();) {
                Node node = ni.nextNode();
                if (node.hasProperty(TagsPlugin.FIELD_NAME)) {
                    Property tagsProperty = node.getProperty(TagsPlugin.FIELD_NAME);
                    Value[] values = tagsProperty.getValues();
                    for (Value value : values) {
                        Tag tag = new Tag(value.getString());
                        tags.add(tag);
                    }
                }
            }
        } catch (RepositoryException e) {
            e.printStackTrace();
        }
        return tags;
    }

    private NodeIterator query(JcrNodeModel nodeModel) throws RepositoryException {
        QueryManager queryManager = nodeModel.getNode().getSession().getWorkspace().getQueryManager();
        // does this work?? TODO needs testing
        Query query = queryManager.createQuery("//element(*,hippostd:taggable)", Query.XPATH);
        return query.execute().getNodes();
    }

}
