<%@ include file="/WEB-INF/jsp/include/imports.jsp" %>

<%--@elvariable id="pageable" type="org.onehippo.cms7.essentials.components.paging.Pageable"--%>
<%--@elvariable id="item" type="{{beansPackage}}.Banner"--%>
<%--@elvariable id="cparam" type="org.onehippo.cms7.essentials.components.info.EssentialsCarouselComponentInfo"--%>
<c:set var="pauseCarousel" value="${requestScope.cparam.pause ? 'hover':''}"/>
<c:if test="${requestScope.pageable ne null && requestScope.pageable.total gt 0}">
  <div id="myCarousel" class="carousel slide" data-ride="carousel" data-interval="${requestScope.cparam.interval}"
       data-pause="${pauseCarousel}" data-wrap="${requestScope.cparam.cycle}">
    <ol class="carousel-indicators">
      <c:forEach begin="0" end="${requestScope.pageable.total -1}" varStatus="index">
        <c:choose>
          <c:when test="${index.first}">
            <li data-target="#myCarousel" data-slide-to="${index.index}" class="active"></li>
          </c:when>
          <c:otherwise>
            <li data-target="#myCarousel" data-slide-to="${index.index}"></li>
          </c:otherwise>
        </c:choose>
      </c:forEach>
    </ol>
    <div class="carousel-inner">
      <c:forEach var="item" items="${requestScope.pageable.items}" varStatus="counter">
        <c:set var="active" value="${counter.first ? ' active':''}"/>
        <div class="item${active}">
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
      <a class="left carousel-control" href="#myCarousel" data-slide="prev"><span class="glyphicon glyphicon-chevron-left"></span></a>
      <a class="right carousel-control" href="#myCarousel" data-slide="next"><span class="glyphicon glyphicon-chevron-right"></span></a>
    </c:if>
  </div>
  <style type="text/css">
    /* Carousel base class */
    .carousel {
      height: ${requestScope.cparam.carouselHeight}px;
      /*width: ${requestScope.cparam.carouselWidth}px;*/
      margin-bottom: 60px;
    }

    /* Since positioning the image, we need to help out the caption */
    .carousel-caption {
      z-index: 10;
    }

    /* Declare heights because of positioning of img element */
    .carousel .item {
      height: ${requestScope.cparam.carouselHeight}px;
      background-color: ${requestScope.cparam.carouselBackgroundColor};
    }
    /* center images*/
    .carousel-inner > .item > img {
      margin: 0 auto;
    }
  </style>

  <hst:headContribution category="htmlBodyEnd">
    <script type="text/javascript" src="<hst:webfile path="/js/jquery-2.1.0.min.js"/>"></script>
    </hst:headContribution>
    <hst:headContribution category="htmlBodyEnd">
      <script type="text/javascript" src="<hst:webfile path="/js/bootstrap.min.js"/>"></script>
  </hst:headContribution></c:if>
<%--@elvariable id="editMode" type="java.lang.Boolean"--%>
<c:if test="${requestScope.editMode && (requestScope.pageable eq null || requestScope.pageable.total lt 1)}">
  <img src="<hst:link path='/images/essentials/catalog-component-icons/carousel.png'/>"> Click to edit Carousel
</c:if>
