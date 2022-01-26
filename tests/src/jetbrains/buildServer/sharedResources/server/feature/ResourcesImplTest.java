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

package jetbrains.buildServer.sharedResources.server.feature;

import java.util.*;
import jetbrains.buildServer.BaseTestCase;
import jetbrains.buildServer.serverSide.ProjectManager;
import jetbrains.buildServer.serverSide.SProject;
import jetbrains.buildServer.serverSide.SProjectFeatureDescriptor;
import jetbrains.buildServer.sharedResources.SharedResourcesPluginConstants;
import jetbrains.buildServer.sharedResources.TestUtils;
import jetbrains.buildServer.sharedResources.model.resources.Resource;
import jetbrains.buildServer.sharedResources.model.resources.ResourceFactory;
import jetbrains.buildServer.sharedResources.server.project.ResourceProjectFeature;
import jetbrains.buildServer.sharedResources.server.project.ResourceProjectFeatureImpl;
import jetbrains.buildServer.sharedResources.server.project.ResourceProjectFeatures;
import jetbrains.buildServer.util.TestFor;
import org.jetbrains.annotations.NotNull;
import org.jmock.Expectations;
import org.jmock.Mockery;
import org.jmock.lib.legacy.ClassImposteriser;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

/**
 * Created with IntelliJ IDEA.
 *
 * @author Oleg Rybak (oleg.rybak@jetbrains.com)
 */
@TestFor(testForClass = {Resources.class, ResourcesImpl.class})
public class ResourcesImplTest extends BaseTestCase {

  private Mockery m;

  private ResourceProjectFeatures myResourceProjectFeatures;

  private ProjectManager myProjectManager;

  private SProject myProject;

  private SProject myRootProject;

  private final String myProjectId = TestUtils.generateRandomName();

  private final String myRootProjectId = "<ROOT>";

  private ResourcesImpl resources;

  @BeforeMethod
  @Override
  protected void setUp() throws Exception {
    super.setUp();
    m = new Mockery() {{
      setImposteriser(ClassImposteriser.INSTANCE);
    }};
    myProjectManager = m.mock(ProjectManager.class);
    myProject = m.mock(SProject.class, "currentProject");
    myRootProject = m.mock(SProject.class, "rootProject");
    myResourceProjectFeatures = m.mock(ResourceProjectFeatures.class);
    resources = new ResourcesImpl(myProjectManager, myResourceProjectFeatures);
  }

  @Override
  @AfterMethod
  public void tearDown() throws Exception {
    super.tearDown();
    m.assertIsSatisfied();
  }

  @Test
  @SuppressWarnings("Duplicates")
  public void testGetResourcesMap() {
    final List<ResourceProjectFeature> projectFeatures = Arrays.asList(
      createFeature(ResourceFactory.newInfiniteResource("project1", myProjectId, "RESOURCE_1", true)),
      createFeature(ResourceFactory.newInfiniteResource("project2", myProjectId, "RESOURCE_2", true))
    );

    final List<ResourceProjectFeature> rootFeatures = Arrays.asList(
      createFeature(ResourceFactory.newInfiniteResource("root1", myProjectId, "RESOURCE_3", true)),
      createFeature(ResourceFactory.newInfiniteResource("root2", myProjectId, "RESOURCE_4", true))
    );

    m.checking(new Expectations() {{
      oneOf(myProjectManager).findProjectById(myProjectId);
      will(returnValue(myProject));

      oneOf(myProject).getProjectPath();
      will(returnValue(Arrays.asList(myRootProject, myProject)));

      oneOf(myResourceProjectFeatures).getOwnFeatures(myProject);
      will(returnValue(projectFeatures));

      oneOf(myResourceProjectFeatures).getOwnFeatures(myRootProject);
      will(returnValue(rootFeatures));
    }});

    final Map<String, Resource> result = resources.getResourcesMap(myProjectId);
    assertNotNull(result);
    assertEquals(projectFeatures.size() + rootFeatures.size(), result.size());
  }

