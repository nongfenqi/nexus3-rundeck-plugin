# Nexus3 Rundeck plugin

<https://github.com/nongfenqi/nexus3-rundeck-plugin>

## How to install


* add file: `NEXUS_HOME/nexus/system/com/nongfenqi/nexus/plugin/${version}/nexus3-rundeck-plugin-${version}.jar`

* `NEXUS_HOME/nexus/etc/karaf/profile.cfg` append config  `bundle.mvn\:com.nongfenqi.nexus.plugin/nexus3-rundeck-plugin/${version} = mvn:com.nongfenqi.nexus.plugin/nexus3-rundeck-plugin/${version}`
* `NEXUS_HOME/nexus/etc/karaf/startup.properties ` append config `reference\:file\:com/nongfenqi/nexus/plugin/${version}/nexus3-rundeck-plugin-${version}.jar = 200`

* restart nexus3

## Usage

If you installed and the bundle is activeed, like this.

![](./doc/image/bundle-activeed.jpeg)

Now you can add url options from the nexus3 to rundeck.

![](./doc/image/rundeck-options.png)

![](./doc/image/rundeck-execution.jpeg)

### Maven repository

The plugin provides the following new HTTP resources :

- `http://NEXUS_HOST/service/rest/rundeck/maven/options/artifactId` : return a json array with the artifacts of the matching group.
  Parameters (all optional) :
  - `r` : repository ID to search in (null for searching in all indexed repositories)
  - `g` : groupId of the artifacts to match
  - `v` : versions of the artifacts to match
  - `p` : packaging of the artifacts to match ('jar', 'war', etc)
  - `c` : classifier of the artifacts to match ('sources', 'javadoc', etc)
  - `l` : limit - max number of results to return, default value is 10000

- `http://NEXUS_HOST/service/rest/rundeck/maven/options/version` : return a json array with the version of the matching artifacts.
  Parameters (all optional) :
  - `r` : repository ID to search in (null for searching in all indexed repositories)
  - `g` : groupId of the artifacts to match
  - `a` : artifactId of the artifacts to match
  - `p` : packaging of the artifacts to match ('jar', 'war', etc)
  - `c` : classifier of the artifacts to match ('sources', 'javadoc', etc)
  - `l` : limit - max number of results to return, default value is 10

- `http://NEXUS_HOST/service/rest/rundeck/maven/options/content` : return artifact stream
  Parameters (all required) :
  - `r` : repository ID to search in (null for searching in all indexed repositories)
  - `g` : groupId of the artifacts to match
  - `a` : artifactId of the artifacts to match
  - `v` : artifact version, default value is latest version
  - `c` : classifier of the artifacts to match ('sources', 'javadoc', etc)
  - `p` : packaging of the artifacts to match ('jar', 'war', etc), default value is jar


Note that if you want to retrieve the artifact from your Rundeck script, you can use content api, example is:

    wget "http://NEXUS_HOST/service/siesta/rundeck/maven/options/content?r=reponame&g=${option.groupId}&a=${option.artifactId}&v=${option.version}" --content-disposition

### Docker repository

https://hub.docker.com/r/chenlmdocker/docker-nexus3/

### Npm repository

Welcome to contribute


## How to build

### Standard build

- Java 1.8
- run "./gradlew jar"

### Using a Docker

You can build the plugin easily using a Docker image. With this method is not needed to install a local Gradle environment.

- docker run -it --rm --name nexus3-rundeck-plugin -v "$PWD":/tmp/nexus3-rundeck-plugin -w /tmp/nexus3-rundeck-plugin springyboing/docker-gradlew ./gradlew jar

This command run a build inside a Docker image (springyboing/docker-gradlew), with gradlew environment ready.

## Run in Docker

The following script allows you to run a Docker with Nexus 3 with nexus3-rundeck-plugin installed.

-  chmod 700 run_docker.sh
- ./run_docker.sh

Nexus 3 will be running on http://localhost:8081/

The script build the plugin using a Docker image, as is explained in [Using a Docker](#using-a-docker) section.
Using the compiled jar will build the Dockerfile and run a container with Nexus 3 with nexus3-rundeck-plugin installed.
