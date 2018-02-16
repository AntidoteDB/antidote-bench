SET current_path=%~dp0

CALL mvn install:install-file -Dfile="%current_path%\javac2.jar" -DgroupId=com.intellij -DartifactId=javac2 -Dversion=17.1.5 -Dpackaging=jar
CALL mvn install:install-file -Dfile="%current_path%\asm-all.jar" -DgroupId=com.intellij -DartifactId=asm-all -Dversion=17.1.5 -Dpackaging=jar
CALL mvn install:install-file -Dfile="%current_path%\forms_rt.jar" -DgroupId=com.intellij -DartifactId=forms_rt -Dversion=17.1.5 -Dpackaging=jar
CALL mvn install