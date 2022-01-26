/*
 * Copyright 2000-2022 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package jetbrains.buildServer.sharedResources.pages.actions;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import jetbrains.buildServer.controllers.BaseControllerTestCase;
import jetbrains.buildServer.controllers.MockRequest;
import jetbrains.buildServer.controllers.XmlResponseUtil;
import jetbrains.buildServer.serverSide.*;
import jetbrains.buildServer.sharedResources.SharedResourcesPluginConstants;
import jetbrains.buildServer.sharedResources.model.resources.QuotedResource;
import jetbrains.buildServer.sharedResources.model.resources.Resource;
import jetbrains.buildServer.sharedResources.model.resources.ResourceType;
import jetbrains.buildServer.sharedResources.pages.SharedResourcesActionsController;
import jetbrains.buildServer.sharedResources.server.feature.Resources;
import jetbrains.buildServer.sharedResources.server.feature.SharedResourcesFeature;
import jetbrains.buildServer.sharedResources.server.feature.SharedResourcesFeatures;
import jetbrains.buildServer.sharedResources.tests.SharedResourcesIntegrationTestsSupport;
import jetbrains.buildServer.util.TestFor;
import jetbrains.buildServer.web.openapi.WebControllerManager;
import org.jdom.Element;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.testng.annotations.Test;

import static jetbrains.buildServer.sharedResources.tests.SharedResourcesIntegrationTestsSupport.*;

/**
 * Created with IntelliJ IDEA.
 *
 * @author Oleg Rybak (oleg.rybak@jetbrains.com)
 */
public class ActionsTest extends BaseControllerTestCase<SharedResourcesActionsController> {

  private Resources myResources;

  private AddResourceAction myAddResourceAction;

  private EditResourceAction myEditResourceAction;

  @Test
  public void testAddResourceAction() throws Exception {
    assertEmpty(myResources.getOwnResources(myProject));
    myRequest.setParameters("action", "addResource",
                            SharedResourcesPluginConstants.WEB.PARAM_PROJECT_ID, myProject.getProjectId(),
                            SharedResourcesPluginConstants.WEB.PARAM_RESOURCE_NAME, "myResource",
                            SharedResourcesPluginConstants.WEB.PARAM_RESOURCE_TYPE, ResourceType.QUOTED.toString());
    processRequest();
    final List<Resource> resources = myResources.getOwnResources(myProject);
    assertEquals(1, resources.size());

    Resource addedResource = resources.get(0);
    assertEquals(ResourceType.QUOTED, addedResource.getType());
    assertEquals("myResource", addedResource.getName());
    assertTrue(((QuotedResource)addedResource).isInfinite());
  }

  private static final String RESOURCE_NAME = "NAME";

  @Test
  @TestFor(issues = "TW-29355")
  public void testPersistNameNotChanged() throws Exception {
    final AtomicBoolean projectPersisted = new AtomicBoolean(false);
    final SProject project = myFixture.createProject("testPersistNameNotChanged_Project");
    assertEmpty(myResources.getOwnResources(project));

    // add resource through API
    final Resource resource = SharedResourcesIntegrationTestsSupport.addResource(myFixture,
                                                                                 project,
                                                                                 createQuotedResource(RESOURCE_NAME, 111));
    project.persist();

    myFixture.getEventDispatcher().addListener(new BuildServerAdapter() {
      @Override
      public void projectPersisted(@NotNull final String projectId) {
        if (projectId.equals(project.getProjectId())) {
          projectPersisted.set(true);
        }
      }
    });

    assertEquals(1, myResources.getOwnResources(project).size());
    setupEditAction(myRequest, project, (QuotedResource)resource, null);
    // call edit. do not change anything
    processRequest();
    // catch edit event
    assertTrue(projectPersisted.get());
    // resource has not been changed
    assertEquals(resource, myResources.getOwnResources(project).get(0));
  }

