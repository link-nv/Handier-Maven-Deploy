package org.apache.maven.plugin.deploy;

/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.deployer.ArtifactDeploymentException;
import org.apache.maven.artifact.metadata.ArtifactMetadata;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.repository.layout.ArtifactRepositoryLayout;
import org.apache.maven.artifact.resolver.ArtifactNotFoundException;
import org.apache.maven.artifact.resolver.ArtifactResolutionException;
import org.apache.maven.artifact.resolver.ArtifactResolver;
import org.apache.maven.model.Dependency;
import org.apache.maven.model.Model;
import org.apache.maven.model.io.xpp3.MavenXpp3Reader;
import org.apache.maven.model.io.xpp3.MavenXpp3Writer;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.MavenProjectBuilder;
import org.apache.maven.project.artifact.ProjectArtifactMetadata;
import org.apache.maven.project.inheritance.ModelInheritanceAssembler;
import org.codehaus.plexus.util.IOUtil;
import org.codehaus.plexus.util.WriterFactory;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.Writer;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Deploys an artifact to remote repository.
 *
 * @author <a href="mailto:evenisse@apache.org">Emmanuel Venisse</a>
 * @author <a href="mailto:jdcasey@apache.org">John Casey (refactoring only)</a>
 * @version $Id: DeployMojo.java 1160164 2011-08-22 09:38:32Z stephenc $
 * @goal deploy
 * @phase deploy
 * @threadSafe
 * @requiresDependencyResolution
 */
