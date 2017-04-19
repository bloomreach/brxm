# hippo-addon-crisp

Hippo Addon CRISP (Common Resource Interface and Service Provider).

## Features

- Generic External (REST) API Service Invoking Broker Service (```ResourceServiceBroker```)
- Generic Resource Object Model (```Resource```, ```ResourceContainer```, ```ValueMap```, etc.)
- Resource Space Routing Configuration (based on Spring Framework Bean configuration XML file(s))
- Provides single resource retrieval and finding multiple resource results through APIs.
- Caching service in ```ResourceServiceBroker``` level. By default, for the same service invocation, it caches
  the response output for 1 minute in EhCache. It is easily configurable and customizable.
- ```ResourceServiceBroker``` can be used in both SITE and CMS applications.
- HTTP invocations are done by using ```RestTemplate``` of Spring Framework.
- OAuth2-based RestTemplate configuration.
- Dynamic Configuration change instead of properties and xmls.
- TODO: ResourceServiceBroker-side pagination.
- TODO: Caching per route.
- TODO: Web-hook based event processing.
- TODO: Optional Java bean binding option from Resource(s).
- TODO: Javadocs and documentation

## Build and install the module itself into local maven repository

```bash
    $ mvn clean install
```

## Running the demo locally

The demo project is located udner demo/ folder. So, move to the demo/ folder to run it locally.

```bash
    $ cd demo
    $ mvn clean verify
    $ mvn -P cargo.run
```

After startup, access the CMS at http://localhost:8080/cms and the site at http://localhost:8080/site.
Logs are located in target/tomcat8x/logs

## Demo Scenarios

- Visit http://localhost:8080/site/news.
- Click on a news article.
- Scroll down to the bottom, and you will see all the product list with generated links.

- Visit http://localhost:8080/cms/
- Select and edit an news document.
- Select 'Related Products' field and try to add some products through the pop up dialog (part of External Document Picker plugin).

Both product list results are from ```ResourceServiceBroker``` service component. See the code example section below for details.

## Installation

In SITE project, add the following in the pom.xml:

```xml
    <dependency>
      <groupId>org.onehippo.cms7</groupId>
      <artifactId>hippo-addon-crisp-api</artifactId>
      <version>${hippo-addon-crisp.version}</version>
      <scope>provided</scope>
    </dependency>

    <dependency>
      <groupId>org.onehippo.cms7</groupId>
      <artifactId>hippo-addon-crisp-core</artifactId>
      <version>${hippo-addon-crisp.version}</version>
    </dependency>

    <dependency>
      <groupId>org.onehippo.cms7</groupId>
      <artifactId>hippo-addon-crisp-hst</artifactId>
      <version>${hippo-addon-crisp.version}</version>
    </dependency>
```

In CMS project, add the following in the pom.xml:

```xml
    <dependency>
      <groupId>org.onehippo.cms7</groupId>
      <artifactId>hippo-addon-crisp-api</artifactId>
      <version>${hippo-addon-crisp.version}</version>
      <scope>provided</scope>
    </dependency>

    <dependency>
      <groupId>org.onehippo.cms7</groupId>
      <artifactId>hippo-addon-crisp-repository</artifactId>
      <version>${hippo-addon-crisp.version}</version>
      <scope>provided</scope>
    </dependency>
```


And, configure your ```ResourceResolver```s in ```/hippo:configuration/hippo:modules/crispregistry/hippo:moduleconfig/crisp:resourceresolvercontainer```.
See the example configurations in the demo project.

**Note**: If you want to use the same ```ResourceServiceBroker``` service component in both SITE and CMS application,
please make sure that the ```hippo-addon-crisp-api``` JAR module should be deployed onto the shared library path, not in each web application's library path.

## How to Use the ResourceBrokerService?

### In SITE application

If you want to use ```ResourceServiceBroker``` API in SITE application (e.g, HstComponent, Relevance collector module, etc.),
then you can use it like the following example (excerpt from [NewsContentComponent.java](demo/site/src/main/java/org/onehippo/cms7/crisp/demo/components/NewsContentComponent.java):

```java
    ResourceServiceBroker resourceServiceBroker = CrispHstServices.getDefaultResourceServiceBroker();
    final Map<String, Object> pathVars = new HashMap<>();
    pathVars.put("fullTextSearchTerm", document.getTitle());
    ResourceContainer productCatalogs =
        resourceServiceBroker.findResources("demoProductCatalogs", "/products?q={fullTextSearchTerm}", pathVars);
    request.setAttribute("demoProductCatalogs", productCatalogs);
```

You can access the resource objects and generate links in freemarker templates like the following examples:

```
  <#if productCatalogs?? && productCatalogs.anyChildContained>
    <article class="has-edit-button">
      <h3>Related Products</h3>
      <ul>
        <#list productCatalogs.childIterator as product>
          <#assign extendedData=product.valueMap['extendedData'] />
          <li>
            <@crisp.link var="productLink" resourceSpace='demoProductCatalogs' resource=product>
              <@crisp.variable name="preview" value="${hstRequestContext.preview?then('true', 'false')}" />
              <@crisp.variable name="name" value="${product.valueMap['name']}" />
            </@crisp.link>
            <a href="${productLink}">
              ${extendedData.valueMap['title']!} (${product.valueMap['SKU']!})
            </a>
          </li>
        </#list>
      </ul>
    </article>
  </#if>
```

**Note**: <@crisp.link /> tag requires ```resourceSpace``` and ```resoruce``` attribute, and it may embed
<@crisp.variable /> tag(s) to pass variables to the underlying ```ResourceLinkResolver```.

### In CMS application

If you want to use ```ResourceServiceBroker``` API in CMS application (e.g, plugin, external document picker
service facade implementation, etc.), then you can use it like the following example
(excerpt from [CommerceProductDataServiceFacade.java](demo/cms/src/main/java/org/onehippo/cms7/crisp/demo/cms/plugin/CommerceProductDataServiceFacade.java):

```java
    private ResourceContainer findAllProductResources(final String queryString) {
        ResourceServiceBroker broker = HippoServiceRegistry.getService(ResourceServiceBroker.class);
        Map<String, Object> variables = new HashMap<>();
        variables.put("queryString", StringUtils.isNotBlank(queryString) ? queryString : "");
        return broker.findResources("productCatalogs", "/products?q={queryString}", variables);
    }
```
