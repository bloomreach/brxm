<%@ include file="/WEB-INF/jsp/essentials/common/imports.jsp" %>
<%--@elvariable id="facets" type="org.hippoecm.hst.content.beans.standard.HippoFacetNavigationBean"--%>
<%--@elvariable id="facetLimit" type="java.lang.Integer"--%>
<c:set var="facetLimit" value="50"/>
<c:if test="${facets ne null}">
  <ul class="nav nav-list">
    <c:forEach var="facetvalue" items="${facets.folders}">
      <c:if test="${not empty facetvalue.folders}">
        <li><label class="nav-header">${facetvalue.name}</label>
          <ul class="nav nav-list">
            <c:forEach items="${facetvalue.folders}" var="item" varStatus="index">
              <c:if test="${index.count <= facetLimit}">
                <c:choose>
                  <c:when test="${item.leaf and item.count gt 0}">
                    <hst:facetnavigationlink remove="${item}" current="${facets}" var="removeLink"/>
                    <li class="active">
                      <a href="${removeLink}">${item.name}&nbsp;<span class="alert-danger">remove</span></a>
                    </li>
                  </c:when>
                  <c:otherwise>
                    <hst:link var="link" hippobean="${item}" navigationStateful="true"/>
                    <li><a href="${link}">${item.name}&nbsp;<span>(${item.count})</span></a></li>
                  </c:otherwise>
                </c:choose>
              </c:if>
              <c:if test="${(index.count > facetLimit)}">
                <c:choose>
                  <c:when test="${item.leaf and item.count gt 0}">
                    <hst:facetnavigationlink remove="${item}" current="${facets}" var="removeLink"/>
                    <li class="active"><a href="${removeLink}"><${item.name}</a></li>
                  </c:when>
                  <c:otherwise>
                    <hst:link var="link" hippobean="${item}" navigationStateful="true"/>
                    <li class="extra">
                      <a href="${link}">${item.name}&nbsp;<span>(${item.count})</span></a>
                    </li>
                  </c:otherwise>
                </c:choose>
              </c:if>
            </c:forEach>
          </ul>
        </li>
      </c:if>

    </c:forEach>
  </ul>
</c:if>