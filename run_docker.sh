# This command run a Docker image (springyboing/docker-gradlew), with gradlew environment ready, to build the plugin
docker run -it --rm --name nexus3-rundeck-plugin -v "$PWD" -w /tmp/nexus3-rundeck-plugin springyboing/docker-gradlew "./gradlew jar"

# Build Nexus 3 + nexus3-rundeck-plugin docker
docker build --rm=true --tag=nexus3-rundeck-plugin/nexus3 .

# Start Nexus 3 with Rundeck Plugin
docker run -d -p 8081:8081 --name nexus nexus3-rundeck-plugin/nexus3
