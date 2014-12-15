<%@ include file="/WEB-INF/jspf/htmlTags.jspf" %>

<hst:defineObjects/>

<c:set var="pageTitle" value="${hstRequestContext.resolvedSiteMapItem.pageTitle}"/>

<c:if test="${not empty pageTitle}">
  <hst:element var="headTitle" name="title">
    <c:out value="${pageTitle}"/>
  </hst:element>
  <hst:headContribution keyHint="headTitle" element="${headTitle}"/>
</c:if>

<hst:include ref="container"/>
