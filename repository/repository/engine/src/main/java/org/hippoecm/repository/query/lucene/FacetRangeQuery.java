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
package org.hippoecm.repository.query.lucene;

import java.util.Calendar;
import java.util.List;

import javax.jcr.PropertyType;

import org.apache.jackrabbit.core.query.lucene.NamespaceMappings;
import org.apache.jackrabbit.spi.commons.conversion.IllegalNameException;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.ConstantScoreRangeQuery;
import org.apache.lucene.search.BooleanClause.Occur;
import org.hippoecm.repository.FacetRange;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FacetRangeQuery {
    @SuppressWarnings("unused")
    private final static String SVN_ID = "$Id$";

    /**
     * The logger instance for this class
     */
    private static final Logger log = LoggerFactory.getLogger(FacetRangeQuery.class);

    /**
     * The lucene query
     */
    private BooleanQuery query;

    public FacetRangeQuery(List<FacetRange> rangeQuery, NamespaceMappings nsMappings, ServicingSearchIndex searchIndex) {
        this.query = new BooleanQuery(true);

        if (rangeQuery != null) {
            for(FacetRange facetRange : rangeQuery) {
                try {
                    String internalName = ServicingNameFormat.getInteralPropertyPathName(nsMappings, facetRange.getNamespacedProperty());
                  
                    int type = facetRange.getRangeType();
                    
                    switch (type) {
                        case PropertyType.DATE:
                            HippoDateTools.Resolution resolution = HippoDateTools.Resolution.RESOLUTIONSMAP.get(facetRange.getResolution());
                            if(resolution == null) {
                                log.warn("Unknown resolution : '{}'. Skip range", facetRange.getResolution());
                            }
                            String compoundInternalName = internalName+ServicingFieldNames.DATE_RESOLUTION_DELIMITER+facetRange.getResolution();
                            String internalFacetName = ServicingNameFormat.getInternalFacetName(compoundInternalName);
                            Calendar calBegin = Calendar.getInstance();
                            Calendar calEnd = Calendar.getInstance();
                            calBegin.add(resolution.getCalendarField(), (int)facetRange.getBegin());
                            calEnd.add(resolution.getCalendarField(), (int)facetRange.getEnd());
                            
                            String begin = HippoDateTools.timeToString(calBegin.getTimeInMillis(), resolution);
                            String end = HippoDateTools.timeToString(calEnd.getTimeInMillis(), resolution);
                            
                            ConstantScoreRangeQuery constantScoreRangeQuery = new ConstantScoreRangeQuery(internalFacetName, begin, end, true, false);
                            query.add(constantScoreRangeQuery, Occur.MUST);
    
                             break;
                        default:
                            log.warn("Range faceted browsing is not supported for property type belonging to '{}'", facetRange.getNamespacedProperty());
                            break;
                    }
                } catch (IllegalNameException e) {
                    log.error(e.toString());
                }  
            }
        }
    }

    public BooleanQuery getQuery() {
        return query;
    }
}
