#!/bin/bash
echo mvn test -Dtest=TestEsv2Yaml -Dsrc=$HIPPO_CODE_HOME/$1/$2/master/$3/src/main/resources -Dtarget=target/esv2yaml/$2/$3
mvn test -Dtest=TestEsv2Yaml -Dsrc=$HIPPO_CODE_HOME/$1/$2/master/$3/src/main/resources -Dtarget=target/esv2yaml/$2/$3

