/*
 * Copyright 2000-2020 JetBrains s.r.o.
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

import java.util.*;
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
import org.jmock.Expectations;
import org.jmock.Mockery;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

/**
 * @author Oleg Rybak <oleg.rybak@jetbrains.com>
 */
public class ConfigurationInspectorTest extends BaseTestCase {

  private Mockery m;

  private SharedResourcesFeature myFeature;

  private Resources myResources;

  private SharedResourcesFeatures myFeatures;

  private ConfigurationInspector myInspector;

  private SProject myProject;

  private static final String PROJECT_ID = "MY_PROJECT";

  @BeforeMethod
  @Override
  protected void setUp() throws Exception {
    super.setUp();
    m = new Mockery();
    myResources = m.mock(Resources.class);
    myFeatures = m.mock(SharedResourcesFeatures.class);
    myInspector = new ConfigurationInspector(myFeatures, myResources);
    myProject = m.mock(SProject.class, "My Project");
    myFeature = m.mock(SharedResourcesFeature.class, "my-default-feature");
  }

  @Test
  public void testInspect_SingleFeature_NoLockedResources() {
    m.checking(new Expectations() {{
      oneOf(myFeature).getLockedResources();
      will(returnValue(Collections.emptyMap()));
    }});
    assertEquals("Feature without locked resources must produce no invalid locks", 0, myInspector.inspect(myProject, myFeature).size());
  }

  @Test
  @SuppressWarnings("Duplicates")
  public void testInspect_SingleFeature_Correct() {
    final Map<String, Lock> locks = new HashMap<String, Lock>() {{
      put("lock1", new Lock("lock1", LockType.READ));
      put("lock2", new Lock("lock2", LockType.WRITE));
      put("lock3", new Lock("lock3", LockType.WRITE, "value1"));
    }};
    final List<Resource> resources = new ArrayList<Resource>() {{
      add(ResourceFactory.newInfiniteResource("lock1", PROJECT_ID, "lock1", true));
      add(ResourceFactory.newQuotedResource("lock2", PROJECT_ID, "lock2", 123, true));
      add(ResourceFactory.newCustomResource("lock3", PROJECT_ID, "lock3", Collections.singletonList("value1"), true));
    }};

    m.checking(new Expectations() {{
      oneOf(myFeature).getLockedResources();
      will(returnValue(locks));

      oneOf(myProject).getProjectPath();
      will(returnValue(Collections.singletonList(myProject)));

      oneOf(myResources).getAllOwnResources(myProject);
      will(returnValue(resources));

      oneOf(myResources).getOwnResources(myProject);
      will(returnValue(resources));

    }});
    final Map<Lock, String> result = myInspector.inspect(myProject, myFeature);
    assertEquals("Correct feature must produce no invalid locks", 0, result.size());
  }

  @Test
  @SuppressWarnings("Duplicates")
  public void testInspect_SingleFeature_DuplicateResource_Locked() {
    final Lock lock = new Lock("lock1", LockType.READ);
    final Map<String, Lock> locks = new HashMap<String, Lock>() {{
      put("lock1", lock);
    }};

    final List<Resource> resources = new ArrayList<Resource>() {{
      add(ResourceFactory.newInfiniteResource("lock1", PROJECT_ID, "lock1", true));
      add(ResourceFactory.newInfiniteResource("lock1", PROJECT_ID, "lock1", true));
    }};

    m.checking(new Expectations() {{
      oneOf(myFeature).getLockedResources();
      will(returnValue(locks));

      oneOf(myProject).getProjectPath();
      will(returnValue(Collections.singletonList(myProject)));

      oneOf(myResources).getAllOwnResources(myProject);
      will(returnValue(resources));
    }});

    final Map<Lock, String> result = myInspector.inspect(myProject, myFeature);
    assertEquals("Duplicate resource should be marked as invalid", 1, result.size());
  }

  @Test
  @SuppressWarnings("Duplicates")
  public void testInspect_SingleFeature_DuplicateResource_NotLocked() {
    final Lock lock = new Lock("lock2", LockType.READ);
    final Map<String, Lock> locks = new HashMap<String, Lock>() {{
      put("lock2", lock);
    }};

    final List<Resource> ownResources = new ArrayList<Resource>() {{
      add(ResourceFactory.newInfiniteResource("lock2", PROJECT_ID, "lock2", true));
    }};

    final List<Resource> allOwnResources = new ArrayList<Resource>() {{
      add(ResourceFactory.newInfiniteResource("lock1", PROJECT_ID, "lock1", true));
      add(ResourceFactory.newInfiniteResource("lock1", PROJECT_ID, "lock1", true));
      addAll(ownResources);
    }};

    m.checking(new Expectations() {{
      oneOf(myFeature).getLockedResources();
      will(returnValue(locks));

      oneOf(myProject).getProjectPath();
      will(returnValue(Collections.singletonList(myProject)));

      oneOf(myResources).getAllOwnResources(myProject);
      will(returnValue(allOwnResources));

      oneOf(myResources).getOwnResources(myProject);
      will(returnValue(ownResources));
    }});

    final Map<Lock, String> result = myInspector.inspect(myProject, myFeature);
    assertEquals("Not locked duplicate resource should not produce error", 0, result.size());
  }

