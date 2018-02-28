/*
 * Copyright 2000-2013 JetBrains s.r.o.
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

import com.intellij.openapi.util.text.StringUtil;
import jetbrains.buildServer.BaseTestCase;
import jetbrains.buildServer.serverSide.*;
import jetbrains.buildServer.sharedResources.model.Lock;
import jetbrains.buildServer.sharedResources.model.LockType;
import jetbrains.buildServer.sharedResources.model.resources.CustomResource;
import jetbrains.buildServer.sharedResources.model.resources.Resource;
import jetbrains.buildServer.sharedResources.model.resources.ResourceFactory;
import jetbrains.buildServer.sharedResources.server.feature.Locks;
import jetbrains.buildServer.sharedResources.server.feature.Resources;
import jetbrains.buildServer.sharedResources.server.feature.SharedResourcesFeature;
import jetbrains.buildServer.sharedResources.server.feature.SharedResourcesFeatures;
import jetbrains.buildServer.sharedResources.server.runtime.LocksStorage;
import jetbrains.buildServer.util.TestFor;
import org.jmock.Expectations;
import org.jmock.Mockery;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.util.*;

/**
 * Created with IntelliJ IDEA.
 *
 * @author Oleg Rybak (oleg.rybak@jetbrains.com)
 */
@TestFor(testForClass = SharedResourcesContextProcessor.class)
public class ContextProcessorTest extends BaseTestCase {
  private final String PROJECT_ID = "PROJECT_ID";
  private Mockery m;
  private SharedResourcesFeatures myFeatures;
  private Locks myLocks;
  private Resources myResources;
  private LocksStorage myLocksStorage;
  private RunningBuildsManager myRunningBuildsManager;
  private SRunningBuild myRunningBuild;
  private SBuildType myBuildType;
  private BuildPromotionEx myBuildPromotion;
  private BuildStartContext myBuildStartContext;

  /** Class under test */
  private SharedResourcesContextProcessor myProcessor;


  @BeforeMethod
  @Override
  protected void setUp() throws Exception {
    super.setUp();
    m = new Mockery();
    myFeatures = m.mock(SharedResourcesFeatures.class);
    myLocks = m.mock(Locks.class);
    myResources = m.mock(Resources.class);
    myLocksStorage = m.mock(LocksStorage.class);
    myRunningBuildsManager = m.mock(RunningBuildsManager.class);
    myBuildStartContext = m.mock(BuildStartContext.class);
    myRunningBuild = m.mock(SRunningBuild.class);
    myBuildType = m.mock(SBuildType.class);
    myBuildPromotion = m.mock(BuildPromotionEx.class);
    myProcessor = new SharedResourcesContextProcessor(myFeatures, myLocks, myResources, myLocksStorage, myRunningBuildsManager);
    m.checking(createCommonExpectations());
  }

  @Test
  @TestFor(issues = "TW-29779")
  public void testWriteLockShouldProvideAllResourceValues() {
    final Map<String, Lock> myTakenLocks = new HashMap<>();
    final Lock lock = new Lock("CustomResource", LockType.WRITE);
    myTakenLocks.put(lock.getName(), lock);

    final Map<String, Resource> myDefinedResources = new HashMap<>();
    final CustomResource resource = (CustomResource) ResourceFactory.newCustomResource("CustomResource", PROJECT_ID, "CustomResource", Arrays.asList("value1", "value2"), true);
    myDefinedResources.put(resource.getName(), resource);

    final String lockParamName = "teamcity.locks.writeLock." + lock.getName();

    final SharedResourcesFeature feature = m.mock(SharedResourcesFeature.class);
    final Collection<SharedResourcesFeature> features = Collections.singleton(feature);

    m.checking(new Expectations() {{
      oneOf(myFeatures).searchForFeatures(myBuildType);
      will(returnValue(features));

      oneOf(feature).getLockedResources();
      will(returnValue(myTakenLocks));

      oneOf(myResources).getResourcesMap(PROJECT_ID);
      will(returnValue(myDefinedResources));

      oneOf(myRunningBuildsManager).getRunningBuilds();
      will(returnValue(Collections.emptyList()));

      oneOf(myLocks).asBuildParameter(lock);
      will(returnValue(lockParamName));

      oneOf(myBuildStartContext).addSharedParameter(lockParamName, StringUtil.join(resource.getValues(), ";"));

      allowing(myLocksStorage);
    }});
    myProcessor.updateParameters(myBuildStartContext);
  }