  @Test
  public void testUpdateLockNameInTree() throws Exception {
    final String NEW_NAME = "NEW_NAME";
    // create projects and build types
    final SProject currentProject = myFixture.createProject("testUpdateLockNameInTree_Project");
    final SProject subProject = currentProject.createProject(currentProject.getExternalId() + "_sub", currentProject.getName() + "_sub");

    final SBuildType currentBuildType = currentProject.createBuildType("currentProject-build-type");
    final BuildTypeTemplate currentBuildTypeTemplate = currentProject.createBuildTypeTemplate("currentProject-template");

    final SBuildType subBuildType = subProject.createBuildType("subProject-build-type");
    final BuildTypeTemplate subBuildTypeTemplate = subProject.createBuildTypeTemplate("subProject-template");

    final List<BuildTypeSettings> containers = Arrays.asList(currentBuildType, currentBuildTypeTemplate, subBuildType, subBuildTypeTemplate);

    assertEmpty(myResources.getOwnResources(currentProject));
    // create resource in current project
    // add resource through API
    final Resource resource = SharedResourcesIntegrationTestsSupport.addResource(myFixture,
                                                                                 currentProject,
                                                                                 createQuotedResource(RESOURCE_NAME, 1));
    currentProject.persist();
    // create locks in build types and templates
    addReadLock(currentBuildType, resource.getName());
    addReadLock(currentBuildTypeTemplate, resource.getName());

    addWriteLock(subBuildType, resource.getName());
    addWriteLock(subBuildTypeTemplate, resource.getName());
    // persist
    currentProject.persist();
    subProject.persist();
    // execute edit action. Change name
    setupEditAction(myRequest,currentProject, (QuotedResource)resource, NEW_NAME);
    processRequest();
    // check resource has changed name
    final List<Resource> ownResources = myResources.getOwnResources(currentProject);
    assertEquals(1, ownResources.size());
    assertEquals(NEW_NAME, ownResources.iterator().next().getName());
    // check locks changed reference

    final SharedResourcesFeatures features = myFixture.getSingletonService(SharedResourcesFeatures.class);

    for (BuildTypeSettings settings: containers) {
      features.searchForFeatures(settings)
              .stream()
              .map(SharedResourcesFeature::getLockedResources)
              .forEach(lockedResources -> {
                assertEquals(1, lockedResources.size());
                assertNotNull(lockedResources.get(NEW_NAME));
              });
    }
  }

  @Test
  @TestFor(issues = {"TW-44397", "TW-44398"})
  public void testUpdateAffectsOnlyProjectsWithUsages() throws Exception {
    final String NEW_NAME = "NEW_NAME";
    // create projects and build types
    final SProject currentProject = myFixture.createProject("testUpdateLockNameInTree_Project");
    final SProject subProjectWithUsages = currentProject.createProject(currentProject.getExternalId() + "_sub", currentProject.getName() + "_sub");
    final SProject subProjectWithoutUsages = currentProject.createProject(currentProject.getExternalId() + "_subNoUsage", currentProject.getName() + "_subNoUsage");

    final SBuildType currentBuildType = currentProject.createBuildType("currentProject-build-type");
    final SBuildType subBuildTypeWithUsages = subProjectWithUsages.createBuildType("subProjectWithUsages-build-type");
    final SBuildType subBuildTypeWithoutUsages = subProjectWithoutUsages.createBuildType("subProjectWithoutUsages-build-type");

    assertEmpty(myResources.getOwnResources(currentProject));
    final Resource resource = SharedResourcesIntegrationTestsSupport.addResource(myFixture,
                                                                                 currentProject,
                                                                                 createQuotedResource(RESOURCE_NAME, 1));

    final Resource otherResource = SharedResourcesIntegrationTestsSupport.addResource(myFixture,
                                                                                      currentProject,
                                                                                      createQuotedResource("otherResource", 1));
    currentProject.persist();
    addReadLock(currentBuildType, resource.getName());
    addReadLock(subBuildTypeWithUsages, resource.getName());
    addReadLock(subBuildTypeWithoutUsages, otherResource.getName());
    currentProject.persist();
    subProjectWithUsages.persist();
    subProjectWithoutUsages.persist();

    final Set<String> updatedProjects = new HashSet<>();
    myFixture.getEventDispatcher().addListener(new BuildServerAdapter() {
      @Override
      public void projectPersisted(@NotNull final String projectId) {
        updatedProjects.add(projectId);
      }
    });
    // execute edit action. Change name
    setupEditAction(myRequest,currentProject, (QuotedResource)resource, NEW_NAME);
    processRequest();
    // only projects that used original resources are updated
    assertEquals(2, updatedProjects.size());
    assertContains(updatedProjects, currentProject.getProjectId());
    assertContains(updatedProjects, subProjectWithUsages.getProjectId());
  }