  @Test
  @SuppressWarnings("Duplicates")
  public void testInspect_SingleFeature_MissingResource() {
    final Lock lock = new Lock("lock1", LockType.READ);
    final Map<String, Lock> locks = new HashMap<String, Lock>() {{
      put("lock1", lock);
    }};

    final List<Resource> resources = new ArrayList<Resource>() {{
      add(ResourceFactory.newInfiniteResource("lock2", PROJECT_ID, "lock2", true));
    }};

    m.checking(new Expectations() {{
      oneOf(myFeature).getLockedResources();
      will(returnValue(locks));

      oneOf(myProject).getProjectPath();
      will(returnValue(Collections.singletonList(myProject)));

      oneOf(myResources).getAllOwnResources(myProject);
      will(returnValue(resources));

      oneOf(myResources).getOwnResources(myProject);
      will(returnValue(resources));
    }});

    final Map<Lock, String> result = myInspector.inspect(myProject, myFeature);
    assertEquals("Missing resource should produce error", 1, result.size());
    assertEquals("Resource 'lock1' does not exist", result.get(lock));
  }

  @Test
  @SuppressWarnings("Duplicates")
  public void testInspect_SingleFeature_WrongType() {
    final Lock lock = new Lock("lock1", LockType.WRITE, "value1");
    final Map<String, Lock> locks = new HashMap<String, Lock>() {{
      put("lock1", lock);
    }};

    final List<Resource> resources = new ArrayList<Resource>() {{
      add(ResourceFactory.newInfiniteResource("lock1", PROJECT_ID, "lock1", true));
    }};

    m.checking(new Expectations() {{
      oneOf(myFeature).getLockedResources();
      will(returnValue(locks));

      oneOf(myProject).getProjectPath();
      will(returnValue(Collections.singletonList(myProject)));

      oneOf(myResources).getAllOwnResources(myProject);
      will(returnValue(resources));

      oneOf(myResources).getOwnResources(myProject);
      will(returnValue(resources));
    }});

    final Map<Lock, String> result = myInspector.inspect(myProject, myFeature);
    assertEquals("Missing resource should produce error", 1, result.size());
    assertEquals("Resource 'lock1' has wrong type: expected 'custom' got 'infinite'", result.get(lock));
  }

  @Test
  @SuppressWarnings("Duplicates")
  public void testInspect_SingleFeature_MissingValue() {
    final Lock lock = new Lock("lock1", LockType.WRITE, "value1");
    final Map<String, Lock> locks = new HashMap<String, Lock>() {{
      put("lock1", lock);
    }};

    final List<Resource> resources = new ArrayList<Resource>() {{
      add(ResourceFactory.newCustomResource("lock1", PROJECT_ID, "lock1", Collections.singletonList("other value"), true));
    }};

    m.checking(new Expectations() {{
      oneOf(myFeature).getLockedResources();
      will(returnValue(locks));

      oneOf(myProject).getProjectPath();
      will(returnValue(Collections.singletonList(myProject)));

      oneOf(myResources).getAllOwnResources(myProject);
      will(returnValue(resources));

      oneOf(myResources).getOwnResources(myProject);
      will(returnValue(resources));
    }});

    final Map<Lock, String> result = myInspector.inspect(myProject, myFeature);
    assertEquals("Missing resource should produce error", 1, result.size());
    assertEquals("Resource 'lock1' does not contain required value 'value1'", result.get(lock));
  }

  /**
   * If some resource triggers the inspection on some level of the project hierarchy,
   * but on the upper level resource with the same name is correct,
   * the error must be returned anyway
   */
  @Test
  @SuppressWarnings("Duplicates")
  public void testInspectShouldNotSearchAgainInAncestor() {
    final SProject parent = m.mock(SProject.class, "parent-project");
    final List<SProject> path = Arrays.asList(parent, myProject);

    final Lock lock = new Lock("lock1", LockType.WRITE, "my value");
    final Map<String, Lock> locks = new HashMap<String, Lock>() {{
      put("lock1", lock);
      put("lock2", new Lock("lock2", LockType.READ));
    }};

    final List<Resource> projectResources = new ArrayList<Resource>() {{
      add(ResourceFactory.newCustomResource("lock1", PROJECT_ID, "lock1", Collections.singletonList("custom value"), true));
    }};

    final List<Resource> parentResources = new ArrayList<Resource>() {{
      add(ResourceFactory.newCustomResource("lock1", "PARENT", "lock1", Collections.singletonList("my value"), true));
      add(ResourceFactory.newInfiniteResource("lock2", "PARENT", "lock2", true));
    }};

    m.checking(new Expectations() {{
      oneOf(myFeature).getLockedResources();
      will(returnValue(locks));

      oneOf(myProject).getProjectPath();
      will(returnValue(path));

      oneOf(myResources).getAllOwnResources(myProject);
      will(returnValue(projectResources));

      oneOf(myResources).getOwnResources(myProject);
      will(returnValue(projectResources));

      oneOf(myResources).getAllOwnResources(parent);
      will(returnValue(parentResources));

      oneOf(myResources).getOwnResources(parent);
      will(returnValue(parentResources));
    }});

    final Map<Lock, String> result = myInspector.inspect(myProject, myFeature);
    assertEquals("Missing resource should produce error", 1, result.size());
    assertEquals("Resource 'lock1' does not contain required value 'my value'", result.get(lock));
  }

