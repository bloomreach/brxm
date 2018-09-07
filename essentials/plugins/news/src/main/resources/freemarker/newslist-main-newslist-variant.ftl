<#include "../../include/imports.ftl">

<#-- @ftlvariable name="item" type="{{beansPackage}}.NewsDocument" -->
<#-- @ftlvariable name="pageable" type="org.onehippo.cms7.essentials.components.paging.Pageable" -->
<#-- @ftlvariable name="cparam" type="org.onehippo.cms7.essentials.components.info.EssentialsNewsComponentInfo" -->
<#if pageable?? && pageable.items?has_content>
<div>
  <#list pageable.items as item>
    <@hst.link var="link" hippobean=item />
    <div class="media">
      <div class="media-left" style="float: left">
        <a href="${link}">
          <#if item.image?? && item.image.thumbnail??>
            <@hst.link var="img" hippobean=item.image.thumbnail/>
            <img src="${img}" title="${item.image.fileName?html}" alt="${item.image.fileName?html}"/>
          </#if>
        </a>
      </div>
      <div class="media-body has-edit-button">
        <@hst.manageContent hippobean=item/>
        <h4 class="media-heading"><a href="${link}">${item.title?html}</a>
            <#if item.date?? && item.date.time??>
              <span class="label label-success pull-right">
                <@fmt.formatDate value=item.date.time type="both" dateStyle="medium" timeStyle="short"/>
              </span>
            </#if>
        </h4>
        <p>${item.introduction?html}</p>
      </div>
    </div>
  </#list>
  <div class="has-new-content-button">
    <@hst.manageContent documentTemplateQuery="new-news-document" rootPath="news" defaultPath="${currentYear}/${currentMonth}"/>
  </div>
  <#if cparam.showPagination>
    <#include "../../include/pagination.ftl">
  </#if>
</div>
<#-- @ftlvariable name="editMode" type="java.lang.Boolean"-->
<#elseif editMode>
<div>
  <img src="<@hst.link path='/images/essentials/catalog-component-icons/news-list.png'/>"> Click to edit News List
  <div class="has-new-content-button">
    <@hst.manageContent documentTemplateQuery="new-news-document" rootPath="news" defaultPath="${currentYear}/${currentMonth}"/>
  </div>
</div>
</#if>