  @Test
  @SuppressWarnings("Duplicates")
  public void testGetResourcesMap_WithDuplicates() {
    final List<ResourceProjectFeature> projectFeatures = Arrays.asList(
      createFeature(ResourceFactory.newInfiniteResource("project1", myProjectId, "RESOURCE_1", true)),
      createFeature(ResourceFactory.newInfiniteResource("project2", myProjectId, "RESOURCE_1", true)),
      createFeature(ResourceFactory.newInfiniteResource("project3", myProjectId, "RESOURCE_2", true))
    );

    final List<ResourceProjectFeature> rootFeatures = Arrays.asList(
      createFeature(ResourceFactory.newInfiniteResource("root1", myProjectId, "RESOURCE_3", true)),
      createFeature(ResourceFactory.newInfiniteResource("root2", myProjectId, "RESOURCE_4", true))
    );

    m.checking(new Expectations() {{
      oneOf(myProjectManager).findProjectById(myProjectId);
      will(returnValue(myProject));

      oneOf(myProject).getProjectPath();
      will(returnValue(Arrays.asList(myRootProject, myProject)));

      oneOf(myResourceProjectFeatures).getOwnFeatures(myProject);
      will(returnValue(projectFeatures));

      oneOf(myResourceProjectFeatures).getOwnFeatures(myRootProject);
      will(returnValue(rootFeatures));
    }});

    final Map<String, Resource> result = resources.getResourcesMap(myProjectId);
    assertNotNull(result);
    assertEquals(3, result.size());
  }


  @Test
  public void testGetOwnResources_NoDuplicates() {
    final List<ResourceProjectFeature> projectFeatures = Arrays.asList(
      createFeature(ResourceFactory.newInfiniteResource("project1", myProjectId, "RESOURCE_1", true)),
      createFeature(ResourceFactory.newInfiniteResource("project2", myProjectId, "RESOURCE_2", true)),
      createFeature(ResourceFactory.newInfiniteResource("project3", myProjectId, "RESOURCE_3", true))
    );

    m.checking(new Expectations() {{
      oneOf(myResourceProjectFeatures).getOwnFeatures(myProject);
      will(returnValue(projectFeatures));
    }});

    final List<Resource> result = resources.getOwnResources(myProject);
    assertEquals(3, result.size());
  }

  @Test
  public void testGetOwnResources_IgnoreDuplicates() {
    final List<ResourceProjectFeature> projectFeatures = Arrays.asList(
      createFeature(ResourceFactory.newInfiniteResource("project1", myProjectId, "RESOURCE_1", true)),
      createFeature(ResourceFactory.newInfiniteResource("project2", myProjectId, "RESOURCE_1", true)),
      createFeature(ResourceFactory.newInfiniteResource("project3", myProjectId, "RESOURCE_3", true))
    );

    m.checking(new Expectations() {{
      oneOf(myResourceProjectFeatures).getOwnFeatures(myProject);
      will(returnValue(projectFeatures));
    }});

    final List<Resource> result = resources.getOwnResources(myProject);
    assertEquals(1, result.size());
  }

  @Test
  @SuppressWarnings("Duplicates")
  public void testGetResources_NoDuplicates() {
    final List<ResourceProjectFeature> projectFeatures = Arrays.asList(
      createFeature(ResourceFactory.newInfiniteResource("project1", myProjectId, "RESOURCE_1", true)),
      createFeature(ResourceFactory.newInfiniteResource("project2", myProjectId, "RESOURCE_2", true))
    );

    final List<ResourceProjectFeature> rootFeatures = Arrays.asList(
      createFeature(ResourceFactory.newInfiniteResource("root1", myProjectId, "RESOURCE_3", true)),
      createFeature(ResourceFactory.newInfiniteResource("root2", myProjectId, "RESOURCE_4", true))
    );

    m.checking(new Expectations() {{
      oneOf(myProject).getProjectPath();
      will(returnValue(Arrays.asList(myRootProject, myProject)));

      oneOf(myResourceProjectFeatures).getOwnFeatures(myProject);
      will(returnValue(projectFeatures));

      oneOf(myResourceProjectFeatures).getOwnFeatures(myRootProject);
      will(returnValue(rootFeatures));

    }});

    final List<Resource> result = resources.getResources(myProject);
    assertEquals(4, result.size());
  }

  @Test
  @SuppressWarnings("Duplicates")
  public void testGetResources_IgnoreDuplicates() {
    final List<ResourceProjectFeature> projectFeatures = Arrays.asList(
      createFeature(ResourceFactory.newInfiniteResource("project1", myProjectId, "RESOURCE_1", true)),
      createFeature(ResourceFactory.newInfiniteResource("project2", myProjectId, "RESOURCE_1", true)),
      createFeature(ResourceFactory.newInfiniteResource("project3", myProjectId, "RESOURCE_2", true))
    );

    final List<ResourceProjectFeature> rootFeatures = Arrays.asList(
      createFeature(ResourceFactory.newInfiniteResource("root1", myProjectId, "RESOURCE_3", true)),
      createFeature(ResourceFactory.newInfiniteResource("root2", myProjectId, "RESOURCE_3", true)),
      createFeature(ResourceFactory.newInfiniteResource("root3", myProjectId, "RESOURCE_4", true))
    );

    m.checking(new Expectations() {{
      oneOf(myProject).getProjectPath();
      will(returnValue(Arrays.asList(myRootProject, myProject)));

      oneOf(myResourceProjectFeatures).getOwnFeatures(myProject);
      will(returnValue(projectFeatures));

      oneOf(myResourceProjectFeatures).getOwnFeatures(myRootProject);
      will(returnValue(rootFeatures));

    }});

    final List<Resource> result = resources.getResources(myProject);
    assertEquals(2, result.size());
  }