  @Test
  @SuppressWarnings("Duplicates")
  public void testDuplicateResourcesShouldNotBeResolvedAgainInAncestor() {
    final SProject parent = m.mock(SProject.class, "parent-project");
    final List<SProject> path = Arrays.asList(parent, myProject);

    final Lock lock = new Lock("lock1", LockType.WRITE, "my value");
    final Map<String, Lock> locks = new HashMap<String, Lock>() {{
      put("lock1", lock);
      put("lock2", new Lock("lock2", LockType.READ));
    }};
    final List<Resource> projectResources = new ArrayList<Resource>() {{
      add(ResourceFactory.newCustomResource("lock1", PROJECT_ID, "lock1", Collections.singletonList("custom value"), true));
      add(ResourceFactory.newCustomResource("lock1", PROJECT_ID, "lock1", Collections.singletonList("custom value"), true));
    }};

    final List<Resource> parentResources = new ArrayList<Resource>() {{
      add(ResourceFactory.newCustomResource("lock1", "PARENT", "lock1", Collections.singletonList("my value"), true));
      add(ResourceFactory.newInfiniteResource("lock2", "PARENT", "lock2", true));
    }};

    m.checking(new Expectations() {{
      oneOf(myFeature).getLockedResources();
      will(returnValue(locks));

      oneOf(myProject).getProjectPath();
      will(returnValue(path));

      oneOf(myResources).getAllOwnResources(myProject);
      will(returnValue(projectResources));

      oneOf(myResources).getOwnResources(myProject);
      will(returnValue(projectResources));

      oneOf(myResources).getAllOwnResources(parent);
      will(returnValue(parentResources));

      oneOf(myResources).getOwnResources(parent);
      will(returnValue(parentResources));
    }});

    final Map<Lock, String> result = myInspector.inspect(myProject, myFeature);
    assertEquals("Missing resource should produce error", 1, result.size());
    assertEquals("Resource 'lock1' cannot be resolved due to duplicate name", result.get(lock));
  }

  @Test
  @SuppressWarnings("Duplicates")
  public void testInspect_MultipleFeatures_Correct() {
    final SProject parent = m.mock(SProject.class, "parent-project");
    final List<SProject> path = Arrays.asList(parent, myProject);
    final SBuildType buildType = m.mock(SBuildType.class);
    final SharedResourcesFeature secondFeature = m.mock(SharedResourcesFeature.class, "second-feature");

    final Map<String, Lock> locked1 = new HashMap<String, Lock>() {{
      put("lock1", new Lock("lock1", LockType.READ));
    }};

    final Map<String, Lock> locked2 = new HashMap<String, Lock>() {{
      put("lock2", new Lock("lock2", LockType.WRITE));
      put("lock3", new Lock("lock3", LockType.READ, "my value"));
    }};

    final List<Resource> projectResources = new ArrayList<Resource>() {{
      add(ResourceFactory.newInfiniteResource("lock1", PROJECT_ID, "lock1", true));
    }};

    final List<Resource> parentResources = new ArrayList<Resource>() {{
      add(ResourceFactory.newInfiniteResource("lock2", "PARENT", "lock2", true));
      add(ResourceFactory.newCustomResource("lock3", "PARENT", "lock3", Collections.singletonList("my value"), true));
    }};

    m.checking(new Expectations() {{
      oneOf(buildType).getProject();
      will(returnValue(myProject));

      oneOf(myFeatures).searchForFeatures(buildType);
      will(returnValue(Arrays.asList(myFeature, secondFeature)));

      oneOf(myFeature).getLockedResources();
      will(returnValue(locked1));

      oneOf(secondFeature).getLockedResources();
      will(returnValue(locked2));

      oneOf(myProject).getProjectPath();
      will(returnValue(path));

      oneOf(myResources).getAllOwnResources(myProject);
      will(returnValue(projectResources));

      oneOf(myResources).getOwnResources(myProject);
      will(returnValue(projectResources));

      oneOf(myResources).getAllOwnResources(parent);
      will(returnValue(parentResources));

      oneOf(myResources).getOwnResources(parent);
      will(returnValue(parentResources));
    }});

    final Map<Lock, String> result = myInspector.inspect(buildType);
    assertEquals("Correct build type should not produce errors", 0, result.size());
  }

  @AfterMethod
  @Override
  protected void tearDown() throws Exception {
    super.tearDown();
    m.assertIsSatisfied();
  }
}
