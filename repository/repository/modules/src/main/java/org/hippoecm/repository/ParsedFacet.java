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
package org.hippoecm.repository;

import java.util.ArrayList;
import java.util.List;

import net.sf.json.JSONArray;

import org.apache.jackrabbit.spi.Name;
import org.hippoecm.repository.jackrabbit.HippoVirtualProvider;

public class ParsedFacet {
    @SuppressWarnings("unused")
    private final static String SVN_ID = "$Id$";

    String displayFacetName;
    
    // property in format {namespace}localname
    String namespacedProperty;
    
    String rangeConfig;
    List<FacetRange> facetRanges;
    
    public ParsedFacet(String facetNameConfig, String facetNodeName, HippoVirtualProvider provider) throws Exception{
        
        displayFacetName = facetNameConfig;
        boolean mapped = false;
        // jcrPropertyName is format: prefix:localname
        String jcrPropertyName = facetNameConfig;
        if(facetNodeName != null && !"".equals(facetNodeName)) {
            displayFacetName = facetNodeName;
            mapped = true;
        }
        if(facetNameConfig.indexOf("$[") > -1) {
            try {
                rangeConfig = facetNameConfig.substring(facetNameConfig.indexOf("$[")+1);
                JSONArray jsonArray = JSONArray.fromObject( rangeConfig );  
                facetRanges = (List<FacetRange>)JSONArray.toCollection(jsonArray, FacetRange.class);
                jcrPropertyName = facetNameConfig.substring(0, facetNameConfig.indexOf("$["));
                if(!mapped) {
                    displayFacetName = jcrPropertyName;
                } 
                validate(facetRanges);
            } catch (IllegalArgumentException e) { 
               throw (e);
            } catch (Exception e) {
                throw new Exception("Malformed facet range configuration '"+facetNameConfig+"'. Valid format is 'myproject:mydate$[{name:'lastweek', range:'-1w'}, {name:'lastmonth', range:'-1m'}]'", e); 
            }
        }
        Name qName = provider.resolveName(jcrPropertyName);
        this.namespacedProperty = qName.toString();
        if(facetRanges != null) {
            for(FacetRange range : facetRanges) {
                range.setNamespacedProperty(namespacedProperty);
            }
        }
    }
    
    private void validate(List<FacetRange> ranges) throws IllegalArgumentException{
        List<String> usedNames = new ArrayList<String>();
        for(FacetRange range : ranges) {
            if(range.getName() == null) {
                throw new IllegalArgumentException("Name is not allowed to be null in range configuration");
            }
            if(usedNames.contains(range.getName())) {
                throw new IllegalArgumentException("Two range with same 'name' are not allowed: '"+range.getName()+"' is duplicate");
            }
            usedNames.add(range.getName());
            
            if(range.getResolution() == null) {
                throw new IllegalArgumentException("Resolution must be set in range"); 
            }
            if(range.getBegin() > range.getEnd()) {
                throw new IllegalArgumentException("Unsupported range for '"+range.getName()+"': 'begin' cannot be larger than 'end'");
            }
        }
    }

    public ParsedFacet(String facetNameConfig, String facetNodeName) throws Exception{
        //in this case, the facetNameConfig is already namespaced
        displayFacetName = facetNameConfig;
        boolean mapped = false;
        // jcrPropertyName is format: prefix:localname
        this.namespacedProperty = facetNameConfig;
        if(facetNodeName != null && !"".equals(facetNodeName)) {
            displayFacetName = facetNodeName;
            mapped = true;
        }
        if(facetNameConfig.indexOf("$[") > -1) {
            try {
                rangeConfig = facetNameConfig.substring(facetNameConfig.indexOf("$[")+1);
                JSONArray jsonArray = JSONArray.fromObject( rangeConfig );  
                facetRanges = (List<FacetRange>)JSONArray.toCollection(jsonArray, FacetRange.class);
                this.namespacedProperty = facetNameConfig.substring(0, facetNameConfig.indexOf("$["));
                if(!mapped) {
                    displayFacetName = this.namespacedProperty;
                }
            } catch (Exception e) {
                throw new Exception("Malformed facet range configuration '"+facetNameConfig + "'", e); 
            }
        }
        if(facetRanges != null) {
            for(FacetRange range : facetRanges) {
                range.setNamespacedProperty(namespacedProperty);
            }
        }
    }
    
    public String getDisplayFacetName() {
        return displayFacetName;
    }
    public String getRangeConfig() {
        return rangeConfig;
    }
    public List<FacetRange> getFacetRanges() {
        return facetRanges;
    }

    public String getNamespacedProperty() {
        return namespacedProperty;
    }
    
}
