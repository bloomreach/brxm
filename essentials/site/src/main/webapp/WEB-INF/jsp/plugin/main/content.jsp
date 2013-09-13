<%@ include file="/WEB-INF/jspf/htmlTags.jspf" %>
<%--
  Copyright 2013 Hippo B.V. (http://www.onehippo.com)
  --%>
<%--@elvariable id="document" type="org.onehippo.cms7.essentials.site.beans.PluginDocument"--%>
<%--@elvariable id="order" type="java.lang.String"--%>
<%--@elvariable id="query" type="java.lang.String"--%>
<%--@elvariable id="size" type="java.lang.String"--%>

<div class="row">
<%--
  <div class="media">
    <a class="pull-left" href="#">
      <img class="media-object" data-src="holder.js/64x64">
    </a>
    <div class="media-body">
      <h4 class="media-heading">Media heading</h4>

      <div class="media">
        ...
      </div>
    </div>
  </div>--%>

    <h2>${document.title}</h2>
    <p>${document.description}</p>
</div>



