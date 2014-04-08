<%@ include file="/WEB-INF/jsp/include/imports.jsp" %>
<%--@elvariable id="document" type="{{beansPackage}}.EventsDocument"--%>

<hst:link var="link" hippobean="${document}"/>
<article>
  <hst:cmseditlink hippobean="${document}"/>
  <h3><a href="${link}"><c:out value="${document.title}"/></a></h3>
  <c:if test="${hst:isReadable(document, 'date.time')}">
    <p>
      <fmt:formatDate value="${document.date.time}" type="both" dateStyle="medium" timeStyle="short"/>
    </p>
  </c:if>
  <c:if test="${hst:isReadable(document, 'enddate.time')}">
    <p>
      <fmt:formatDate value="${document.enddate.time}" type="both" dateStyle="medium" timeStyle="short"/>
    </p>
  </c:if>

  <c:if test="${not empty document.location}">
    <p><c:out value="${document.location}"/></p>
  </c:if>

  <c:if test="${not empty document.introduction}">
    <p><c:out value="${document.introduction}"/></p>
  </c:if>

  <c:if test="${hst:isReadable(document, 'image.original')}">
    <hst:link var="img" hippobean="${document.image.original}"/>
    <figure>
      <img src="${img}" title="${fn:escapeXml(document.image.fileName)}"
           alt="${fn:escapeXml(document.image.fileName)}"/>
      <figcaption>${fn:escapeXml(document.image.description)}</figcaption>
    </figure>
  </c:if>

  <hst:html hippohtml="${document.content}"/>

</article>