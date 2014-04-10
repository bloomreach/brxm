<#include "/WEB-INF/freemarker/include/imports.ftl">
<#-- @ftlvariable name="item" type="{{beansPackage}}.Banner" -->
<#-- @ftlvariable name="showPagination" type="java.lang.Boolean" -->
<#-- @ftlvariable name="pageable" type="org.onehippo.cms7.essentials.components.paging.Pageable" -->
<#-- @ftlvariable name="pageable" type="org.onehippo.cms7.essentials.components.paging.Pageable" -->
<#-- @ftlvariable name="item" type="org.onehippo.appstore.beans.Banner" -->
<#-- @ftlvariable name="pause" type="java.lang.Boolean" -->
<#-- @ftlvariable name="cycle" type="java.lang.Boolean" -->
<#-- @ftlvariable name="interval" type="java.lang.Integer" -->
<#-- @ftlvariable name="carouselHeight" type="java.lang.Integer" -->
<#-- @ftlvariable name="carouselWidth" type="java.lang.Integer" -->
<#-- @ftlvariable name="carouselBackgroundColor" type="java.lang.String" -->
<#-- @ftlvariable name="showNavigation" type="java.lang.Boolean" -->
<#if pageable??>
    <#if pause>
        <#assign pauseCarousel =  'hoover'/>
    <#else>
        <#assign pauseCarousel = ''/>
    </#if>
<div id="myCarousel" class="carousel slide" data-ride="carousel" data-interval="${interval?c}" data-pause="${pauseCarousel}" data-wrap="${cycle?string}">
    <ol class="carousel-indicators">
        <#list 0..(pageable.total-1) as index>
            <#if index==0>
                <li data-target="#myCarousel" data-slide-to="${index}" class="active"></li>
            <#else>
                <li data-target="#myCarousel" data-slide-to="${index}"></li>
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
                <img src="<@hst.link hippobean=item.image />" alt="${item.title}"/>
                <div class="carousel-caption">
                    <h3>${item.title}</h3>
                    <@hst.html hippohtml=item.content/>
                </div>
            </div>
        </#list>
    </div>
    <#if  showNavigation>
        <a class="left carousel-control" href="#myCarousel" data-slide="prev"><span class="glyphicon glyphicon-chevron-left"></span></a>
        <a class="right carousel-control" href="#myCarousel" data-slide="next"><span class="glyphicon glyphicon-chevron-right"></span></a>
    </#if>
</div>
<style type="text/css">
    /* Carousel base class */
    .carousel {
        height: ${carouselHeight}px;
        /*width: ${carouselWidth}px;*/
        margin-bottom: 60px;
    }

    /* Since positioning the image, we need to help out the caption */
    .carousel-caption {
        z-index: 10;
    }

    /* Declare heights because of positioning of img element */
    .carousel .item {
        height: ${carouselHeight}px;
        background-color: ${carouselBackgroundColor};
    }

    /* center images*/
    .carousel-inner > .item > img {
        margin: 0 auto;
    }
</style>

    <@hst.headContribution category="componentsJavascript">
    <script type="text/javascript" src="<@hst.link path="/js/jquery-2.1.0.min.js"/>"></script>
    </@hst.headContribution>
    <@hst.headContribution category="componentsJavascript">
    <script type="text/javascript" src="<@hst.link path="/js/bootstrap.min.js"/>"></script>
    </@hst.headContribution>
</#if>
