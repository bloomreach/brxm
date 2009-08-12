<%@ page contentType="text/html; charset=UTF-8" language="java"%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c"%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/xml" prefix="x"%>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt"%>
<%@ taglib uri="http://www.hippoecm.org/jsp/hst/core" prefix='hst'%>

<hst:actionURL var="documentActionUrl" />

<c:choose>
  <c:when test="${not empty todoList}">
    <ul class="todo">
      <c:forEach var="item" items="${todoList}">
        <li class="todo">
          <a href="<hst:link hippobean="${item.document}"/>">${item.document.title}</a> (Requested by ${item.requestUsername})
          <div>
            <c:choose>
              <c:when test="${item.type == 'publish'}">
                <form method="POST" action="${documentActionUrl}">
                  Your action:
                  <input type="hidden" name="requestPath" value="${item.path}" />
                  <input type="hidden" name="requestType" value="${item.type}" />
                  <input type="submit" name="documentAction" value="Accept" />
                  <input type="submit" name="documentAction" value="Reject" />
                </form>
              </c:when>
            </c:choose>
          </div>
        </li>
      </c:forEach>
    </ul>
  </c:when>
  <c:otherwise>
    <I>There's no item now.</I>
  </c:otherwise>
</c:choose>