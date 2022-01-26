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

package jetbrains.buildServer.sharedResources.server.runtime;

import com.intellij.openapi.util.Trinity;
import java.util.*;
import jetbrains.buildServer.BaseTestCase;
import jetbrains.buildServer.serverSide.*;
import jetbrains.buildServer.serverSide.buildDistribution.BuildDistributorInput;
import jetbrains.buildServer.serverSide.buildDistribution.DefaultAgentsFilterContext;
import jetbrains.buildServer.serverSide.buildDistribution.QueuedBuildInfo;
import jetbrains.buildServer.sharedResources.model.Lock;
import jetbrains.buildServer.sharedResources.model.LockType;
import jetbrains.buildServer.sharedResources.model.TakenLock;
import jetbrains.buildServer.sharedResources.model.resources.Resource;
import jetbrains.buildServer.sharedResources.model.resources.ResourceFactory;
import jetbrains.buildServer.sharedResources.server.feature.Locks;
import jetbrains.buildServer.sharedResources.server.feature.Resources;
import jetbrains.buildServer.sharedResources.server.feature.SharedResourcesFeature;
import jetbrains.buildServer.sharedResources.server.feature.SharedResourcesFeatures;
import jetbrains.buildServer.util.TestFor;
import org.jetbrains.annotations.NotNull;
import org.jmock.Expectations;
import org.jmock.Mockery;
import org.jmock.lib.legacy.ClassImposteriser;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import static jetbrains.buildServer.sharedResources.TestUtils.generateRandomName;

/**
 * Created with IntelliJ IDEA.
 *
 * @author Oleg Rybak (oleg.rybak@jetbrains.com)
 */
@SuppressWarnings("Duplicates")
@TestFor(testForClass = {TakenLocks.class, TakenLocksImpl.class})
public class TakenLocksImplTest extends BaseTestCase {

  private static final String myMockBuildTypeName = "Mock / mock";

  private Mockery m;

  private Locks myLocks;

  private Resources myResources;

  private LocksStorage myLocksStorage;

  /**
   * Class under test
   */
  private TakenLocks myTakenLocks;

  private SharedResourcesFeatures myFeatures;

  private BuildPromotion myPromotion;

  private DistributionDataAccessor myAccessor;

  private SBuildType myBuildType;

  private final String myProjectId = "MY_PROJECT_ID";


  @BeforeMethod
  @Override
  protected void setUp() throws Exception {
    super.setUp();
    m = new Mockery() {{
      setImposteriser(ClassImposteriser.INSTANCE);
    }};
    myLocks = m.mock(Locks.class);
    myResources = m.mock(Resources.class);
    myLocksStorage = m.mock(LocksStorage.class);
    myFeatures = m.mock(SharedResourcesFeatures.class);
    myPromotion = m.mock(BuildPromotion.class);
    myBuildType = m.mock(SBuildType.class);
    m.checking(new Expectations() {{
      allowing(any(BuildTypeEx.class)).method("getExtendedFullName");
      will(returnValue(myMockBuildTypeName));

      allowing(myPromotion).getBuildType();
      will(returnValue(myBuildType));
    }});

    myAccessor = new DistributionDataAccessor(new DefaultAgentsFilterContext(new HashMap<>()) {
      @NotNull
      @Override
      public QueuedBuildInfo getStartingBuild() {
        return m.mock(QueuedBuildInfo.class, "context-queued-build");
      }

      @NotNull
      @Override
      public Collection<SBuildAgent> getAgentsForStartingBuild() {
        return Collections.emptyList();
      }

      @NotNull
      @Override
      public Map<QueuedBuildInfo, SBuildAgent> getDistributedBuilds() {
        return Collections.emptyMap();
      }

      @NotNull
      @Override
      public BuildDistributorInput getDistributorInput() {
        return m.mock(BuildDistributorInput.class, "context-distributor-input");
      }

      @Override
      public boolean isEmulationMode() {
        return false;
      }
    });
    myTakenLocks = new TakenLocksImpl(myLocks, myResources, myLocksStorage, myFeatures);
  }

  @Test
  public void testCollectTakenLocks_EmptyInput() {
    final Map<Resource, TakenLock> result = myTakenLocks.collectTakenLocks(
            Collections.emptyList(), Collections.emptyList());
    assertNotNull(result);
    assertEquals(0, result.size());
  }

