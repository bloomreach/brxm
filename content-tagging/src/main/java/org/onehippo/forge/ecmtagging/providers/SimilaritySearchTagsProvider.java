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
package org.onehippo.forge.ecmtagging.providers;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Value;
import javax.jcr.query.Query;
import javax.jcr.query.Row;
import javax.jcr.query.RowIterator;

import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.frontend.plugin.IPluginContext;
import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.onehippo.forge.ecmtagging.Tag;
import org.onehippo.forge.ecmtagging.TagCollection;
import org.onehippo.forge.ecmtagging.TaggingNodeType;
import org.hippoecm.repository.api.HippoQuery;

/**
 * Uses the similarity search query from Jackrabbit to retrieve tags from
 * documents similar to the current document
 * 
 * @author Jeroen Tietema
 *
 */
public class SimilaritySearchTagsProvider extends AbstractTagsProvider {
    private static final long serialVersionUID = 1L;

    public final static String SCORE = "score";
    public final static double DEFAULT_SCORE = 1.0;
    public static final int RESULT_LIMIT = 25;

    private double score;

    public SimilaritySearchTagsProvider(IPluginContext context, IPluginConfig config) {
        super(context, config);
        score = config.getDouble(SCORE, DEFAULT_SCORE);
    }

    @Override
    public TagCollection getTags(JcrNodeModel nodeModel) throws RepositoryException {
        TagCollection collection = new TagCollection();
        Node parentNode = nodeModel.getNode().getParent();
        
        String statement = "//element(*, hippo:document)[rep:similar(., '" + nodeModel.getNode().getPath() + "')]";
        if (log.isDebugEnabled()) {
        	log.debug("Executing query: " + statement);
        }
        HippoQuery query = (HippoQuery) nodeModel.getNode().getSession().getWorkspace().getQueryManager().createQuery(statement,
                Query.XPATH);
		query.setLimit(RESULT_LIMIT);
        
        RowIterator r = query.execute().getRows();
        int i = 0;
        while (r.hasNext()) {
            // retrieve the query results from the row
            Row row = r.nextRow();
            String path = row.getValue("jcr:path").getString();
            long score = row.getValue("jcr:score").getLong();
            
            // retrieve the found document from the repository for tags extraction
            Node document = (Node) nodeModel.getNode().getSession().getItem(path);
            if (document.hasProperty(TaggingNodeType.PROP_TAGS)) {
            	// same parent? skip
            	if (parentNode.isSame(document.getParent())) {
            		continue;
            	}
	            if (log.isDebugEnabled()) {
	            	log.debug("Found document {}",  document.getPath());
	            }
                for (Value vTag : document.getProperty(TaggingNodeType.PROP_TAGS).getValues()){
                	if (log.isDebugEnabled()) {
                		log.debug("Found tag: {}", vTag.getString());
                	}
                    collection.add(new Tag(vTag.getString(), this.score*score));
                }
            }
            i++;
        }

        return collection;
    }
}
