SET INTELLIJ_HOME=C:\MasterProjectRepo\AntidoteBenchmark\extras
CALL mvn install:install-file -Dfile="%INTELLIJ_HOME%\javac2.jar" -DgroupId=com.intellij -DartifactId=javac2 -Dversion=17.1.5 -Dpackaging=jar
CALL mvn install:install-file -Dfile="%INTELLIJ_HOME%\asm-all.jar" -DgroupId=com.intellij -DartifactId=asm-all -Dversion=17.1.5 -Dpackaging=jar
CALL mvn install:install-file -Dfile="%INTELLIJ_HOME%\forms_rt.jar" -DgroupId=com.intellij -DartifactId=forms_rt -Dversion=17.1.5 -Dpackaging=jar