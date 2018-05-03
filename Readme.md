# Installation Instructions for antidote-bench<br />

Before you can run mvn install or mvn package on the maven project in this repository you must install some additional dependencies.<br />

Just run the installdependencies.sh (Windows -> run with Git Bash alias Git for Windows) to install all necessary dependencies.

Check the script to make sure it does not do anything you don't want to happen.

The script installs several maven plugins and dependencies that are required to build this project.

In the folder SNAPSHOT_BUILD is the latest build of the project (subject to change because of potential conflicts)

To run the built application, Docker must be running. (Not sure if this will work properly with Linux)

In Windows, the following Docker Settings are required (not sure for Linux)

- In General Settings -> Activate "Expose daemon to tcp://localhost:2375 without TLS"
- In Daemon Setting (not sure if always necessary) -> Use Basic and Deactivate "Experimental features"

Also on Windows the Docker Host is sometimes in a "bad" state which prevents proper interaction with containers.

This typically is the case when Docker starts instantly without needing extra starting time.

If the Docker Host is in a "bad" state this application will tell you and you need to restart Docker.


## Using antidote-bench<br />

The folder SNAPSHOT_BUILD was created to run and test the application. This way you don't need to build the application yourself and can simply run it in the SNAPSHOT_BUILD folder. The local .gitignore makes sure that only the build application goes into the git repository and not settings and other stuff that is generated automatically when the application starts. That means you can use persistent settings and reuse a local git repository of Antidote that is used by the application for commit information.   


## Other stuff that is relevant but not described properly.

Link to Intellij Designer solution explanation -> https://stackoverflow.com/questions/32747917/intellij-gui-designer-maven-executable-jar-export/45125398#45125398 <br />

Link to repository -> https://github.com/jorichard/ideauidesigner-maven-plugin <br />

If everything worked you can now run mvn install or mvn package on the maven project in the folder AntidoteBenchmark.


https://syncfree.github.io/antidote/  <br />
https://arewefastyet.com/  <br />
https://github.com/brianfrankcooper/YCSB/wiki  <br />
https://github.com/basho/basho_bench  <br />
https://www.docker.com/  <br />
https://github.com/SyncFree/antidote

java benchmarking frameworks  <br />

https://github.com/brianfrankcooper/YCSB  <br />
http://jmeter.apache.org/  <br />

Antidote java client  <br />
https://github.com/SyncFree/antidote-java-client


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