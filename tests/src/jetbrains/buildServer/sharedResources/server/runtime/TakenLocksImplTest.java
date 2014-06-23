package jetbrains.buildServer.sharedResources.server.runtime;

import jetbrains.buildServer.BaseTestCase;
import jetbrains.buildServer.serverSide.*;
import jetbrains.buildServer.serverSide.buildDistribution.BuildPromotionInfo;
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
@TestFor(testForClass = {TakenLocks.class, TakenLocksImpl.class})
public class TakenLocksImplTest extends BaseTestCase {

  private Mockery m;

  private Locks myLocks;

  private Resources myResources;

  private LocksStorage myLocksStorage;

  /**
   * Class under test
   */
  private TakenLocks myTakenLocks;

  private SharedResourcesFeatures myFeatures;

  private final String myProjectId = "MY_PROJECT_ID";

  private ResourceFactory myResourceFactory;

  @BeforeMethod
  @Override
  protected void setUp() throws Exception {
    super.setUp();
    m = new Mockery();
    myLocks = m.mock(Locks.class);
    myResources = m.mock(Resources.class);
    myLocksStorage = m.mock(LocksStorage.class);
    myFeatures = m.mock(SharedResourcesFeatures.class);
    myResourceFactory = ResourceFactory.getFactory(myProjectId);
    myTakenLocks = new TakenLocksImpl(myLocks, myResources, myLocksStorage, myFeatures);
  }

  @Test
  public void testCollectTakenLocks_EmptyInput() throws Exception {
    final Map<Resource, TakenLock> result = myTakenLocks.collectTakenLocks(
            myProjectId, Collections.<SRunningBuild>emptyList(), Collections.<QueuedBuildInfo>emptyList());
    assertNotNull(result);
    assertEquals(0, result.size());
  }

  @Test
  public void testCollectRunningBuilds_Stored() throws Exception {
    final SharedResourcesFeature feature = m.mock(SharedResourcesFeature.class);
    final Collection<SharedResourcesFeature> features = Collections.singleton(feature);

    final Resource resource1 = myResourceFactory.newInfiniteResource("resource1", true);
    final Resource resource2 = myResourceFactory.newInfiniteResource("resource2", true);

    final Map<String, Resource> resources = new HashMap<String, Resource>() {{
      put(resource1.getName(), resource1);
      put(resource2.getName(), resource2);
    }};

    final Map<String, Lock> takenLocks1 = new HashMap<String, Lock>() {{
      put("resource1", new Lock("resource1", LockType.READ, ""));
      put("resource2", new Lock("resource2", LockType.WRITE, ""));

    }};

    final Map<String, Lock> takenLocks2 = new HashMap<String, Lock>() {{
      put("resource1", new Lock("resource1", LockType.READ, ""));
    }};

    final RunningBuildEx rb1 = m.mock(RunningBuildEx.class, "runningBuild_1");
    final RunningBuildEx rb2 = m.mock(RunningBuildEx.class, "runningBuild_2");

    final SBuildType rb1_bt = m.mock(SBuildType.class, "runningBuild_1-buildType");
    final SBuildType rb2_bt = m.mock(SBuildType.class, "runningBuild_2-buildType");

    final BuildPromotionEx bp1 = m.mock(BuildPromotionEx.class, "buildPromotion_1");
    final BuildPromotionEx bp2 = m.mock(BuildPromotionEx.class, "buildPromotion_2");

    final Collection<SRunningBuild> runningBuilds = new ArrayList<SRunningBuild>() {{
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

      oneOf(myLocksStorage).locksStored(rb1);
      will(returnValue(true));

      oneOf(myLocksStorage).load(rb1);
      will(returnValue(takenLocks1));

      oneOf(rb1_bt).getProjectId();
      will(returnValue(myProjectId));

      oneOf(myResources).asMap(myProjectId);
      will(returnValue(resources));

      oneOf(rb2).getBuildType();
      will(returnValue(rb2_bt));

      oneOf(myFeatures).searchForFeatures(rb2_bt);
      will(returnValue(features));

      oneOf(rb2).getBuildPromotionInfo();
      will(returnValue(bp2));

      oneOf(myLocksStorage).locksStored(rb2);
      will(returnValue(true));

      oneOf(myLocksStorage).load(rb2);
      will(returnValue(takenLocks2));

      oneOf(rb2_bt).getProjectId();
      will(returnValue(myProjectId));

    }});

