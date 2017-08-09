package com.nongfenqi.nexus.plugin.rundeck;

import org.apache.http.client.utils.DateUtils;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.sort.FieldSortBuilder;
import org.elasticsearch.search.sort.SortOrder;
import org.sonatype.goodies.common.ComponentSupport;
import org.sonatype.nexus.repository.search.SearchService;
import org.sonatype.nexus.rest.Resource;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import javax.ws.rs.*;
import java.util.*;
import java.util.stream.Collectors;

import static com.google.common.base.Preconditions.checkNotNull;
import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static org.elasticsearch.index.query.QueryBuilders.boolQuery;
import static org.elasticsearch.index.query.QueryBuilders.termQuery;
import static org.sonatype.nexus.common.text.Strings2.isBlank;

@Named
@Singleton
@Path("/rundeck/maven/options")
public class RundeckMavenResource extends ComponentSupport implements Resource {

    private final SearchService searchService;

    @Inject
    public RundeckMavenResource(SearchService searchService) {
        this.searchService = checkNotNull(searchService);
    }

    @GET
    @Path("version")
    @Produces(APPLICATION_JSON)
    public List<RundeckXO> version(
            @DefaultValue("10") @QueryParam("step") int limit,
            @QueryParam("r") String repository,
            @QueryParam("g") String groupId,
            @QueryParam("a") String artifactId,
            @QueryParam("c") String classifier,
            @QueryParam("p") String extension
    ) {

        log.debug("param value, repository: {}, limit: {}, groupId: {}, artifactId: {}, classifier: {}, extension: {}", repository, limit, groupId, artifactId, classifier, extension);

        BoolQueryBuilder query = boolQuery();
        query.filter(termQuery("format", "maven2"));

        if (!isBlank(repository)) {
            query.filter(termQuery("repository_name", groupId));
        }
        if (!isBlank(groupId)) {
            query.filter(termQuery("attributes.maven2.groupId", groupId));
        }
        if (!isBlank(artifactId)) {
            query.filter(termQuery("attributes.maven2.artifactId", artifactId));
        }
        if (!isBlank(classifier)) {
            query.filter(termQuery("assets.attributes.maven2.classifier", classifier));
        }
        if (!isBlank(extension)) {
            query.filter(termQuery("assets.attributes.maven2.extension", extension));
        }

        log.debug("rundeck maven version query: {}", query);
        SearchResponse result = searchService.search(
                query,
                Collections.singletonList(new FieldSortBuilder("assets.attributes.content.last_modified").order(SortOrder.DESC)),
                0,
                limit
        );
        return Arrays.stream(result.getHits().hits())
                .map(this::his2RundeckXO)
                .collect(Collectors.toList());
    }


    private RundeckXO his2RundeckXO(SearchHit hit) {
        String version = (String) hit.getSource().get("version");

        List<Map<String, Object>> assets = (List<Map<String, Object>>) hit.getSource().get("assets");
        Map<String, Object> attributes = (Map<String, Object>) assets.get(0).get("attributes");
        Map<String, Object> content = (Map<String, Object>) attributes.get("content");
        long lastModified = (long) content.get("last_modified");

        String lastModifiedTime = DateUtils.formatDate(new Date(lastModified), "yyyy-MM-dd HH:mm:ss");

        return RundeckXO.builder().name(version + " (" + lastModifiedTime + ")").value(version).build();
    }

}