  @Test
  public void testCollectRunningBuilds_Stored() {
    final SharedResourcesFeature feature = m.mock(SharedResourcesFeature.class);
    final Collection<SharedResourcesFeature> features = Collections.singleton(feature);

    final Resource resource1 = ResourceFactory.newInfiniteResource("resource1_id", myProjectId, "resource1", true);
    final Resource resource2 = ResourceFactory.newInfiniteResource("resource2_id", myProjectId, "resource2", true);

    final Map<String, Resource> resources = new HashMap<String, Resource>() {{
      put(resource1.getName(), resource1);
      put(resource2.getName(), resource2);
    }};

    final Map<String, Lock> takenLocks1 = new HashMap<String, Lock>() {{
      put(resource1.getName(), new Lock(resource1.getName(), LockType.READ, ""));
      put(resource2.getName(), new Lock(resource2.getName(), LockType.WRITE, ""));

    }};

    final Map<String, Lock> takenLocks2 = new HashMap<String, Lock>() {{
      put(resource1.getName(), new Lock(resource1.getName(), LockType.READ, ""));
    }};

    final RunningBuildEx rb1 = m.mock(RunningBuildEx.class, "runningBuild_1");
    final RunningBuildEx rb2 = m.mock(RunningBuildEx.class, "runningBuild_2");

    final SBuildType rb1_bt = m.mock(SBuildType.class, "runningBuild_1-buildType");
    final SBuildType rb2_bt = m.mock(SBuildType.class, "runningBuild_2-buildType");

    final BuildPromotionEx bp1 = m.mock(BuildPromotionEx.class, "buildPromotion_1");
    final BuildPromotionEx bp2 = m.mock(BuildPromotionEx.class, "buildPromotion_2");

    final Collection<RunningBuildEx> runningBuilds = new ArrayList<RunningBuildEx>() {{
      add(rb1);
      add(rb2);
    }};

    m.checking(new Expectations() {{
      oneOf(rb1).getBuildType();
      will(returnValue(rb1_bt));

      oneOf(myFeatures).searchForFeatures(rb1_bt);
      will(returnValue(features));

      oneOf(rb1).getBuildPromotionInfo();
      will(returnValue(bp1));

      oneOf(myLocksStorage).locksStored(bp1);
      will(returnValue(true));

      oneOf(myLocksStorage).load(bp1);
      will(returnValue(takenLocks1));

      oneOf(rb1_bt).getProjectId();
      will(returnValue(myProjectId));

      oneOf(myResources).getResourcesMap(myProjectId);
      will(returnValue(resources));

      oneOf(rb2).getBuildType();
      will(returnValue(rb2_bt));

      oneOf(myFeatures).searchForFeatures(rb2_bt);
      will(returnValue(features));

      oneOf(rb2).getBuildPromotionInfo();
      will(returnValue(bp2));

      oneOf(myLocksStorage).locksStored(bp2);
      will(returnValue(true));

      oneOf(myLocksStorage).load(bp2);
      will(returnValue(takenLocks2));

      oneOf(rb2_bt).getProjectId();
      will(returnValue(myProjectId));

      allowing(rb1_bt).getExtendedFullName();
      will(returnValue("rb1_bt"));

      allowing(rb1_bt).getExtendedFullName();
      will(returnValue("rb2_bt"));

    }});

    final Map<Resource, TakenLock> result = myTakenLocks.collectTakenLocks(
            runningBuilds, Collections.emptyList());
    assertNotNull(result);
    assertEquals(2, result.size());
    final TakenLock tl1 = result.get(resource1);
    assertNotNull(tl1);
    assertEquals(2, tl1.getReadLocks().size());

    final TakenLock tl2 = result.get(resource2);
    assertNotNull(tl2);
    assertTrue(tl2.hasWriteLocks());
    m.assertIsSatisfied();
  }

  @Test
  @TestFor(issues = "TW-33790")
  public void testShouldNotAskParametersNoFeatures() {
    final RunningBuildEx rb = m.mock(RunningBuildEx.class, "rb");
    final SBuildType rb_bt = m.mock(SBuildType.class, "rb_bt");
    final Collection<RunningBuildEx> runningBuilds = new ArrayList<RunningBuildEx>() {{
      add(rb);
    }};

    m.checking(new Expectations() {{
      oneOf(rb).getBuildType();
      will(returnValue(rb_bt));

      oneOf(myFeatures).searchForFeatures(rb_bt);
      will(returnValue(Collections.emptyList()));

    }});

    final Map<Resource, TakenLock> result = myTakenLocks.collectTakenLocks(
            runningBuilds, Collections.emptyList());
    assertNotNull(result);
    assertEquals(0, result.size());
  }

  @Test
  public void testCollectRunningQueued_Promotions() {
    final SharedResourcesFeature rFeature = m.mock(SharedResourcesFeature.class, "r-feature");
    final Collection<SharedResourcesFeature> rFeatures = Collections.singleton(rFeature);

    final SharedResourcesFeature qFeature = m.mock(SharedResourcesFeature.class, "q-feature");
    final Collection<SharedResourcesFeature> qFeatures = Collections.singleton(qFeature);

    final Resource resource1 = ResourceFactory.newInfiniteResource("resource1_id", myProjectId, "resource1", true);
    final Resource resource2 = ResourceFactory.newInfiniteResource("resource2_id", myProjectId, "resource2", true);

    final Map<String, Resource> resources = new HashMap<String, Resource>() {{
      put(resource1.getName(), resource1);
      put(resource2.getName(), resource2);
    }};

    final Map<String, Lock> takenLocks1 = new HashMap<String, Lock>() {{
      put(resource1.getName() , new Lock(resource1.getName(), LockType.READ));
      put(resource2.getName(), new Lock(resource2.getName(), LockType.WRITE));
    }};

    final Map<String, Lock> takenLocks2 = new HashMap<String, Lock>() {{
      put(resource1.getName(), new Lock(resource1.getName(), LockType.READ));
    }};

    final RunningBuildEx rb1 = m.mock(RunningBuildEx.class, "rb-1");
    final BuildTypeEx rb1_bt = m.mock(BuildTypeEx.class, "rb1_bt");
    final BuildPromotionEx bp1 = m.mock(BuildPromotionEx.class, "bp-1");

    final QueuedBuildInfo qb1 = m.mock(QueuedBuildInfo.class, "qb-1");
    final BuildTypeEx qb1_bt = m.mock(BuildTypeEx.class, "qb1_bt");
    final BuildPromotionEx bp2 = m.mock(BuildPromotionEx.class, "bp-2");
    final Collection<RunningBuildEx> runningBuilds = new ArrayList<RunningBuildEx>() {{
      add(rb1);
    }};

    final Collection<QueuedBuildInfo> queuedBuilds = new ArrayList<QueuedBuildInfo>() {{
      add(qb1);
    }};

    m.checking(new Expectations() {{
      oneOf(rb1).getBuildType();
      will(returnValue(rb1_bt));

      oneOf(rb1).getBuildPromotionInfo();
      will(returnValue(bp1));

      oneOf(myLocksStorage).locksStored(bp1);
      will(returnValue(false));

      oneOf(myFeatures).searchForFeatures(rb1_bt);
      will(returnValue(rFeatures));

      oneOf(myLocks).fromBuildFeaturesAsMap(rFeatures);
      will(returnValue(takenLocks1));

      oneOf(rb1_bt).getProjectId();
      will(returnValue(myProjectId));

      oneOf(myResources).getResourcesMap(myProjectId);
      will(returnValue(resources));

      oneOf(qb1).getBuildPromotionInfo();
      will(returnValue(bp2));

      oneOf(bp2).getBuildType();
      will(returnValue(qb1_bt));

      oneOf(myFeatures).searchForFeatures(qb1_bt);
      will(returnValue(qFeatures));

      oneOf(myLocks).fromBuildFeaturesAsMap(qFeatures);
      will(returnValue(takenLocks2));

      oneOf(qb1_bt).getProjectId();
      will(returnValue(myProjectId));

    }});

    final Map<Resource, TakenLock> result = myTakenLocks.collectTakenLocks(
            runningBuilds, queuedBuilds);
    assertNotNull(result);
    assertEquals(2, result.size());
    final TakenLock tl1 = result.get(resource1);
    assertNotNull(tl1);
    assertEquals(2, tl1.getReadLocks().size());

    final TakenLock tl2 = result.get(resource2);
    assertNotNull(tl2);
    assertTrue(tl2.hasWriteLocks());
    m.assertIsSatisfied();
  }

