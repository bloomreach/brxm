# hippo-addon-crisp

Hippo Addon CRISP (Common Resource Interface and Service Provider).

## Features

- Generic External (REST) API Service Invoking Broker Service (```ResourceServiceBroker```)
- Generic Resource Object Model (```Resource```, ```ResourceContainer```, ```ValueMap```, etc.)
- Resource Space Routing Configuration (based on Spring Framework Bean configuration XML file(s))
- Provides single resource retrieval and finding multiple resource results through APIs.
- Caching service in ```ResourceServiceBroker``` level. By default, for the same service invocation, it caches
  the response output for 1 minute in EhCache. It can be easily configurable and customizable.
- ```ResourceServiceBroker``` can be used in both SITE and CMS applications.
- HTTP invocations are done by using ```RestTemplate``` of Spring Framework.
- TODO: Provide OAuth2-based RestTemplate as a default option with easy configurability.
- TODO: ResourceServiceBroker-side pagination
- TODO: Dynamic Configuration change instead of properties and xmls.

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

In a SITE project, add the following in the pom.xml:

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

And, configure your Resource Resolver Map in spring bean assembly XML file
under ```site/src/main/resources/META-INF/hst-assembly/addon/crisp/overrides/``` folder.
See the [resource-resolvers.xml](demo/site/src/main/resources/META-INF/hst-assembly/addon/crisp/overrides/resource-resolvers.xml).

```xml
<?xml version="1.0" encoding="UTF-8"?>
<beans xmlns="http://www.springframework.org/schema/beans"
       xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
       xsi:schemaLocation="http://www.springframework.org/schema/beans http://www.springframework.org/schema/beans/spring-beans-4.1.xsd">

  <bean id="defaultResourceResolverMap"
        class="org.springframework.beans.factory.config.MapFactoryBean">
    <property name="sourceMap">
      <map>
        <entry key="productCatalogs">
          <bean parent="abstractSimpleJacksonRestTemplateResourceResolver"
                class="org.onehippo.cms7.crisp.core.resource.jackson.SimpleJacksonRestTemplateResourceResolver">
            <property name="baseUri" value="${crisp.resource.resolver.productCatalogs.baseUri}" />
            <property name="resourceLinkResolver">
              <bean class="org.onehippo.cms7.crisp.core.resource.FreemarkerTemplateResourceLinkResolver">
                <property name="templateSource">
                  <value>http://www.example.com/products/${(preview == "true")?then("staging", "current")}/sku/${resource.valueMap['SKU']!"unknown"}/overview.html</value>
                </property>
              </bean>
            </property>
          </bean>
        </entry>
      </map>
    </property>
  </bean>

</beans>
```

In the example above, you're defining only one Resource Resolver in the map: ```productCatalogs```.
```productCatalogs``` is a **resource space** that should be used in API calls later through ```ResourceServiceBroker```.

Also, the Resource Resolver class for the ```productCatalogs``` resource space is set to
```org.onehippo.cms7.crisp.core.resource.jackson.SimpleJacksonRestTemplateResourceResolver``` which provides a
very generic resource handlings for JSON response outputs from external REST APIs.

If you want to use ```ResourceServiceBroker``` in your CMS project as well, add the following in the pom.xml:

```xml
    <dependency>
      <groupId>org.onehippo.cms7</groupId>
      <artifactId>hippo-addon-crisp-api</artifactId>
      <version>${hippo-addon-crisp.version}</version>
      <scope>provided</scope>
    </dependency>
```

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
    ResourceContainer productCatalogs = resourceServiceBroker.findResources(RESOURCE_SPACE_PRODUCT_CATALOG,
            "/products?q={fullTextSearchTerm}", pathVars);
    request.setAttribute("productCatalogs", productCatalogs);
```

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
