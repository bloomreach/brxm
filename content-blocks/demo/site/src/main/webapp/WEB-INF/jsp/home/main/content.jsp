<%@ include file="/WEB-INF/jspf/htmlTags.jspf" %>
<%--@elvariable id="document" type="org.onehippo.forge.contentblocksdemo.beans.ProviderCompoundDocument"--%>
<%--@elvariable id="headTitle" type="java.lang.String"--%>

<c:choose>
  <c:when test="${empty document}">
    <tag:pagenotfound/>
  </c:when>
  <c:otherwise>

    <c:if test="${not empty document.title}">
      <hst:element var="headTitle" name="title">
        <c:out value="${document.title}"/>
      </hst:element>
      <hst:headContribution keyHint="headTitle" element="${headTitle}"/>
    </c:if>

    <article class="well well-large">
      <hst:cmseditlink hippobean="${document}"/>
      <header>
        <h2>${fn:escapeXml(document.title)}</h2>
      </header>

      <c:if test="${not empty document.contentBlocks}">
        <c:forEach var="item" items="${document.contentBlocks}">
          <c:if test="${not empty item}">
            <c:choose>
              <c:when test="${item.type=='text'}">
                <hst:html hippohtml="${item.text}"/>
              </c:when>
              <c:when test="${item.type=='image'}">
                <c:if test="${hst:isReadable(item, 'image.original')}">
                  <hst:link var="img" hippobean="${item.image.original}"/>
                  <figure>
                    <img src="${img}" title="${fn:escapeXml(item.image.fileName)}"
                         alt="${fn:escapeXml(item.image.fileName)}"/>
                    <figcaption>${fn:escapeXml(item.image.description)}</figcaption>
                  </figure>
                </c:if>
                </p>
              </c:when>
              <c:when test="${item.type=='video'}">
                <iframe width="560" height="315" src="//www.youtube.com/embed/${item.video}" frameborder="0" allowfullscreen></iframe>
              </c:when>
            </c:choose>
          </c:if>
        </c:forEach>
      </c:if>
    </article>
  </c:otherwise>
</c:choose>

<hst:include ref="container"/>
