<#include "../include/imports.ftl">

<#if title??>
  <@hst.headContribution category="SEO" keyHint="hst.seo.document.title">
  <title>${title?xml}</title>
  </@hst.headContribution>
</#if>

<#if metaDescription??>
  <@hst.headContribution category="SEO" keyHint="hst.seo.document.description">
  <meta name="description" content="${metaDescription?xml}"/>
  </@hst.headContribution>
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

<#if hstRequest.requestContext.cmsRequest>
  <img src="<@hst.link path="/images/essentials/catalog-component-icons/seo.png" />"> Click to edit SEO parameters
</#if>