  @Test
  public void testProvideValueAny() {
    final Map<String, Lock> myTakenLocks = new HashMap<>();
    final Lock lock = new Lock("CustomResource", LockType.READ);
    myTakenLocks.put(lock.getName(), lock);

    final Map<String, Resource> myDefinedResources = new HashMap<>();
    final CustomResource resource = (CustomResource) ResourceFactory.newCustomResource("CustomResource", PROJECT_ID, "CustomResource", Arrays.asList("value1", "value2"), true);
    myDefinedResources.put(resource.getName(), resource);

    final String lockParamName = "teamcity.locks.readLock." + lock.getName();

    final SharedResourcesFeature feature = m.mock(SharedResourcesFeature.class);
    final Collection<SharedResourcesFeature> features = Collections.singleton(feature);

    m.checking(new Expectations() {{
      oneOf(myFeatures).searchForFeatures(myBuildType);
      will(returnValue(features));

      oneOf(feature).getLockedResources();
      will(returnValue(myTakenLocks));

      oneOf(myResources).getResourcesMap(PROJECT_ID);
      will(returnValue(myDefinedResources));

      oneOf(myRunningBuildsManager).getRunningBuilds();
      will(returnValue(Collections.emptyList()));

      oneOf(myLocks).asBuildParameter(lock);
      will(returnValue(lockParamName));

      oneOf(myBuildStartContext).addSharedParameter(lockParamName, resource.getValues().iterator().next());

      allowing(myLocksStorage);
    }});
    myProcessor.updateParameters(myBuildStartContext);
  }

  @Test
  public void testProvideValueAny_SomeTaken() {
    final Map<String, Lock> myTakenLocks = new HashMap<>();
    final Lock lock = new Lock("CustomResource", LockType.READ);
    myTakenLocks.put(lock.getName(), lock);

    final Map<String, Resource> myDefinedResources = new HashMap<>();
    final CustomResource resource = (CustomResource) ResourceFactory.newCustomResource("CustomResource", PROJECT_ID, "CustomResource", Arrays.asList("value1", "value2"), true);
    myDefinedResources.put(resource.getName(), resource);

    final String lockParamName = "teamcity.locks.readLock." + lock.getName();

    final SRunningBuild otherRunningBuild = m.mock(SRunningBuild.class, "other-running-build");
    final Map<String, Lock> otherTakenLocks = new HashMap<>();
    otherTakenLocks.put("CustomResource", new Lock("CustomResource", LockType.READ, "value1"));

    final SharedResourcesFeature currentFeature = m.mock(SharedResourcesFeature.class, "my-build-feature");
    final Collection<SharedResourcesFeature> currentFeatures = Collections.singleton(currentFeature);

    m.checking(new Expectations() {{
      oneOf(myFeatures).searchForFeatures(myBuildType);
      will(returnValue(currentFeatures));

      oneOf(currentFeature).getLockedResources();
      will(returnValue(myTakenLocks));

      oneOf(myResources).getResourcesMap(PROJECT_ID);
      will(returnValue(myDefinedResources));

      oneOf(myRunningBuildsManager).getRunningBuilds();
      will(returnValue(Collections.singletonList(otherRunningBuild)));

      oneOf(myLocksStorage).load(otherRunningBuild);
      will(returnValue(otherTakenLocks));

      oneOf(myLocks).asBuildParameter(lock);
      will(returnValue(lockParamName));

      final Map<Lock, String> storedLocks = new HashMap<>();
      storedLocks.put(lock, "value2");
      oneOf(myLocksStorage).store(myRunningBuild, storedLocks);

      oneOf(myBuildStartContext).addSharedParameter(lockParamName, "value2");
    }});
    myProcessor.updateParameters(myBuildStartContext);
  }

  @Test
  public void testProvideValueSpecific() {
    final Map<String, Lock> myTakenLocks = new HashMap<>();
    final Lock lock = new Lock("CustomResource", LockType.READ, "value2");
    myTakenLocks.put(lock.getName(), lock);

    final Map<String, Resource> myDefinedResources = new HashMap<>();
    final CustomResource resource = (CustomResource) ResourceFactory.newCustomResource("CustomResource", PROJECT_ID, "CustomResource", Arrays.asList("value1", "value2"), true);
    myDefinedResources.put(resource.getName(), resource);

    final String lockParamName = "teamcity.locks.readLock." + lock.getName();

    final SharedResourcesFeature feature = m.mock(SharedResourcesFeature.class);
    final Collection<SharedResourcesFeature> features = Collections.singleton(feature);

    m.checking(new Expectations() {{
      createCommonExpectations();

      oneOf(myFeatures).searchForFeatures(myBuildType);
      will(returnValue(features));

      oneOf(feature).getLockedResources();
      will(returnValue(myTakenLocks));

      oneOf(myResources).getResourcesMap(PROJECT_ID);
      will(returnValue(myDefinedResources));

      oneOf(myRunningBuildsManager).getRunningBuilds();
      will(returnValue(Collections.emptyList()));

      oneOf(myLocks).asBuildParameter(lock);
      will(returnValue(lockParamName));

      oneOf(myBuildStartContext).addSharedParameter(lockParamName, lock.getValue());

      allowing(myLocksStorage);
    }});
    myProcessor.updateParameters(myBuildStartContext);
  }

