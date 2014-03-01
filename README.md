# view amp for alfresco repository (overlay on alfresco.war)
This project contains the content model and webscripts for accessing and modifying the alfresco repository. 

Location of content model: src/main/amp/config/alfresco/module/view-repo/viewModel.xml

This is registered by spring config in module-context.xml (which imports another spring config xml)

Location of webscripts: src/main/amp/config/alfresco/extension/templates/webscripts

Eclipse/Maven
    Make sure your JAVA_HOME is set to a java 1.7 installation.

    Install maven if you don't have it (mac & linux may have it pre-installed)	
		To install maven (not included in OS X 10.9 (Mavericks)):
		1. Install homebrew by copy/pasting the last command in this link into the terminal: http://brew.sh/
		2. Install maven using the command: 'brew install maven'. 
		Note: This is maven 3.1.1. If this version causes problems, install maven 3.0 using command: 'brew install maven30'
		
    You might want to avoid yoxos.

    For a fresh Eclipse Indigo for JavaEE installation, install new software: egit and maven (no need to add update site)
    		- If Maven is not installed in Eclipse, go to Help -> Eclipse Marketplace -> type in "m2eclipse" in search box 
		      & install the first item (Maven Integration for Eclipse WTP (Juno))	

    Make sure you have a local checkout of alfresco from git.
    	- Clone Alfresco-View-Repo from git. To do so: 
    	1. Go to Git Repo Perspective in Eclipse
    	2. Click clone a git repository icon in "Git Repositories" menu
    	3. Copy the Https link from Stash for Alfresco-View-Repo Refractor or Master branch (Use Refractor to set up a working Alfresco; you can switch to other branches later)
    	4. Paste into "URI" textbox in Clone Git Repository window in Eclipse
    	5. Change Protocol to HTTPS
    	6. Make sure to enter fields for "User" and "Password" with your username and password
    	7. Click Next, Select the branches you want to clone, and click next again
    	8. Ensure that "Import all existing projects after clone finishes" item is checked & click finish

    Import a maven project from the local ~/git/alfresco-view-repo
        1. Go to Java Perspective in Eclipse
        2. File -> Import ->In dialog window click: Git -> Projects from Git. Click next.
        3. Select Local, Click next.
        4. Select Git Repository you want to include in Java workspace. Click Next.
        5. Select "Import Existing Projects". Click  Next.
        6. Select projects to import and then click finish.
        7. If project is not a maven project, right click project in Java perspective->Maven->Convert to Maven Project
    
    To connect the project to git, right-click the project in the Package Explorer and select Team->Share Project->Git->"Use or create repository in parent folder of project".

    If there are errors and you can resolve them later, choose to resolve them later.  After importing, open the pom.xml, and use the second quick-fix choice for each error.
		- If errors inhibit the later steps, then delete the project from java perspective, and re-import it from the EGit Perspective as shown above.
		
# building, setting up maven, jrebel
To build the amp file, do 

    mvn package
    
To run in embedded jetty container and H2 db, (with jrebel and remote debugging!), the javaagent should point to your licensed jrebel.jar, address should be a port you can attach a debugger to

	export MAVEN_OPTS='-Xms256m -Xmx1G -XX:PermSize=300m -Xdebug -Xrunjdwp:transport=dt_socket,address=10000,server=y,suspend=n -javaagent:/Applications/jrebel/jrebel.jar'
	
    mvn integration-test -Pamp-to-war -Dmaven.test.skip=false
    
NOTE: It's possible that Eclipse can get in the way of the maven execution. So, when running maven, temporarily turn off the Eclipse->Project->Build Automatically". Once the Jetty server is up and running, turn it back on so you can make changes hot swap your code updates.
If you get an error about avmRemote or something like that, you may need to update your /etc/hosts to with something like COMPUTER_NAME 127.0.0.1
    
To clean all data and artifacts

    mvn clean -Ppurge

To execute JUnit tests and attach a debugger

    mvn -Dmaven.surefire.debug -Dmaven.test.skip=false test
    
	Put JUnit test java files in src/test/java
    Set a breakpoint in a JUnit test.
    Run a Remote Java Application configuration with localhost for Host and 5005 for Port.
    Follow DemoComponentTest.java and its entry in service-content.xml


