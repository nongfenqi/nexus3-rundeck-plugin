#FROM openjdk:8 as builder

#COPY . .
#RUN ./gradlew jar

FROM sonatype/nexus3:3.60.0
#FROM  klo2k/nexus3
#docker build . -t nex --platform linux/arm64/v8
#export DOCKER_DEFAULT_PLATFORM=linux/arm64/v8
#docker run -v $PWD/nexus-data:/nexus-data -p 8081:8081 nex
#Nexus 3 version to use
ARG NEXUS_VERSION=3.49.0
ARG RUNDECK_PLUGIN_VERSION=1.1.3

USER root

# Stop Nexus 3
#CMD ["bin/nexus", "stop"]

# Copy and Configure Nexus Rundeck Plugin
RUN mkdir -p system/com/nongfenqi/nexus/plugin/${RUNDECK_PLUGIN_VERSION}


COPY build/libs/nexus3-rundeck-plugin-${RUNDECK_PLUGIN_VERSION}.jar \
    $NEXUS_HOME/system/com/nongfenqi/nexus/plugin/${RUNDECK_PLUGIN_VERSION}/nexus3-rundeck-plugin-${RUNDECK_PLUGIN_VERSION}.jar


RUN chmod 644 $NEXUS_HOME/system/com/nongfenqi/nexus/plugin/$RUNDECK_PLUGIN_VERSION/nexus3-rundeck-plugin-$RUNDECK_PLUGIN_VERSION.jar \
    && echo "bundle.mvn\:com.nongfenqi.nexus.plugin/nexus3-rundeck-plugin/$RUNDECK_PLUGIN_VERSION = mvn:com.nongfenqi.nexus.plugin/nexus3-rundeck-plugin/$RUNDECK_PLUGIN_VERSION" >> $NEXUS_HOME/etc/karaf/profile.cfg \
    && echo "reference\:file\:com/nongfenqi/nexus/plugin/$RUNDECK_PLUGIN_VERSION/nexus3-rundeck-plugin-$RUNDECK_PLUGIN_VERSION.jar = 200" >> $NEXUS_HOME/etc/karaf/startup.properties

USER nexus

# Start Nexus 3
#CMD ["bin/nexus", "run"]
