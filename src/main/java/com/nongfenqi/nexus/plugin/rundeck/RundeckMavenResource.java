/*
 * Copyright 2017
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated
 * documentation files (the "Software"), to deal in the Software without restriction, including without limitation the
 * rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to
 * permit persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions
 * of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO
 * THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT,
 * TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS
 * IN THE SOFTWARE.
 */
package com.nongfenqi.nexus.plugin.rundeck;

import org.apache.http.client.utils.DateUtils;
import org.sonatype.nexus.repository.search.*;
import org.sonatype.goodies.common.ComponentSupport;
import org.sonatype.nexus.blobstore.api.Blob;
import org.sonatype.nexus.repository.Repository;
import org.sonatype.nexus.repository.manager.RepositoryManager;
import org.sonatype.nexus.repository.storage.Asset;
import org.sonatype.nexus.repository.storage.Bucket;
import org.sonatype.nexus.repository.storage.StorageFacet;
import org.sonatype.nexus.repository.storage.StorageTx;
import org.sonatype.nexus.rest.Resource;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Response;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import static com.google.common.base.Preconditions.checkNotNull;
import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static org.sonatype.nexus.common.text.Strings2.isBlank;

@Named
@Singleton
@Path("/rundeck/maven/options")
public class RundeckMavenResource
        extends ComponentSupport
        implements Resource {
    private final SearchService searchService;

    private final RepositoryManager repositoryManager;

    private static final Response NOT_FOUND = Response.status(404).build();


    @Inject
    public RundeckMavenResource(
            SearchService searchService,
            RepositoryManager repositoryManager
    ) {
        this.searchService = checkNotNull(searchService);
        this.repositoryManager = checkNotNull(repositoryManager);
    }

    @GET
    @Path("content")
    public Response content(
            @QueryParam("r") String repositoryName,
            @QueryParam("g") String groupId,
            @QueryParam("a") String artifactId,
            @QueryParam("v") String version,
            @QueryParam("c") String classifier,
            @QueryParam("p") @DefaultValue("jar") String extension
    ) {


        // default version
        if ("LATEST".equals(version)) {
            version = null;
        }
        version = Optional.ofNullable(version).orElse(latestVersion(
                repositoryName, groupId, artifactId, classifier, extension
        ));

        // valid params
        if (isBlank(repositoryName) || isBlank(groupId) || isBlank(artifactId) || isBlank(version)) {
            return NOT_FOUND;
        }

        Repository repository = repositoryManager.get(repositoryName);
        if (null == repository || !repository.getFormat().getValue().equals("maven2")) {
            return NOT_FOUND;
        }

        StorageFacet facet = repository.facet(StorageFacet.class);
        Supplier<StorageTx> storageTxSupplier = facet.txSupplier();

        log.debug("rundeck download repository: {}", repository);
        final StorageTx tx = storageTxSupplier.get();
        tx.begin();
        Bucket bucket = tx.findBucket(repository);
        log.debug("rundeck download bucket: {}", bucket);

        if (null == bucket) {
            return commitAndReturn(NOT_FOUND, tx);
        }

        String folderVersion = version.replaceAll("-\\d{8}\\.\\d{6}\\-\\d+", "-SNAPSHOT");
        String fileName = artifactId + "-" + version + (isBlank(classifier) ? "" : ("-" + classifier)) + "." + extension;
        String path = groupId.replace(".", "/") +
                "/" + artifactId +
                "/" + folderVersion +
                "/" + fileName;

        Asset asset = tx.findAssetWithProperty("name", path, bucket);
        log.debug("rundeck download asset: {}", asset);
        if (null == asset) {
            return commitAndReturn(NOT_FOUND, tx);
        }
        asset.markAsDownloaded();
        tx.saveAsset(asset);
        Blob blob = tx.requireBlob(asset.requireBlobRef());
        Response.ResponseBuilder ok = Response.ok(blob.getInputStream());
        ok.header("Content-Type", blob.getHeaders().get("BlobStore.content-type"));
        ok.header("Content-Disposition", "attachment;filename=\"" + fileName + "\"");
        return commitAndReturn(ok.build(), tx);
    }

    @GET
    @Path("artifactId")
    @Produces(APPLICATION_JSON)
    public List<String> artifactId(
            @DefaultValue("50") @QueryParam("l") int limit,
            @QueryParam("r") String repository,
            @QueryParam("g") String groupId,
            @QueryParam("v") String version,
            @QueryParam("c") String classifier,
            @QueryParam("p") String extension
    ) {
        log.debug("param value, limit: {}, repository: {}, groupId: {}, version: {}, classifier: {}, extension: {}", limit, repository, groupId, version, classifier, extension);

        SearchRequest.Builder searchRequestBuilder = SearchRequest.builder();
        searchRequestBuilder.searchFilter("format", "maven2");

        if (!isBlank(repository)) {
            searchRequestBuilder.searchFilter("repository_name", repository);
        }
        if (!isBlank(groupId)) {
            searchRequestBuilder.searchFilter("attributes.maven2.groupId", groupId);
        }
        if (!isBlank(version)) {
            searchRequestBuilder.searchFilter("attributes.maven2.version", version);
        }

        classifier = !isBlank(classifier) ? classifier : "";
        searchRequestBuilder.searchFilter("assets.attributes.maven2.classifier", classifier);

        if (!isBlank(extension)) {
            searchRequestBuilder.searchFilter("assets.attributes.maven2.extension", extension);
        }

        searchRequestBuilder.offset(0)
                .limit(limit)
                .sortField("assets.attributes.content.last_modified")
                .sortDirection(SortDirection.DESC);

        SearchRequest searchRequest = searchRequestBuilder.build();
        log.debug("rundeck maven version query: {}", searchRequest);

        SearchResponse result = searchService.search(searchRequest);
        return result.getSearchResults().stream()
                .map(this::searchResults2RundeckXOArtifact)
                .collect(Collectors.toList());
    }

    @GET
    @Path("version")
    @Produces(APPLICATION_JSON)
    public List<RundeckXO> version(
            @DefaultValue("10") @QueryParam("l") int limit,
            @QueryParam("r") String repository,
            @QueryParam("g") String groupId,
            @QueryParam("a") String artifactId,
            @QueryParam("c") String classifier,
            @QueryParam("p") String extension
    ) {

        log.debug("param value, repository: {}, limit: {}, groupId: {}, artifactId: {}, classifier: {}, extension: {}", repository, limit, groupId, artifactId, classifier, extension);

        SearchRequest.Builder searchRequestBuilder = SearchRequest.builder();
        searchRequestBuilder.searchFilter("format", "maven2");

        if (!isBlank(repository)) {
            searchRequestBuilder.searchFilter("repository_name", repository);
        }
        if (!isBlank(groupId)) {
            searchRequestBuilder.searchFilter("attributes.maven2.groupId", groupId);
        }
        if (!isBlank(artifactId)) {
            searchRequestBuilder.searchFilter("attributes.maven2.artifactId", artifactId);
        }

        classifier = !isBlank(classifier) ? classifier : "";
        searchRequestBuilder.searchFilter("assets.attributes.maven2.classifier", classifier);

        if (!isBlank(extension)) {
            searchRequestBuilder.searchFilter("assets.attributes.maven2.extension", extension);
        }

        searchRequestBuilder.offset(0)
                .limit(limit)
                .sortField("assets.attributes.content.last_modified")
                .sortDirection(SortDirection.DESC);

        SearchRequest searchRequest = searchRequestBuilder.build();
        log.debug("rundeck maven version query: {}", searchRequest);

        SearchResponse result = searchService.search(searchRequest);
        return result.getSearchResults().stream()
                .map(this::searchResults2RundeckXO)
                .collect(Collectors.toList());
    }

    private String latestVersion(String repositoryName, String groupId, String artifactId, String classifier, String extension) {
        List<RundeckXO> latestVersion = version(1, repositoryName, groupId, artifactId, classifier, extension);
        if (!latestVersion.isEmpty()) {
            return latestVersion.get(0).getValue();
        }
        return null;
    }

    private String searchResults2RundeckXOArtifact(ComponentSearchResult componentSearchResult) {
        String artifactId = null;
        List<AssetSearchResult> assets = componentSearchResult.getAssets();
        Map<String, Object> attributes = assets.get(0).getAttributes();
        Map<String, Object> content = (Map<String, Object>) attributes.get("maven2");
        
        if (content != null && content.containsKey("artifactId")) {
            artifactId = (String) content.get("artifactId");
        }

        return artifactId;
    }

    private RundeckXO searchResults2RundeckXO(ComponentSearchResult componentSearchResult) {
        String version = componentSearchResult.getVersion();
        List<AssetSearchResult> assets = componentSearchResult.getAssets();
        Map<String, Object> attributes = assets.get(0).getAttributes();
        Map<String, Object> content = (Map<String, Object>) attributes.get("content");
        String lastModifiedTime = "null";
        if (content != null && content.containsKey("last_modified")) {
            Long lastModified = (Long) content.get("last_modified");
            lastModifiedTime = DateUtils.formatDate(new Date(lastModified), "yyyy-MM-dd HH:mm:ss");
        }

        return RundeckXO.builder().name(version + " (" + lastModifiedTime + ")").value(version).build();
    }

    private Response commitAndReturn(Response response, StorageTx tx) {
        if (tx.isActive()) {
            tx.commit();
        }
        return response;
    }

}
