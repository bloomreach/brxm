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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.jcr.NamespaceException;

import org.apache.jackrabbit.spi.Name;
import org.apache.jackrabbit.spi.commons.conversion.IllegalNameException;
import org.hippoecm.repository.dataprovider.HippoVirtualProvider;

public class FacetFilters {

    // the only operators we currently support
    public static final Operator CONTAINS_OPERATOR = Operator.CONTAINS;
    public static final Operator NOTEQUAL_OPERATOR = Operator.NOTEQUAL;
    public static final Operator EQUAL_OPERATOR = Operator.EQUAL;
    public static final Operator NOOP_OPERATOR = Operator.NOOP;

    public static  final Map<String, Operator> operatorsByName = new HashMap<String, Operator>();
    {
        operatorsByName.put(CONTAINS_OPERATOR.name, CONTAINS_OPERATOR);
        operatorsByName.put(NOTEQUAL_OPERATOR.name, NOTEQUAL_OPERATOR);
        operatorsByName.put(EQUAL_OPERATOR.name, EQUAL_OPERATOR);
        operatorsByName.put(NOOP_OPERATOR.name, NOOP_OPERATOR);
    }
    
    private  List<FacetFilter> facetFilters = new ArrayList<FacetFilter>();
    
    /**
     * tries to parse this these filters to a FacetedFilters object. If this fails, an IllegalArgumentException will be thrown
     * @param filter
     * @param provider temporarily used to parse a prefix:localname property to a jackrabbit {namespace}localname property
     * @throws IllegalArgumentException
     */
    public FacetFilters(String[] filters, HippoVirtualProvider provider) throws IllegalArgumentException {
        for(String filter : filters) {
            facetFilters.add(new FacetFilter(filter, provider));
        }
    }

    private FacetFilters() {
        // meant for internal use only
    }

    public List<FacetFilter> getFilters(){
        return this.facetFilters;
    }
    
    /**
     * Parses a string for a FacetedFilter.
     *
     * @param facetFilterString the formatted FacetedFilter String to parse.
     * @return FacetedFilter if the facetFilterString can be parsed to a FacetedFilter
     * @throws IllegalArgumentException the String must be a properly formatted
     *                                  
     */
    public static FacetFilters fromString(String facetFilters) throws IllegalArgumentException {
        FacetFilters ffs = new FacetFilters();
        String[] split = facetFilters.split(String.valueOf(FacetedNavigationEngine.Query.FILTER_DELIMITER));
        for(String filter : split) {
            ffs.facetFilters.add(FacetFilter.fromString(filter));
        }
        return ffs;
    }
    
    /**
     * Returns a formatted string representation of the FacetedFilter. When using {@link #fromString(String)} the original
     * FacetFilters object is rebuild
     * @return a formatted string representation of the FacetedFilter 
     */
    @Override
    public String toString(){
        StringBuilder builder = new StringBuilder();
        for(FacetFilter filter : this.facetFilters) {
            if(builder.length() > 0) {
                builder.append(FacetedNavigationEngine.Query.FILTER_DELIMITER);
            }
            builder.append(filter.toString());
        }
        return builder.toString();
    }
    
    public static class FacetFilter {

        public static final String IGNORE_NOTEQUAL_OPERATOR = "\\!=";
        public static final String IGNORE_EQUAL_OPERATOR = "\\!=";
        public static final char IGNORE_NOTEQUAL_OPERATOR_DELIM = '\uFAFF';
        public static final char IGNORE_EQUAL_OPERATOR_DELIM = '\uFBFF';
        
        private static final char INSTANCE_ATTR_DELIM = '\uFCFF';

        public static final String NOT_OPERATOR = "not()";
        
        public Operator operator = null; 
        public String queryString = null;
        public String namespacedProperty = null;
        public boolean negated = false;
        