  @Test
  @SuppressWarnings("Duplicates")
  public void testGetAllOwnResources_IncludeDuplicates() {
    final List<ResourceProjectFeature> projectFeatures = Arrays.asList(
      createFeature(ResourceFactory.newInfiniteResource("project1", myProjectId, "RESOURCE_1", true)),
      createFeature(ResourceFactory.newInfiniteResource("project2", myProjectId, "RESOURCE_1", true)),
      createFeature(ResourceFactory.newInfiniteResource("project3", myProjectId, "RESOURCE_3", true))
    );

    m.checking(new Expectations() {{
      oneOf(myResourceProjectFeatures).getOwnFeatures(myProject);
      will(returnValue(projectFeatures));
    }});

    final List<Resource> result = resources.getAllOwnResources(myProject);
    assertEquals(3, result.size());
  }


  @Test
  public void testGetCount_SingleProject() {
    final List<ResourceProjectFeature> projectFeatures = Collections.singletonList(
      createFeature(ResourceFactory.newInfiniteResource("project1", myProjectId, "RESOURCE_2", true))
    );

    m.checking(new Expectations() {{
      oneOf(myProject).getProjectPath();
      will(returnValue(Collections.singletonList(myProject)));

      oneOf(myResourceProjectFeatures).getOwnFeatures(myProject);
      will(returnValue(projectFeatures));

    }});
    assertEquals(1, resources.getCount(myProject));
  }

  @Test
  @SuppressWarnings("Duplicates")
  public void testGetCount_Hierarchy() {
    final List<ResourceProjectFeature> rootFeatures = Collections.singletonList(
      createFeature(ResourceFactory.newInfiniteResource("root1", myRootProjectId, "RESOURCE_1", true))
    );

    final List<ResourceProjectFeature> projectFeatures = Collections.singletonList(
      createFeature(ResourceFactory.newInfiniteResource("project1", myProjectId, "RESOURCE_2", true))
    );

    m.checking(new Expectations() {{
      oneOf(myProject).getProjectPath();
      will(returnValue(Arrays.asList(myRootProject, myProject)));

      oneOf(myResourceProjectFeatures).getOwnFeatures(myProject);
      will(returnValue(projectFeatures));

      oneOf(myResourceProjectFeatures).getOwnFeatures(myRootProject);
      will(returnValue(rootFeatures));
    }});
    assertEquals(2, resources.getCount(myProject));
  }


  @Test
  @SuppressWarnings("Duplicates")
  public void testGetCount_HierarchyOverriding() throws Exception {
    final List<ResourceProjectFeature> rootFeatures = Collections.singletonList(
      createFeature(ResourceFactory.newInfiniteResource("root1", myRootProjectId, "RESOURCE", true))
    );

    final List<ResourceProjectFeature> projectFeatures = Collections.singletonList(
      createFeature(ResourceFactory.newInfiniteResource("project1", myProjectId, "RESOURCE", true))
    );

    m.checking(new Expectations() {{
      oneOf(myProject).getProjectPath();
      will(returnValue(Arrays.asList(myRootProject, myProject)));

      oneOf(myResourceProjectFeatures).getOwnFeatures(myProject);
      will(returnValue(projectFeatures));

      oneOf(myResourceProjectFeatures).getOwnFeatures(myRootProject);
      will(returnValue(rootFeatures));
    }});
    assertEquals(1, resources.getCount(myProject));
  }

  private ResourceProjectFeature createFeature(@NotNull final Resource resource) {
    final SProjectFeatureDescriptor descriptor = m.mock(SProjectFeatureDescriptor.class, "descriptor" + resource.getProjectId() + "_" + resource.getId());
    m.checking(new Expectations() {{
      allowing(descriptor).getId();
      will(returnValue(resource.getId()));

      allowing(descriptor).getParameters();
      will(returnValue(resource.getParameters()));

      allowing(descriptor).getProjectId();
      will(returnValue(resource.getProjectId()));

      allowing(descriptor).getType();
      will(returnValue(SharedResourcesPluginConstants.FEATURE_TYPE));
    }});
    return new ResourceProjectFeatureImpl(descriptor);
  }
}
