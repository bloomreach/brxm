<%--
  Copyright 2008 Hippo

  Licensed under the Apache License, Version 2.0 (the  "License");
  you may not use this file except in compliance with the License.
  You may obtain a copy of the License at

  http://www.apache.org/licenses/LICENSE-2.0

  Unless required by applicable law or agreed to in writing, software
  distributed under the License is distributed on an "AS IS"
  BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  See the License for the specific language governing permissions and
  limitations under the License. --%>

<%--
This example page demonstrates a dispatched servlet or jsp which is responsible for managing all dispatched invocations.
If the component class in the repository component configuration is set to 'org.hippoecm.hst.component.support.SimpleDispatcherHstComponent'
and the parameter named 'dispatch-url' is set to this page, all HST request lifecycle invocations will be delegated to this page.
Therefore, the delegating servlet or jsp like this page should handle all HST request lifecycle invocations properly.
Also, this example page might give hints to make bridge solutions for various web application frameworks.  
--%>

<%@ page language="java" %>
<%@ page import="java.io.IOException" %>
<%@ page import="javax.servlet.http.HttpSession" %>
<%@ page import="org.hippoecm.hst.core.linking.HstLinkCreator" %>
<%@ page import="org.hippoecm.hst.configuration.sitemap.HstSiteMap" %>
<%@ page import="org.hippoecm.hst.core.linking.HstLink" %>
<%@ page import="org.hippoecm.hst.component.support.forms.FormMap" %>
<%@ page import="org.hippoecm.hst.configuration.sitemap.HstSiteMapItem" %>
<%@ page import="org.hippoecm.hst.core.component.HstComponentException" %>
<%@ page import="org.hippoecm.hst.core.component.HstRequest" %>
<%@ page import="org.hippoecm.hst.core.component.HstResponse" %>
<%@ page import="org.hippoecm.hst.component.support.SimpleDispatcherHstComponent" %>
<%@ page import="org.slf4j.Logger" %>
<%@ page import="org.slf4j.LoggerFactory" %>

<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/xml" prefix="x" %>
<%@ taglib uri="http://www.hippoecm.org/jsp/hst/core" prefix='hst'%>

<%!
static Logger log = LoggerFactory.getLogger("org.hippoecm.hst.demo.components.contactdispatch");

private static String[] formFields = {"name","email","textarea"};

private void doBeforeRender(HstRequest request, HstResponse response) throws HstComponentException {
    HttpSession session = request.getSession(true);
    FormMap formMap = (FormMap) session.getAttribute("contactdispatch:formMap");
    
    if (formMap == null) {
        formMap = new FormMap();
        session.setAttribute("contactdispatch:formMap", formMap);
    }
    
    request.setAttribute("form", formMap);
}

private void doBeforeServeResource(HstRequest request, HstResponse response) throws HstComponentException {
    // do nothing here
}

private void doAction(HstRequest request, HstResponse response) throws HstComponentException {
    HttpSession session = request.getSession(true);
    FormMap formMap = new FormMap(request, formFields);
    session.setAttribute("contactdispatch:formMap", formMap);
    
    // Do a really simple validation: 
    if (formMap.getField("email") != null && formMap.getField("email").getValue().contains("@")) {
        // success
        
        // do your business logic
        
        // possible do a redirect to a thankyou page: do not use directly response.sendRedirect;
        HstSiteMapItem item = request.getRequestContext().getResolvedSiteMapItem().getHstSiteMapItem().getChild("thankyou");
        
        if (item != null) {
            sendRedirect(request, response, item.getId());
        } else {
            log.warn("Cannot redirect because siteMapItem not found. ");
        }
    } else {
        // validation failed. Persist form map, and add possible error messages to the formMap
        formMap.addMessage("email", "Email address must contain '@'");
    }
}

private void sendRedirect(HstRequest request, HstResponse response, String redirectToSiteMapItemId) {
    HstLinkCreator linkCreator = request.getRequestContext().getHstLinkCreator();
    HstSiteMap siteMap = request.getRequestContext().getResolvedSiteMapItem().getHstSiteMapItem().getHstSiteMap();
    HstLink link = linkCreator.create(siteMap.getSiteMapItemById(redirectToSiteMapItemId), request.getRequestContext().getResolvedMount().getMount());

    StringBuffer url = new StringBuffer();
    
    for (String elem : link.getPathElements()) {
        String enc = response.encodeURL(elem);
        url.append("/").append(enc);
    }

    String urlString = ((HstResponse) response).createNavigationalURL(url.toString()).toString();
    
    try {
        response.sendRedirect(urlString);
    } catch (IOException e) {
        throw new HstComponentException("Could not redirect. ",e);
    }
}
%>

<%
HstRequest hstRequest = (HstRequest) request;
HstResponse hstResponse = (HstResponse) response;

String hstRequestLifecyclePhase = hstRequest.getLifecyclePhase();
String dispatchLifecyclePhase = (String) hstRequest.getAttribute(SimpleDispatcherHstComponent.LIFECYCLE_PHASE_ATTRIBUTE);

if (HstRequest.ACTION_PHASE.equals(hstRequestLifecyclePhase)) {
    doAction(hstRequest, hstResponse);
} else if (SimpleDispatcherHstComponent.BEFORE_RESOURCE_PHASE.equals(dispatchLifecyclePhase)) {
    doBeforeServeResource(hstRequest, hstResponse);
} else if (SimpleDispatcherHstComponent.BEFORE_RENDER_PHASE.equals(dispatchLifecyclePhase)) {
    doBeforeRender(hstRequest, hstResponse);
} else if (HstRequest.RENDER_PHASE.equals(hstRequestLifecyclePhase)) {
%>

<div>

    <form method="POST" name="myform" action="<hst:actionURL/>">
    <input type="hidden" name="previous" value="${form.previous}"/>
    <br/>
    <table>
        <tr>

            <td>Name</td>
            <td><input type="text" name="name" value="<c:out value="${form.value['name'].value}"/>" /></td>
            <td><font style="color:red">${form.message['name']}</font></td>
        </tr>
        <tr>
            <td>Email</td>
            <td><input type="text" name="email" value="<c:out value="${form.value['email'].value}"/>" /></td>
            <td><font style="color:red">${form.message['email']}</font></td>
        </tr>
        <tr>
            <td>Text</td>
            <td><textarea name="textarea"><c:out value="${form.value['textarea'].value}"/></textarea></td>
            <td><font style="color:red">${form.message['textarea']}</font></td>
        </tr>
        <tr>
            <td>
                <c:if test="${form.previous != null}">
                  <input type="submit" name="prev" value="prev"/>
                </c:if>
            </td>
            <td><input type="submit" value="send"/></td>
        </tr>
    </table>
    </form>

</div>

<% } else if (HstRequest.RESOURCE_PHASE.equals(hstRequestLifecyclePhase)) { %>

<% } %>
