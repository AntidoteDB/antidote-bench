Installation Instructions

-> go to the folder \AntidoteBenchmark\extras\ideauidesigner-maven-plugin in the repository and open the command line on that folder

Run the following 4 commands (note that "%current_path%" means the full path to that file) 
These commands can also be run by the .bat file or .sh file in the directory (depending on the operating system) 
But check their contents before you run them

mvn install:install-file -Dfile="%current_path%\javac2.jar" -DgroupId=com.intellij -DartifactId=javac2 -Dversion=17.1.5 -Dpackaging=jar
mvn install:install-file -Dfile="%current_path%\asm-all.jar" -DgroupId=com.intellij -DartifactId=asm-all -Dversion=17.1.5 -Dpackaging=jar
mvn install:install-file -Dfile="%current_path%\forms_rt.jar" -DgroupId=com.intellij -DartifactId=forms_rt -Dversion=17.1.5 -Dpackaging=jar
mvn install

The first three commands install the necessary Intellij libraries for the UI designer
The last command installs the maven plugin that allows compiling and packaging the project with maven
Link to solution explanation -> https://stackoverflow.com/questions/32747917/intellij-gui-designer-maven-executable-jar-export/45125398#45125398
Link to repository -> https://github.com/jorichard/ideauidesigner-maven-plugin 




Links for Documentation


Antidote Java Client

https://github.com/SyncFree/antidote-java-client

https://www.javadoc.io/doc/eu.antidotedb/antidote-java-client/0.1.0

https://static.javadoc.io/eu.antidotedb/antidote-java-client/0.1.0/eu/antidotedb/client/package-summary.html#package.description


Docker Java API

https://github.com/spotify/docker-client

https://github.com/spotify/docker-client/blob/master/docs/user_manual.md


Docker API Docs (Useful for understanding the Java API)

https://docs.docker.com/engine/api/v1.32/


MapDB Java Database

https://jankotek.gitbooks.io/mapdb/content/