  @Test
  public void testGetUnavailableLocks_Custom_All() {
    final Map<String, Resource> resources = new HashMap<>();
    final Resource myCustomResource = ResourceFactory.newCustomResource("custom_resource1_id", myProjectId, "custom_resource1", Arrays.asList("v1", "v2"), true);
    resources.put(myCustomResource.getName(), myCustomResource);

    final Collection<Lock> locksToTake = new ArrayList<Lock>() {{
      add(new Lock("custom_resource1", LockType.WRITE));
    }};

    final BuildPromotionEx bp1 = m.mock(BuildPromotionEx.class, "Custom_All_promo");
    final BuildTypeEx bp1bt = m.mock(BuildTypeEx.class, "Custom_All__bt");

    final Map<Resource, TakenLock> takenLocks = new HashMap<Resource, TakenLock>() {{
      TakenLock tl1 = new TakenLock(myCustomResource);

      tl1.addLock(bp1, new Lock("custom_resource1", LockType.READ));
      put(tl1.getResource(), tl1);
    }};

    m.checking(new Expectations() {{
      allowing(bp1).getBuildType();
      will(returnValue(bp1bt));

      oneOf(myResources).getResourcesMap(myProjectId);
      will(returnValue(resources));
    }});

    final Map<Resource, String> result = myTakenLocks.getUnavailableLocks(locksToTake, takenLocks, myProjectId, myAccessor, myPromotion);
    assertNotNull(result);
    assertEquals(1, result.size());
  }

  @Test
  public void testGetUnavailableLocks_Custom_Specific() {
    final Map<String, Resource> resources = new HashMap<>();
    final Resource myCustomResource = ResourceFactory.newCustomResource("custom_resource1_id", myProjectId, "custom_resource1", Arrays.asList("v1", "v2"), true);
    resources.put(myCustomResource.getName(), myCustomResource);

    final Collection<Lock> locksToTake = new ArrayList<Lock>() {{
      add(new Lock("custom_resource1", LockType.READ, "v1"));
    }};

    final BuildPromotionEx bp1 = m.mock(BuildPromotionEx.class, "Custom_Specific_promo");
    final BuildTypeEx bp1bt = m.mock(BuildTypeEx.class, "Custom_Specific_bt");

    final Map<Resource, TakenLock> takenLocks = new HashMap<Resource, TakenLock>() {{
      TakenLock tl1 = new TakenLock(myCustomResource);
      tl1.addLock(bp1, new Lock("custom_resource1", LockType.READ, "v1"));
      put(tl1.getResource(), tl1);
    }};

    m.checking(new Expectations() {{
      allowing(bp1).getBuildType();
      will(returnValue(bp1bt));

      oneOf(myResources).getResourcesMap(myProjectId);
      will(returnValue(resources));
    }});

    final Map<Resource, String> result = myTakenLocks.getUnavailableLocks(locksToTake, takenLocks, myProjectId, myAccessor, myPromotion);
    assertNotNull(result);
    assertEquals(1, result.size());
  }

