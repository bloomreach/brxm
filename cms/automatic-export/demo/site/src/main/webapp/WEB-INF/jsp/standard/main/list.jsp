<%@ include file="/WEB-INF/jspf/htmlTags.jspf" %>
<%--@elvariable id="info" type="org.onehippo.cms7.autoexport.demo.componentsinfo.ListInfo"--%>
<%--@elvariable id="result" type="org.hippoecm.hst.content.beans.query.HstQueryResult"--%>

<c:choose>
  <c:when test="${empty info}">
    <tag:pagenotfound/>
  </c:when>
  <c:otherwise>
    <div class=${info.cssClass}>
      <p>${info.title}</p>
    
      <ul>
        <c:forEach var="item" items="${result.hippoBeans}" varStatus="counter">
          <c:if test="${counter.index == 0}">
            <hst:link var="link" hippobean="${item}"/>
            <li style="background-color:${info.bgColor};">
              <div>
                <c:if test="${hst:isReadable(item, 'image.thumbnail')}">
                  <hst:link var="img" hippobean="${item.image.thumbnail}"/>
                  <div style="float:left;margin-right:10px;">
                    <img src="${img}" title="${item.image.fileName}"
                      alt="${item.image.fileName}"/>
                  </div>
                </c:if>
                <div>
                  <p>
                    <c:if test="${hst:isReadable(item, 'date.time')}">
                      <fmt:formatDate value="${item.date.time}" type="Date" pattern="MMMM d, yyyy"/> -
                    </c:if>
                    <a href="${link}">${item.title}</a>
                  </p>
                  <p>${item.summary}</p>
                </div>
              </div>
            </li>
          </c:if>
        </c:forEach>
      </ul>
    
      <ul>
        <c:forEach var="item" items="${result.hippoBeans}" varStatus="counter">
          <c:if test="${counter.index > 0}">
            <hst:link var="link" hippobean="${item}"/>
            <li style="background-color:${info.bgColor};">
              <c:if test="${hst:isReadable(item, 'date')}">
                <fmt:formatDate value="${item.date.time}" type="Date" pattern="MMMM d, yyyy"/> -
              </c:if>
              <a href="${link}">${item.title}</a>
            </li>
          </c:if>
        </c:forEach>
    
      </ul>
    </div>
  </c:otherwise>
</c:choose>
