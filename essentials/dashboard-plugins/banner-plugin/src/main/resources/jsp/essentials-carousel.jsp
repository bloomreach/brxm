<%@ include file="/WEB-INF/jsp/include/imports.jsp" %>
<%--@elvariable id="pageable" type="org.onehippo.cms7.essentials.components.paging.Pageable"--%>
<%--@elvariable id="item" type="{{beansPackage}}.Banner"--%>
<%--@elvariable id="cparam" type="org.onehippo.cms7.essentials.components.info.EssentialsCarouselComponentInfo"--%>
<c:set var="pauseCarousel" value="${cparam.pause ? 'hover':''}"/>
<div id="myCarousel" class="carousel slide" data-ride="carousel" data-interval="${cparam.interval}"
     data-pause="${pauseCarousel}" data-wrap="${cparam.cycle}">
  <ol class="carousel-indicators">
    <c:forEach begin="0" end="${pageable.total -1}" varStatus="index">
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
    <c:forEach var="item" items="${pageable.items}" varStatus="counter">
      <c:set var="active" value="${counter.first ? ' active':''}"/>
      <div class="item${active}">
        <img src="<hst:link hippobean="${item.image}" />" alt="${item.title}"/>
        <div class="carousel-caption">
          <h3>${item.title}</h3>
          <hst:html hippohtml="${item.content}"/>
        </div>
      </div>
    </c:forEach>
  </div>
  <c:if test="${cparam.showNavigation}">
    <a class="left carousel-control" href="#myCarousel" data-slide="prev"><span class="glyphicon glyphicon-chevron-left"></span></a>
    <a class="right carousel-control" href="#myCarousel" data-slide="next"><span class="glyphicon glyphicon-chevron-right"></span></a>
  </c:if>
</div>
<style type="text/css">
  /* Carousel base class */
  .carousel {
    height: ${cparam.carouselHeight}px;
    /*width: ${cparam.carouselWidth}px;*/
    margin-bottom: 60px;
  }

  /* Since positioning the image, we need to help out the caption */
  .carousel-caption {
    z-index: 10;
  }

  /* Declare heights because of positioning of img element */
  .carousel .item {
    height: ${cparam.carouselHeight}px;
    background-color: ${cparam.carouselBackgroundColor};
  }
  /* center images*/
  .carousel-inner > .item > img {
    margin: 0 auto;
  }
</style>

<hst:headContribution category="componentsJavascript">
  <script type="text/javascript" src="<hst:link path="/js/jquery-2.1.0.min.js"/>"></script>
</hst:headContribution>
<hst:headContribution category="componentsJavascript">
  <script type="text/javascript" src="<hst:link path="/js/bootstrap.min.js"/>"></script>
</hst:headContribution>