  @Test
  public void testGetUnavailableLocks_Custom_Any() {
    // case when write lock ALL is taken
    final Map<String, Resource> resources = new HashMap<>();
    final Resource myCustomResource = ResourceFactory.newCustomResource("custom_resource1_id", myProjectId, "custom_resource1", Arrays.asList("v1", "v2"), true);
    resources.put(myCustomResource.getName(), myCustomResource);

    final Collection<Lock> locksToTake = new ArrayList<Lock>() {{
      add(new Lock("custom_resource1", LockType.READ));
    }};

    final BuildPromotionEx promo = m.mock(BuildPromotionEx.class, "Custom_Any_promo");
    final BuildTypeEx buildType = m.mock(BuildTypeEx.class, "Custom_Any_bt");

    final Map<Resource, TakenLock> takenLocks = new HashMap<Resource, TakenLock>() {{
      TakenLock tl1 = new TakenLock(myCustomResource);
      tl1.addLock(promo, new Lock("custom_resource1", LockType.WRITE));
      put(tl1.getResource(), tl1);
    }};

    m.checking(new Expectations() {{
      allowing(promo).getBuildType();
      will(returnValue(buildType));

      oneOf(myResources).getResourcesMap(myProjectId);
      will(returnValue(resources));                  
    }});

    final Map<Resource, String> result = myTakenLocks.getUnavailableLocks(locksToTake, takenLocks, myProjectId, myAccessor, myPromotion);
    assertNotNull(result);
    assertEquals(1, result.size());
  }

  @Test
  public void testGetUnavailableLocks_Custom_Any_NoValuesAvailable() {
    final Map<String, Resource> resources = new HashMap<>();
    final Resource myCustomResource = ResourceFactory.newCustomResource("custom_resource1_id", myProjectId, "custom_resource1", Arrays.asList("v1", "v2"), true);
    resources.put(myCustomResource.getName(), myCustomResource);

    final Collection<Lock> locksToTake = new ArrayList<Lock>() {{
      add(new Lock("custom_resource1", LockType.READ));
    }};

    final BuildPromotionEx bp1 = m.mock(BuildPromotionEx.class, "bp1");
    final BuildPromotionEx bp2 = m.mock(BuildPromotionEx.class, "bp2");

    final BuildTypeEx bp1bt = m.mock(BuildTypeEx.class, "bp1.bt");
    final BuildTypeEx bp2bt = m.mock(BuildTypeEx.class, "bp2.bt");

    final Map<Resource, TakenLock> takenLocks = new HashMap<Resource, TakenLock>() {{
      TakenLock tl1 = new TakenLock(myCustomResource);
      tl1.addLock(bp1, new Lock("custom_resource1", LockType.READ, "v1"));
      tl1.addLock(bp2, new Lock("custom_resource1", LockType.READ, "v2"));
      put(tl1.getResource(), tl1);
    }};

    m.checking(new Expectations() {{
      allowing(bp1).getBuildType();
      will(returnValue(bp1bt));

      allowing(bp2).getBuildType();
      will(returnValue(bp2bt));

      oneOf(myResources).getResourcesMap(myProjectId);
      will(returnValue(resources));
    }});

    
    final Map<Resource, String> result = myTakenLocks.getUnavailableLocks(locksToTake, takenLocks, myProjectId, myAccessor, myPromotion);
    assertNotNull(result);
    assertEquals(1, result.size());
  }

  @Test
  public void testGetUnavailableLocks_ReadRead_Quota() {
    final Map<String, Resource> resources = new HashMap<>();
    final Resource quotedResource = ResourceFactory.newQuotedResource("quoted_resource1_id", myProjectId, "quoted_resource1", 2, true);
    resources.put(quotedResource.getName(), quotedResource);

    final Collection<Lock> locksToTake = new ArrayList<Lock>() {{
      add(new Lock("quoted_resource1", LockType.READ));
    }};

    final BuildPromotionEx bp1 = m.mock(BuildPromotionEx.class, "bp1");
    final BuildPromotionEx bp2 = m.mock(BuildPromotionEx.class, "bp2");

    final BuildTypeEx bp1bt = m.mock(BuildTypeEx.class, "bp1.bt");
    final BuildTypeEx bp2bt = m.mock(BuildTypeEx.class, "bp2.bt");


    final Map<Resource, TakenLock> takenLocks = new HashMap<Resource, TakenLock>() {{
      TakenLock tl1 = new TakenLock(quotedResource);
      tl1.addLock(bp1, new Lock("quoted_resource1", LockType.READ));
      tl1.addLock(bp2, new Lock("quoted_resource1", LockType.READ));
      put(tl1.getResource(), tl1);
    }};

    m.checking(new Expectations() {{
      allowing(bp1).getBuildType();
      will(returnValue(bp1bt));

      allowing(bp2).getBuildType();
      will(returnValue(bp2bt));

      oneOf(myResources).getResourcesMap(myProjectId);
      will(returnValue(resources));
    }});
    

    final Map<Resource, String> result = myTakenLocks.getUnavailableLocks(locksToTake, takenLocks, myProjectId, myAccessor, myPromotion);
    assertNotNull(result);
    assertEquals(1, result.size());

  }

  @Test
  public void testGetUnavailableLocks_ReadWrite() {
    final Map<String, Resource> resources = new HashMap<>();
    final Resource quotedResource = ResourceFactory.newQuotedResource("custom_resource1_id", myProjectId, "quoted_resource1", 2, true);
    resources.put(quotedResource.getName(), quotedResource);

    final Collection<Lock> locksToTake = new ArrayList<Lock>() {{
      add(new Lock("quoted_resource1", LockType.READ));
    }};

    final BuildPromotionEx bp1 = m.mock(BuildPromotionEx.class, "bp1");
    final BuildTypeEx bp1bt = m.mock(BuildTypeEx.class, "bp1bt");

    final Map<Resource, TakenLock> takenLocks = new HashMap<Resource, TakenLock>() {{
      TakenLock tl1 = new TakenLock(quotedResource);
      tl1.addLock(bp1, new Lock("quoted_resource1", LockType.WRITE));
      put(tl1.getResource(), tl1);
    }};

    m.checking(new Expectations() {{
      allowing(bp1).getBuildType();
      will(returnValue(bp1bt));

      oneOf(myResources).getResourcesMap(myProjectId);
      will(returnValue(resources));
    }});

    final Map<Resource, String> result = myTakenLocks.getUnavailableLocks(locksToTake, takenLocks, myProjectId, myAccessor, myPromotion);
    assertNotNull(result);
    assertEquals(1, result.size());
  }

