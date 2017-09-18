FROM sonatype/nexus3

#Nexus 3 version to use
ARG NEXUS_VERSION=3.5.1-02
ARG RUNDECK_PLUGIN_VERSION=1.0.0-SNAPSHOT

USER root

# Stop Nexus 3
CMD ["bin/nexus", "stop"]

# Copy and Configure Nexus Rundeck Plugin
RUN mkdir -p system/com/nongfenqi/nexus/plugin/${RUNDECK_PLUGIN_VERSION}

ADD build/libs/nexus3-rundeck-plugin-${RUNDECK_PLUGIN_VERSION}.jar \
    system/com/nongfenqi/nexus/plugin/${RUNDECK_PLUGIN_VERSION}/nexus3-rundeck-plugin-${RUNDECK_PLUGIN_VERSION}.jar
RUN chomd 644 system/com/nongfenqi/nexus/plugin/${RUNDECK_PLUGIN_VERSION}/nexus3-rundeck-plugin-${RUNDECK_PLUGIN_VERSION}.jar

RUN echo "bundle.mvn\:com.nongfenqi.nexus.plugin/nexus3-rundeck-plugin/"${RUNDECK_PLUGIN_VERSION}" = mvn:com.nongfenqi.nexus.plugin/nexus3-rundeck-plugin/"${RUNDECK_PLUGIN_VERSION} \
    >> etc/karaf/profile.cfg \
    && echo "reference\:file\:com/nongfenqi/nexus/plugin/"${RUNDECK_PLUGIN_VERSION}"/nexus3-rundeck-plugin-"${RUNDECK_PLUGIN_VERSION}".jar = 200" \
    >> etc/karaf/startup.properties

USER nexus

# Start Nexus 3
CMD ["bin/nexus", "run"]
