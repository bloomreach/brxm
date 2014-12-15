<%@ page language="java" contentType="text/html; charset=UTF-8"
    pageEncoding="UTF-8"%>
<%@ page trimDirectiveWhitespaces="true" %>
<%@ taglib uri="http://www.hippoecm.org/jsp/hst/core" prefix='hst'%>

<table width="100%">
  <tr>
    <td align="left">
      <h1>Taxonomy Demo Website</h1>
    </td>
    <td align="right">
      <a href="<hst:link path='/' mount='site' />">English</a>
      |
      <a href="<hst:link path='/'  mount='site-fr' />">French</a>
      |
      <a href="<hst:link path='/'  mount='site-cy' />">Welsh</a>
    </td>
  </tr>
  <tr>
    <td align="right" colspan="2">
      <form action="<hst:link path="/search"/>">
        <input type="text" name="query"/>
        <input type="submit"/>
      </form>
    </td>
  </tr>
</table>