  @Test
  public void testGetUnavailableLocks_WriteRead() {
    final Map<String, Resource> resources = new HashMap<>();
    final Resource quotedResource = ResourceFactory.newQuotedResource("custom_resource1_id", myProjectId, "quoted_resource1", 2, true);
    resources.put(quotedResource.getName(), quotedResource);

    final Collection<Lock> locksToTake = new ArrayList<Lock>() {{
      add(new Lock("quoted_resource1", LockType.WRITE));
    }};

    final BuildPromotionEx bp1 = m.mock(BuildPromotionEx.class, "bp1");
    final BuildTypeEx bp1bt = m.mock(BuildTypeEx.class, "bp1bt");

    final Map<Resource, TakenLock> takenLocks = new HashMap<Resource, TakenLock>() {{
      TakenLock tl1 = new TakenLock(quotedResource);
      tl1.addLock(bp1, new Lock("quoted_resource1", LockType.READ));
      put(tl1.getResource(), tl1);
    }};

    m.checking(new Expectations() {{
      allowing(bp1).getBuildType();
      will(returnValue(bp1bt));

      oneOf(myResources).getResourcesMap(myProjectId);
      will(returnValue(resources));
    }});

    

    final Map<Resource, String> result = myTakenLocks.getUnavailableLocks(locksToTake, takenLocks, myProjectId, myAccessor, myPromotion);
    assertNotNull(result);
    assertEquals(1, result.size());
  }

  /**
   *
   * Test setup:
   * - fairSet is empty
   * - 1 infinite resource
   * - 1 build holds read lock
   * - 1 build tries to pass through agents filter (with write lock request)
   *
   * Expected results:
   *  - wait reason is returned
   *  - fairSet should contain write lock name requested
   *
   */
  @Test
  @TestFor (issues = "TW-28307")
  public void testGetUnavailableLocks_WritePrioritySet() {
    final Map<String, Resource> resources = new HashMap<>();

    final Resource infiniteResource = ResourceFactory.newInfiniteResource("resource_id", myProjectId, "resource", true);
    resources.put(infiniteResource.getName(), infiniteResource);

    final Collection<Lock> locksToTake = new ArrayList<Lock>() {{
      add(new Lock(infiniteResource.getName(), LockType.WRITE));
    }};

    final BuildPromotionEx bp1 = m.mock(BuildPromotionEx.class, "bp1");
    final BuildTypeEx bp1bt = m.mock(BuildTypeEx.class, "bp1bt");

    final Map<Resource, TakenLock> takenLocks = new HashMap<Resource, TakenLock>() {{
      final TakenLock tl1 = new TakenLock(infiniteResource);
      tl1.addLock(bp1, new Lock(infiniteResource.getName(), LockType.READ));
      put(tl1.getResource(), tl1);
    }};

    m.checking(new Expectations() {{
      allowing(bp1).getBuildType();
      will(returnValue(bp1bt));

      oneOf(myResources).getResourcesMap(myProjectId);
      will(returnValue(resources));
    }});
    
    final Map<Resource, String> result = myTakenLocks.getUnavailableLocks(locksToTake, takenLocks, myProjectId, myAccessor, myPromotion);
    assertNotNull(result);
    assertEquals(1, result.size());
    assertNotNull(result.get(infiniteResource));    

    assertNotEmpty(myAccessor.getFairSet().keySet());
    assertEquals(1, myAccessor.getFairSet().size());
    assertEquals(infiniteResource.getId(), myAccessor.getFairSet().keySet().iterator().next());
  }

  /**
   * Test setup:
   * - fairSet is empty
   * - 1 infinite resource
   * - 1 build holds read lock
   * - 1 build tries to pass through agents filter (with write lock request)
   * - 1 another build tries to pass through agents filter (with read lock request)
   *
   * Expected results
   * - collection of unavailable locks for both builds is not empty (i.e. both builds are not allowed to start)
   *
   */
  @Test
  @TestFor (issues = "TW-28307")
  public void testGetUnavailableLocks_PreservePriority() {
    final Map<String, Resource> resources = new HashMap<>();

    final Resource infiniteResource = ResourceFactory.newInfiniteResource("resource_id", myProjectId, "resource", true);
    resources.put(infiniteResource.getName(), infiniteResource);

    final Collection<Lock> writeLockToTake = new ArrayList<Lock>() {{
      add(new Lock(infiniteResource.getName(), LockType.WRITE));
    }};

    final Collection<Lock> readLockToTake = new ArrayList<Lock>() {{
      add(new Lock(infiniteResource.getName(), LockType.READ));
    }};

    final BuildPromotionEx bp1 = m.mock(BuildPromotionEx.class, "bp1");
    final BuildTypeEx bp1bt = m.mock(BuildTypeEx.class, "bp1bt");
    
    final Map<Resource, TakenLock> takenLocks = new HashMap<Resource, TakenLock>() {{
      final TakenLock tl1 = new TakenLock(infiniteResource);      
      tl1.addLock(bp1, new Lock(infiniteResource.getName(), LockType.READ));
      put(tl1.getResource(), tl1);
    }};

    m.checking(new Expectations() {{
      allowing(bp1).getBuildType();
      will(returnValue(bp1bt));
      
      allowing(myResources).getResourcesMap(myProjectId);
      will(returnValue(resources));
    }});

    { // 1) Check that read-read locks are working
      final Map<Resource, String> result = myTakenLocks.getUnavailableLocks(readLockToTake, takenLocks, myProjectId, myAccessor, myPromotion);
      assertNotNull(result);
      assertEquals(0, result.size());
      assertEquals(0, myAccessor.getFairSet().size());
    }

    { // 2) Check that fair set influences read lock processing
      Map<Resource, String> result = myTakenLocks.getUnavailableLocks(writeLockToTake, takenLocks, myProjectId, myAccessor, myPromotion);
      assertNotNull(result);
      assertEquals(1, result.size());
      assertNotNull(result.get(infiniteResource));
      assertEquals(1, myAccessor.getFairSet().size());
      assertEquals(infiniteResource.getId(), myAccessor.getFairSet().keySet().iterator().next());

      // now we have lock name in fair set. read lock must not be acquired
      result = myTakenLocks.getUnavailableLocks(readLockToTake, takenLocks, myProjectId, myAccessor, myPromotion);
      assertNotNull(result);
      assertEquals(1, result.size());
      assertNotNull(result.get(infiniteResource));
      assertEquals(1, myAccessor.getFairSet().size());
      assertEquals(infiniteResource.getId(), myAccessor.getFairSet().keySet().iterator().next());
    }
  }

