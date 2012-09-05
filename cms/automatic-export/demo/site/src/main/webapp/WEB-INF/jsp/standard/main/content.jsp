<%@ include file="/WEB-INF/jspf/htmlTags.jspf" %>
<%--@elvariable id="document" type="org.onehippo.cms7.autoexport.demo.beans.TextDocument"--%>

<c:choose>
  <c:when test="${empty document}">
    <tag:pagenotfound/>
  </c:when>
  <c:otherwise>
    <c:if test="${not empty document.title}">
      <hst:element var="headTitle" name="title">
        <c:out value="${document.title}"/>
      </hst:element>
      <hst:headContribution keyHint="headTitle" element="${headTitle}"/>
    </c:if>
    
    <hst:cmseditlink hippobean="${document}"/>
    <h2>${document.title}</h2>
    <p>${document.summary}</p>
    <hst:html hippohtml="${document.html}"/>
  </c:otherwise>
</c:choose>