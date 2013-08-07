/*
 *  Copyright 2010-2013 Hippo B.V. (http://www.onehippo.com)
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
import java.util.Collections;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;

import net.sf.json.JSONArray;

import org.apache.jackrabbit.spi.Path;
import org.hippoecm.repository.dataprovider.HippoVirtualProvider;

public class ParsedFacet {

    public static final String VALID_RANGE_EXAMPLE = "hippo:date$[{name:'this week', resolution:'week', begin:-1, end:0}, {name:'last 7 days', resolution:'day', begin:-7, end:0 }]";

    
    /*
     * A cache shared between all threads that keep parsedFacets as they are expensive to create 
     */
    private static final Map<String, ParsedFacet> sharedCache = Collections.synchronizedMap(new LRUMap<String, ParsedFacet>());
    
    String displayFacetName;

    // property in format {namespace}localname
    String namespacedProperty;
    
    // jcrPropertyName is format: prefix:localname
    String jcrPropertyName;

    String rangeConfig;
    List<FacetRange> facetRanges;

    int hashCode;
    
    
    public static final ParsedFacet     getInstance(String facetNameConfig, String facetNodeName, HippoVirtualProvider provider) throws Exception {
        String cacheKey = facetNameConfig + '\uFFFF' + facetNodeName;        
        ParsedFacet pf = sharedCache.get(cacheKey);   
        if (pf == null) {
            pf = new ParsedFacet(facetNameConfig, facetNodeName, provider);
            sharedCache.put(cacheKey,pf);
        } else {
            // we need to check if the namespace did not bump or change. IF so, we evict this item from cache.
            Path currentQName = provider.resolvePath(pf.jcrPropertyName);
            // if the currentQName is equal to pf.namespacedProperty , there was no namespace bumb and we can return pf
            if (!currentQName.toString().equals(pf.namespacedProperty)) {
                pf = new ParsedFacet(facetNameConfig, facetNodeName, provider);
                sharedCache.put(cacheKey,pf);
            }
        } 
        return pf;
    }
    
    public static final ParsedFacet getInstance(String namespacedFacetConfig) throws Exception{
        ParsedFacet pf = sharedCache.get(namespacedFacetConfig);   
        if (pf == null) {
            pf = new ParsedFacet(namespacedFacetConfig);
            sharedCache.put(namespacedFacetConfig,pf);
        } 
        return pf;
    }
    
    public ParsedFacet(String facetNameConfig, String facetNodeName, HippoVirtualProvider provider) throws Exception {
        displayFacetName = facetNameConfig;
        boolean mapped = false;
        // jcrPropertyName is format: prefix:localname
        jcrPropertyName = facetNameConfig;
        if (facetNodeName != null && !"".equals(facetNodeName)) {
            displayFacetName = facetNodeName;
            mapped = true;
        }
        if (facetNameConfig.indexOf("$[") > -1) {
            try {
                rangeConfig = facetNameConfig.substring(facetNameConfig.indexOf("$[") + 1);
                JSONArray jsonArray = JSONArray.fromObject(rangeConfig);
                facetRanges = (List<FacetRange>) JSONArray.toCollection(jsonArray, FacetRange.class);
                jcrPropertyName = facetNameConfig.substring(0, facetNameConfig.indexOf("$["));
                if (!mapped) {
                    displayFacetName = jcrPropertyName;
                }
                validate(facetRanges);
            } catch (IllegalArgumentException e) {
                throw (e);
            } catch (Exception e) {
                throw new Exception("Malformed facet range configuration '" + facetNameConfig
                        + "'. Valid format is for example '" + VALID_RANGE_EXAMPLE + "'", e);
            }
        }
        Path qName = provider.resolvePath(jcrPropertyName);
        this.namespacedProperty = qName.toString();
        if (facetRanges != null) {
            for (FacetRange range : facetRanges) {
                range.setNamespacedProperty(namespacedProperty);
            }
        }
    }
    
    public ParsedFacet(String namespacedFacetConfig) throws Exception {
        //in this case, the facetNameConfig is already namespaced
        displayFacetName = namespacedFacetConfig;
        boolean mapped = false;
        // jcrPropertyName is format: prefix:localname
        this.namespacedProperty = namespacedFacetConfig;
        if (namespacedFacetConfig.indexOf("$[") > -1) {
            try {
                rangeConfig = namespacedFacetConfig.substring(namespacedFacetConfig.indexOf("$[") + 1);
                JSONArray jsonArray = JSONArray.fromObject(rangeConfig);
                facetRanges = (List<FacetRange>) JSONArray.toCollection(jsonArray, FacetRange.class);
                this.namespacedProperty = namespacedFacetConfig.substring(0, namespacedFacetConfig.indexOf("$["));
                if (!mapped) {
                    displayFacetName = this.namespacedProperty;
                }
            } catch (Exception e) {
                throw new Exception("Malformed facet range configuration '" + namespacedFacetConfig + "'", e);
            }
        }
        if (facetRanges != null) {
            for (FacetRange range : facetRanges) {
                range.setNamespacedProperty(namespacedProperty);
            }
        }
    }

    private void validate(List<FacetRange> ranges) throws IllegalArgumentException {
        List<String> usedNames = new ArrayList<String>();
        for (FacetRange range : ranges) {
            if (range.getName() == null) {
                throw new IllegalArgumentException("Name is not allowed to be null in range configuration");
            }
            if (usedNames.contains(range.getName())) {
                throw new IllegalArgumentException("Two range with same 'name' are not allowed: '" + range.getName()
                        + "' is duplicate");
            }
            usedNames.add(range.getName());

            if (range.getResolution() == null) {
                throw new IllegalArgumentException("Resolution must be set in range");
            }
            if (range.getBegin() > range.getEnd()) {
                throw new IllegalArgumentException("Unsupported range for '" + range.getName()
                        + "': 'begin' cannot be larger than 'end'");
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
    
    @Override
    public boolean equals(Object obj) {
       if(obj == null || !(obj instanceof ParsedFacet)) {
           return false;
       }
       if(this == obj) {
           return true;
       }
       
       ParsedFacet o = (ParsedFacet)obj;
       
       if ((displayFacetName != null && o.displayFacetName == null) || (o.displayFacetName != null && displayFacetName == null)) {
           return false;
       }
       if(displayFacetName != null && !displayFacetName.equals(o.displayFacetName)) {
           return false;
       }
       
       if ((jcrPropertyName != null && o.jcrPropertyName == null) || (o.jcrPropertyName != null && jcrPropertyName == null)) {
           return false;
       }
       if(jcrPropertyName != null && !jcrPropertyName.equals(o.jcrPropertyName)) {
           return false;
       }
       
       if ((namespacedProperty != null && o.namespacedProperty == null) || (o.namespacedProperty != null && namespacedProperty == null)) {
           return false;
       }
       if(rangeConfig != null && !rangeConfig.equals(o.rangeConfig)) {
           return false;
       }
       
       if ((displayFacetName != null && o.displayFacetName == null) || (o.displayFacetName != null && displayFacetName == null)) {
           return false;
       }
       if(displayFacetName != null && !displayFacetName.equals(o.displayFacetName)) {
           return false;
       }
       
       if(facetRanges == null && o.facetRanges == null) {
           return true;
       }
       if( (facetRanges != null && o.facetRanges == null) || (facetRanges == null && o.facetRanges != null)) {
           return false;
       }
       
       ListIterator<FacetRange> e1 = facetRanges.listIterator();
       ListIterator<FacetRange> e2 = o.facetRanges.listIterator();
       while(e1.hasNext() && e2.hasNext()) {
           FacetRange o1 = e1.next();
           FacetRange o2 = e2.next();
           if (!(o1==null ? o2==null : o1.equals(o2)))
           return false;
       }
       return !(e1.hasNext() || e2.hasNext());
    }

    @Override
    public int hashCode() {
        if(hashCode != 0) {
            return hashCode;
        }
        int result = 1;
        
        result = 31 * result + (displayFacetName == null ? 0 : displayFacetName.hashCode());
        result = 31 * result + (namespacedProperty == null ? 0 : namespacedProperty.hashCode());
        result = 31 * result + (rangeConfig == null ? 0 : rangeConfig.hashCode());
        result = 31 * result + (jcrPropertyName == null ? 0 : jcrPropertyName.hashCode());
        if(facetRanges != null) {
            for(FacetRange range : facetRanges) {
                result = 31 * result + range.hashCode();
            }
        }
        
        hashCode = result;
        return hashCode;
    }
    
}
