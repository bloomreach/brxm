#set( $symbol_dollar = '$' )
<%@ include file="/WEB-INF/jspf/htmlTags.jspf" %>
<%--@elvariable id="document" type="${package}.beans.TextDocument"--%>

<c:choose>
  <c:when test="${symbol_dollar}{empty document}">
    <tag:pagenotfound/>
  </c:when>
  <c:otherwise>

    <c:if test="${symbol_dollar}{not empty document.title}">
      <hst:element var="headTitle" name="title">
        <c:out value="${symbol_dollar}{document.title}"/>
      </hst:element>
      <hst:headContribution keyHint="headTitle" element="${symbol_dollar}{headTitle}"/>
    </c:if>

    <article class="well well-large">
      <hst:cmseditlink hippobean="${symbol_dollar}{document}"/>
      <header>
        <h2>${symbol_dollar}{fn:escapeXml(document.title)}</h2>
        <p>${symbol_dollar}{fn:escapeXml(document.summary)}</p>
      </header>
      <hst:html hippohtml="${symbol_dollar}{document.html}"/>
    </article>

  </c:otherwise>
</c:choose>