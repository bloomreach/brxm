<#include "../include/imports.ftl">

<#-- @ftlvariable name="componentId" type="java.lang.String" -->
<#-- @ftlvariable name="item" type="{{beansPackage}}.Banner" -->
<#-- @ftlvariable name="pageable" type="org.onehippo.cms7.essentials.components.paging.Pageable" -->
<#-- @ftlvariable name="cparam" type="org.onehippo.cms7.essentials.components.info.EssentialsCarouselComponentInfo" -->
<#if pageable?? && pageable.items?has_content>
    <#if cparam.pause>
        <#assign pauseCarousel = 'hover'/>
    <#else>
        <#assign pauseCarousel = ''/>
    </#if>
    <div id="${componentId}" class="carousel slide" data-ride="carousel" data-interval="${cparam.interval?c}"
         data-pause="${pauseCarousel}" data-wrap="${cparam.cycle?string}">
        <ol class="carousel-indicators">
            <#list 0..(pageable.total-1) as index>
                <#if index==0>
                    <li data-target="#${componentId}" data-slide-to="${index}" class="active"></li>
                <#else>
                    <li data-target="#${componentId}" data-slide-to="${index}"></li>
                </#if>
            </#list>
        </ol>
        <div class="carousel-inner">
            <#list pageable.items as item>
                <#if item_index==0>
                    <#assign active = ' active'/>
                <#else>
                    <#assign active = ''/>
                </#if>
                <div class="item${active}">
                  <@hst.manageContent hippobean=item/>
                  <img src="<@hst.link hippobean=item.image />" alt="${item.title?html}"/>
                    <div class="carousel-caption">
                        <#if item.link??>
                            <h3><a href="<@hst.link hippobean=item.link/>">${item.title?html}</a></h3>
                        <#else>
                            <h3>${item.title?html}</h3>
                        </#if>
                        <@hst.html hippohtml=item.content/>
                    </div>
                </div>
            </#list>
        </div>
        <#if cparam.showNavigation>
            <a class="left carousel-control" href="#${componentId}" data-slide="prev"><span class="glyphicon glyphicon-chevron-left"></span></a>
            <a class="right carousel-control" href="#${componentId}" data-slide="next"><span class="glyphicon glyphicon-chevron-right"></span></a>
        </#if>
        <#--
          The Carousel component is initialized on page-load by means of the data attributes. However, when the
          channel-manager redraws a container (after actions like adding, removing or reordering components) it will only
          do a page reload if one of the affected components adds a headContribution that has not been processed yet
          (see HSTTWO-3747). To ensure it is also initialiazed when the page is *not* reloaded, the following snippet is
          used.
        -->
        <#if editMode>
          <script type="text/javascript">
            if (window.jQuery && window.jQuery.fn.carousel) {
              jQuery('#${componentId}').carousel();
            }
          </script>
        </#if>
    </div>

    <@hst.headContribution category="htmlHead">
        <style type="text/css">
            /* Carousel base class */
            #${componentId} {
                height: ${cparam.carouselHeight}px;
                margin-bottom: 60px;
            }

            /* Since positioning the image, we need to help out the caption */
            .carousel-caption {
                z-index: 10;
                background: rgba(51, 122, 183, 0.7);
            }

            /* Declare heights because of positioning of img element */
            #${componentId} .item {
                height: ${cparam.carouselHeight}px;
                background-color: ${cparam.carouselBackgroundColor};
            }

            /* center images*/
            .carousel-inner > .item > img {
                margin: 0 auto;
            }
        </style>
    </@hst.headContribution>

    <@hst.headContribution category="htmlBodyEnd">
        <script type="text/javascript" src="<@hst.webfile path="/js/jquery-2.1.0.min.js"/>"></script>
    </@hst.headContribution>

    <@hst.headContribution category="htmlBodyEnd">
        <script type="text/javascript" src="<@hst.webfile path="/js/bootstrap.min.js"/>"></script>
    </@hst.headContribution>

<#elseif editMode>
  <div>
    <img src="<@hst.link path='/images/essentials/catalog-component-icons/carousel.png'/>"> Click to edit Carousel
  </div>
  <div class="has-new-content-button">
    <@hst.manageContent templateQuery="new-banner-document" parameterName="document1" rootPath="banners"/>
  </div>
</#if>
