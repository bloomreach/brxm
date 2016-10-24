<%@ include file="/WEB-INF/jsp/include/imports.jsp" %>

<%--@elvariable id="facets" type="org.hippoecm.hst.content.beans.standard.HippoFacetNavigationBean"--%>
<%--@elvariable id="facetLimit" type="java.lang.Integer"--%>
<%--@elvariable id="query" type="java.lang.String"--%>
<hst:setBundle basename="essentials.facets"/>
<div>
  <form action="<hst:link />" method="get">
    <div class="row form-group">
      <div class="col-xs-8">
        <fmt:message key='facets.placeholder' var="placeholder"/>
        <input type="search" value="<c:out value='${requestScope.query}'/>" name="query" class="form-control"
               placeholder="${fn:escapeXml(placeholder)}">
      </div>
      <div class="col-xs-4">
        <button type="submit" class="btn btn-primary pull-right">
          <fmt:message key='facets.searchbutton' var="button"/><c:out value="${button}"/>
        </button>
      </div>
    </div>
  </form>
  <c:if test="${requestScope.facets ne null}">
    <c:set var="facetLimit" value="50"/>
    <ul class="nav nav-list">
      <c:forEach var="facetvalue" items="${requestScope.facets.folders}">
        <c:if test="${not empty facetvalue.folders}">
          <li><label class="nav-header"><c:out value="${facetvalue.name}"/></label>
            <ul class="nav nav-list">
              <c:forEach items="${facetvalue.folders}" var="item" varStatus="index">
                <c:choose>
                  <c:when test="${item.leaf and item.count gt 0}">
                    <hst:facetnavigationlink remove="${item}" current="${requestScope.facets}" var="removeLink"/>
                    <li class="active">
                      <a href="${removeLink}"><c:out value="${item.name}"/>&nbsp;
                        <span class="alert-danger"><fmt:message key='facets.remove' var="remove"/><c:out value="${remove}"/></span>
                      </a>
                    </li>
                  </c:when>
                  <c:otherwise>
                    <hst:link var="link" hippobean="${item}" navigationStateful="true"/>
                    <li <c:if test="${index.count > facetLimit}">class="extra"</c:if>>
                      <a href="${link}"><c:out value="${item.name}"/>&nbsp;<span>(${item.count})</span></a>
                    </li>
                  </c:otherwise>
                </c:choose>
              </c:forEach>
            </ul>
          </li>
        </c:if>
      </c:forEach>
    </ul>
  </c:if>
  <%--@elvariable id="editMode" type="java.lang.Boolean"--%>
  <c:if test="${requestScope.editMode and requestScope.facets eq null}">
    <img src="<hst:link path='/images/essentials/catalog-component-icons/facets.png'/>"> Click to edit Facets
  </c:if>
</div>