  /**
   * Same as TakenLocksImplTest#testGetUnavailableLocks_PreservePriority but for custom resources
   *
   */
  @Test
  @TestFor (issues = "TW-28307")
  public void testGetUnavailableLocks_Custom_Fair() {
    final Map<String, Resource> resources = new HashMap<>();
    final Resource customResource = ResourceFactory.newCustomResource("custom_resource_id", myProjectId, "custom_resource", Arrays.asList("val1", "val2", "val3"), true);
    resources.put(customResource.getName(), customResource);

    final Collection<Lock> allLockToTake = new ArrayList<Lock>() {{
      add(new Lock(customResource.getName(), LockType.WRITE));
    }};

    final Collection<Lock> anyLockToTake = new ArrayList<Lock>() {{
      add(new Lock(customResource.getName(), LockType.READ));
    }};

    final BuildPromotionEx bp1 = m.mock(BuildPromotionEx.class, "bp1");
    final BuildPromotionEx bp2 = m.mock(BuildPromotionEx.class, "bp2");

    final BuildTypeEx bp1bt = m.mock(BuildTypeEx.class, "bp1.bt");
    final BuildTypeEx bp2bt = m.mock(BuildTypeEx.class, "bp2.bt");


    final Map<Resource, TakenLock> takenLocksAny = new HashMap<Resource, TakenLock>() {{
      final TakenLock tl1 = new TakenLock(customResource);
      tl1.addLock(bp1, new Lock(customResource.getName(), LockType.READ));
      put(tl1.getResource(), tl1);
    }};

    final Map<Resource, TakenLock> takenLocksSpecific = new HashMap<Resource, TakenLock>() {{
      final TakenLock tl = new TakenLock(customResource);
      tl.addLock(bp2, new Lock(customResource.getName(), LockType.READ, "val1"));
      put(tl.getResource(), tl);
    }};

    m.checking(new Expectations() {{
      allowing(bp1).getBuildType();
      will(returnValue(bp1bt));

      allowing(bp2).getBuildType();
      will(returnValue(bp2bt));

      allowing(myResources).getResourcesMap(myProjectId);
      will(returnValue(resources));

    }});


    { // Check that any-any locks are working
      final Map<Resource, String> result = myTakenLocks.getUnavailableLocks(anyLockToTake, takenLocksAny, myProjectId, myAccessor, myPromotion);
      assertNotNull(result);
      assertEquals(0, result.size());
      assertEquals(0, myAccessor.getFairSet().size());
    }

    { // Check that any-specific locks are working
      final Map<Resource, String> result = myTakenLocks.getUnavailableLocks(anyLockToTake, takenLocksSpecific, myProjectId, myAccessor, myPromotion);
      assertNotNull(result);
      assertEquals(0, result.size());
      assertEquals(0, myAccessor.getFairSet().size());
    }

    { // Check that fair set influences read lock processing
      Map<Resource, String> result = myTakenLocks.getUnavailableLocks(allLockToTake, takenLocksAny, myProjectId, myAccessor, myPromotion);
      assertNotNull(result);
      assertEquals(1, result.size());
      assertNotNull(result.get(customResource));
      assertEquals(1, myAccessor.getFairSet().size());
      assertEquals(customResource.getId(), myAccessor.getFairSet().keySet().iterator().next());

      // now we have lock name in fair set. any lock must not be acquired
      result = myTakenLocks.getUnavailableLocks(anyLockToTake, takenLocksAny, myProjectId, myAccessor, myPromotion);
      assertNotNull(result);
      assertEquals(1, result.size());
      assertNotNull(result.get(customResource));
      assertEquals(1, myAccessor.getFairSet().size());
      assertEquals(customResource.getId(), myAccessor.getFairSet().keySet().iterator().next());
    }
  }

