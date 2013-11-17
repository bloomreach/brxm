<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>
<%@ taglib prefix="hst" uri="http://www.hippoecm.org/jsp/hst/core" %>
<%--@elvariable id="document" type="{{beansPackage}}\.NewsDocument"--%>

<hst:link var="link" hippobean="${document}"/>
<article>
  <hst:cmseditlink hippobean="${document}"/>
  <h3><a href="${link}"><c:out value="${document.title}"/></a></h3>
  <c:if test="${hst:isReadable(document, 'date.time')}">
    <p>
      <fmt:formatDate value="${document.date.time}" type="both" dateStyle="medium" timeStyle="short"/>
    </p>
  </c:if>

  <c:if test="${not empty document.author}">
    <p><c:out value="${document.author}"/></p>
  </c:if>
  <c:if test="${not empty document.source}">
    <p><c:out value="${document.source}"/></p>
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