    final Map<Resource, TakenLock> result = myTakenLocks.collectTakenLocks(
            myProjectId, runningBuilds, Collections.<QueuedBuildInfo>emptyList());
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
  public void testShouldNotAskParametersNoFeatures() throws Exception {
    final RunningBuildEx rb = m.mock(RunningBuildEx.class, "rb");
    final SBuildType rb_bt = m.mock(SBuildType.class, "rb_bt");
    final Collection<SRunningBuild> runningBuilds = new ArrayList<SRunningBuild>() {{
      add(rb);
    }};

    m.checking(new Expectations() {{
      oneOf(rb).getBuildType();
      will(returnValue(rb_bt));

      oneOf(myFeatures).searchForFeatures(rb_bt);
      will(returnValue(Collections.emptyList()));

    }});

    final Map<Resource, TakenLock> result = myTakenLocks.collectTakenLocks(
            myProjectId, runningBuilds, Collections.<QueuedBuildInfo>emptyList());
    assertNotNull(result);
    assertEquals(0, result.size());
  }

  @Test
  public void testCollectRunningQueued_Promotions() throws Exception {
    final SharedResourcesFeature rFeature = m.mock(SharedResourcesFeature.class, "r-feature");
    final Collection<SharedResourcesFeature> rFeatures = Collections.singleton(rFeature);

    final SharedResourcesFeature qFeature = m.mock(SharedResourcesFeature.class, "q-feature");
    final Collection<SharedResourcesFeature> qFeatures = Collections.singleton(qFeature);

    final Resource resource1 = myResourceFactory.newInfiniteResource("resource1", true);
    final Resource resource2 = myResourceFactory.newInfiniteResource("resource2", true);

    final Map<String, Resource> resources = new HashMap<String, Resource>() {{
      put(resource1.getName(), resource1);
      put(resource2.getName(), resource2);
    }};

    final Map<String, Lock> takenLocks1 = new HashMap<String, Lock>() {{
      put("resource1" , new Lock("resource1", LockType.READ));
      put("resource2", new Lock("resource2", LockType.WRITE));
    }};

    final Map<String, Lock> takenLocks2 = new HashMap<String, Lock>() {{
      put("resource1", new Lock("resource1", LockType.READ));
    }};

    final RunningBuildEx rb1 = m.mock(RunningBuildEx.class, "rb-1");
    final BuildTypeEx rb1_bt = m.mock(BuildTypeEx.class, "rb1_bt");
    final BuildPromotionEx bp1 = m.mock(BuildPromotionEx.class, "bp-1");

    final QueuedBuildInfo qb1 = m.mock(QueuedBuildInfo.class, "qb-1");
    final BuildTypeEx qb1_bt = m.mock(BuildTypeEx.class, "qb1_bt");
    final BuildPromotionEx bp2 = m.mock(BuildPromotionEx.class, "bp-2");
    final Collection<SRunningBuild> runningBuilds = new ArrayList<SRunningBuild>() {{
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

      oneOf(myLocksStorage).locksStored(rb1);
      will(returnValue(false));

      oneOf(myFeatures).searchForFeatures(rb1_bt);
      will(returnValue(rFeatures));

      oneOf(myLocks).fromBuildFeaturesAsMap(rFeatures);
      will(returnValue(takenLocks1));

      oneOf(rb1_bt).getProjectId();
      will(returnValue(myProjectId));

      oneOf(myResources).asMap(myProjectId);
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
            myProjectId, runningBuilds, queuedBuilds);
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
  public void testGetUnavailableLocks_Custom_All() throws Exception {
    final Map<String, Resource> resources = new HashMap<String, Resource>();
    final Resource myCustomResource = myResourceFactory.newCustomResource("custom_resource1", Arrays.asList("v1", "v2"), true);
    resources.put("custom_resource1", myCustomResource);

    final Collection<Lock> locksToTake = new ArrayList<Lock>() {{
      add(new Lock("custom_resource1", LockType.WRITE));
    }};

    final Map<Resource, TakenLock> takenLocks = new HashMap<Resource, TakenLock>() {{
      TakenLock tl1 = new TakenLock(myCustomResource);
      tl1.addLock(m.mock(BuildPromotionInfo.class), new Lock("custom_resource1", LockType.READ));
      put(tl1.getResource(), tl1);
    }};

    m.checking(new Expectations() {{
      oneOf(myResources).asMap(myProjectId);
      will(returnValue(resources));
    }});

    final Set<String> fairSet = new HashSet<String>();

    final Map<Resource, Lock> result = myTakenLocks.getUnavailableLocks(locksToTake, takenLocks, myProjectId, fairSet);
    assertNotNull(result);
    assertEquals(1, result.size());
  }

  @Test
  public void testGetUnavailableLocks_Custom_Specific() throws Exception {
    final Map<String, Resource> resources = new HashMap<String, Resource>();
    final Resource myCustomResource = myResourceFactory.newCustomResource("custom_resource1", Arrays.asList("v1", "v2"), true);
    resources.put("custom_resource1", myCustomResource);

    final Collection<Lock> locksToTake = new ArrayList<Lock>() {{
      add(new Lock("custom_resource1", LockType.WRITE, "v1"));
    }};

    final Map<Resource, TakenLock> takenLocks = new HashMap<Resource, TakenLock>() {{
      TakenLock tl1 = new TakenLock(myCustomResource);
      tl1.addLock(m.mock(BuildPromotionInfo.class), new Lock("custom_resource1", LockType.READ, "v1"));
      put(tl1.getResource(), tl1);
    }};

    m.checking(new Expectations() {{
      oneOf(myResources).asMap(myProjectId);
      will(returnValue(resources));
    }});

    final Set<String> fairSet = new HashSet<String>();

    final Map<Resource, Lock> result = myTakenLocks.getUnavailableLocks(locksToTake, takenLocks, myProjectId, fairSet);
    assertNotNull(result);
    assertEquals(1, result.size());

  }

  @Test
  public void testGetUnavailableLocks_Custom_Any() throws Exception {
    // case when write lock ALL is taken
    final Map<String, Resource> resources = new HashMap<String, Resource>();
    final Resource myCustomResource = myResourceFactory.newCustomResource("custom_resource1", Arrays.asList("v1", "v2"), true);
    resources.put("custom_resource1", myCustomResource);

    final Collection<Lock> locksToTake = new ArrayList<Lock>() {{
      add(new Lock("custom_resource1", LockType.READ));
    }};

    final Map<Resource, TakenLock> takenLocks = new HashMap<Resource, TakenLock>() {{
      TakenLock tl1 = new TakenLock(myCustomResource);
      tl1.addLock(m.mock(BuildPromotionInfo.class), new Lock("custom_resource1", LockType.WRITE));
      put(tl1.getResource(), tl1);
    }};

    m.checking(new Expectations() {{
      oneOf(myResources).asMap(myProjectId);
      will(returnValue(resources));
    }});

    final Set<String> fairSet = new HashSet<String>();

    final Map<Resource, Lock> result = myTakenLocks.getUnavailableLocks(locksToTake, takenLocks, myProjectId, fairSet);
    assertNotNull(result);
    assertEquals(1, result.size());
  }

  @Test
  public void testGetUnavailableLocks_Custom_Any_NoValuesAvailable() throws Exception {
    final Map<String, Resource> resources = new HashMap<String, Resource>();
    final Resource myCustomResource = myResourceFactory.newCustomResource("custom_resource1", Arrays.asList("v1", "v2"), true);
    resources.put("custom_resource1", myCustomResource);

    final Collection<Lock> locksToTake = new ArrayList<Lock>() {{
      add(new Lock("custom_resource1", LockType.READ));
    }};

    final Map<Resource, TakenLock> takenLocks = new HashMap<Resource, TakenLock>() {{
      TakenLock tl1 = new TakenLock(myCustomResource);
      tl1.addLock(m.mock(BuildPromotionInfo.class, "bp1"), new Lock("custom_resource1", LockType.READ, "v1"));
      tl1.addLock(m.mock(BuildPromotionInfo.class, "bp2"), new Lock("custom_resource1", LockType.READ, "v2"));
      put(tl1.getResource(), tl1);
    }};

    m.checking(new Expectations() {{
      oneOf(myResources).asMap(myProjectId);
      will(returnValue(resources));
    }});

    final Set<String> fairSet = new HashSet<String>();
    final Map<Resource, Lock> result = myTakenLocks.getUnavailableLocks(locksToTake, takenLocks, myProjectId, fairSet);
    assertNotNull(result);
    assertEquals(1, result.size());
  }

  @Test
  public void testGetUnavailableLocks_ReadRead_Quota() throws Exception {
    final Map<String, Resource> resources = new HashMap<String, Resource>();
    final Resource quotedResource = myResourceFactory.newQuotedResource("quoted_resource1", 2, true);
    resources.put("quoted_resource1", quotedResource);

    final Collection<Lock> locksToTake = new ArrayList<Lock>() {{
      add(new Lock("quoted_resource1", LockType.READ));
    }};

    final Map<Resource, TakenLock> takenLocks = new HashMap<Resource, TakenLock>() {{
      TakenLock tl1 = new TakenLock(quotedResource);
      tl1.addLock(m.mock(BuildPromotionInfo.class, "bp1"), new Lock("quoted_resource1", LockType.READ));
      tl1.addLock(m.mock(BuildPromotionInfo.class, "bp2"), new Lock("quoted_resource1", LockType.READ));
      put(tl1.getResource(), tl1);
    }};

    m.checking(new Expectations() {{
      oneOf(myResources).asMap(myProjectId);
      will(returnValue(resources));
    }});
    final Set<String> fairSet = new HashSet<String>();

    final Map<Resource, Lock> result = myTakenLocks.getUnavailableLocks(locksToTake, takenLocks, myProjectId, fairSet);
    assertNotNull(result);
    assertEquals(1, result.size());

  }

  @Test
  public void testGetUnavailableLocks_ReadWrite() throws Exception {
    final Map<String, Resource> resources = new HashMap<String, Resource>();
    final Resource quotedResource = myResourceFactory.newQuotedResource("quoted_resource1", 2, true);
    resources.put("quoted_resource1", quotedResource);

    final Collection<Lock> locksToTake = new ArrayList<Lock>() {{
      add(new Lock("quoted_resource1", LockType.READ));
    }};

    final Map<Resource, TakenLock> takenLocks = new HashMap<Resource, TakenLock>() {{
      TakenLock tl1 = new TakenLock(quotedResource);
      tl1.addLock(m.mock(BuildPromotionInfo.class, "bp1"), new Lock("quoted_resource1", LockType.WRITE));
      put(tl1.getResource(), tl1);
    }};

    m.checking(new Expectations() {{
      oneOf(myResources).asMap(myProjectId);
      will(returnValue(resources));
    }});

    final Set<String> fairSet = new HashSet<String>();

    final Map<Resource, Lock> result = myTakenLocks.getUnavailableLocks(locksToTake, takenLocks, myProjectId, fairSet);
    assertNotNull(result);
    assertEquals(1, result.size());
  }

  @Test
  public void testGetUnavailableLocks_WriteRead() throws Exception {
    final Map<String, Resource> resources = new HashMap<String, Resource>();
    final Resource quotedResource = myResourceFactory.newQuotedResource("quoted_resource1", 2, true);
    resources.put("quoted_resource1", quotedResource);

    final Collection<Lock> locksToTake = new ArrayList<Lock>() {{
      add(new Lock("quoted_resource1", LockType.WRITE));
    }};

    final Map<Resource, TakenLock> takenLocks = new HashMap<Resource, TakenLock>() {{
      TakenLock tl1 = new TakenLock(quotedResource);
      tl1.addLock(m.mock(BuildPromotionInfo.class, "bp1"), new Lock("quoted_resource1", LockType.READ));
      put(tl1.getResource(), tl1);
    }};

    m.checking(new Expectations() {{
      oneOf(myResources).asMap(myProjectId);
      will(returnValue(resources));
    }});

    final Set<String> fairSet = new HashSet<String>();

    final Map<Resource, Lock> result = myTakenLocks.getUnavailableLocks(locksToTake, takenLocks, myProjectId, fairSet);
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
   * @throws Exception if something goes wrong
   */
  @Test
  @TestFor (issues = "TW-28307")
  public void testGetUnavailableLocks_WritePrioritySet() throws Exception {
    final String resourceName = "resource";
    final Map<String, Resource> resources = new HashMap<String, Resource>();

    final Resource infiniteResource = myResourceFactory.newInfiniteResource(resourceName, true);
    resources.put(resourceName, infiniteResource);

    final Collection<Lock> locksToTake = new ArrayList<Lock>() {{
      add(new Lock(resourceName, LockType.WRITE));
    }};

    final Map<Resource, TakenLock> takenLocks = new HashMap<Resource, TakenLock>() {{
      final TakenLock tl1 = new TakenLock(infiniteResource);
      tl1.addLock(m.mock(BuildPromotionInfo.class, "bp1"), new Lock(resourceName, LockType.READ));
      put(tl1.getResource(), tl1);
    }};

    m.checking(new Expectations() {{
      oneOf(myResources).asMap(myProjectId);
      will(returnValue(resources));
    }});
    final Set<String> fairSet = new HashSet<String>();
    final Map<Resource, Lock> result = myTakenLocks.getUnavailableLocks(locksToTake, takenLocks, myProjectId, fairSet);
    assertNotNull(result);
    assertEquals(1, result.size());
    assertEquals(resourceName, result.get(infiniteResource).getName());

    assertNotEmpty(fairSet);
    assertEquals(1, fairSet.size());
    assertEquals(resourceName, fairSet.iterator().next());
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
   * @throws Exception if something goes wrong
   */
  @Test
  @TestFor (issues = "TW-28307")
  public void testGetUnavailableLocks_PreservePriority() throws Exception {
    final String resourceName = "resource";
    final Map<String, Resource> resources = new HashMap<String, Resource>();

    final Resource infiniteResource = myResourceFactory.newInfiniteResource(resourceName, true);
    resources.put(resourceName, infiniteResource);

    final Collection<Lock> writeLockToTake = new ArrayList<Lock>() {{
      add(new Lock(resourceName, LockType.WRITE));
    }};

    final Collection<Lock> readLockToTake = new ArrayList<Lock>() {{
      add(new Lock(resourceName, LockType.READ));
    }};

    final Map<Resource, TakenLock> takenLocks = new HashMap<Resource, TakenLock>() {{
      final TakenLock tl1 = new TakenLock(infiniteResource);
      tl1.addLock(m.mock(BuildPromotionInfo.class, "bp1"), new Lock(resourceName, LockType.READ));
      put(tl1.getResource(), tl1);
    }};

    m.checking(new Expectations() {{
      allowing(myResources).asMap(myProjectId);
      will(returnValue(resources));
    }});


    final Set<String> fairSet = new HashSet<String>();

    { // 1) Check that read-read locks are working
      final Map<Resource, Lock> result = myTakenLocks.getUnavailableLocks(readLockToTake, takenLocks, myProjectId, fairSet);
      assertNotNull(result);
      assertEquals(0, result.size());
      assertEmpty(fairSet);
    }

    { // 2) Check that fair set influences read lock processing
      Map<Resource, Lock> result = myTakenLocks.getUnavailableLocks(writeLockToTake, takenLocks, myProjectId, fairSet);
      assertNotNull(result);
      assertEquals(1, result.size());
      assertEquals(resourceName, result.get(infiniteResource).getName());
      assertEquals(1, fairSet.size());
      assertEquals(resourceName, fairSet.iterator().next());

      // now we have lock name in fair set. read lock must not be acquired
      result = myTakenLocks.getUnavailableLocks(readLockToTake, takenLocks, myProjectId, fairSet);
      assertNotNull(result);
      assertEquals(1, result.size());
      assertEquals(resourceName, result.get(infiniteResource).getName());
      assertEquals(1, fairSet.size());
      assertEquals(resourceName, fairSet.iterator().next());
    }
  }


  /**
   * Same as TakenLocksImplTest#testGetUnavailableLocks_PreservePriority but for custom resources
   *
   * @throws Exception if something goes wrong
   */
  @Test
  @TestFor (issues = "TW-28307")
  public void testGetUnavailableLocks_Custom_Fair() throws Exception {
    final String resourceName = "resource";
    final Map<String, Resource> resources = new HashMap<String, Resource>();
    final Resource customResource = myResourceFactory.newCustomResource(resourceName, Arrays.asList("val1", "val2", "val3"), true);
    resources.put(resourceName, customResource);

    final Collection<Lock> allLockToTake = new ArrayList<Lock>() {{
      add(new Lock(resourceName, LockType.WRITE));
    }};

    final Collection<Lock> anyLockToTake = new ArrayList<Lock>() {{
      add(new Lock(resourceName, LockType.READ));
    }};

    final Map<Resource, TakenLock> takenLocksAny = new HashMap<Resource, TakenLock>() {{
      final TakenLock tl1 = new TakenLock(customResource);
      tl1.addLock(m.mock(BuildPromotionInfo.class, "bp1"), new Lock(resourceName, LockType.READ));
      put(tl1.getResource(), tl1);
    }};

    final Map<Resource, TakenLock> takenLocksSpecific = new HashMap<Resource, TakenLock>() {{
      final TakenLock tl = new TakenLock(customResource);
      tl.addLock(m.mock(BuildPromotionInfo.class, "bp2"), new Lock(resourceName, LockType.READ, "val1"));
      put(tl.getResource(), tl);
    }};

    m.checking(new Expectations() {{
      allowing(myResources).asMap(myProjectId);
      will(returnValue(resources));
    }});

    final Set<String> fairSet = new HashSet<String>();

    { // Check that any-any locks are working
      final Map<Resource, Lock> result = myTakenLocks.getUnavailableLocks(anyLockToTake, takenLocksAny, myProjectId, fairSet);
      assertNotNull(result);
      assertEquals(0, result.size());
      assertEmpty(fairSet);
    }

    { // Check that any-specific locks are working
      final Map<Resource, Lock> result = myTakenLocks.getUnavailableLocks(anyLockToTake, takenLocksSpecific, myProjectId, fairSet);
      assertNotNull(result);
      assertEquals(0, result.size());
      assertEmpty(fairSet);
    }

    { // Check that fair set influences read lock processing
      Map<Resource, Lock> result = myTakenLocks.getUnavailableLocks(allLockToTake, takenLocksAny, myProjectId, fairSet);
      assertNotNull(result);
      assertEquals(1, result.size());
      assertEquals(resourceName, result.get(customResource).getName());
      assertEquals(1, fairSet.size());
      assertEquals(resourceName, fairSet.iterator().next());

      // now we have lock name in fair set. any lock must not be acquired
      result = myTakenLocks.getUnavailableLocks(anyLockToTake, takenLocksAny, myProjectId, fairSet);
      assertNotNull(result);
      assertEquals(1, result.size());
      assertEquals(resourceName, result.get(customResource).getName());
      assertEquals(1, fairSet.size());
      assertEquals(resourceName, fairSet.iterator().next());
    }
  }

  @Test
  @TestFor (issues = "TW-27930")
  public void testGetUnavailableLocks_ResourceDisabled() throws Exception {
    final Map<String, Resource> resources = new HashMap<String, Resource>();
    final Resource quotedResource = myResourceFactory.newQuotedResource("quoted_resource1", 1, false);
    resources.put("quoted_resource1", quotedResource);

    final Lock lockToTake = new Lock("quoted_resource1", LockType.READ);
    final Collection<Lock> locksToTake = new ArrayList<Lock>() {{
      add(lockToTake);
    }};

    m.checking(new Expectations() {{
      oneOf(myResources).asMap(myProjectId);
      will(returnValue(resources));
    }});

    final Set<String> fairSet = new HashSet<String>();

    final Map<Resource, Lock> result = myTakenLocks.getUnavailableLocks(locksToTake, Collections.<Resource, TakenLock>emptyMap(), myProjectId, fairSet);
    assertNotNull(result);
    assertEquals(1, result.size());
    assertEquals(lockToTake, result.get(quotedResource));
  }

  @Test
  @TestFor (issues = "TW-34917")
  public void testGetUnavailableLocks_ZeroQuota_Read() throws Exception {
    final Map<String, Resource> resources = new HashMap<String, Resource>();
    resources.put("quoted_resource1", myResourceFactory.newQuotedResource("quoted_resource1", 0, true));

    final Collection<Lock> locksToTake = new ArrayList<Lock>() {{
      add(new Lock("quoted_resource1", LockType.READ));
    }};

    m.checking(new Expectations() {{
      oneOf(myResources).asMap(myProjectId);
      will(returnValue(resources));
    }});

    final Set<String> fairSet = new HashSet<String>();

    final Map<Resource, TakenLock> takenLocks = new HashMap<Resource, TakenLock>();

    final Map<Resource, Lock> result = myTakenLocks.getUnavailableLocks(locksToTake, takenLocks, myProjectId, fairSet);
    assertNotNull(result);
    assertGreater(result.size(), 0);
    assertContains(result.values(), locksToTake.iterator().next());
  }

  @Test
  @TestFor (issues = "TW-34917")
  public void testGetUnavailableLocks_ZeroQuota_Write() throws Exception {
    final Map<String, Resource> resources = new HashMap<String, Resource>();
    resources.put("quoted_resource1", myResourceFactory.newQuotedResource("quoted_resource1", 0, true));

    final Collection<Lock> locksToTake = new ArrayList<Lock>() {{
      add(new Lock("quoted_resource1", LockType.WRITE));
    }};

    m.checking(new Expectations() {{
      oneOf(myResources).asMap(myProjectId);
      will(returnValue(resources));
    }});

    final Set<String> fairSet = new HashSet<String>();

    final Map<Resource, TakenLock> takenLocks = new HashMap<Resource, TakenLock>();

    final Map<Resource, Lock> result = myTakenLocks.getUnavailableLocks(locksToTake, takenLocks, myProjectId, fairSet);
    assertNotNull(result);
    assertGreater(result.size(), 0);
    assertContains(result.values(), locksToTake.iterator().next());
  }

  /**
   * Test setup:
   * 1 infinite resource
   * 0 locks on the resource
   *
   * build should succeed in acquiring resource
   *
   * @throws Exception if something goes wrong
   */
  @Test
  @TestFor(issues = "TW-36042")
  public void testGetUnavailableLocks_MultipleBuilds_InfiniteLock() throws Exception {
    final Map<String, Resource> resources = new HashMap<String, Resource>();
    resources.put("infinite_resource", myResourceFactory.newInfiniteResource("infinite_resource", true));

    final Collection<Lock> locksToTake = new ArrayList<Lock>() {{
      add(new Lock("infinite_resource", LockType.WRITE));
    }};

    m.checking(new Expectations() {{
      oneOf(myResources).asMap(myProjectId);
      will(returnValue(resources));
    }});

    final Set<String> fairSet = new HashSet<String>();

    final Map<Resource, TakenLock> takenLocks = new HashMap<Resource, TakenLock>();

    final Map<Resource, Lock> result = myTakenLocks.getUnavailableLocks(locksToTake, takenLocks, myProjectId, fairSet);
    assertNotNull(result);
    assertEquals(0, result.size());
  }
}