        public FacetFilter(String filter, HippoVirtualProvider provider) throws IllegalArgumentException {
            String origFilter = filter;
            filter = filter.trim();
            String lcaseFilter = filter.toLowerCase(); 
            if(lcaseFilter.startsWith(NOT_OPERATOR.substring(0, NOT_OPERATOR.length() -1)) && lcaseFilter.endsWith(NOT_OPERATOR.substring(NOT_OPERATOR.length() -1)) ) {
                // remove not( AND )
                filter = filter.substring(NOT_OPERATOR.length() -1);
                filter = filter.substring(0, filter.length() -1);
                negated = true;
            }
            if(filter.indexOf(IGNORE_NOTEQUAL_OPERATOR) > -1) {
                filter.replaceAll(IGNORE_NOTEQUAL_OPERATOR, String.valueOf(IGNORE_NOTEQUAL_OPERATOR_DELIM));
            }
            if(filter.indexOf(IGNORE_EQUAL_OPERATOR) > -1) {
                filter.replaceAll(IGNORE_EQUAL_OPERATOR , String.valueOf(IGNORE_EQUAL_OPERATOR_DELIM));
            }
            
            for(Operator op: Operator.SUPPORTED_OPERATORS) {
                if(lcaseFilter.indexOf(op.operator) > -1) {
                    if(lcaseFilter.indexOf(op.operator) != lcaseFilter.lastIndexOf(op.operator)) {
                        throw new IllegalArgumentException("Cannot have 2 operators: '"+op.operator+"' occurs more then once in '"+origFilter+"'");
                    }
                    operator = op;
                    // found operator, stop
                    break;
                }
            }
            
            if(operator == null) {
                // do nothing: the string will be parsed by the Lucene QueryParser  
                operator = NOOP_OPERATOR;
                queryString = filter;
            } else if (operator == EQUAL_OPERATOR || operator == NOTEQUAL_OPERATOR){
                String[] split = filter.split(operator.operator);
                String jcrPropertyName = split[0].trim();
                queryString = split[1].trim();
                try {
                    Name qName = provider.resolveName(jcrPropertyName);
                    namespacedProperty = qName.toString();
                } catch (IllegalNameException e) {
                    throw new IllegalArgumentException("Unsupported property '"+namespacedProperty+"' found in '"+origFilter+"': "+e.getMessage()+"");
                } catch (NamespaceException e) {
                    throw new IllegalArgumentException("Unsupported property '"+namespacedProperty+"' found in '"+origFilter+"': "+e.getMessage()+"");
                }
            } else if(operator == CONTAINS_OPERATOR) {
                if(filter.startsWith(CONTAINS_OPERATOR.operator) && filter.endsWith(")")) {
                    filter = filter.substring(CONTAINS_OPERATOR.operator.length());
                    filter = filter.substring(0, filter.length() -1);
                    
                    // we now should have a format like:  prefix:localname , search terms  if we want to search in some property
                    // and it will be  . , search terms when we need to search in all fields.
                    
                    filter = filter.trim();
                    if(filter.indexOf(",") > -1 && filter.lastIndexOf(",") == filter.indexOf(",") ){
                        // we have a single comma ,
                        String[] split = filter.split(",");
                        String jcrPropertyName = split[0].trim();
                        queryString = split[1].trim();
                        if(jcrPropertyName.equals(".")) {
                            // we effectively have the same as the NOOP_OPERATOR, and only the right terms
                            operator = NOOP_OPERATOR;
                        } else {
                            if(jcrPropertyName.indexOf("$") > -1) {
                                // Not yet supported is searching in 'innerpart' properties, like hippo:date$year
                                throw new IllegalArgumentException("We do not (yet) support contains() in combination with a 'partial' property, like 'hippo:date$year' ");
                            } else {
                                try {
                                    Name qName = provider.resolveName(jcrPropertyName);
                                    namespacedProperty = qName.toString();
                                } catch (IllegalNameException e) {
                                    throw new IllegalArgumentException("Unsupported property found in contains() for property '"+namespacedProperty+"' in '"+origFilter+"' : "+e.getMessage()+"");
                                } catch (NamespaceException e) {
                                    throw new IllegalArgumentException("Unsupported property found in contains() for property '"+namespacedProperty+"' in '"+origFilter+"' : "+e.getMessage()+"");
                                }
                            }
                        }
                        if(queryString.startsWith("*") || queryString.startsWith("?")) {
                            throw new IllegalArgumentException("Invalid filter is '"+origFilter+"'  . On purpose we do not support prefix wildcard searches as they blow up in inverted indexes such as lucene");
                        }
                    } else {
                        throw new IllegalArgumentException("Invalid filter is '"+origFilter+"'  .When using the 'contains(my:prop,foo bar)' format, in contains only a single comma ',' is allowed");
                    }
                } else {
                    throw new IllegalArgumentException("Invalid filter is '"+origFilter+"'  .When using 'contains(my:prop,foo bar)', the filter must start with 'contains(' and end with ')'");
                }
            }   
            if(operator == null || queryString == null || (operator != NOOP_OPERATOR && namespacedProperty == null) ) {
                throw new IllegalArgumentException("Could not parse filter '"+origFilter+"'");
            }
            
            // restore possibly escaped operators
            if(queryString.indexOf(IGNORE_NOTEQUAL_OPERATOR_DELIM) > -1) {
                queryString.replaceAll(String.valueOf(IGNORE_NOTEQUAL_OPERATOR_DELIM), IGNORE_NOTEQUAL_OPERATOR);
            }
            if(queryString.indexOf(IGNORE_EQUAL_OPERATOR_DELIM) > -1) {
                queryString.replaceAll(String.valueOf(IGNORE_EQUAL_OPERATOR_DELIM), IGNORE_EQUAL_OPERATOR);
            }
        }
        
