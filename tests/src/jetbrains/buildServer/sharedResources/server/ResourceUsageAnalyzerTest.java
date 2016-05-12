/*
 * Copyright 2000-2014 JetBrains s.r.o.
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

package jetbrains.buildServer.sharedResources.server;

import jetbrains.buildServer.BaseTestCase;
import jetbrains.buildServer.serverSide.SBuildType;
import jetbrains.buildServer.serverSide.SProject;
import jetbrains.buildServer.sharedResources.model.Lock;
import jetbrains.buildServer.sharedResources.model.LockType;
import jetbrains.buildServer.sharedResources.model.resources.Resource;
import jetbrains.buildServer.sharedResources.model.resources.ResourceFactory;
import jetbrains.buildServer.sharedResources.server.feature.Resources;
import jetbrains.buildServer.sharedResources.server.feature.SharedResourcesFeature;
import jetbrains.buildServer.sharedResources.server.feature.SharedResourcesFeatures;
import jetbrains.buildServer.util.TestFor;
import org.jmock.Expectations;
import org.jmock.Mockery;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.util.*;

/**
 * Created with IntelliJ IDEA.
 *
 * @author Oleg Rybak (oleg.rybak@jetbrains.com)
 */
@TestFor(testForClass = ResourceUsageAnalyzer.class)
public class ResourceUsageAnalyzerTest extends BaseTestCase {

  private Mockery m;

  private Resources myResources;

  private SharedResourcesFeatures myFeatures;

  private ResourceUsageAnalyzer myAnalyzer;

  private SProject myProject;

  private static final String myProjectId = "PROJECT_ID";

  @BeforeMethod
  @Override
  protected void setUp() throws Exception {
    super.setUp();
    m = new Mockery();
    myResources = m.mock(Resources.class);
    myFeatures = m.mock(SharedResourcesFeatures.class);

    myProject = m.mock(SProject.class, "projectId: " + myProjectId);

    myAnalyzer = new ResourceUsageAnalyzer(myResources, myFeatures);
  }

  @AfterMethod
  @Override
  protected void tearDown() throws Exception {
    super.tearDown();
    m.assertIsSatisfied();
  }

  @Test
  public void testNoResources() throws Exception {
    m.checking(new Expectations() {{
      oneOf(myProject).getProjectId();
      will(returnValue(myProjectId));

      oneOf(myResources).asMap(myProjectId);
      will(returnValue(Collections.emptyMap()));
    }});

    final Map<Resource, Map<SBuildType, List<Lock>>> result = myAnalyzer.collectResourceUsages(myProject);
    assertTrue(result.isEmpty());
  }


  @Test
  public void testNoBuildConfigurations() throws Exception {
    final Resource resource = ResourceFactory.newInfiniteResource("resource1", true);
    final Map<String, Resource> resourceMap = new HashMap<String, Resource>() {{
       put(resource.getName(), resource);
    }};

    m.checking(new Expectations() {{
      oneOf(myProject).getProjectId();
      will(returnValue(myProjectId));

      oneOf(myResources).asMap(myProjectId);
      will(returnValue(resourceMap));

      oneOf(myProject).getBuildTypes();
      will(returnValue(Collections.emptyList()));
    }});

    final Map<Resource, Map<SBuildType, List<Lock>>> result = myAnalyzer.collectResourceUsages(myProject);
    assertTrue(result.isEmpty());
  }

  @Test
  public void testNoFeatures() throws Exception {
    final Resource resource = ResourceFactory.newInfiniteResource("resource1", true);
    final Map<String, Resource> resourceMap = new HashMap<String, Resource>() {{
      put(resource.getName(), resource);
    }};

    final SBuildType bt = m.mock(SBuildType.class, "buildType: " + myProjectId);

    m.checking(new Expectations() {{
      oneOf(myProject).getProjectId();
      will(returnValue(myProjectId));

      oneOf(myResources).asMap(myProjectId);
      will(returnValue(resourceMap));

      oneOf(myProject).getBuildTypes();
      will(returnValue(Collections.singletonList(bt)));

      oneOf(myFeatures).searchForFeatures(bt);
      will(returnValue(Collections.emptyList()));
    }});

    final Map<Resource, Map<SBuildType, List<Lock>>> result = myAnalyzer.collectResourceUsages(myProject);
    assertTrue(result.isEmpty());
  }

