<%@ include file="/WEB-INF/jspf/htmlTags.jspf" %>
<%--@elvariable id="document" type="org.onehippo.cms7.essentials.site.beans.TextDocument"--%>
<%--@elvariable id="headTitle" type="java.lang.String"--%>
<c:if test="${not empty document.title}">
  <hst:element var="headTitle" name="title">
    <c:out value="${document.title}"/>
  </hst:element>
  <hst:headContribution keyHint="headTitle" element="${headTitle}"/>
</c:if>

<div class="row">
  <div class="large-12 columns">

    <hst:include ref="content-container"/>
    <%--

    <h2>${document.title} <hst:cmseditlink hippobean="${document}"/></h2>
    <p>${fn:escapeXml(document.summary)}</p>
    <hr/>
    <hst:html hippohtml="${document.html}"/>

    --%>
  </div>
</div>


