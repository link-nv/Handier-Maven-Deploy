package org.apache.maven.plugin.deploy;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.DefaultArtifact;
import org.apache.maven.artifact.handler.ArtifactHandler;
import org.apache.maven.artifact.versioning.VersionRange;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;

import java.util.Collections;
import java.util.HashSet;
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
     * @parameter default-value="jar" expression="${type}"
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
            Artifact target = new DefaultArtifact(groupId, artifactId, vrange, Artifact.SCOPE_RUNTIME,
                    type, "", handler);

            Set deps = project.getDependencyArtifacts();
            deps.add(target);
            project.setDependencyArtifacts(Collections.singleton(target));

            Set<String> scopes = Collections.singleton(Artifact.SCOPE_RUNTIME);
            lcdResolver.resolveProjectDependencies(project, scopes, scopes, session, false, Collections.<Artifact>emptySet());

            Set<Artifact> targets = new HashSet<Artifact>();
            targets.add(target);
            super.executeWithArtifacts(targets);

        } catch (Exception e) {
            throw new MojoExecutionException(e.getMessage(), e);
        }
    }
}