public class DeployMojo
        extends AbstractDeployMojo {

    private static final Pattern ALT_REPO_SYNTAX_PATTERN = Pattern.compile("(.+)::(.+)::(.+)");

    /**
     * @parameter default-value="${project}"
     * @required
     * @readonly
     */
    protected MavenProject project;

    /**
     * @parameter default-value="${project.packaging}"
     * @required
     * @readonly
     */
    private String packaging;

    /**
     * @parameter default-value="${project.file}"
     * @required
     * @readonly
     */
    private File pomFile;

    /**
     * @parameter default-value=false expression="${deployDependencies}"
     * @required
     */
    private boolean deployDependencies;

    /**
     * @parameter default-value=false expression="${filterPom}"
     * @required
     */
    private boolean filterPom;

    /**
     * Specifies an alternative repository to which the project artifacts should be deployed ( other
     * than those specified in &lt;distributionManagement&gt; ).
     * <br/>
     * Format: id::layout::url
     *
     * @parameter expression="${altDeploymentRepository}"
     */
    private String altDeploymentRepository;

    /**
     * @parameter default-value="${project.attachedArtifacts}
     * @required
     * @readonly
     */
    private List attachedArtifacts;

    /**
     * Set this to 'true' to bypass artifact deploy
     *
     * @parameter expression="${maven.deploy.skip}" default-value="false"
     * @since 2.4
     */
    private boolean skip;

    /**
     * Used to look up Artifacts in the remote repository.
     *
     * @parameter expression=
     * "${component.org.apache.maven.artifact.resolver.ArtifactResolver}"
     * @required
     * @readonly
     */
    protected ArtifactResolver artifactResolver;

    /**
     * List of Remote Repositories used by the resolver
     *
     * @parameter expression="${project.remoteArtifactRepositories}"
     * @readonly
     * @required
     */
    protected java.util.List remoteRepos;

    /**
     * Location of the local repository.
     *
     * @parameter expression="${localRepository}"
     * @readonly
     * @required
     */
    protected org.apache.maven.artifact.repository.ArtifactRepository local;

    /**
     * Used to look up overlay the parent models on the project's model.
     *
     * @parameter expression=
     * "${component.org.apache.maven.project.inheritance.ModelInheritanceAssembler}"
     * @required
     * @readonly
     */
    private ModelInheritanceAssembler modelInheritanceAssembler;

    /**
     * Used to create a model
     *
     * @parameter expression=
     * "${component.org.apache.maven.project.MavenProjectBuilder}"
     * @required
     * @readonly
     */
    protected MavenProjectBuilder mavenProjectBuilder;

    public void execute() throws MojoExecutionException, MojoFailureException {

        Set<Artifact> toBeDeployedArtifacts = new HashSet<Artifact>();
        if (true == deployDependencies) {
            toBeDeployedArtifacts.addAll(project.getArtifacts());
        }
        toBeDeployedArtifacts.add(project.getArtifact());

        executeWithArtifacts(toBeDeployedArtifacts);
    }

    public void executeWithArtifacts(Set<Artifact> toBeDeployedArtifacts)
            throws MojoExecutionException, MojoFailureException {
        if (skip) {
            getLog().info("Skipping artifact deployment");
            return;
        }

        failIfOffline();

        ArtifactRepository repo = getDeploymentRepository();

        String protocol = repo.getProtocol();

        if (protocol.equalsIgnoreCase("scp")) {
            File sshFile = new File(System.getProperty("user.home"), ".ssh");

            if (!sshFile.exists()) {
                sshFile.mkdirs();
            }
        }

        for (Object iter : toBeDeployedArtifacts) {
            Artifact artifactTBD = (Artifact) iter;

            getLog().debug("Deploying artifact: " + artifactTBD.getGroupId() + ":" + artifactTBD.getArtifactId());

            Artifact thePomArtifact = artifactFactory
                    .createArtifact(artifactTBD.getGroupId(), artifactTBD.getArtifactId(), artifactTBD.getVersion(), "",
                            "pom");
            try {
                artifactResolver.resolve(thePomArtifact, this.remoteRepos, this.local);
            } catch (Exception e) {
                throw new MojoExecutionException(e.getMessage(), e);
            }

            if (true == filterPom) {
                pomFile = filterPom(thePomArtifact);
            }
            pomFile = thePomArtifact.getFile();


            // Deploy the POM
            boolean isPomArtifact = "pom".equals(packaging);
            if (!isPomArtifact) {
                ArtifactMetadata metadata = new ProjectArtifactMetadata(artifactTBD, pomFile);
                artifactTBD.addMetadata(metadata);
            }

            if (updateReleaseInfo) {
                artifactTBD.setRelease(true);
            }

            try {
                if (isPomArtifact) {
                    deploy(pomFile, artifactTBD, repo, getLocalRepository());
                } else {
                    File file = artifactTBD.getFile();

                    if (file != null && file.isFile()) {
                        deploy(file, artifactTBD, repo, getLocalRepository());
                    } else if (!attachedArtifacts.isEmpty()) {
                        getLog().info("No primary artifact to deploy, deploying attached artifacts instead.");

                        Artifact pomArtifact =
                                artifactFactory.createProjectArtifact(artifactTBD.getGroupId(), artifactTBD.getArtifactId(),
                                        artifactTBD.getBaseVersion());
                        pomArtifact.setFile(pomFile);
                        if (updateReleaseInfo) {
                            pomArtifact.setRelease(true);
                        }

                        deploy(pomFile, pomArtifact, repo, getLocalRepository());

                        // propagate the timestamped version to the main artifact for the attached artifacts to pick it up
                        artifactTBD.setResolvedVersion(pomArtifact.getVersion());
                    } else {
                        String message = "The packaging for this project did not assign a file to the build artifact";
                        throw new MojoExecutionException(message);
                    }
                }

                for (Iterator i = attachedArtifacts.iterator(); i.hasNext(); ) {
                    Artifact attached = (Artifact) i.next();

                    deploy(attached.getFile(), attached, repo, getLocalRepository());
                }
                if (true == filterPom) {
                    pomFile = filterPom(thePomArtifact);
                    deploy(pomFile, thePomArtifact, repo, getLocalRepository());
                }
            } catch (ArtifactDeploymentException e) {
                throw new MojoExecutionException(e.getMessage(), e);
            }
        }
    }

    private ArtifactRepository getDeploymentRepository()
            throws MojoExecutionException, MojoFailureException {
        ArtifactRepository repo = null;

        if (altDeploymentRepository != null) {
            getLog().info("Using alternate deployment repository " + altDeploymentRepository);

            Matcher matcher = ALT_REPO_SYNTAX_PATTERN.matcher(altDeploymentRepository);

            if (!matcher.matches()) {
                throw new MojoFailureException(altDeploymentRepository, "Invalid syntax for repository.",
                        "Invalid syntax for alternative repository. Use \"id::layout::url\".");
            } else {
                String id = matcher.group(1).trim();
                String layout = matcher.group(2).trim();
                String url = matcher.group(3).trim();

                ArtifactRepositoryLayout repoLayout = getLayout(layout);

                repo = repositoryFactory.createDeploymentArtifactRepository(id, url, repoLayout, true);
            }
        }

        if (repo == null) {
            repo = project.getDistributionManagementArtifactRepository();
        }

        if (repo == null) {
            String msg = "Deployment failed: repository element was not specified in the POM inside"
                    + " distributionManagement element or in -DaltDeploymentRepository=id::layout::url parameter";

            throw new MojoExecutionException(msg);
        }

        return repo;
    }

    private File filterPom(Artifact thePomArtifact) throws MojoExecutionException {

        MavenProject bareProject;

        // get the unresolved project by reading the file
        try {
            bareProject = mavenProjectBuilder.buildFromRepository(thePomArtifact, this.remoteRepos, this.local);


            MavenProject parentProject = bareProject.getParent();

            Stack hierarchy = new Stack();
            hierarchy.push(bareProject);


            while (parentProject != null) {

                try {
                    // get Maven to resolve the parent artifact (download if
                    // needed)
                    Artifact theParentPomArtifact = artifactFactory
                            .createArtifact(parentProject.getGroupId(), parentProject.getArtifactId(), parentProject.getVersion(), "",
                                    "pom");

                    artifactResolver.resolve(theParentPomArtifact, this.remoteRepos, this.local);

                    // get the file from the local repository and read the bare
                    // project
                    File parentPomFile = theParentPomArtifact.getFile();

                    MavenProject parentParentProject = parentProject.getParent();

                    parentProject = mavenProjectBuilder.buildFromRepository(theParentPomArtifact, this.remoteRepos, this.local);

                    hierarchy.push(parentProject);

                    parentProject = parentParentProject;
                } catch (ArtifactResolutionException e) {
                    getLog().error("can't resolve parent pom", e);
                } catch (ArtifactNotFoundException e) {
                    getLog().error("can't resolve parent pom", e);
                }
            }

            // merge each model starting with the oldest ancestors
            MavenProject currentParent = (MavenProject) hierarchy.pop();
            MavenProject currentProject = currentParent;
            while (hierarchy.size() != 0) {
                currentProject = (MavenProject) hierarchy.pop();
                modelInheritanceAssembler.assembleModelInheritance(currentProject.getModel(), currentParent.getModel());
                currentParent = currentProject;
            }

            Model currentModel = currentProject.getModel();

            currentModel.setParent(null);
            currentModel.setBuild(null);
            currentModel.setCiManagement(null);
            currentModel.setContributors(null);
            currentModel.setCiManagement(null);
            currentModel.setDevelopers(null);
            currentModel.setIssueManagement(null);
            currentModel.setMailingLists(null);
            currentModel.setProfiles(null);
            currentModel.setModules(null);
            currentModel.setDistributionManagement(null);
            currentModel.setPluginRepositories(null);
            currentModel.setReporting(null);
            currentModel.setReports(null);
            currentModel.setRepositories(null);
            currentModel.setScm(null);
            currentModel.setUrl(null);

            // use the resolved dependencies instead of the bare ones
            List<Dependency> goodDeps = new ArrayList<Dependency>();
            for (Object obj : bareProject.getDependencies()) {
                Dependency dep = (Dependency) obj;

                String scope = dep.getScope();

                /* only add dependencies that
                * have no scope
                * (dependencies in a pom file have no default scope)
                * or have a different scope than test
                * isn't there a constant for "test"?
                */
                if (null == scope || !scope.equals("test")) {
                    goodDeps.add(dep);
                }
            }
            currentModel.setDependencies(goodDeps);

            currentModel.setDependencyManagement(null);
            currentModel.setProperties(null);

            // spit the merged model to the output file.
            Writer fw = null;
            try {
                File tempFile = File.createTempFile("mvndeploy", ".pom");
                tempFile.deleteOnExit();

                fw = WriterFactory.newXmlWriter(tempFile);
                new MavenXpp3Writer().write(fw, currentModel);

                return tempFile;
            } catch (IOException e) {
                throw new MojoExecutionException("Error writing temporary pom file: " + e.getMessage(), e);
            } finally {
                IOUtil.close(fw);
            }
        } catch (Exception e) {
            throw new MojoExecutionException(e.getMessage(), e);
        }
    }

}
