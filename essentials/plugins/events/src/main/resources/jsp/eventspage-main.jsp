<%@ include file="/WEB-INF/jsp/include/imports.jsp" %>

<%--@elvariable id="document" type="{{beansPackage}}.EventsDocument"--%>

<hst:link var="link" hippobean="${requestScope.document}"/>
<article class="has-edit-button">
  <hst:manageContent document="${requestScope.document}"/>
  <h3><a href="${link}"><c:out value="${requestScope.document.title}"/></a></h3>
  <c:if test="${hst:isReadable(requestScope.document, 'date.time')}">
    <p>
      <fmt:formatDate value="${requestScope.document.date.time}" type="both" dateStyle="medium" timeStyle="short"/>
    </p>
  </c:if>
  <c:if test="${hst:isReadable(requestScope.document, 'enddate.time')}">
    <p>
      <fmt:formatDate value="${requestScope.document.enddate.time}" type="both" dateStyle="medium" timeStyle="short"/>
    </p>
  </c:if>

  <c:if test="${not empty requestScope.document.location}">
    <p><c:out value="${requestScope.document.location}"/></p>
  </c:if>

  <c:if test="${not empty requestScope.document.introduction}">
    <p><c:out value="${requestScope.document.introduction}"/></p>
  </c:if>

  <c:if test="${hst:isReadable(requestScope.document, 'image.original')}">
    <hst:link var="img" hippobean="${requestScope.document.image.original}"/>
    <figure>
      <img src="${img}" title="${fn:escapeXml(requestScope.document.image.fileName)}"
           alt="${fn:escapeXml(requestScope.document.image.fileName)}"/>
      <figcaption><c:out value="${requestScope.document.image.description}"/></figcaption>
    </figure>
  </c:if>

  <hst:html hippohtml="${requestScope.document.content}"/>

</article>