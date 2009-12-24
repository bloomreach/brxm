package org.hippoecm.repository.jackrabbit.facetnavigation;

import java.util.ArrayList;
import java.util.List;

import javax.jcr.NamespaceException;
import javax.jcr.PropertyType;
import javax.jcr.RepositoryException;

import org.apache.jackrabbit.core.NodeId;
import org.apache.jackrabbit.core.PropertyId;
import org.apache.jackrabbit.core.state.NodeState;
import org.apache.jackrabbit.core.state.PropertyState;
import org.apache.jackrabbit.core.value.InternalValue;
import org.apache.jackrabbit.spi.Name;
import org.apache.jackrabbit.spi.commons.conversion.IllegalNameException;
import org.hippoecm.repository.OrderBy;
import org.hippoecm.repository.ParsedFacet;
import org.hippoecm.repository.api.HippoNodeType;
import org.hippoecm.repository.api.NodeNameCodec;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FacetNavigationProvider extends AbstractFacetNavigationProvider {

    private final Logger log = LoggerFactory.getLogger(FacetNavigationProvider.class);
    
	protected FacetsAvailableNavigationProvider facetsAvailableNavigationProvider = null;
	
    Name docbaseName;
    Name facetsName;
    Name facetNodeNamesName;

    Name facetLimit;
    Name facetSortBy;
    Name facetSortOrder;
    
	@Override
	protected void initialize() throws RepositoryException {
		super.initialize();
		facetsAvailableNavigationProvider = (FacetsAvailableNavigationProvider) lookup(FacetsAvailableNavigationProvider.class.getName());
		docbaseName = resolveName(HippoNodeType.HIPPO_DOCBASE);
        facetsName = resolveName(HippoNodeType.HIPPO_FACETS);
        facetNodeNamesName = resolveName(HippoNodeType.HIPPO_FACETNODENAMES);

        facetLimit = resolveName(HippoNodeType.HIPPO_FACETLIMIT);
        facetSortBy = resolveName(HippoNodeType.HIPPO_FACETSORTBY);
        facetSortOrder = resolveName(HippoNodeType.HIPPO_FACETSORTORDER);
        
		virtualNodeName = resolveName(HippoNodeType.NT_FACETSAVAILABLENAVIGATION);
		register(resolveName(HippoNodeType.NT_FACETNAVIGATION), virtualNodeName);
	}
	
	@Override
	public NodeState populate(NodeState state) throws RepositoryException {
		NodeId nodeId = state.getNodeId();
		
        String[] property = getProperty(nodeId, docbaseName);
        String docbase = (property != null && property.length > 0 ? property[0] : null);
        int limit = -1;
        try {
            limit = getPropertyAsInt(nodeId, facetLimit);
            if(limit < 0 ) {
                log.warn("Skipping negative limit '{}'. Using default", limit);
            }
        } catch (NumberFormatException e) {
            // no limit configured, ignore
        }

        String[] facets = getProperty(nodeId, facetsName);
        String[] facetNodeNames = getProperty(nodeId, facetNodeNamesName);

        String[] sortbys = getProperty(nodeId, facetSortBy);
        String[] sortorders = getProperty(nodeId, facetSortOrder);
        List<OrderBy> orderByList = null;
        if(sortbys != null) {

            orderByList = new ArrayList<OrderBy>();
            if(sortorders != null && sortorders.length != sortbys.length) {
                log.error("When using multivalued '{}', and '{}', then both should have equal number of values (or delete property "+HippoNodeType.HIPPO_FACETSORTORDER+" at all)", HippoNodeType.HIPPO_FACETSORTBY, HippoNodeType.HIPPO_FACETSORTORDER);
                return state;
            }
            for(int i = 0; i < sortbys.length; i++) {
                try {
                    Name propertyName = resolveName(NodeNameCodec.encode(sortbys[i]));
                    if(sortorders != null && "descending".equals(sortorders[i])) {
                        orderByList.add(new OrderBy(propertyName.toString(), true));
                    } else {
                        // default orderby is ascending
                        orderByList.add(new OrderBy(propertyName.toString()));
                    }
                } catch (IllegalNameException e){
                    log.error("Skipping illegal name as sortby : " + sortbys[i] + " because : " +  e.getMessage());
                } catch (NamespaceException e) {
                    log.error("Skipping illegal name as sortby : " + sortbys[i] + " because : " +  e.getMessage());
                }
            }
        }
        
        if (facets != null && facets.length > 0) {
            if(facetNodeNames != null) {
                if(facets.length != facetNodeNames.length) {
                    log.error("When using multivalued property '{}', it must have equal number of values" +
                    		"as for property '{}'", HippoNodeType.HIPPO_FACETNODENAMES, HippoNodeType.HIPPO_FACETS);
                    return state;
                }
            }
            int i = 0;
        	for(String facet : facets){
        		try {
        		    String configuredNodeName = null;
        		    if(facetNodeNames != null && facetNodeNames[i] != null && !"".equals(facetNodeNames[i])) {
        		        configuredNodeName = facetNodeNames[i];
        		    }
        		    ParsedFacet parsedFacet;
        		    try {
        		        parsedFacet = new ParsedFacet(facet, configuredNodeName, this);
                    } catch (Exception e) {
                        log.error("Malformed facet range configuration '"+facet+"'. Valid format is "+VALID_RANGE_EXAMPLE,
                                        e);
                        return state;
                    }
                    
	        		Name childName = resolveName(NodeNameCodec.encode(parsedFacet.getDisplayFacetName()));
	        		FacetNavigationNodeId childNodeId = new FacetNavigationNodeId(facetsAvailableNavigationProvider,state.getNodeId(), childName);
	        		childNodeId.availableFacets = facets;
                    childNodeId.facetNodeNames = facetNodeNames;
                    childNodeId.currentFacet = facet;
	        		childNodeId.docbase = docbase;
	        		if(limit > -1) {
	        		    childNodeId.limit = limit;
	        		}
	        		childNodeId.orderByList = orderByList;
	        		inheritParentFilters(childNodeId, state);
	        		state.addChildNodeEntry(childName, childNodeId);
	        		i++;
        		} catch (IllegalNameException e){
        			log.error("Skipping illegal name as facet : " + facet + " because : " +  e.getMessage());
        		} catch (NamespaceException e) {
        			log.error("Skipping illegal name as facet : " + facet + " because : " +  e.getMessage());
        		}
        	}
        }
	    
	    return state;
	}

	protected final int getPropertyAsInt(NodeId nodeId, Name propName) throws NumberFormatException{
        PropertyState propState = getPropertyState(new PropertyId(nodeId, propName));
        if(propState == null) {
            if(log.isDebugEnabled()) {
                log.debug("possible expected property state " + propName + " in " + nodeId + " not found");
            }
            throw new NumberFormatException("Cannot get value for property '"+propName+"' as integer because there is no such property");
        }
        
        if(propState.getType() == PropertyType.DOUBLE) {
            InternalValue[] values = propState.getValues();
            if(values.length != 1) {
                throw new NumberFormatException("Cannot parse value for multivalued property '"+propName+"' to an integer");
            }
            return (int)values[0].getDouble();
        } else if(propState.getType() == PropertyType.LONG) {
            InternalValue[] values = propState.getValues();
            if(values.length != 1) {
                throw new NumberFormatException("Cannot parse value for multivalued property '"+propName+"' to an integer");
            }
            return (int)values[0].getLong();
        } else if(propState.getType() == PropertyType.STRING) {
            InternalValue[] values = propState.getValues();
            if(values.length != 1) {
                throw new NumberFormatException("Cannot parse value for multivalued property '"+propName+"' to an integer");
            }
            return Integer.parseInt(values[0].getString());
        }
        throw new NumberFormatException("Cannot parse value for property '"+propName+"' to an integer");
    }
	
}
