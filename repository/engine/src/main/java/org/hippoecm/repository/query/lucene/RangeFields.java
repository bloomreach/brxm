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
package org.hippoecm.repository.query.lucene;

import java.util.Calendar;

import javax.jcr.PropertyType;

import org.apache.jackrabbit.core.query.lucene.DoubleField;
import org.apache.jackrabbit.core.query.lucene.LongField;
import org.hippoecm.repository.FacetRange;
import org.hippoecm.repository.util.DateTools;

public class RangeFields {

    public String begin = null;
    public String end = null;

    // important internalFacetName must be an interned String such that == comparison can be used
    public String internalFacetName;
    public String compoundInternalName;
    public String facetRangeName;
    public int type;

    public RangeFields(String internalName, FacetRange facetRange) throws IllegalArgumentException {
        this.type = facetRange.getRangeType();
        this.facetRangeName = facetRange.getName();
        switch (type) {
        case PropertyType.DATE: {
            DateTools.Resolution resolution = DateTools.Resolution.RESOLUTIONSMAP.get(facetRange
                    .getResolution());
            if (resolution == null) {
                throw new IllegalArgumentException("Unknown resolution : '" + facetRange.getResolution()
                        + "'. Skip range");
            }
            this.compoundInternalName = internalName + ServicingFieldNames.DATE_RESOLUTION_DELIMITER
                    + facetRange.getResolution();
            this.internalFacetName = ServicingNameFormat.getInternalFacetName(compoundInternalName).intern();

            if (facetRange.getBegin() != Double.NEGATIVE_INFINITY) {
                Calendar calBegin = Calendar.getInstance();
                calBegin.add(resolution.getCalendarField(), (int) facetRange.getBegin());
                this.begin = DateTools.timeToString(calBegin.getTimeInMillis(), resolution);
            }

            if (facetRange.getEnd() != Double.POSITIVE_INFINITY) {
                Calendar calEnd = Calendar.getInstance();
                calEnd.add(resolution.getCalendarField(), (int) facetRange.getEnd());
                this.end = DateTools.timeToString(calEnd.getTimeInMillis(), resolution);
            }
            break;
        }
        case PropertyType.DOUBLE: {
            this.compoundInternalName = internalName + ServicingFieldNames.DOUBLE_POSTFIX;
            this.internalFacetName = ServicingNameFormat.getInternalFacetName(compoundInternalName).intern();
            if (facetRange.getBegin() != Double.NEGATIVE_INFINITY) {
                this.begin = DoubleField.doubleToString(facetRange.getBegin());
            }

            if (facetRange.getEnd() != Double.POSITIVE_INFINITY) {
                this.end = DoubleField.doubleToString(facetRange.getEnd());
            }
            break;
        }

        case PropertyType.LONG: {
            this.compoundInternalName = internalName + ServicingFieldNames.LONG_POSTFIX;
            this.internalFacetName = ServicingNameFormat.getInternalFacetName(compoundInternalName).intern();
            if (facetRange.getBegin() != Double.NEGATIVE_INFINITY) {
                this.begin = LongField.longToString((long) facetRange.getBegin());
            }

            if (facetRange.getEnd() != Double.POSITIVE_INFINITY) {
                this.end = LongField.longToString((long) facetRange.getEnd());
            }
            break;
        }

        case PropertyType.STRING: {
            // see if there is an NGRAM possible for the ranges. We index up to the first 3 chars separately for efficient ranges on things like 'a' to 'b'

            if (facetRange.getLower() != null || facetRange.getUpper() != null) {
                this.compoundInternalName = internalName + ServicingFieldNames.STRING_DELIMITER;
                if (facetRange.getLower() != null) {
                    this.compoundInternalName += facetRange.getLower().length()
                            + ServicingFieldNames.STRING_CHAR_POSTFIX;
                } else {
                    this.compoundInternalName += facetRange.getUpper().length()
                            + ServicingFieldNames.STRING_CHAR_POSTFIX;
                }
                this.internalFacetName = ServicingNameFormat.getInternalFacetName(this.compoundInternalName).intern();
            } else {
                // no range, both are null: no delimiter needed
                this.compoundInternalName = internalName;
                this.internalFacetName = ServicingNameFormat.getInternalFacetName(internalName).intern();
            }

            if (facetRange.getLower() != null && !"".equals(facetRange.getLower())) {
                this.begin = facetRange.getLower().toLowerCase();
            }
            if (facetRange.getUpper() != null && !"".equals(facetRange.getUpper())) {
                this.end = facetRange.getUpper().toLowerCase();
            }

            break;
        }

        default:
            throw new IllegalArgumentException(
                    "Range faceted browsing is not supported for property type belonging to '"
                            + facetRange.getNamespacedProperty() + "'");
        }

    }
}
