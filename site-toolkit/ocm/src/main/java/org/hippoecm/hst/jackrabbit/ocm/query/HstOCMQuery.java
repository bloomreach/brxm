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
package org.hippoecm.hst.jackrabbit.ocm.query;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;
import javax.jcr.query.InvalidQueryException;
import javax.jcr.query.QueryResult;

import org.apache.jackrabbit.ocm.exception.IncorrectPersistentClassException;
import org.apache.jackrabbit.ocm.exception.ObjectContentManagerException;
import org.apache.jackrabbit.ocm.manager.ManagerConstant;
import org.apache.jackrabbit.ocm.manager.ObjectContentManager;
import org.apache.jackrabbit.ocm.mapper.Mapper;
import org.apache.jackrabbit.ocm.mapper.model.ClassDescriptor;
import org.apache.jackrabbit.ocm.query.Filter;
import org.apache.jackrabbit.ocm.query.impl.FilterImpl;
import org.hippoecm.hst.content.beans.query.exceptions.FilterException;
import org.hippoecm.hst.content.beans.query.exceptions.ScopeException;
import org.hippoecm.hst.content.beans.query.filter.HstCtxWhereFilter;
import org.hippoecm.hst.core.component.HstRequest;
import org.hippoecm.hst.core.search.HstCtxWhereClauseComputer;
import org.hippoecm.hst.jackrabbit.ocm.HippoStdNode;
import org.hippoecm.hst.jackrabbit.ocm.HippoStdNodeIterator;
import org.hippoecm.hst.jackrabbit.ocm.impl.HippoStdNodeIteratorImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HstOCMQuery {

    private static Logger log = LoggerFactory.getLogger(HstOCMQuery.class);
    
    private ObjectContentManager ocm;
    private HstRequest request;
    private HstCtxWhereFilter hstCtxWhereFilter;
    private HippoStdFilter hippoStdFilter;
    private Mapper mapper;
    private HstCtxWhereClauseComputer ctxWhereClauseComputer;
    
    ClassDescriptor classDescriptor;

    private final static String ORDER_BY_STRING =  "order by ";
    
    private String orderByExpression = "";
    private ClassLoader classLoader;
    
    public HstOCMQuery(HstCtxWhereClauseComputer ctxWhereClauseComputer, Mapper mapper, ObjectContentManager ocm ,HstRequest request){
        this(ctxWhereClauseComputer, mapper, ocm, request, null);
    }
    
    public HstOCMQuery(HstCtxWhereClauseComputer ctxWhereClauseComputer, Mapper mapper, ObjectContentManager ocm ,HstRequest request, ClassLoader classLoader){
        this.ocm = ocm;
        this.mapper = mapper;
        this.ctxWhereClauseComputer = ctxWhereClauseComputer;
        this.request = request;
        this.classLoader = classLoader;
    }

    public void setScope(HippoStdNode hippoStdNode) throws ScopeException {
      if(hippoStdNode == null) {
          throw new ScopeException("Cannot create a search for the scope when the hippoStdNode is null");
      }
      if(hippoStdNode.getNode() == null) {
          throw new ScopeException("Cannot create a search for the scope when the jcrNode in hippoStdNode is null");
      }
      setScope(hippoStdNode.getNode());
    }

    public void setScope(Node node) throws ScopeException{
        try {
            this.hstCtxWhereFilter = new HstCtxWhereFilter(ctxWhereClauseComputer, node);
        } catch (FilterException e) {
            throw new ScopeException("Cannot create scope because ctx where filter failed");
        }
    }
    
    public HippoStdFilter createFilter(String fullQName) throws FilterException{
        try {
            Class clazz = (classLoader != null ? classLoader.loadClass(fullQName) : Class.forName(fullQName));
            return this.createFilter(clazz);
        } catch (ClassNotFoundException e) {
            throw new FilterException("Could not create Filter: " + e.getMessage(), e);
        }
    }
        
    public HippoStdFilter createFilter(Class clazz) throws FilterException{
        try {
            Filter filter = ocm.getQueryManager().createFilter(clazz);
            classDescriptor = mapper.getClassDescriptorByClass(filter.getFilterClass());
            return  new HippoStdFilter(filter);
        } catch (IncorrectPersistentClassException e) {
            throw new FilterException("Could not create Filter: " + e.getMessage() , e);
        }
    }

    
    public void setFilter(HippoStdFilter filter) {
        this.hippoStdFilter = filter;
    }

    
    public HippoStdNodeIterator execute(){
        StringBuffer query = new StringBuffer();
        if(hippoStdFilter.getFilter() == null) {
            if(hstCtxWhereFilter.getJcrExpression() == null) {
                log.warn("Not allowed to have a HstQuery with an empty hstCtxWhereFilter. Return an empty HippoStdNodeIterator");
                return new EmptyHippoStdNodeIterator();
            } else {
                query.append("//(element, hippo:document)[" + hstCtxWhereFilter.getJcrExpression() +"]");
            }
        }
        if( hippoStdFilter.getFilter() instanceof FilterImpl) {
            FilterImpl filter = (FilterImpl)hippoStdFilter.getFilter();
            
            if(hstCtxWhereFilter != null && hstCtxWhereFilter.getJcrExpression() != null) {
                filter.addJCRExpression(hstCtxWhereFilter.getJcrExpression());
            } else {
                log.warn("Not allowed to have a HstQuery with an empty hstCtxWhereFilter. Return an empty HippoStdNodeIterator");
                return new EmptyHippoStdNodeIterator();
            }
            
            query.append("//element(*, " + this.getNodeType(filter)+ ")") ;
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
    
    class EmptyHippoStdNodeIterator implements HippoStdNodeIterator {

        public long getPosition() {
            return 0;
        }

        public long getSize() {
            return 0;
        }

        public HippoStdNode nextHippoStdNode() {
            return null;
        }

        public void skip(int skipNum) {
            
        }

        public boolean hasNext() {
            return false;
        }

        public Object next() {
            return null;
        }

        public void remove() {
            
        }

    
    }

}
