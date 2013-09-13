<%@ include file="/WEB-INF/jspf/htmlTags.jspf" %>
<%--@elvariable id="menu" type="org.hippoecm.hst.core.sitemenu.HstSiteMenu"--%>
<%--@elvariable id="query" type="java.lang.String"--%>
<div class="container-fluid">
  <div class="container">
    <div class="row">
      <div class="offset5 span3">
      </div>
      <div class="span4">
        <%--<div class="btn-group">
          <a href="http://www.onehippo.com" class="btn btn-blue top-left-corner enterprise">Enterprise Edition</a>
          <a href="http://www.onehippo.org" target="_blank" class="btn btn-blue top-left-corner community">Hippo Community</a>
        </div>--%>
      </div>
    </div>
    <div class="row mainmenu-wrapper">
      <div class="span2">
        <div id="hippologo">
          <a title="Home Hippo CMS plugins website" href="<hst:link path="/"/>"></a>
        </div>
      </div>
      <div class="span7">
        <ul id="top-nav">
          <!-- top navigation -->
          <%--<li class="first">+1 877 414 47 76 (toll free)</li>--%>
        </ul>
        <ul id="mainmenu" class="nav nav-pills">
          <c:forEach var="item" items="${menu.siteMenuItems}">
            <li class="<c:if test="${item.selected || item.expanded}">active</c:if>">
              <hst:link var="link" link="${item.hstLink}"/>
              <a href="${link}">${item.name}</a>
            </li>
          </c:forEach>
        </ul>

      </div>
      <div class="span3">
        <hst:link path="/search" var="searchLink" />
        <form class="navbar-search pull-right" action="${searchLink}" method="get">
          <input type="text" class="search-query" placeholder="Search" value="${query}" name="query">
        </form>
      </div>

    </div>
  </div>
</div>