  @Test
  public void testNoLocks() throws Exception {
    final Resource resource = ResourceFactory.newInfiniteResource("resource1", true);
    final Map<String, Resource> resourceMap = new HashMap<String, Resource>() {{
      put(resource.getName(), resource);
    }};

    final SBuildType bt = m.mock(SBuildType.class, "buildType: " + myProjectId);
    final SharedResourcesFeature feature = m.mock(SharedResourcesFeature.class);

    m.checking(new Expectations() {{
      oneOf(myProject).getProjectId();
      will(returnValue(myProjectId));

      oneOf(myResources).asMap(myProjectId);
      will(returnValue(resourceMap));

      oneOf(myProject).getBuildTypes();
      will(returnValue(Collections.singletonList(bt)));

      oneOf(myFeatures).searchForFeatures(bt);
      will(returnValue(Collections.singletonList(feature)));

      oneOf(bt).getProjectId();
      will(returnValue(myProjectId));

      oneOf(feature).getLockedResources();
      will(returnValue(Collections.emptyMap()));
    }});

    final Map<Resource, Map<SBuildType, List<Lock>>> result = myAnalyzer.collectResourceUsages(myProject);
    assertTrue(result.isEmpty());
  }

  /**
   * Some resources and some locks on them only in current project
   *
   * @throws Exception if something goes wrong
   */
  @Test
  public void testNoSubProjects() throws Exception {
    final Resource resource = ResourceFactory.newInfiniteResource("resource1", true);
    final Map<String, Resource> resourceMap = new HashMap<String, Resource>() {{
      put(resource.getName(), resource);
    }};

    final SBuildType bt = m.mock(SBuildType.class, "buildType: " + myProjectId);
    final SharedResourcesFeature feature = m.mock(SharedResourcesFeature.class);

    final Lock lock = new Lock("resource1", LockType.READ);
    final Map<String, Lock> lockedResources = new HashMap<String, Lock>() {{
      put(lock.getName(), lock);
    }};

    m.checking(new Expectations() {{
      oneOf(myProject).getProjectId();
      will(returnValue(myProjectId));

      oneOf(myResources).asMap(myProjectId);
      will(returnValue(resourceMap));

      oneOf(myProject).getBuildTypes();
      will(returnValue(Collections.singletonList(bt)));

      oneOf(myFeatures).searchForFeatures(bt);
      will(returnValue(Collections.singletonList(feature)));

      oneOf(bt).getProjectId();
      will(returnValue(myProjectId));

      oneOf(feature).getLockedResources();
      will(returnValue(lockedResources));
    }});

    final Map<Resource, Map<SBuildType, List<Lock>>> result = myAnalyzer.collectResourceUsages(myProject);
    assertFalse(result.isEmpty());
    assertEquals(1, result.size());
    final Map<SBuildType, List<Lock>> btLocks = result.get(resource);
    assertNotNull(btLocks);
    assertEquals(1, btLocks.size());
    final List<Lock> locksList = btLocks.get(bt);
    assertNotNull(locksList);
    assertEquals(1, locksList.size());
    assertContains(locksList, lock);
  }

  @Test
  public void testSubProjectNoOverride() throws Exception {
    final Resource resource = ResourceFactory.newInfiniteResource("resource1", true);
    final Map<String, Resource> resourceMap = new HashMap<String, Resource>() {{
      put(resource.getName(), resource);
    }};

    final String subProjectId = "MY_SUB_PROJECT";

    final Resource subResource = ResourceFactory.newInfiniteResource("resource2", true);
    final Map<String, Resource> subProjectResourceMap = new HashMap<String, Resource>() {{
      putAll(resourceMap);
      put(subResource.getName(), subResource);
    }};

    final SBuildType bt = m.mock(SBuildType.class, "buildType: " + myProjectId);
    final SharedResourcesFeature feature = m.mock(SharedResourcesFeature.class, "feature: " + myProjectId);

    final SBuildType subBt = m.mock(SBuildType.class, "buildType: " + subProjectId);
    final SharedResourcesFeature subFeature = m.mock(SharedResourcesFeature.class, "feature: " + subProjectId);

    final Lock lock = new Lock("resource1", LockType.READ);
    final Map<String, Lock> lockedResources = new HashMap<String, Lock>() {{
      put(lock.getName(), lock);
    }};
    setupResourcesForProject(resourceMap, subProjectId, subProjectResourceMap, bt, feature, subBt, subFeature, lockedResources);

    final Map<Resource, Map<SBuildType, List<Lock>>> result = myAnalyzer.collectResourceUsages(myProject);
    assertFalse(result.isEmpty());
    assertEquals(1, result.size());
    final Map<SBuildType, List<Lock>> btLocks = result.get(resource);
    assertNotNull(btLocks);
    assertEquals(2, btLocks.size());
    final List<Lock> locksList = btLocks.get(bt);
    assertNotNull(locksList);
    assertEquals(1, locksList.size());
    assertContains(locksList, lock);
    final List<Lock> subLocksList = btLocks.get(subBt);
    assertNotNull(subLocksList);
    assertEquals(1, subLocksList.size());
    assertContains(subLocksList, lock);
  }

