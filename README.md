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

New feature: deploy:find-and-deploy
-----------------------------------

This new mojo reads an artifactId, groupId and version from the commandline and looks for the artifact in your remote repositories. Once found it deploys it to your repository.

This mojo is useful to import artifacts in your own repository without using a repository manager.

Building the plugin
-------------------
The unit tests are broken, so ...
mvn -Dmaven.test.skip=true clean install
