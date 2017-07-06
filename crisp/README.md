# hippo-addon-crisp

Hippo Addon CRISP (Common Resource Interface and Service Provider).

## Release Version Compatibility

| Plugin Version | CMS Version  |
|:--------------:|:------------:|
| 2.x            | 12.x         |
| 1.x            | 11.x         |

Release notes are maintained in SITE Documentation Source: [release-notes.xml](src/site/xdoc/release-notes.xml) for release notes.

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