        private FacetFilter() {
           // only meant for internal use
        }

        /*
         * The string format is: operator:delim:querystringdelim:namespacedproperty where the namespacedproperty is only available if present
         */
        @Override
        public String toString(){
            StringBuilder builder = new StringBuilder();
            builder.append(this.operator.name);
            builder.append(INSTANCE_ATTR_DELIM).append(queryString);
            builder.append(INSTANCE_ATTR_DELIM).append(negated);
            if(namespacedProperty != null) {
                builder.append(INSTANCE_ATTR_DELIM).append(namespacedProperty);
            }
            return builder.toString();
        }
        
        public static FacetFilter fromString(String filter) throws IllegalArgumentException {
            String[] split = filter.split(String.valueOf(INSTANCE_ATTR_DELIM));
            if(split.length != 3 && split.length != 4) {
                throw new IllegalArgumentException("Unable to create a FacetFilter from '"+filter+"'");
            }
            FacetFilter ff = new FacetFilter();
            ff.operator = getOperatorByKey(split[0]);
            ff.queryString = split[1];
            ff.negated = Boolean.parseBoolean(split[2]);
            ff.namespacedProperty = (split.length == 4) ? split[3] : null ;
            
            if(ff.operator == null || ff.queryString == null) {
                throw new IllegalArgumentException("Unable to create a FacetFilter from '"+filter+"'");
            }
            
            return ff;
        }
    }
    
    static public Operator getOperatorByKey(String operatorName) {
        return operatorsByName.get(operatorName);
    }
    
    
    
    static public final class Operator {

        static public final Operator CONTAINS = new Operator("CONTAINS", "contains(");
        static public final Operator EQUAL = new Operator("EQUAL", "=");
        static public final Operator NOTEQUAL = new Operator("NOTEQUAL", "!=");
        static public final Operator NOOP = new Operator("NOOP", "");
        
        // the order of operater array is important, as it will be used in this order
        static public final Operator[] SUPPORTED_OPERATORS = {CONTAINS, NOTEQUAL, EQUAL};
        String name; 
        String operator; 
        
        public Operator(String name, String operator) {
          this.name = name;
          this.operator = operator;
        }
        
        @Override
        public String toString(){
          return this.name;   
        }
        

        
      }
    
}
