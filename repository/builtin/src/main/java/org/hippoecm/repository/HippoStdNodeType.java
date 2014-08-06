/*
 *  Copyright 2009-2013 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.repository;

/*
 * This file has to be kept in sync with:
 * src/main/resources/hippostd.cnd
 */

/**
 * This interface defines the node types and item names that are in use by
 * standard extensions of the Hippo repository.
 */
public interface HippoStdNodeType {

    /**
     * Within the hippo document model, there are two basic default container documents that can hold a set of other documents,
     * akin to directories or folders on a file-system.  One is a document of JCR primary type hippostd:directory which refers
     * to an unordered set of documents.
     * @see #NT_FOLDER
     */
    String NT_DIRECTORY = "hippostd:directory";

    /**
     * The hippostd:folder JCR primary type is the second of two basic default container documents, which refers to a ordered
     * set of documents.
     * @see #NT_DIRECTORY
     */
    String NT_FOLDER = "hippostd:folder";

    /**
     * The hippostd:html JCR primary type is used within documents as a child-node that holds HTML content.  The HTML is contained
     * within a single property, while the internal links to other documents in the HTML content are duplicated as nodes that
     * defer to the other documents.
     */
    String NT_HTML = "hippostd:html";
    
    /**
     * The hippostd:date JCR primary type is used to contain dates, separated out in their constituent fields (year, month, etcetera).
     * It is typically used within documents to allow for faceted navigation using the individual fields, or even fields derived from
     * them (e.g. week number, quarter).
     */
    String NT_DATE = "hippostd:date";
    
    /**
     * The hippostd:languageable is a JCR mix-in type that can be added to documents indicating that the document can have multiple
     * variants for different languages.  This adds a field to the documents indicating the language the document variant is in,
     * and is used in the work-flow process to add logic for the language process.
     */
    String NT_LANGUAGEABLE = "hippostd:languageable";
    
    /**
     * The hippostd:publishable is a JCR mix-in type that can be added to documents indicating that the document can be in different
     * publication state, meaning whether documents should appear on all web-sites, just preview and/or are being edited.
     */
    String NT_PUBLISHABLE = "hippostd:publishable";
    
    /**
     * The hippostd:publishableSummary is a JCR mix-in type that can be placed on documents that are already hippostd:publishable
     * and provides a human-readable description of the overall state of all related document states.
     */
    String NT_PUBLISHABLESUMMARY = "hippostd:publishableSummary";
    
    /**
     * The hippostd:translations is a JCR mix-in type that can be added some other node types amongst which documents, indicating
     * that besides the node name itself, a number of internationalized, personalized, or otherwise sub-classed names are available
     * for human consumption.  The actual translated names are properties of child-nodes that introduced by this mix-in type.
     */
    String NT_TRANSLATIONS = "hippostd:translations";
    
    /**
     * The hippostd:container is a JCR mix-in type that can be added to document variants which allows the document to contain
     * unstructured child nodes with either date (hippostd:date) or HTML (hippostd:html) content.
     */
    String NT_CONTAINER = "hippostd:container";
    
    /**
     * The hippostd:relaxed is a JCR mix-in type that can be added to document variants which allows the document to contain\
     * unstructured properties of any name and type.
     */
    String NT_RELAXED = "hippostd:relaxed";

    /**
     * The hippostd:gallery type
     */
    String NT_GALLERY = "hippostd:gallery";

    /**
     * The property in a hippostd:html node instance that hold the HTML fragment.
     */
    String HIPPOSTD_CONTENT = "hippostd:content";
    
    /**
     * The user-id that currently holds the document.  Holding a document can have various meaning, depending on the document
     * type and variant.  The most common case is the user editing a document.
     */
    String HIPPOSTD_HOLDER = "hippostd:holder";
    
    /**
     * The property holding the language code as defined by ISO-639-1, used for instance in a hippostd:languageable node type.
     */
    String HIPPOSTD_LANGUAGE = "hippostd:language";

    /**
     * The property defining the state of a document as defined by hippostd:publishable node type.
     */
    String HIPPOSTD_STATE = "hippostd:state";

    /**
     * The property containing the human readable summary of all states of the document variants as defined by
     * hippostd:publishableSummary node type.
     */
    String HIPPOSTD_STATESUMMARY = "hippostd:stateSummary";
    
    /**
     * The child node name defined by a hippostd:translated containing the translated names.  Multiple translations,
     * due to internationalization or other diversification of the name are placed as same-name-siblings below the
     * node as defined by the hippostd:translated mix-in type.  The actual individual name is placed in a property
     * of these child nodes to allow more characters than a name of a JCR node allows.
     */
    String HIPPOSTD_TRANSLATIONS = "hippostd:translations";

    /**
     * The actual datetime in a hippostd:date JCR primary node type.  This is the date actually set, normally the other
     * fields, like month, year, etcetera are autocomputed from the date set to this property of the hippostd:date node.
     */
    String HIPPOSTD_DATE = "hippostd:date";
    
    /**
     * The property in a hippostd:date that contains the month as an integer representation according to #java.util.Calendar.
     */
    String HIPPOSTD_MONTH = "hippostd:month";
    
    /**
     * The property in a hippostd:date that contains the year as an integer (normally four digits), also according to #java.util.Calendar.
     */
    String HIPPOSTD_YEAR = "hippostd:year";

    /**
     * The property in a hippostd:date that contains the day-of-the-year as an integer according to #java.util.Calendar rules.
     */
    String HIPPOSTD_DAYOFYEAR = "hippostd:dayofyear";
    
    /**
     * The property in a hippostd:date that contains the week-of-the-year as an integer according to #java.util.Calendar.
     */
    String HIPPOSTD_WEEKOFYEAR = "hippostd:weekofyear";
    
    /**
     * The property in a hippostd:date that contains the day-of-the-week as an integer according to #java.util.Calendar.
     */
    String HIPPOSTD_DAYOFWEEK = "hippostd:dayofweek";

    /**
     * The property in a hippostd:second that contains the number of seconds as an integer according to #java.util.Calendar.
     */
    String HIPPOSTD_SECOND = "hippostd:second";

    /**
     * The property defining the type of this hippostd:gallery
     */
    String HIPPOSTD_GALLERYTYPE = "hippostd:gallerytype";

    /**
     * One of the possible values a hippostd:state property can hold, indicating a document variant is to be made available
     * on a live web-site and possibly a preview web-site when no specific document variant is available.
     */
    String PUBLISHED = "published";
    
    /**
     * One of the possible values a hippostd:state property can hold, indicating a document variant is to be made available
     * on a preview web-site only.
     */
    String UNPUBLISHED = "unpublished";
    
    /**
     * One of the possible values a hippostd:state property can hold, indicating a document variant is being edited and
     * should not be available for web-sites.
     */
    String DRAFT = "draft";
    
    String NEW = "new";
}