  @Test
  @TestFor (issues = "TW-27930")
  public void testGetUnavailableLocks_ResourceDisabled() {
    final Map<String, Resource> resources = new HashMap<>();
    final Resource quotedResource = ResourceFactory.newQuotedResource("quoted_resource1_id", myProjectId, "quoted_resource1", 1, false);
    resources.put(quotedResource.getName(), quotedResource);

    final Lock lockToTake = new Lock("quoted_resource1", LockType.READ);
    final Collection<Lock> locksToTake = new ArrayList<Lock>() {{
      add(lockToTake);
    }};

    m.checking(new Expectations() {{
      oneOf(myResources).getResourcesMap(myProjectId);
      will(returnValue(resources));
    }});

    final Map<Resource, String> result = myTakenLocks.getUnavailableLocks(locksToTake, Collections.emptyMap(), myProjectId, myAccessor, myPromotion);
    assertNotNull(result);
    assertEquals(1, result.size());
    assertNotNull(result.keySet().stream().filter(it -> Objects.equals(it.getName(), lockToTake.getName())).findAny().orElse(null));
  }

  @Test
  @TestFor (issues = "TW-34917")
  public void testGetUnavailableLocks_ZeroQuota_Read() {
    final Map<String, Resource> resources = new HashMap<>();
    final Resource quotedResource = ResourceFactory.newQuotedResource("quoted_resource1_id", myProjectId, "quoted_resource1", 0, true);
    resources.put(quotedResource.getName(), quotedResource);

    final Collection<Lock> locksToTake = new ArrayList<Lock>() {{
      add(new Lock(quotedResource.getName(), LockType.READ));
    }};

    m.checking(new Expectations() {{
      oneOf(myResources).getResourcesMap(myProjectId);
      will(returnValue(resources));
    }});

    final Map<Resource, TakenLock> takenLocks = new HashMap<>();

    final Map<Resource, String> result = myTakenLocks.getUnavailableLocks(locksToTake, takenLocks, myProjectId, myAccessor, myPromotion);
    assertNotNull(result);
    assertGreater(result.size(), 0);
    String name = locksToTake.iterator().next().getName();
    assertNotNull(result.keySet().stream().filter(it -> Objects.equals(it.getName(), name)).findAny().orElse(null));
  }

  @Test
  @TestFor (issues = "TW-34917")
  public void testGetUnavailableLocks_ZeroQuota_Write() {
    final Map<String, Resource> resources = new HashMap<>();
    final Resource quotedResource = ResourceFactory.newQuotedResource("quoted_resource1_id", myProjectId, "quoted_resource1", 0, true);
    resources.put(quotedResource.getName(), quotedResource);

    final Collection<Lock> locksToTake = new ArrayList<Lock>() {{
      add(new Lock(quotedResource.getName(), LockType.WRITE));
    }};

    m.checking(new Expectations() {{
      oneOf(myResources).getResourcesMap(myProjectId);
      will(returnValue(resources));
    }});

    
    final Map<Resource, TakenLock> takenLocks = new HashMap<>();

    final Map<Resource, String> result = myTakenLocks.getUnavailableLocks(locksToTake, takenLocks, myProjectId, myAccessor, myPromotion);
    assertNotNull(result);
    assertGreater(result.size(), 0);
    String name = locksToTake.iterator().next().getName();
    assertNotNull(result.keySet().stream().filter(it -> Objects.equals(it.getName(), name)).findAny().orElse(null));
  }

  /**
   * Test setup:
   * 1 infinite resource
   * 0 locks on the resource
   *
   * build should succeed in acquiring resource
   *
   */
  @Test
  @TestFor(issues = "TW-36042")
  public void testGetUnavailableLocks_MultipleBuilds_InfiniteLock() {
    final Map<String, Resource> resources = new HashMap<>();
    final Resource infiniteResource = ResourceFactory.newInfiniteResource("infinite_resource1_id", myProjectId, "infinite_resource", true);
    resources.put(infiniteResource.getName(), infiniteResource);

    final Collection<Lock> locksToTake = new ArrayList<Lock>() {{
      add(new Lock(infiniteResource.getName(), LockType.WRITE));
    }};

    m.checking(new Expectations() {{
      oneOf(myResources).getResourcesMap(myProjectId);
      will(returnValue(resources));
    }});

    

    final Map<Resource, TakenLock> takenLocks = new HashMap<>();

    final Map<Resource, String> result = myTakenLocks.getUnavailableLocks(locksToTake, takenLocks, myProjectId, myAccessor, myPromotion);
    assertNotNull(result);
    assertEquals(0, result.size());
  }

