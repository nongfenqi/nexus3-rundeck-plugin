package com.nongfenqi.nexus.plugin.rundeck;

import org.hamcrest.CoreMatchers;
import org.junit.Test;
import org.sonatype.nexus.repository.manager.RepositoryManager;
import org.sonatype.nexus.repository.search.SearchService;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.mock;

public class RundeckMavenResourceTest {

    @Test
    public void artifactPathIsBuiltProperlyNoClassifierNoExtension() throws Exception {
        SearchService searchService = mock(SearchService.class);
        RepositoryManager repositoryManager = mock(RepositoryManager.class);

        RundeckMavenResource resource = new RundeckMavenResource(searchService, repositoryManager);

        String path = resource.artifactPath("com.example", "my-example", "1.2-SNAPSHOT", null, null);

        assertThat(path, CoreMatchers.equalTo("com/example/my-example/1.2-SNAPSHOT/my-example-1.2-SNAPSHOT.jar"));
    }

    @Test
    public void artifactPathIsBuiltProperlyNoClassifierWithExtension() throws Exception {
        SearchService searchService = mock(SearchService.class);
        RepositoryManager repositoryManager = mock(RepositoryManager.class);

        RundeckMavenResource resource = new RundeckMavenResource(searchService, repositoryManager);

        String path = resource.artifactPath("com.example", "my-example", "1.2-SNAPSHOT", null, ".sassi");

        assertThat(path, CoreMatchers.equalTo("com/example/my-example/1.2-SNAPSHOT/my-example-1.2-SNAPSHOT.sassi"));
    }

    @Test
    public void artifactPathIsBuiltProperlyWithClassifierWithExtension() throws Exception {
        SearchService searchService = mock(SearchService.class);
        RepositoryManager repositoryManager = mock(RepositoryManager.class);

        RundeckMavenResource resource = new RundeckMavenResource(searchService, repositoryManager);

        String path = resource.artifactPath("com.example", "my-example", "1.2-SNAPSHOT", "sources", ".sassi");

        assertThat(path, CoreMatchers.equalTo("com/example/my-example/1.2-SNAPSHOT/my-example-1.2-SNAPSHOT-sources.sassi"));
    }


}
