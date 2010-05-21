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
package org.hippoecm.hst.services.support.jaxrs.content.workflow;

import java.lang.reflect.Method;
import java.util.List;

import javax.jcr.Item;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.FormParam;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.QueryParam;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.PathSegment;
import javax.ws.rs.core.UriInfo;

import org.apache.commons.beanutils.MethodUtils;
import org.apache.commons.lang.StringUtils;
import org.hippoecm.hst.content.beans.manager.workflow.WorkflowPersistenceManager;
import org.hippoecm.hst.content.beans.standard.HippoBean;
import org.hippoecm.hst.services.support.jaxrs.content.BaseHstContentService;
import org.hippoecm.hst.util.NodeUtils;
import org.hippoecm.repository.api.Document;
import org.hippoecm.repository.api.Workflow;
import org.hippoecm.repository.standardworkflow.EditableWorkflow;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * WorkflowService
 * 
 * @version $Id$
 */
@Path("/workflowservice/")
public class WorkflowService extends BaseHstContentService {
    
    private static final String SERVICE_PATH = StringUtils.removeEnd(WorkflowService.class.getAnnotation(Path.class).value(), "/");
    
    private static Logger log = LoggerFactory.getLogger(WorkflowService.class);
    
    public WorkflowService() {
        super();
    }
    
    @GET
    @Path("/{path:.*}")
    public WorkflowContent getWorkflow(@Context HttpServletRequest servletRequest, @Context UriInfo uriInfo, 
            @PathParam("path") List<PathSegment> pathSegments, 
            @QueryParam("cat") @DefaultValue("default") String category) {
        
        String itemPath = getContentItemPath(servletRequest, pathSegments);
        WorkflowContent wfContent = null;
        
        try {
            Item item = getHstRequestContext(servletRequest).getSession().getItem(itemPath);
            
            if (!item.isNode()) {
                throw new IllegalArgumentException("Invalid node path: " + itemPath);
            } else {
                WorkflowPersistenceManager wpm = createWorkflowPersistenceManager(servletRequest);
                HippoBean contentBean = (HippoBean) wpm.getObject(itemPath);
                Workflow wf = wpm.getWorkflow(category, NodeUtils.getCanonicalNode(contentBean.getNode()));
                
                if (wf != null) {
                    wfContent = new WorkflowContent(wf);
                }
            }
        } catch (Exception e) {
            if (log.isDebugEnabled()) {
                log.warn("Failed to retrieve content bean.", e);
            } else {
                log.warn("Failed to retrieve content bean. {}", e.toString());
            }
            
            throw new WebApplicationException(e);
        }
        
        return (wfContent != null ? wfContent : new WorkflowContent());
    }
    
    @POST
    @Path("/{path:.*}")
    public void processAction(@Context HttpServletRequest servletRequest, @Context UriInfo uriInfo, 
            @PathParam("path") List<PathSegment> pathSegments, 
            @QueryParam("cat") @DefaultValue("default") String category,
            @QueryParam("editaction") String editableWorkflowAction,
            @FormParam("action") String action,
            @FormParam("params") Object [] params) {
        
        String itemPath = getContentItemPath(servletRequest, pathSegments);
        
        try {
            Item item = getHstRequestContext(servletRequest).getSession().getItem(itemPath);
            
            if (!item.isNode()) {
                throw new IllegalArgumentException("Invalid node path: " + itemPath);
            } else {
                WorkflowPersistenceManager wpm = createWorkflowPersistenceManager(servletRequest);
                HippoBean bean = (HippoBean) wpm.getObject(itemPath);
                Workflow workflow = wpm.getWorkflow(category, bean.getNode());
                
                if (!StringUtils.isBlank(editableWorkflowAction)) {
                    if (workflow instanceof EditableWorkflow) {
                        EditableWorkflow ewf = (EditableWorkflow) workflow;
                        Document document = ewf.obtainEditableInstance();
                        document = (Document) MethodUtils.invokeMethod(ewf, editableWorkflowAction, null);
                        workflow = wpm.getWorkflow(category, document);
                    } else {
                        throw new IllegalArgumentException("The workflow for '" + itemPath + "' is not an EditableWorkflow: " + workflow.getClass().getName());
                    }
                }
                
                if (!isInvokable(workflow, action)) {
                    throw new IllegalArgumentException("Invalid action name: " + action);
                } else {
                    MethodUtils.invokeMethod(workflow, action, params);
                }
            }
        } catch (Exception e) {
            if (log.isDebugEnabled()) {
                log.warn("Failed to retrieve content bean.", e);
            } else {
                log.warn("Failed to retrieve content bean. {}", e.toString());
            }
            
            throw new WebApplicationException(e);
        }
    }
    
    private boolean isInvokable(Object bean, String methodName) {
        boolean invokable = false;
        
        try {
            for (Method method : bean.getClass().getMethods()) {
                if (methodName.equals(method.getName())) {
                    return true;
                }
            }
        } catch (Exception ignore) {
            
        }
        
        return invokable;
    }
}
