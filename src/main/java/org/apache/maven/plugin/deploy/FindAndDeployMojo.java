package org.apache.maven.plugin.deploy;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.DefaultArtifact;
import org.apache.maven.artifact.handler.ArtifactHandler;
import org.apache.maven.artifact.versioning.VersionRange;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
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
     */
    private String groupId = null;

    /**
     * @parameter expression="${artifactId}"
     */
    private String artifactId = null;

    /**
     * @parameter expression="${version}"
     */
    private String version = null;

    /**
     * @parameter default-value="jar" expression="${type}"
     */
    private String type = "jar";

    /**
     * @parameter default-value="" expression="${classifier}"
     */
    private String classifier = "";

    /**
     * artifact handling
     *
     * @component
     */
    protected ArtifactHandler handler;

    public void execute() throws MojoExecutionException, MojoFailureException {
        try {
            fillInBlanks();

            VersionRange vrange = VersionRange.createFromVersionSpec(version);
            Artifact target = new DefaultArtifact(groupId, artifactId, vrange, Artifact.SCOPE_RUNTIME,
                    type, classifier, handler);

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

    private void fillInBlanks() throws IOException {
        if (version == null || artifactId == null || groupId == null) {
            BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));

            String suggestion = "";
            if (groupId != null) suggestion = "[" + groupId +"]";
            System.out.print("groupId? " + suggestion + " > ");
            String input = reader.readLine().trim();
            if (!input.isEmpty()) groupId = input;

            if (artifactId != null) suggestion = "[" + artifactId +"]";
            System.out.print("artifactId? " + suggestion + " > ");
            input = reader.readLine().trim();
            if (!input.isEmpty()) artifactId = input;

            if (version != null) suggestion = "[" + version +"]";
            System.out.print("version? " + suggestion + " > ");
            input = reader.readLine().trim();
            if (!input.isEmpty()) version = input;

            suggestion = "[" + type +"]";
            System.out.print("type? " + suggestion + " > ");
            input = reader.readLine().trim();
            if (!input.isEmpty()) type = input;

            suggestion = "[" + classifier +"]";
            System.out.print("classifier? " + suggestion + " > ");
            input = reader.readLine().trim();
            if (!input.isEmpty()) classifier = input;
        }
    }
}
