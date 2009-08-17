#set( $symbol_pound = '#' )
#set( $symbol_dollar = '$' )
#set( $symbol_escape = '\' )
<%@ page language="java" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/xml" prefix="x" %>
<%@ taglib uri="http://www.hippoecm.org/jsp/hst/core" prefix='hst'%>

<table class="main">
  <tr>
    <td class="leftmenu">
      <hst:include ref="leftmenu"/>
    </td>
    <td class="content">
      <hst:include ref="content"/>
    </td>
    <td class="rightmenu">
      <hst:include ref="right"/>
    </td>
  </tr>
</table>
