#!/bin/bash

rm -rf MavenDependenciesAndPlugins
mkdir MavenDependenciesAndPlugins
cd MavenDependenciesAndPlugins

git clone https://github.com/FairPlayer4/YCSB.git YCSB-Embedded --depth 1
cd YCSB-Embedded/core
mvn install

cd ../..

git clone https://github.com/FairPlayer4/maven-shaded-log4j-transformer.git Log4jShadeMavenPlugin --depth 1
cd Log4jShadeMavenPlugin
mvn install

cd ..

git clone https://github.com/FairPlayer4/ideauidesigner-maven-plugin IntellijUIDesignerMavenPlugin --depth 1
cd IntellijUIDesignerMavenPlugin
./install-intellij-libs.sh

cd ../..
rm -rf MavenDependenciesAndPlugins