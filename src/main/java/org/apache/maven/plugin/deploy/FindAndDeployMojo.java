package org.apache.maven.plugin.deploy;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.DefaultArtifact;
import org.apache.maven.artifact.factory.ArtifactFactory;
import org.apache.maven.artifact.handler.ArtifactHandler;
import org.apache.maven.artifact.metadata.ArtifactMetadataSource;
import org.apache.maven.artifact.resolver.ArtifactCollector;
import org.apache.maven.artifact.resolver.ArtifactResolutionResult;
import org.apache.maven.artifact.resolver.ResolutionListener;
import org.apache.maven.artifact.resolver.filter.ArtifactFilter;
import org.apache.maven.artifact.resolver.filter.ScopeArtifactFilter;
import org.apache.maven.artifact.versioning.VersionRange;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.MavenProjectBuilder;

import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Set;

/**
 * Find an artifact in a repo and deploys it
 *
 * @author <a href="mailto:dieter.houthooft@gmail.com">Dieter Houthooft</a>
 * @goal find-and-deploy
 * @threadSafe
 */
public class FindAndDeployMojo extends DeployMojo {

    /**
     * @parameter expression="${groupId}"
     * @required
     */
    private String groupId;

    /**
     * @parameter expression="${artifactId}"
     * @required
     */
    private String artifactId;

    /**
     * @parameter expression="${version}"
     * @required
     */
    private String version;

    /**
     * @parameter expression="${type}"
     * @required
     */
    private String type;

    /**
     * Used to look up Artifacts in the remote repository.
     *
     * @component
     */
    protected ArtifactFactory factory;

    /**
     * Used to look up Artifacts in the remote repository.
     *
     * @component
     */
    protected ArtifactHandler handler;

    /**
     * Used to look up Artifacts in the remote repository.
     *
     * @component
     */
    protected ArtifactCollector collector;

    /**
     * Used to look up Artifacts in the remote repository.
     *
     * @component
     */
    protected ArtifactMetadataSource metadataSource;

    public void execute() throws MojoExecutionException, MojoFailureException {
        try {

            Artifact artifact = artifactFactory.createBuildArtifact( project.getGroupId(), project.getArtifactId(),
                                                                 project.getVersion(), project.getPackaging() );

            VersionRange vrange = VersionRange.createFromVersionSpec(version);
            Artifact target = new DefaultArtifact(groupId, artifactId, vrange, Artifact.SCOPE_RUNTIME, type, "", handler);

            project.setDependencyArtifacts(Collections.singleton(target));
            project.resolveActiveArtifacts();

            ArtifactResolutionResult result = artifactResolver.resolveTransitively(project.getDependencyArtifacts(),
                    artifact,
                    project.getManagedVersionMap(),
                    this.local,
                    this.remoteRepos,
                    metadataSource, new DummyArtifactFilter());
            Set resolvedArtifacts = result.getArtifacts();

            executeWithArtifacts(resolvedArtifacts);

        } catch (Exception e) {
            throw new MojoExecutionException(e.getMessage(), e);
        }
    }

    public class DummyArtifactFilter implements ArtifactFilter {

        public boolean include(Artifact artifact) {
            return true;
        }
    }
}
