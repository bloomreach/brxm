How to test and build Hippo Site Toolkit
========================================

Introduction
------------

  Very brief introduction to how to build, test and run the Hippo Site Toolkit!

Requirements
------------

* Java 8
* Maven 3 (3.2.1+)
* [Optional] Tomcat 8.0.x if you want to deploy and run on Tomcat.)

Build
----- 
     
1. Build with testing
    
       mvn clean install
    
2. Build with skipping tests

       mvn clean install -DskipTests 

3. Build with Hippo Snapshot Repository Flag
      - Note: Mostly you do not need this when you check out and build a released version!
              Needed only when you build the TRUNK having snapshot dependencies!
      - Note: Please beware that the '-Dhippo.snapshots' option is effective only when you define
              a Maven profile with this property in your settings.xml for the Hippo snapshot repository.
              For details, see https://www.onehippo.org/library/development/build-hippo-cms-from-scratch.html
      - Add '-Dhippo.snapshots' for snapshot dependencies.
        For example,
        
          $ mvn clean install -Dhippo.snapshots -DskipTests

Creating the mvn site with javadocs: type from the root
-------------------------------------------------------

    mvn site

   All aggregated javadocs are generated in target/site/apidocs and all aggregated test javadocs are generated in target/site/testapidocs.

   In addition, "The User API JavaDocs", which includes only api, mock, commons, content-beans and client, are generated in target/site/userapidocs/ separately.
   Also, a menu link item for "The User API JavaDocs" has been added in site.xml.

   Use

    mvn site:stage

   If you want to see sub-module links in your generated site, also see
   http://maven.apache.org/plugins/maven-site-plugin/usage.html
      