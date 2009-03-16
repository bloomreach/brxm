package org.hippoecm.hst.ocm.query;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;
import javax.jcr.query.InvalidQueryException;
import javax.jcr.query.QueryResult;

import org.apache.jackrabbit.ocm.exception.ObjectContentManagerException;
import org.apache.jackrabbit.ocm.manager.ManagerConstant;
import org.apache.jackrabbit.ocm.manager.ObjectContentManager;
import org.apache.jackrabbit.ocm.mapper.Mapper;
import org.apache.jackrabbit.ocm.mapper.model.ClassDescriptor;
import org.apache.jackrabbit.ocm.query.Filter;
import org.apache.jackrabbit.ocm.query.impl.FilterImpl;
import org.hippoecm.hst.core.component.HstRequest;
import org.hippoecm.hst.ocm.HippoStdNode;
import org.hippoecm.hst.ocm.HippoStdNodeIterator;
import org.hippoecm.hst.ocm.impl.HippoStdNodeIteratorImpl;
import org.hippoecm.hst.ocm.query.impl.HstCtxWhereFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HstQuery {

    private static Logger log = LoggerFactory.getLogger(HstQuery.class);
    
    private ObjectContentManager ocm;
    private HstRequest request;
    private HstCtxWhereFilter hstCtxWhereFilter;
    private HippoStdFilter hippoStdFilter;
    private Mapper mapper;
    
    ClassDescriptor classDescriptor;

    private final static String ORDER_BY_STRING =  "order by ";
    
    private String orderByExpression = "";
    
    public HstQuery(Mapper mapper, ObjectContentManager ocm ,HstRequest request){
        this.ocm = ocm;
        this.mapper = mapper;
        this.request = request;
    }
    

    public void setScope(HippoStdNode hippoStdNode) {
      setScope(hippoStdNode.getNode());
    }

    public void setScope(Node node) {
        this.hstCtxWhereFilter = new HstCtxWhereFilter(this.request.getRequestContext(), node);
    }
    
    public HippoStdFilter createFilter(Class clazz) {
        Filter filter = ocm.getQueryManager().createFilter(clazz);
        classDescriptor = mapper.getClassDescriptorByClass(filter.getFilterClass());
        return  new HippoStdFilter(filter);
    }

    
    public void setFilter(HippoStdFilter filter) {
        this.hippoStdFilter = filter;
    }

    
    public HippoStdNodeIterator execute(){
        StringBuffer query = new StringBuffer();
        if(hippoStdFilter.getFilter() == null) {
            query.append("//(element, hippo:document)[" + hstCtxWhereFilter.getJcrExpression() +"]");
        }
        if( hippoStdFilter.getFilter() instanceof FilterImpl) {
            FilterImpl filter = (FilterImpl)hippoStdFilter.getFilter();
           
            filter.addJCRExpression(hstCtxWhereFilter.getJcrExpression());
            
            query.append("//(element, " + this.getNodeType(filter)+ ")") ;
            query = query.append("[").append(filter.getJcrExpression()).append("]");
        }
        
        query.append(this.getOrderByExpression());
        
        return new HippoStdNodeIteratorImpl(this.ocm, getNodeIterator(query.toString(), "xpath"));
    }
   
    
    private NodeIterator getNodeIterator(String query, String language) {
       
        log.debug("Executing query: '{}'", query);
        javax.jcr.query.Query jcrQuery;
        try {
            jcrQuery = request.getRequestContext().getSession().getWorkspace().getQueryManager().createQuery(query, language);
            long start = System.currentTimeMillis();
            QueryResult queryResult = jcrQuery.execute();
            log.debug("Query took {} ms to complete.", (System.currentTimeMillis() - start));
            NodeIterator nodeIterator = queryResult.getNodes();
            return nodeIterator;
        } catch (InvalidQueryException iqe) {
            throw new org.apache.jackrabbit.ocm.exception.InvalidQueryException(iqe);
        } catch (RepositoryException re) {
            throw new ObjectContentManagerException(re.getMessage(), re);
        }
    }

    private String getNodeType(Filter filter) {
        ClassDescriptor classDescriptor = mapper.getClassDescriptorByClass(filter.getFilterClass());

        String jcrNodeType = classDescriptor.getJcrType();
        if (jcrNodeType == null || jcrNodeType.equals(""))
            {
           return ManagerConstant.NT_UNSTRUCTURED;
            }
        else
        {
           return jcrNodeType;
        }
    }
    
    public void addOrderByDescending(String fieldNameAttribute)
    {
        //Changes made to maintain the query state updated with every addition
        //@author Shrirang Edgaonkar
        addExpression("@" + this.getJcrFieldName(fieldNameAttribute) + " descending");
    }

    /**
     *
     * @see org.apache.jackrabbit.ocm.query.Query#addOrderByAscending(java.lang.String)
     */
    public void addOrderByAscending(String fieldNameAttribute)
    {
        addExpression("@" + this.getJcrFieldName(fieldNameAttribute) + " ascending");
    }
    
    public void addJCRExpression(String jcrExpression) {
        addExpression(jcrExpression);
     }
    
    
    private void addExpression(String jcrExpression) {
         if(this.orderByExpression.equals(""))
         {  
             this.orderByExpression += jcrExpression ;
         }else
             this.orderByExpression += (" , " + jcrExpression) ;
    }

    
    public String getOrderByExpression()
    {
        if(orderByExpression.equals(""))
            return "";
        else
        {   
            if(this.orderByExpression.contains(ORDER_BY_STRING))
                return this.orderByExpression;
            else
                return (ORDER_BY_STRING + this.orderByExpression);
        }
    }
    
    
    private String getJcrFieldName(String fieldAttribute)
    {
        if(classDescriptor == null) {
            log.warn("Can only add a order by after a filter is set");
            return fieldAttribute;
        }
        return classDescriptor.getJcrName(fieldAttribute);
            
    }

}
