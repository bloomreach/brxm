<%@ include file="/WEB-INF/jsp/include/imports.jsp" %>

<%--@elvariable id="componentId" type="java.lang.String"--%>
<%--@elvariable id="pageable" type="org.onehippo.cms7.essentials.components.paging.Pageable"--%>
<%--@elvariable id="item" type="{{beansPackage}}.Banner"--%>
<%--@elvariable id="cparam" type="org.onehippo.cms7.essentials.components.info.EssentialsCarouselComponentInfo"--%>
<%--@elvariable id="editMode" type="java.lang.Boolean"--%>
<c:set var="pauseCarousel" value="${requestScope.cparam.pause ? 'hover':''}"/>

<c:if test="${requestScope.pageable ne null && requestScope.pageable.total gt 0}">
  <div id="${componentId}" class="carousel slide" data-ride="carousel" data-interval="${requestScope.cparam.interval}"
       data-pause="${pauseCarousel}" data-wrap="${requestScope.cparam.cycle}">
    <ol class="carousel-indicators">
      <c:forEach begin="0" end="${requestScope.pageable.total -1}" varStatus="index">
        <c:choose>
          <c:when test="${index.first}">
            <li data-target="#${componentId}" data-slide-to="${index.index}" class="active"></li>
          </c:when>
          <c:otherwise>
            <li data-target="#${componentId}" data-slide-to="${index.index}"></li>
          </c:otherwise>
        </c:choose>
      </c:forEach>
    </ol>
    <div class="carousel-inner">
      <c:forEach var="item" items="${requestScope.pageable.items}" varStatus="counter">
        <c:set var="active" value="${counter.first ? ' active':''}"/>
        <div class="item${active}">
          <hst:manageContent hippobean="${item}"/>
          <img src="<hst:link hippobean="${item.image}" />" alt="${fn:escapeXml(item.title)}"/>
          <div class="carousel-caption">
            <c:choose>
              <c:when test="${item.link ne null}">
                <h3><a href="<hst:link hippobean="${item.link}"/>"><c:out value="${item.title}"/></a></h3>
              </c:when>
              <c:otherwise>
                <h3><c:out value="${item.title}"/></h3>
              </c:otherwise>
            </c:choose>
            <hst:html hippohtml="${item.content}"/>
          </div>
        </div>
      </c:forEach>
    </div>
    <c:if test="${requestScope.cparam.showNavigation}">
      <a class="left carousel-control" href="#${componentId}" data-slide="prev"><span class="glyphicon glyphicon-chevron-left"></span></a>
      <a class="right carousel-control" href="#${componentId}" data-slide="next"><span class="glyphicon glyphicon-chevron-right"></span></a>
    </c:if>
      <%--
        The Carousel component is initialized on page-load by means of the data attributes. However, when the
        channel-manager redraws a container (after actions like adding, removing or reordering components) it will only
        do a page reload if one of the affected components adds a headContribution that has not been processed yet
        (see HSTTWO-3747). To ensure it is also initialiazed when the page is *not* reloaded, the following snippet is
        used.
      --%>
    <c:if test="${requestScope.editMode}">
      <script type="text/javascript">
        if (window.jQuery && window.jQuery.fn.carousel) {
          jQuery('#${componentId}').carousel();
        }
      </script>
    </c:if>
  </div>

  <hst:headContribution category="htmlHead">
    <style type="text/css">
      /* Carousel base class */
      #${componentId} {
        height: ${requestScope.cparam.carouselHeight}px;
        margin-bottom: 60px;
      }

      /* Since positioning the image, we need to help out the caption */
      .carousel-caption {
        z-index: 10;
        background: rgba(51, 122, 183, 0.7);
      }

      /* Declare heights because of positioning of img element */
      #${componentId} .item {
        height: ${requestScope.cparam.carouselHeight}px;
        background-color: ${requestScope.cparam.carouselBackgroundColor};
      }
      /* center images*/
      .carousel-inner > .item > img {
        margin: 0 auto;
      }
    </style>
  </hst:headContribution>

  <hst:headContribution category="htmlBodyEnd">
    <script type="text/javascript" src="<hst:webfile path="/js/jquery-2.1.0.min.js"/>"></script>
  </hst:headContribution>

  <hst:headContribution category="htmlBodyEnd">
    <script type="text/javascript" src="<hst:webfile path="/js/bootstrap.min.js"/>"></script>
  </hst:headContribution>

</c:if>

<c:if test="${requestScope.editMode && (requestScope.pageable eq null || requestScope.pageable.total lt 1)}">
  <div>
    <img src="<hst:link path='/images/essentials/catalog-component-icons/carousel.png'/>"> Click to edit Carousel
  </div>
  <div class="has-new-content-button">
    <hst:manageContent templateQuery="new-banner-document" parameterName="document" rootPath="banners"/>
  </div>
</c:if>
