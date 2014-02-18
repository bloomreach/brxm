<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>
<%@ taglib prefix="hst" uri="http://www.hippoecm.org/jsp/hst/core" %>
<%@ taglib prefix="x" uri="http://java.sun.com/jsp/jstl/xml" %>
<%@ taglib prefix="tag" tagdir="/WEB-INF/tags" %>
<%--@elvariable id="pageable" type="org.onehippo.cms7.essentials.components.paging.Pageable"--%>

<hst:link var="link" hippobean="${item}"/>
<article>
  <hst:cmseditlink hippobean="${item}"/>
  <h3><a href="${link}"><c:out value="${item.title}"/></a></h3>
  <c:if test="${hst:isReadable(item, 'date.time')}">
    <p>
      <fmt:formatDate value="${item.date.time}" type="both" dateStyle="medium" timeStyle="short"/>
    </p>
  </c:if>
  <p><c:out value="${item.number}"/></p>
  <p><c:out value="${item.street}"/></p>
  <p><c:out value="${item.city}"/></p>
  <p><c:out value="${item.province}"/></p>
  <p><c:out value="${item.country}"/></p>
</article>