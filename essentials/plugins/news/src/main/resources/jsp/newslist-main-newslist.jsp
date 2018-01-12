<%@ include file="/WEB-INF/jsp/include/imports.jsp" %>
<%@ page import="java.util.Calendar" %>

<% pageContext.setAttribute("year", Calendar.getInstance().get(Calendar.YEAR)); %>
<% pageContext.setAttribute("month", Calendar.getInstance().get(Calendar.MONTH)); %>
<%--@elvariable id="item" type="{{beansPackage}}.NewsDocument"--%>
<%--@elvariable id="pageable" type="org.onehippo.cms7.essentials.components.paging.Pageable"--%>

<c:if test="${not empty requestScope.pageable}">
  <div>
    <c:forEach var="item" items="${requestScope.pageable.items}" varStatus="status">
      <hst:link var="link" hippobean="${item}"/>
      <article class="has-edit-button">
        <hst:manageContent document="${item}"/>
        <h3><a href="${link}"><c:out value="${item.title}"/></a></h3>
        <c:if test="${hst:isReadable(item, 'date.time')}">
          <p>
            <fmt:formatDate value="${item.date.time}" type="both" dateStyle="medium" timeStyle="short"/>
          </p>
        </c:if>
        <p><c:out value="${item.introduction}"/></p>
      </article>
    </c:forEach>
    <div class="has-new-content-button">
      <hst:manageContent templateQuery="new-news-document" rootPath="news" defaultPath="${year}/${month}"/>
    </div>
    <c:if test="${requestScope.cparam.showPagination}">
      <%@ include file="/WEB-INF/jsp/include/pagination.jsp" %>
    </c:if>
  </div>
</c:if>
<%--@elvariable id="editMode" type="java.lang.Boolean"--%>
<c:if test="${requestScope.editMode and empty requestScope.pageable}">
  <div>
    <img src="<hst:link path='/images/essentials/catalog-component-icons/news-list.png'/>"> Click to edit News List
    <div class="has-new-content-button">
      <hst:manageContent templateQuery="new-news-document" rootPath="news" defaultPath="${year}/${month}"/>
    </div>
  </div>
</c:if>