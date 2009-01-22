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
package org.hippoecm.hst.core.template.node.content;

import javax.jcr.Node;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PathTranslatorImpl implements PathTranslator {

    /*
     * log all rewriting to the SourceRewriter interface
     */
    private Logger log = LoggerFactory.getLogger(ContentRewriter.class);
    
    private PathToHrefTranslator pathToHrefTranslator;
    private PathToSrcTranslator pathToSrcTranslator;
    
    public PathTranslatorImpl(PathToHrefTranslator pathToHrefTranslator, PathToSrcTranslator pathToSrcTranslator) {
        this.pathToHrefTranslator = pathToHrefTranslator;
        this.pathToSrcTranslator = pathToSrcTranslator;
    }
    
    public String documentPathToHref(Node node, String documentPath,boolean externalize) {
        if(pathToHrefTranslator == null) {
            log.debug("pathToHrefTranslator is null, returning untranslated href");
            return documentPath;
        } else {
           return pathToHrefTranslator.documentPathToHref(node, documentPath, externalize);
        }
        
    }

    public String documentPathToSrc(Node node, String documentPath,boolean externalize) {
        if(pathToSrcTranslator == null) {
            log.debug("pathToSrcTranslator is null, returning untranslated href");
            return documentPath;
        } else {
            return pathToSrcTranslator.documentPathToSrc(node, documentPath, externalize);
        }
    }

}