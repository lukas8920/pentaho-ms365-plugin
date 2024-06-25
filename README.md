# MS365 Plugin for Pentaho KETTLE

This project contains a plugin for simplifying access to Microsoft Sharepoints via Microsoft Graph for Pentaho KETTLE applications.

## Getting the Plugin

You can either download the plugin from the Github releases page https://xy.com and place the files in ../[your-pentaho-directory]/plugins/ms365-plugin/, or you can build them from source.

## Building from Source Code

The plugin can be built from Source code by installing the prerequisites and by following the steps below:

### Pre-requisites for building the project:
* [Apache Maven](https://maven.apache.org/), version 3+
* [Java JDK](https://adoptopenjdk.net/) 1.8
* [Git](https://git-scm.com)

### Build steps:

1. Clone the Git repository
    ```
    $ git clone https://github.com/lukas8920/pentaho-kettle.git
    ```

2. Set environment variable PENTAHO_HOME for installation directory 
    e.g. Windows:
    ```
    set PENTAHO_HOME="/home/lukas/pdi-ce-9.4.0.0-343"
    ```
    e.g. Linux:
    ```
    export PENTAHO_HOME="/home/lukas/pdi-ce-9.4.0.0-343"
    ```

3. Compile a package
    ```
    $ cd ms365-plugin
    $ mvn clean package
    ```

4. The plugin in the automatically available in the Plugin directory of your Pentaho installation

### Installing the plugin
* Tested with Pentaho Data Integration - Community Edition - version: 9.4.0.0-324 on Linux

When built from the source, no additional steps are necessary. When downloaded, the files need to be placed in the plugins directory of your PDI installation.

## Using the plugin

1. Set up a client application in azure portal
2. Create an MS365 connection in Pentaho with the access credentials taken from the azure portal
3. Ready to use the additional MS365 steps of the plugin

https://github.com/lukas8920/pentaho-kettle/assets/42778010/c86a40a9-cfdb-4489-876b-a76f7187c4b3



Below video shows how you can set up a simple application in Pentaho.
