A handier maven deploy plugin
=============================

This repo is fork of the official maven deploy plugin, not a real github fork as the original maven deploy plugin is not on git hub :-(

New feature: filterPom
----------------------

This new configuration parameter to the deploy:deploy mojo filters your pom files before being deployed. It removes all non-useful information to people who don't need to build your code, but just want to use it. 

The motivation for this feature is that sometimes in a corporate environment you don't want to leak details about your build environment to the outside.

New feature: deployDependencies
-------------------------------

This new configuration parameter to the deploy:deploy mojo also deploys your dependencies to the remote repo. This is useful if you don't want to bother your clients to look for your dependencies themselves.

It is also useful if you want to deploy only some specific artifacts within your project to some public repo (like the SDK's for your commercial product). You can create an artifact which lists all your public artifacts as dependencies and set the deploy plugin to deployDependencies. This is handier than messing around with profiles.

New feature: failureIsAnOption
------------------------------

This configuration parameter prevents the build from crashing when the artifact or a dependency fails to deploy. This can be handy when you don't want your build to fail when your repository is not available. 

New feature: deploy:find-and-deploy
-----------------------------------

This new mojo reads an artifactId, groupId and version from the commandline and looks for the artifact in your remote repositories. Once found it deploys it to your repository.

This mojo is useful to import artifacts in your own repository without using a repository manager.

options of this mojo are:

*    groupId
*    artifactId
*    type
*    version
*    classifier
*    id (this one overrides all the previous ones by parsing an artifact id string groupId:artifactId:(type):(classifier):version ) 

New feature: white and black listing
------------------------------------
In the plugin configuration a black list of regular expressions can be given. If an artifact matches an expression in the black list it will not be deployed. The expressions are matched against the artifact id string: groupid:artifactid:type:classifier:version

Similary a white list of regular expressions can be given. If an artifact does not match an expression in the white list it will not be deployed.

This feature can be used to enforce a policy.

Configuration looks like this:

    <configuration>
      <blackListPatterns>
         <blackListPattern>mycompany.secret.*</blackListPattern>
         <blackListPattern>.*SNAPSHOT</blackListPattern>
      </blackListPatterns>
    </configuration>

Using the plugin
----------------
Configure this as one of your repositories:

    <pluginRepositories>
        <pluginRepository>
            <id>maven-deploy</id>
            <url>https://raw.github.com/link-nv/maven-repository/master/repository</url>
        </pluginRepository>
    </pluginRepositories>

And add this to your plugins:

    <plugin>
        <artifactId>maven-deploy-plugin</artifactId>
        <version>2.7-6l</version>
    </plugin>


Building the plugin
-------------------
mvn clean install
