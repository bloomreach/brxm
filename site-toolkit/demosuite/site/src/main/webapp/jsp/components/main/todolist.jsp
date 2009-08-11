<%@ page contentType="text/html; charset=UTF-8" language="java"%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c"%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/xml" prefix="x"%>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt"%>
<%@ taglib uri="http://www.hippoecm.org/jsp/hst/core" prefix='hst'%>

<ul class="todo">
  <c:forEach var="item" items="${todoList}">
    <li class="todo">
      <a href="<hst:link hippobean="${item.document}"/>">${item.document.title}</a> (Requested by ${item.requestUsername})
      <div>
        Your action: 
        <c:choose>
          <c:when test="${item.type == 'publish'}">
            <input type="button" value="Accept" />
            <input type="button" value="Reject" />
          </c:when>
          <c:when test="${item.type == 'depublish'}">
            <input type="button" value="Accept" />
            <input type="button" value="Reject" />
          </c:when>
        </c:choose>
      </div>
    </li>
  </c:forEach>
</ul>