  /**
   * While build is running, resource can be deleted from the build type.
   * In this case the exception is thrown during queue processing that can cause builds to remain in queue
   * and build estimates are not computed either
   *
   * Setup: 2 running builds, 2 queued builds,
   * 1st running build and 1st queued build contain only the locks on existing resources
   * 2nd running build and 2nd queued build contain the lock on deleted resources along with existing one
   *
   * Expected: non-existing resources are ignored in taken locks computation
   *
   */
  @Test
  @TestFor(issues = "TW-48931")
  public void testShouldProcessLocksOnDeletedResource() {
    final Map<String, Resource> resources = new HashMap<>();
    final Resource existingResource = ResourceFactory.newInfiniteResource("existing_1_id", myProjectId, "existing", true);
    resources.put(existingResource.getName(), existingResource);

    final SharedResourcesFeature feature = m.mock(SharedResourcesFeature.class);
    final Collection<SharedResourcesFeature> features = Collections.singleton(feature);

    final SharedResourcesFeature featureWithAllResources = m.mock(SharedResourcesFeature.class, "feature-with-all-resources");
    final Collection<SharedResourcesFeature> featuresWithAllResources = Collections.singleton(featureWithAllResources);

    final SharedResourcesFeature featureWithDeletedResources = m.mock(SharedResourcesFeature.class, "feature-with-deleted-resources");
    final Collection<SharedResourcesFeature> featuresWithDeletedResources = Collections.singleton(featureWithDeletedResources);

    final Map<String, Lock> allExistingLocks = new HashMap<String, Lock>() {{
      put(existingResource.getName(), new Lock(existingResource.getName(), LockType.READ, ""));
    }};

    final Map<String, Lock> withDeletedLocks = new HashMap<String, Lock>() {{
      put(existingResource.getName(), new Lock(existingResource.getName(), LockType.READ, ""));
      put("deleted", new Lock("deleted", LockType.READ, ""));
    }};

    final Trinity<RunningBuildEx, BuildTypeEx, BuildPromotionEx> rb1 = createMockRunningBuild(myProjectId);
    final Trinity<RunningBuildEx, BuildTypeEx, BuildPromotionEx> rb2 = createMockRunningBuild(myProjectId);

    final Collection<RunningBuildEx> runningBuilds = new ArrayList<RunningBuildEx>() {{
      add(rb1.getFirst());
      add(rb2.getFirst());
    }};

    final Trinity<QueuedBuildInfo, BuildTypeEx, BuildPromotionEx> qb1 = createMockQueuedBuild(myProjectId);
    final Trinity<QueuedBuildInfo, BuildTypeEx, BuildPromotionEx> qb2 = createMockQueuedBuild(myProjectId);

    final Collection<QueuedBuildInfo> queuedBuilds = new ArrayList<QueuedBuildInfo>() {{
      add(qb1.getFirst());
      add(qb2.getFirst());
    }};

    m.checking(new Expectations() {{
      allowing(myFeatures).searchForFeatures(rb1.getSecond());
      will(returnValue(features));

      allowing(myFeatures).searchForFeatures(rb2.getSecond());
      will(returnValue(features));

      allowing(myLocksStorage).locksStored(with(any(BuildPromotion.class)));
      will(returnValue(true));

      oneOf(myLocksStorage).load(rb1.getThird());
      will(returnValue(allExistingLocks));

      oneOf(myLocksStorage).load(rb2.getThird());
      will(returnValue(withDeletedLocks));

      allowing(myResources).getResourcesMap(myProjectId);
      will(returnValue(resources));

      allowing(myFeatures).searchForFeatures(qb1.getSecond());
      will(returnValue(featuresWithAllResources));

      oneOf(myLocks).fromBuildFeaturesAsMap(featuresWithAllResources);
      will(returnValue(allExistingLocks));

      allowing(myFeatures).searchForFeatures(qb2.getSecond());
      will(returnValue(featuresWithDeletedResources));

      oneOf(myLocks).fromBuildFeaturesAsMap(featuresWithDeletedResources);
      will(returnValue(withDeletedLocks));
    }});


    final Map<Resource, TakenLock> takenLocksMap = myTakenLocks.collectTakenLocks(runningBuilds, queuedBuilds);
    assertFalse(takenLocksMap.isEmpty());
    assertEquals(1, takenLocksMap.size());
    TakenLock takenLock = takenLocksMap.get(existingResource);
    assertNotNull(takenLock);
    assertEquals(4, takenLock.getLocksCount());
    final Map<BuildPromotionEx, String> readLocks = takenLock.getReadLocks();
    assertEquals(4, readLocks.size());
    assertEquals(0, takenLock.getWriteLocks().size());
    final Set<BuildPromotionEx> readLockPromotions = readLocks.keySet();
    assertContains(readLockPromotions, rb1.getThird());
    assertContains(readLockPromotions, rb2.getThird());
    assertContains(readLockPromotions, qb1.getThird());
    assertContains(readLockPromotions, qb2.getThird());
  }

  @SuppressWarnings("SameParameterValue")
  private Trinity<RunningBuildEx, BuildTypeEx, BuildPromotionEx> createMockRunningBuild(@NotNull final String projectId) {
    final String name = generateRandomName();
    final RunningBuildEx build = m.mock(RunningBuildEx.class, "runningBuild_" + name);
    final BuildTypeEx buildType = m.mock(BuildTypeEx.class, "runningBuild_ " + name + "-buildType");
    final BuildPromotionEx buildPromotion = m.mock(BuildPromotionEx.class, "runningBuild_" + name + "-buildPromotion");
    m.checking(new Expectations() {{
      allowing(build).getBuildType();
      will(returnValue(buildType));

      allowing(build).getBuildPromotionInfo();
      will(returnValue(buildPromotion));

      allowing(buildType).getProjectId();
      will(returnValue(projectId));
    }});
    return new Trinity<>(build, buildType, buildPromotion);
  }

  @SuppressWarnings("SameParameterValue")
  private Trinity<QueuedBuildInfo, BuildTypeEx, BuildPromotionEx> createMockQueuedBuild(@NotNull final String projectId) {
    final String name = generateRandomName();
    final QueuedBuildInfo build = m.mock(QueuedBuildInfo.class, "queuedBuildInfo" + name);
    final BuildTypeEx buildType = m.mock(BuildTypeEx.class, "runningBuild_ " + name + "-buildType");
    final BuildPromotionEx buildPromotion = m.mock(BuildPromotionEx.class, "runningBuild_" + name + "-buildPromotion");
    m.checking(new Expectations() {{
      allowing(build).getBuildPromotionInfo();
      will(returnValue(buildPromotion));

      allowing(buildPromotion).getBuildType();
      will(returnValue(buildType));

      allowing(buildType).getProjectId();
      will(returnValue(projectId));
    }});
    return new Trinity<>(build, buildType, buildPromotion);
  }
}