  @Test
  @TestFor(issues = "TW-44929")
  public void testProvideDuplicateValue() {
    final String VALUE = "a";
    final Map<String, Resource> definedResources = new HashMap<>();
    final CustomResource resource = (CustomResource) ResourceFactory.newCustomResource("CustomResource", PROJECT_ID, "CustomResource", Arrays.asList(VALUE, VALUE), true);
    definedResources.put(resource.getName(), resource);

    final Map<String, Lock> takenLocks = new HashMap<>();
    final Lock lock = new Lock("CustomResource", LockType.READ);
    takenLocks.put(lock.getName(), lock);

    final String lockParamName = "teamcity.locks.readLock." + lock.getName();
    final SharedResourcesFeature feature = m.mock(SharedResourcesFeature.class);
    final Collection<SharedResourcesFeature> features = Collections.singleton(feature);

    final SRunningBuild runningBuild = m.mock(SRunningBuild.class, "running-build");
    final Map<String, Lock> runningBuildLocks = new HashMap<>();
    runningBuildLocks.put(resource.getName(), new Lock(resource.getName(), LockType.READ, VALUE));

    m.checking(new Expectations() {{

      oneOf(myFeatures).searchForFeatures(myBuildType);
      will(returnValue(features));

      oneOf(feature).getLockedResources();
      will(returnValue(takenLocks));

      oneOf(myResources).getResourcesMap(PROJECT_ID);
      will(returnValue(definedResources));

      oneOf(myRunningBuildsManager).getRunningBuilds();
      will(returnValue(Collections.singletonList(runningBuild)));

      oneOf(myLocksStorage).load(runningBuild);
      will(returnValue(runningBuildLocks));

      oneOf(myLocks).asBuildParameter(lock);
      will(returnValue(lockParamName));

      final Map<Lock, String> storedLocks = new HashMap<>();
      storedLocks.put(lock, VALUE);
      oneOf(myLocksStorage).store(myRunningBuild, storedLocks);

      oneOf(myBuildStartContext).addSharedParameter(lockParamName, VALUE);

      allowing(myLocksStorage);
    }});
    myProcessor.updateParameters(myBuildStartContext);
  }

  /**
   * 2 running builds hold same duplicate value
   * tests that the build about to start gets the different one
   */
  @Test
  @TestFor(issues = "TW-44929")
  public void testDupValues_CollectLockedResources() {
    final String VALUE_HELD = "a";
    final String VALUE_AVAILABLE = "b";

    final Map<String, Resource> definedResources = new HashMap<>();
    final CustomResource resource = (CustomResource) ResourceFactory.newCustomResource("CustomResource", PROJECT_ID, "CustomResource", Arrays.asList(VALUE_HELD, VALUE_HELD, VALUE_AVAILABLE), true);
    definedResources.put(resource.getName(), resource);

    final Map<String, Lock> takenLocks = new HashMap<>();
    final Lock lock = new Lock("CustomResource", LockType.READ);
    takenLocks.put(lock.getName(), lock);

    final String lockParamName = "teamcity.locks.readLock." + lock.getName();
    final SharedResourcesFeature feature = m.mock(SharedResourcesFeature.class);
    final Collection<SharedResourcesFeature> features = Collections.singleton(feature);

    final SRunningBuild runningBuild1 = m.mock(SRunningBuild.class, "running-build-1");
    final Map<String, Lock> runningBuildLocks1 = new HashMap<>();
    runningBuildLocks1.put(resource.getName(), new Lock(resource.getName(), LockType.READ, VALUE_HELD));

    final SRunningBuild runningBuild2 = m.mock(SRunningBuild.class, "running-build-2");
    final Map<String, Lock> runningBuildLocks2 = new HashMap<>();
    runningBuildLocks2.put(resource.getName(), new Lock(resource.getName(), LockType.READ, VALUE_HELD));

    m.checking(new Expectations() {{
      oneOf(myFeatures).searchForFeatures(myBuildType);
      will(returnValue(features));

      oneOf(feature).getLockedResources();
      will(returnValue(takenLocks));

      oneOf(myResources).getResourcesMap(PROJECT_ID);
      will(returnValue(definedResources));

      oneOf(myRunningBuildsManager).getRunningBuilds();
      will(returnValue(Arrays.asList(runningBuild1, runningBuild2)));

      oneOf(myLocksStorage).load(runningBuild1);
      will(returnValue(runningBuildLocks1));

      oneOf(myLocksStorage).load(runningBuild2);
      will(returnValue(runningBuildLocks2));

      oneOf(myLocks).asBuildParameter(lock);
      will(returnValue(lockParamName));

      final Map<Lock, String> storedLocks = new HashMap<>();
      storedLocks.put(lock, VALUE_AVAILABLE);
      oneOf(myLocksStorage).store(myRunningBuild, storedLocks);

      oneOf(myBuildStartContext).addSharedParameter(lockParamName, VALUE_AVAILABLE);

      allowing(myLocksStorage);
    }});
    myProcessor.updateParameters(myBuildStartContext);
  }

  private Expectations createCommonExpectations() {
    return new Expectations() {{
      oneOf(myBuildStartContext).getBuild();
      will(returnValue(myRunningBuild));

      allowing(myRunningBuild).getBuildType();
      will(returnValue(myBuildType));

      allowing(myRunningBuild).getProjectId();
      will(returnValue(PROJECT_ID));

      oneOf(myRunningBuild).getBuildPromotion();
      will(returnValue(myBuildPromotion));

      oneOf(myBuildPromotion).isPartOfBuildChain();
      will(returnValue(false));
    }};
  }
}
