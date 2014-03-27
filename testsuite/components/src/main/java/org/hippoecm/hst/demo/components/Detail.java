/*
 *  Copyright 2008-2013 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.hst.demo.components;

import java.io.IOException;
import java.util.Calendar;
import java.util.List;

import javax.servlet.ServletContext;

import org.hippoecm.hst.component.support.bean.BaseHstComponent;
import org.hippoecm.hst.content.annotations.Persistable;
import org.hippoecm.hst.content.beans.ObjectBeanPersistenceException;
import org.hippoecm.hst.content.beans.manager.workflow.BaseWorkflowCallbackHandler;
import org.hippoecm.hst.content.beans.manager.workflow.WorkflowPersistenceManager;
import org.hippoecm.hst.content.beans.query.HstQuery;
import org.hippoecm.hst.content.beans.query.exceptions.QueryException;
import org.hippoecm.hst.content.beans.query.filter.Filter;
import org.hippoecm.hst.content.beans.standard.HippoBean;
import org.hippoecm.hst.content.beans.standard.HippoDocumentBean;
import org.hippoecm.hst.core.component.HstComponentException;
import org.hippoecm.hst.core.component.HstRequest;
import org.hippoecm.hst.core.component.HstResponse;
import org.hippoecm.hst.core.request.ComponentConfiguration;
import org.hippoecm.hst.core.request.HstRequestContext;
import org.hippoecm.hst.demo.beans.BaseBean;
import org.hippoecm.hst.demo.beans.CommentBean;
import org.hippoecm.hst.util.ContentBeanUtils;
import org.hippoecm.hst.utils.SimpleHtmlExtractor;
import org.onehippo.repository.documentworkflow.DocumentWorkflow;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Detail extends BaseHstComponent {

    public static final Logger log = LoggerFactory.getLogger(Detail.class);

    private String cmsApplicationUrl = "/cms/";

    @Override
    public void init(ServletContext servletContext, ComponentConfiguration componentConfig) throws HstComponentException {
        super.init(servletContext, componentConfig);

        String param = servletContext.getInitParameter("cmsApplicationUrl");
        if (param != null) {
            cmsApplicationUrl = param;
        }
    }


    @Override
    public void doBeforeRender(HstRequest request, HstResponse response) throws HstComponentException {

        super.doBeforeRender(request, response);
        final HstRequestContext requestContext = request.getRequestContext();
        HippoBean crBean = requestContext.getContentBean();

        request.setAttribute("isPreview", isPreview(request) ? Boolean.TRUE : Boolean.FALSE);

        request.setAttribute("cmsApplicationUrl", cmsApplicationUrl);

        // we only have a goBackLink for sitemap items that have configured one.
        String goBackLink = requestContext.getResolvedSiteMapItem().getParameter("go-back-link");
        request.setAttribute("goBackLink", goBackLink);


        if (crBean == null || !(crBean instanceof BaseBean)) {
            response.setStatus(HstResponse.SC_NOT_FOUND);
            try {
                response.forward("/error");
                return;
            } catch (IOException e) {
                throw new HstComponentException("Could not forward the request to the error page.", e);
            }
        }
        request.setAttribute("document", crBean);

        try {
            HstQuery commentQuery = ContentBeanUtils.createIncomingBeansQuery((BaseBean) crBean,
                    requestContext.getSiteContentBaseBean(), "demosite:commentlink/@hippo:docbase", CommentBean.class, false);
            commentQuery.addOrderByDescending("demosite:date");
            commentQuery.setLimit(15);

            boolean onlyLastWeek = false;
            if (onlyLastWeek) {
                // example how to get the comments only of last week
                Calendar sinceLastWeek = Calendar.getInstance();
                sinceLastWeek.add(Calendar.DAY_OF_MONTH, -7);
                Filter f = commentQuery.createFilter();
                f.addGreaterOrEqualThan("demosite:date", sinceLastWeek);
                ((Filter) commentQuery.getFilter()).addAndFilter(f);
            }
            List<CommentBean> comments = ContentBeanUtils.getIncomingBeans(commentQuery, CommentBean.class);
            request.setAttribute("comments", comments);
        } catch (QueryException e) {
            log.warn("QueryException ", e);
        }

    }

@Persistable
@Override
public void doAction(HstRequest request, HstResponse response) throws HstComponentException {
    HstRequestContext requestContext = request.getRequestContext();
    String type = request.getParameter("type");

    if ("add".equals(type)) {
        String title = request.getParameter("title");
        String comment = request.getParameter("comment");
        HippoBean commentTo = request.getRequestContext().getContentBean();
        if (!(commentTo instanceof HippoDocumentBean)) {
            log.warn("Cannot comment on non documents");
            return;
        }
        String commentToUuidOfHandle = ((HippoDocumentBean) commentTo).getCanonicalHandleUUID();
        if (title != null && !"".equals(title.trim()) && comment != null) {
            WorkflowPersistenceManager wpm = null;

            try {
                wpm = getWorkflowPersistenceManager(requestContext.getSession());
                wpm.setWorkflowCallbackHandler(new BaseWorkflowCallbackHandler<DocumentWorkflow>() {
                    public void processWorkflow(DocumentWorkflow wf) throws Exception {
                        wf.requestPublication();
                    }
                });

                // it is not important where we store comments. WE just use some timestamp path below our project content
                String siteCanonicalBasePath = request.getRequestContext().getResolvedMount().getMount().getCanonicalContentPath();
                Calendar currentDate = Calendar.getInstance();

                String commentsFolderPath = siteCanonicalBasePath + "/comment/" + currentDate.get(Calendar.YEAR) + "/"
                        + currentDate.get(Calendar.MONTH) + "/" + currentDate.get(Calendar.DAY_OF_MONTH);
                // comment node name is simply a concatenation of 'comment-' and current time millis.
                String commentNodeName = "comment-for-" + commentTo.getName() + "-" + System.currentTimeMillis();

                // create comment node now
                wpm.createAndReturn(commentsFolderPath, "demosite:commentdocument", commentNodeName, true);

                // retrieve the comment content to manipulate
                CommentBean commentBean = (CommentBean) wpm.getObject(commentsFolderPath + "/" + commentNodeName);
                // update content properties
                if (commentBean == null) {
                    throw new HstComponentException("Failed to add Comment");
                }
                commentBean.setTitle(SimpleHtmlExtractor.getText(title));

                commentBean.setHtml(SimpleHtmlExtractor.getText(comment));

                commentBean.setDate(currentDate);

                commentBean.setCommentTo(commentToUuidOfHandle);

                // update now
                wpm.update(commentBean);

            } catch (Exception e) {
                log.warn("Failed to create a comment: ", e);

                if (wpm != null) {
                    try {
                        wpm.refresh();
                    } catch (ObjectBeanPersistenceException e1) {
                        log.warn("Failed to refresh: ", e);
                    }
                }
            }
        }
    } else if ("remove".equals(type)) {
    }
}

    @Persistable
    @Override
    public void doBeforeServeResource(HstRequest request, HstResponse response) throws HstComponentException {

        super.doBeforeServeResource(request, response);

        HstRequestContext requestContext = request.getRequestContext();

        boolean succeeded = true;
        String errorMessage = "";

        String workflowAction = request.getParameter("workflowAction");

        String field = request.getParameter("field");

        final boolean requestPublication = "requestPublication".equals(workflowAction);
        final boolean saveDocument = ("save".equals(workflowAction) || requestPublication);

        if (saveDocument || requestPublication) {
            String documentPath = requestContext.getContentBean().getPath();
            WorkflowPersistenceManager cpm = null;

            try {
                cpm = getWorkflowPersistenceManager(requestContext.getSession());
                cpm.setWorkflowCallbackHandler(new BaseWorkflowCallbackHandler<DocumentWorkflow>() {
                    public void processWorkflow(DocumentWorkflow wf) throws Exception {
                        if (requestPublication) {
                            wf.requestPublication();
                        }
                    }
                });

                BaseBean page = (BaseBean) cpm.getObject(documentPath);

                if (saveDocument) {
                    String content = request.getParameter("editor");

                    if ("demosite:summary".equals(field)) {
                        page.setSummary(SimpleHtmlExtractor.getText(content));
                    } else if ("demosite:body".equals(field)) {
                        page.setHtml(content);
                    }
                }

                // update now
                cpm.update(page);

            } catch (Exception e) {
                log.warn("Failed to create a comment: ", e);

                if (cpm != null) {
                    try {
                        cpm.refresh();
                    } catch (ObjectBeanPersistenceException e1) {
                        log.warn("Failed to refresh: ", e);
                    }
                }
            }
        }

        request.setAttribute("payload", "{\"success\": " + succeeded + ", \"message\": \"" + errorMessage + "\"}");
    }

}