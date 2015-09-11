#!/bin/bash

for pom in */pom.xml
do
    ( cd `dirname $pom` ; mvn deploy:deploy-file )
done