  /**
   * SubProject contains resource with the same name as parent project
   *
   * @throws Exception if something goes wrong
   */
  @Test
  public void testSubProjectOverride() throws Exception {
    final Resource resource = ResourceFactory.newInfiniteResource("resource1", true);
    final Map<String, Resource> resourceMap = new HashMap<String, Resource>() {{
      put(resource.getName(), resource);
    }};

    final String subProjectId = "MY_SUB_PROJECT";

    final Resource subResource = ResourceFactory.newInfiniteResource("resource1", true);
    final Map<String, Resource> subProjectResourceMap = new HashMap<String, Resource>() {{
      putAll(resourceMap);
      put(subResource.getName(), subResource);
    }};

    final SBuildType bt = m.mock(SBuildType.class, "buildType: " + myProjectId);
    final SharedResourcesFeature feature = m.mock(SharedResourcesFeature.class, "feature: " + myProjectId);

    final SBuildType subBt = m.mock(SBuildType.class, "buildType: " + subProjectId);
    final SharedResourcesFeature subFeature = m.mock(SharedResourcesFeature.class, "feature: " + subProjectId);

    final Lock lock = new Lock("resource1", LockType.READ);
    final Map<String, Lock> lockedResources = new HashMap<String, Lock>() {{
      put(lock.getName(), lock);
    }};

    setupResourcesForProject(resourceMap, subProjectId, subProjectResourceMap, bt, feature, subBt, subFeature, lockedResources);

    final Map<Resource, Map<SBuildType, List<Lock>>> result = myAnalyzer.collectResourceUsages(myProject);
    assertFalse(result.isEmpty());
    assertEquals(1, result.size());
    final Map<SBuildType, List<Lock>> btLocks = result.get(resource);
    assertNotNull(btLocks);
    assertEquals(1, btLocks.size());
    final List<Lock> locksList = btLocks.get(bt);
    assertNotNull(locksList);
    assertEquals(1, locksList.size());
    assertContains(locksList, lock);
  }

  private void setupResourcesForProject(final Map<String, Resource> resourceMap, final String subProjectId, final Map<String, Resource> subProjectResourceMap, final SBuildType bt, final SharedResourcesFeature feature, final SBuildType subBt, final SharedResourcesFeature subFeature, final Map<String, Lock> lockedResources) {
    m.checking(new Expectations() {{
      oneOf(myProject).getProjectId();
      will(returnValue(myProjectId));

      oneOf(myResources).asMap(myProjectId);
      will(returnValue(resourceMap));

      oneOf(myProject).getBuildTypes();
      will(returnValue(Arrays.asList(bt, subBt)));

      oneOf(myFeatures).searchForFeatures(bt);
      will(returnValue(Collections.singletonList(feature)));

      oneOf(bt).getProjectId();
      will(returnValue(myProjectId));

      oneOf(feature).getLockedResources();
      will(returnValue(lockedResources));

      oneOf(myFeatures).searchForFeatures(subBt);
      will(returnValue(Collections.singletonList(subFeature)));

      oneOf(subBt).getProjectId();
      will(returnValue(subProjectId));

      oneOf(myResources).asMap(subProjectId);
      will(returnValue(subProjectResourceMap));

      oneOf(subFeature).getLockedResources();
      will(returnValue(lockedResources));
    }});
  }
}
