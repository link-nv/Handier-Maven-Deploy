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
     * artifact handling
     *
     * @component
     */
    protected ArtifactHandler handler;

    public void execute() throws MojoExecutionException, MojoFailureException {
        try {

            VersionRange vrange = VersionRange.createFromVersionSpec(version);
            Artifact target = new DefaultArtifact(groupId, artifactId, vrange, Artifact.SCOPE_RUNTIME, type, "", handler);

            Set<String> scopes = Collections.singleton(Artifact.SCOPE_RUNTIME);
            lcdResolver.resolveProjectDependencies(project, scopes, scopes, session, true, Collections.singleton(target));

            Set deps = project.getDependencyArtifacts();
            deps.add(target);
            project.setDependencyArtifacts(deps);

            this.deployDependencies = true;

            execute();

        } catch (Exception e) {
            throw new MojoExecutionException(e.getMessage(), e);
        }
    }
}
