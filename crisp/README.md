# hippo-addon-crisp

Hippo Addon CRISP (Common Resource Interface and Service Provider).

## TODOs

- Document how to implement a web hook:

```java
    ResourceServiceBroker resourceServiceBroker = CrispHstServices.getDefaultResourceServiceBroker();
    // clear all the cache for instance...
    resourceServiceBroker.getResourceDataCache("demoProductCatalogs").clear();
```

- Document how to use convenient property access by relative path through ```Resource#getValue(String relPath)```.
- Javadocs and documentation (how to configure json api rest service backend, caching, OAuth2 configuration, how to add custom ```ResourceResolver``` for non-JSON-API backends such as RDBMS, NoSQL Database, etc.)
- Web-hook example

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

## Generate Site Documentation

```bash
    $ mvn site
```

Open ```target/site/index.html``` after generation succeeds.

You can also generate a PDF document:

```bash
    $ mvn pdf:pdf
```

Open ```target/pdf/hippo-addon-crisp.pdf``` after generation succeeds.
