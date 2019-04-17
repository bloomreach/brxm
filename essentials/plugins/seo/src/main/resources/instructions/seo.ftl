<#include "../include/imports.ftl">

<#if title??>
  <@hst.headContribution category="SEO" keyHint="hst.seo.document.title">
  <title>${title?xml}</title>
  </@hst.headContribution>
  <@hst.headContribution category="SEO,metadata">
  <meta property="og:title" content="${title?xml}" />
  </@hst.headContribution>
</#if>

<#if metaDescription??>
  <@hst.headContribution category="SEO" keyHint="hst.seo.document.description">
  <meta name="description" content="${metaDescription?xml}"/>
  </@hst.headContribution>
  <@hst.headContribution category="SEO,metadata">
  <meta property="og:description" content="${metaDescription?xml}" />
  </@hst.headContribution>
</#if>

<#if document??>
    <@hst.headContribution category="metadata">
    <meta property="og:url" content="<@hst.link hippobean=document canonical=true fullyQualified=true/>"/>
    </@hst.headContribution>
</#if>

<#if image??>
    <@hst.headContribution category="SEO,metadata">
    <meta property="twitter:card" content="summary_large_image"/>
    </@hst.headContribution>
    <@hst.headContribution category="SEO,metadata">
        <meta property="og:image" content="<@hst.link hippobean=image.original fullyQualified=true/>"/>
    </@hst.headContribution>
    <#if image.description??>
        <@hst.headContribution category="SEO,metadata">
            <meta property="og:image:alt" content="${image.description}"/>
       </@hst.headContribution>
    </#if>
</#if>

<#if dublinCoreSchemaLink??>
  <@hst.headContribution category="SEO" keyHint="hst.seo.schema.dc">
  <link rel="schema.DC" href="${dublinCoreSchemaLink?xml}"/>
  </@hst.headContribution>
</#if>

<#if dublinCoreTermsLink??>
  <@hst.headContribution category="SEO" keyHint="hst.seo.dc.terms">
  <link rel="schema.DCTERMS" href="${dublinCoreTermsLink?xml}"/>
  </@hst.headContribution>
</#if>

<#if dublinCoreCopyrightLink??>
  <@hst.headContribution category="SEO" keyHint="hst.seo.dc.copyright">
  <link rel="DC.rights copyright" href="${dublinCoreCopyrightLink?xml}"/>
  </@hst.headContribution>
</#if>

<#if dublinCoreLanguage??>
  <@hst.headContribution category="SEO" keyHint="hst.seo.dc.language">
  <meta scheme="DCTERMS.RFC3066" name="DC.language" content="${dublinCoreLanguage?xml}"/>
  </@hst.headContribution>
</#if>

<#if dublinCoreTermsCreated??>
  <@hst.headContribution category="SEO" keyHint="hst.seo.dc.terms.created">
  <meta name="DCTERMS.created" content="${dublinCoreTermsCreated?xml}"/>
  </@hst.headContribution>
</#if>

<#if dublinCoreTermsModified??>
  <@hst.headContribution category="SEO" keyHint="hst.seo.dc.terms.modified">
  <meta name="DCTERMS.modified" content="${dublinCoreTermsModified?xml}"/>
  </@hst.headContribution>
</#if>


<#if hstRequest.requestContext.channelManagerPreviewRequest>
<div>
  <img src="<@hst.link path="/images/essentials/catalog-component-icons/seo.svg" />"> Click to edit SEO parameters
  <#if title??>
    <p>Title: ${title?xml}</p>
  </#if>
  <#if metaDescription??>
    <p>Description: ${metaDescription?xml}</p>
  </#if>
  <#if image??>
      <p> Social Media image:<#if image.description??> ${image.description}</#if></p>
      <img src="<@hst.link hippobean=image.thumbnail fullyQualified=true/>"/>
  </#if>
</div>
</#if>
