<%@ include file="/WEB-INF/jspf/htmlTags.jspf" %>
<%--@elvariable id="results" type="org.onehippo.cms7.essentials.site.components.service.SearchCollection<org.onehippo.cms7.essentials.site.beans.BaseDocument>"--%>
<%--@elvariable id="order" type="java.lang.String"--%>
<%--@elvariable id="query" type="java.lang.String"--%>
<%--@elvariable id="size" type="java.lang.String"--%>


<div class="row">
  <c:if test="${results.total eq 0}">
    <h2>No results found</h2>
  </c:if>
  <c:forEach var="result" items="${results.items}">
    <%--@elvariable id="result" type="org.onehippo.cms7.essentials.site.beans.BaseDocument"--%>
    <h3>${result.title}</h3>
    <p>${result.description}</p>
    <hst:link var="link" fullyQualified="true" hippobean="${result}"/>
    <a href="${link}">${link}</a>
    <hr/>
  </c:forEach>


</div>
<div class="row  pull-left">
  <%--
   PAGINATION
 --%>
  <c:if test="${results.total gt 0}">
  <div class="pagination">
    <ul>
      <c:if test="${results.previous}">
        <li>
          <hst:renderURL var="link">
            <hst:param name="order" value="${order}"/>
            <hst:param name="page" value="${results.previousPage}"/>
            <hst:param name="size" value="${size}"/>
            <hst:param name="query" value="${query}"/>
          </hst:renderURL>
          <a href="${link}">Previous</a>
        </li>
      </c:if>
      <c:forEach var="pageNr" items="${results.currentRange}" varStatus="status">
        <hst:renderURL var="link">
          <hst:param name="order" value="${order}"/>
          <hst:param name="page" value="${pageNr}"/>
          <hst:param name="size" value="${size}"/>
          <hst:param name="query" value="${query}"/>
        </hst:renderURL>
        <c:choose>
          <c:when test="${pageNr == results.currentPage}">
            <li class="disabled"><a href="${link}">${pageNr}</a></li>
          </c:when>
          <c:otherwise>
            <li><a href="${link}">${pageNr}</a></li>
          </c:otherwise>
        </c:choose>
      </c:forEach>
      <c:if test="${results.next}">
        <hst:renderURL var="link">
          <hst:param name="order" value="${order}"/>
          <hst:param name="page" value="${results.nextPage}"/>
          <hst:param name="size" value="${size}"/>
          <hst:param name="query" value="${query}"/>
        </hst:renderURL>
        <li>
          <a href="${link}">Next</a>
        </li>
      </c:if>
    </ul>
    </c:if>


  </div>
</div>


