# Bloomreach Experience Manager (brXM)
This git repository contains all modules that are part of the brXM releases.
Projects included here are Apache licensed and sources are published as open source (foss).


## Build Instructions
This product uses Maven (http://maven.apache.org/) for build tooling.

Build with testing:

    mvn clean install

Build without testing:

    mvn clean install -DskipTests -DskipITs
    
Build in parallel without testing:

    mvn clean install -DskipTests -DskipITs -T1C

Note: Parallel builds are only supported with -DskipITs. Integration tests must be run
with a single-threaded build.

Validate license headers:
```shell
mvn clean && mvn validate -Pdefault,pedantic
```