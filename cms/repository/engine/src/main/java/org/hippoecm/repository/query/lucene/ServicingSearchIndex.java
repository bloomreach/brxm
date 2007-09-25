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

import java.io.File;
import java.io.IOException;
import java.io.InputStream;

import javax.jcr.RepositoryException;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import org.apache.jackrabbit.core.query.lucene.IndexingConfigurationEntityResolver;
import org.apache.jackrabbit.core.query.lucene.MultiIndex;
import org.apache.jackrabbit.core.query.lucene.NamespaceMappings;
import org.apache.jackrabbit.core.query.lucene.SearchIndex;
import org.apache.jackrabbit.core.state.NodeState;
import org.apache.lucene.document.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Element;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

public class ServicingSearchIndex extends SearchIndex {

    /** The logger instance for this class */
    private static final Logger log = LoggerFactory.getLogger(ServicingSearchIndex.class);

    /**
     * The DOM with the indexing configuration or <code>null</code> if there
     * is no such configuration.
     */
    private Element indexingConfiguration;
    
    public ServicingSearchIndex() {
        super();
    }

    /**
     * Returns the multi index.
     *
     * @return the multi index.
     */
    public MultiIndex getIndex() {
        return super.getIndex();
    }
    
    // below used for parsing original xpath to get lucene query back
    //  public Query getLuceneQuery(ServicingSessionImpl session,
    //  String statement,
    //  String language)throws RepositoryException {
    //
    //  QueryRootNode root = QueryParser.parse(statement, language,
    //  session.getNamespaceResolver(), getQueryNodeFactory());
    //
    //  Query q = LuceneQueryBuilder.createQuery(root, session.getSessionImpl(),
    //  this.getContext().getItemStateManager(), this.getNamespaceMappings(),
    //  this.getTextAnalyzer(), this.getContext().getPropertyTypeRegistry(), this.getSynonymProvider());
    //
    //  return q;
    //}
    
    /**
     * Returns the document element of the indexing configuration or
     * <code>null</code> if there is no indexing configuration.
     *
     * @return the indexing configuration or <code>null</code> if there is
     *         none.
     */
    @Override
    protected Element getIndexingConfigurationDOM() {
       
        String configName = this.getIndexingConfiguration();
        if (indexingConfiguration != null) {
            return indexingConfiguration;
        }
        if (configName == null) {
            return null;
        }
        
        InputSource configurationInputSource = null;
        File config = null;
        
        if (!configName.startsWith("file:")) {
            log.info("Using resource repository indexing_configuration: " + configName);
            configurationInputSource = new InputSource(getIndexingConfigurationAsStream(configName));
            if(configurationInputSource == null) {
                log.warn("indexing configuration not found: " + getClass().getName() +"/"+ configName);
            }
        }
        else {
        // parse file name
            if (configName.startsWith("file://")) {
                configName = configName.substring(6);
            } else if (configName.startsWith("file:/")) {
                configName = configName.substring(5);
            } else if (configName.startsWith("file:")) {
                configName = "/" + configName.substring(5);
            }
            config = new File(configName);
            log.info("Using indexing configuration: " + configName);
            if (!config.exists()) {
                log.warn("File does not exist: " + this.getIndexingConfiguration());
                return null;
            } else if (!config.canRead()) {
                log.warn("Cannot read file: " + this.getIndexingConfiguration());
                return null;
            }
        }
    
        try {
            DocumentBuilderFactory factory =
                    DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();
            builder.setEntityResolver(new IndexingConfigurationEntityResolver());
            if(configurationInputSource != null){
                indexingConfiguration = builder.parse(configurationInputSource).getDocumentElement();
            } else if(config != null){
                indexingConfiguration = builder.parse(config).getDocumentElement();
            } else {
                log.warn("indexing configuration not found: " + configName);
            }
            
        } catch (ParserConfigurationException e) {
            log.warn("Unable to create XML parser", e);
        } catch (IOException e) {
            log.warn("Exception parsing " + this.getIndexingConfiguration(), e);
        } catch (SAXException e) {
            log.warn("Exception parsing " + this.getIndexingConfiguration(), e);
        } 
        return indexingConfiguration;
    }

    private InputStream getIndexingConfigurationAsStream(String configName){
        return getClass().getResourceAsStream(configName);
    }
    
    @Override
    protected Document createDocument(NodeState node, NamespaceMappings nsMappings) throws RepositoryException {
        ServicingNodeIndexer indexer = new ServicingNodeIndexer(node,
                getContext().getItemStateManager(), nsMappings, super.getTextExtractor());
        indexer.setSupportHighlighting(super.getSupportHighlighting());
        indexer.setServicingIndexingConfiguration((ServicingIndexingConfiguration)super.getIndexingConfig());
        Document doc = indexer.createDoc(); 
        mergeAggregatedNodeIndexes(node, doc);
        return doc;
    }
  
}
