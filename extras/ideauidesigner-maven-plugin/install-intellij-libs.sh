#!/bin/sh

if [ ! -d "$PWD" ]
then
  echo "Directory does not exist: $PWD"
  exit 1
fi

echo 'Installing Intellij artifacts to Maven local repository'
echo "Intellij home: $PWD"

mvn install:install-file -Dfile="$PWD/javac2.jar" -DgroupId=com.intellij -DartifactId=javac2 -Dversion=17.1.5 -Dpackaging=jar
mvn install:install-file -Dfile="$PWD/asm-all.jar" -DgroupId=com.intellij -DartifactId=asm-all -Dversion=17.1.5 -Dpackaging=jar
mvn install:install-file -Dfile="$PWD/forms_rt.jar" -DgroupId=com.intellij -DartifactId=forms_rt -Dversion=17.1.5 -Dpackaging=jar
mvn install
