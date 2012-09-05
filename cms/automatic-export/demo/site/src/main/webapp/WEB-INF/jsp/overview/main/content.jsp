<%@ include file="/WEB-INF/jspf/htmlTags.jspf" %>
<%--@elvariable id="crPage" type="java.lang.Integer"--%>
<%--@elvariable id="info" type="org.onehippo.cms7.autoexport.demo.componentsinfo.GeneralListInfo"--%>
<%--@elvariable id="pages" type="java.util.Collection<java.lang.Integer>"--%>
<%--@elvariable id="query" type="java.lang.String"--%>
<%--@elvariable id="result" type="org.hippoecm.hst.content.beans.query.HstQueryResult"--%>
<%--@elvariable id="totalSize" type="java.lang.Integer"--%>

<c:choose>
  <c:when test="${empty info}">
    <tag:pagenotfound/>
  </c:when>
  <c:otherwise>
    <c:if test="${not empty info.title}">
      <hst:element var="headTitle" name="title">
        <c:out value="${info.title}"/>
      </hst:element>
      <hst:headContribution keyHint="headTitle" element="${headTitle}"/>
    </c:if>
    
    <h2>
      ${info.title}
      <c:if test="${not empty totalSize}"> Total results ${totalSize}</c:if>
    </h2>
    <ul>
      <c:forEach var="item" items="${result.hippoBeans}">
        <hst:link var="link" hippobean="${item}"/>
        <li class="overview-item">
          <hst:cmseditlink hippobean="${item}"/>
          <a href="${link}">${item.title}</a>
          <div>
            <c:if test="${hst:isReadable(item, 'date.time')}">
              <p><fmt:formatDate value="${item.date.time}" type="Date" pattern="MMMM d, yyyy h:mm a"/></p>
            </c:if>
            <p>${item.summary}</p>
          </div>
        </li>
      </c:forEach>
    </ul>
    
    <!--if there are pages on the request, they will be printed by the tag:pages -->
    <tag:pages pages="${pages}" page="${page}"/>
    
  </c:otherwise>
</c:choose>