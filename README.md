sonar-jacococd-plugin
=====================

This is a extraction version of [sonar-jacoco-plugin](https://github.com/SonarSource/sonar-java) which enables comment directives offered by @mchr3k 's [jacoco fork](https://github.com/mchr3k/jacoco).  It is made separated from sonar-java so that there is no conflict between this one and the original sonar-jacoco-plugin. 

how to build 
=====================

**this plugin depends on following artifacts to build:**

* sonar-java-plugin     standard version >= 1.3
* jacoco-*              from @mchr3k 's fork, [branch filters](https://github.com/mchr3k/jacoco/tree/filters)

since this is no separated versioning for the jacoco fork, please set the right jacoco version in the pom.xml. 

how to install
======================

copy the generated target/sonar-jacococd-plugin/ into sonarqube's extensions/plugins and restart the server. 
When built,  the plugin relies on nothing except the standard sonar-java provided in sonarqube. 

how to use
======================

set your coverage tool to "jacococd" instead of standard "jacoco" in your pom.xml. The original jacoco still works. 


more advanced
======================

if you uses the "reuseReport" functions with maven-sonar-plugin, which generally generates coverage data directly from maven-jacoco-plugin and parse the result with sonar-jacoco-plugin, please use this built [maven-jacoco-plugin] (https://github.com/huangxiwei/jacoco/tree/filters) from my fork instead of the standard one.  mchr3k's fork has only enabled ant plugin. 