  @Test
  @TestFor(issues = "TW-55849")
  public void testAddResource_DuplicateName() {
    final String usedResourceName = "SOME_USED_RESOURCE";
    final SProject currentProject = myFixture.createProject("testAddResource_DuplicateName_Project");
    assertEmpty(myResources.getOwnResources(currentProject));
    // add resource through the features api
    SharedResourcesIntegrationTestsSupport.addResource(myFixture,
                                                       currentProject,
                                                       createQuotedResource(usedResourceName, 1));
    currentProject.persist();
    assertEquals(1, myResources.getAllOwnResources(currentProject).size());
    myResponse.reset();
    myRequest.setParameters("action", "addResource",
                            SharedResourcesPluginConstants.WEB.PARAM_PROJECT_ID, currentProject.getProjectId(),
                            SharedResourcesPluginConstants.WEB.PARAM_RESOURCE_NAME, usedResourceName,
                            SharedResourcesPluginConstants.WEB.PARAM_RESOURCE_TYPE, ResourceType.QUOTED.toString());
    assertEquals(1, myResources.getAllOwnResources(currentProject).size());

    final Element ajaxResponse = XmlResponseUtil.newXmlResponse();
    myAddResourceAction.process(myRequest, myResponse, ajaxResponse);
    assertEquals(1, ajaxResponse.getContent().size());
    final Element errors = ajaxResponse.getChild("errors");
    assertNotNull("Expected resource name error. None found", errors);
    assertEquals("Name", "Name " + usedResourceName + " is already used by another resource", errors.getChildText("error"));
  }

  @Test
  @TestFor(issues = "TW-55849")
  public void testEditResource_DuplicateName() {
    final String usedResourceName = "SOME_USED_RESOURCE";
    final SProject currentProject = myFixture.createProject("testEditResource_DuplicateName_Project");
    assertEmpty(myResources.getOwnResources(currentProject));
    // add resource through the features api
    final Resource resource1 = addResource(myFixture,
                                          currentProject,
                                          createQuotedResource(usedResourceName, 1));
    final Resource resource2 = addResource(myFixture,
                                           currentProject,
                                           createQuotedResource("valid name", 1));
    currentProject.persist();
    assertEquals(2, myResources.getAllOwnResources(currentProject).size());
    myResponse.reset();
    setupEditAction(myRequest, currentProject, (QuotedResource)resource2, usedResourceName);
    final Element ajaxResponse = XmlResponseUtil.newXmlResponse();
    myEditResourceAction.process(myRequest, myResponse, ajaxResponse);
    assertEquals(1, ajaxResponse.getContent().size());
    final Element errors = ajaxResponse.getChild("errors");
    assertNotNull("Expected resource name error. None found", errors);
    assertEquals("Name", "Name " + usedResourceName + " is already used by another resource", errors.getChildText("error"));
  }

  private static void setupEditAction(@NotNull final MockRequest request,
                                      @NotNull final SProject project,
                                      @NotNull final QuotedResource resource,
                                      @Nullable final String newName) {
    request.setParameters("action", "editResource",
                          SharedResourcesPluginConstants.WEB.PARAM_PROJECT_ID, project.getProjectId(),
                          SharedResourcesPluginConstants.WEB.PARAM_OLD_RESOURCE_NAME, resource.getName(),
                          SharedResourcesPluginConstants.WEB.PARAM_RESOURCE_ID, resource.getId(),
                          SharedResourcesPluginConstants.WEB.PARAM_RESOURCE_TYPE, resource.getType().toString(),
                          SharedResourcesPluginConstants.WEB.PARAM_RESOURCE_QUOTA, resource.getQuota());
    if (newName == null) {
      request.addParameters(SharedResourcesPluginConstants.WEB.PARAM_RESOURCE_NAME, resource.getName());
    } else {
      request.addParameters(SharedResourcesPluginConstants.WEB.PARAM_RESOURCE_NAME, newName);
    }
  }

  @Override
  protected SharedResourcesActionsController createController() {
    SharedResourcesIntegrationTestsSupport.apply(myFixture);
    myResources = myFixture.getSingletonService(Resources.class);
    myAddResourceAction = myFixture.getSingletonService(AddResourceAction.class);
    myEditResourceAction = myFixture.getSingletonService(EditResourceAction.class);

    return new SharedResourcesActionsController(
      myFixture.getSingletonService(WebControllerManager.class),
      myAddResourceAction,
      myFixture.getSingletonService(DeleteResourceAction.class),
      myEditResourceAction,
      myFixture.getSingletonService(EnableDisableResourceAction.class)
    );
  }
}