To update the target/view-repo-war manually

	mvn package -Pamp-to-war
	
# Debug
To attach an eclipse debugger, there's a view-repo.launch, you can use it to debug at the 10000 port, or change the debug config port if you didn't use 10000 in the maven opts

Jrebel is monitoring the target/classes and src/main/amp/config dir for changes, and the src/main/amp/web for static file changes, make sure the eclipse build automatically is on, and usually any changes to java classes or spring configs will be reloaded automagically

# Testing
Go to [http://localhost:8080/view-repo/](http://localhost:8080/view-repo/) for the alfresco explorer interface (it'll take a while to startup)

In the repository, create a "/ViewEditor/model" folder and "/ViewEditor/snapshots" folder, the view information will be created in these spaces.

Use the view import/export from the latest dev release of mdk, or you can use curl commands below

Post test view:

    curl -H "Content-Type: application/json" --data @test-data/postview.json -X POST -u admin:admin "http://localhost:8080/view-repo/service/rest/views/_17_0_1_2_407019f_1340300322136_11866_17343?force=true&recurse=true&doc=true&user=dlam"

Post view hierarchy:

    curl -H "Content-Type: application/json" --data @test-data/postviewhierarchy.json -X POST -u admin:admin "http://localhost:8080/view-repo/service/rest/views/_17_0_1_2_407019f_1340300322136_11866_17343/hierarchy"

Post DocGen Manual

    curl -H "Content-Type: application/json" --data @test-data/docgenManual.json -X POST -u admin:admin "http://localhost:8080/view-repo/service/rest/views/_17_0_3_244e03eb_1333856871739_813876_16836?force=true&recurse=true&doc=true&user=dlam"

Post comments

	curl -H "Content-Type: application/json" --data @test-data/postcomment.json -X POST -u admin:admin "http://localhost:8080/view-repo/service/rest/views/_17_0_1_2_407019f_1340300322136_11866_17343/comments?recurse=true"

Post Project organization

	curl -H "Content-Type: application/json" --data @test-data/postproject.json -X POST -u admin:admin "http://localhost:8080/view-repo/service/rest/projects/europa"
	
Get test view

	curl -u admin:admin "http://localhost:8080/view-repo/service/rest/views/_17_0_1_2_407019f_1340300322136_11866_17343?recurse=true"

Get DocGen Manual

	curl -u admin:admin "http://localhost:8080/view-repo/service/rest/views/_17_0_3_244e03eb_1333856871739_813876_16836?recurse=true"

Get comments

	curl -u admin:admin "http://localhost:8080/view-repo/service/rest/views/_17_0_1_2_407019f_1340300322136_11866_17343/comments?recurse=true"
	
To open the javascript debugger: [http://localhost:8080/view-repo/service/api/javascript/debugger](http://localhost:8080/view-repo/service/api/javascript/debugger) (you may have to close and reopen to get it to step through on consecutive script calls)

To refresh changes to scripts (they have to be updated in the "target/view-repo-war/WEB-INF/classes/alfresco/extension/..."): [http://localhost:8080/view-repo/service/index](http://localhost:8080/view-repo/service/index) hit refresh at the botton

Maven archetype from [Alfresco Maven SDK](https://artifacts.alfresco.com/nexus/content/repositories/alfresco-docs/alfresco-lifecycle-aggregator/latest/index.html)

Documentation links:

* [Web Scripts](http://docs.alfresco.com/4.2/index.jsp?topic=%2Fcom.alfresco.enterprise.doc%2Fconcepts%2Fws-architecture.html)
* [AMP modules](http://docs.alfresco.com/4.2/index.jsp?topic=%2Fcom.alfresco.enterprise.doc%2Fconcepts%2Fdev-extensions-modules-intro.html)
* [Content modeling](http://docs.alfresco.com/4.2/index.jsp?topic=%2Fcom.alfresco.enterprise.doc%2Fconcepts%2Fcontent-modeling-about.html)
* [Forms?](http://wiki.alfresco.com/wiki/Forms)
