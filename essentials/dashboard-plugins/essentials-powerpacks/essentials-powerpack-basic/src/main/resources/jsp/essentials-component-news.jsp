<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>
<%@ taglib prefix="hst" uri="http://www.hippoecm.org/jsp/hst/core" %>
<%@ taglib prefix="x" uri="http://java.sun.com/jsp/jstl/xml" %>
<%--@elvariable id="document" type="{{beansPackage}}\.EventsDocument"--%>
<%--@elvariable id="pageable" type="org.onehippo.cms7.essentials.components.paging.Pageable"--%>

<c:forEach var="item" items="${pageable.items}" varStatus="status">
  <hst:link var="link" hippobean="${item}"/>
  <article>
    <hst:cmseditlink hippobean="${item}"/>
    <h3><a href="${link}"><c:out value="${item.title}"/></a></h3>
    <c:if test="${hst:isReadable(item, 'date.time')}">
      <p>
        <fmt:formatDate value="${item.date.time}" type="both" dateStyle="medium" timeStyle="short"/>
      </p>
    </c:if>
    <p><c:out value="${item.introduction}"/></p>
  </article>
</c:forEach>