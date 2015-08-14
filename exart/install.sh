#!/bin/bash

for pom in */pom.xml
do
    ( cd `dirname $pom` ; mvn install:install-file )
done
