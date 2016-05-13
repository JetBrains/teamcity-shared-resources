package jetbrains.buildServer.sharedResources.server;

import jetbrains.buildServer.BaseTestCase;
import jetbrains.buildServer.BuildAgent;
import jetbrains.buildServer.serverSide.*;
import jetbrains.buildServer.serverSide.buildDistribution.*;
import jetbrains.buildServer.sharedResources.model.Lock;
import jetbrains.buildServer.sharedResources.model.LockType;
import jetbrains.buildServer.sharedResources.model.TakenLock;
import jetbrains.buildServer.sharedResources.model.resources.Resource;
import jetbrains.buildServer.sharedResources.model.resources.ResourceFactory;
import jetbrains.buildServer.sharedResources.server.feature.Locks;
import jetbrains.buildServer.sharedResources.server.feature.SharedResourcesFeature;
import jetbrains.buildServer.sharedResources.server.feature.SharedResourcesFeatures;
import jetbrains.buildServer.sharedResources.server.runtime.TakenLocks;
import jetbrains.buildServer.util.TestFor;
import org.jetbrains.annotations.NotNull;
import org.jmock.Expectations;
import org.jmock.Mockery;
import org.jmock.lib.legacy.ClassImposteriser;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.util.*;

/**
 * Created with IntelliJ IDEA.
 *
 * @author Oleg Rybak (oleg.rybak@jetbrains.com)
 */
@TestFor(testForClass = SharedResourcesAgentsFilter.class)
public class SharedResourcesAgentsFilterTest extends BaseTestCase {

  private Mockery m;

  private Locks myLocks;

  private SharedResourcesFeatures myFeatures;

  private QueuedBuildInfo myQueuedBuild;

  private BuildPromotionEx myBuildPromotion;

  private BuildDistributorInput myBuildDistributorInput;

  private BuildTypeEx myBuildType;

  private final String myProjectId = "PROJECT_ID";

  private TakenLocks myTakenLocks;

  private RunningBuildsManager myRunningBuildsManager;

  private Map<String, Object> myCustomData;

  private ConfigurationInspector myInspector;

  private Set<String> fairSet = new HashSet<>();

  /**
   * Class under test
   */
  private SharedResourcesAgentsFilter myAgentsFilter;


  @BeforeMethod
  @Override
  protected void setUp() throws Exception {
    super.setUp();
    m = new Mockery() {{
      setImposteriser(ClassImposteriser.INSTANCE);
    }};
    myLocks = m.mock(Locks.class);
    myFeatures = m.mock(SharedResourcesFeatures.class);
    myBuildType = m.mock(BuildTypeEx.class);
    myQueuedBuild = m.mock(QueuedBuildInfo.class);
    myBuildPromotion = m.mock(BuildPromotionEx.class);
    myTakenLocks = m.mock(TakenLocks.class);
    myBuildDistributorInput = m.mock(BuildDistributorInput.class);
    myRunningBuildsManager = m.mock(RunningBuildsManager.class);
    myCustomData = new HashMap<>();
    myCustomData.put(SharedResourcesAgentsFilter.CUSTOM_DATA_KEY, fairSet);
    myInspector = m.mock(ConfigurationInspector.class);
    myAgentsFilter = new SharedResourcesAgentsFilter(myFeatures, myLocks, myTakenLocks, myRunningBuildsManager, myInspector);
  }

  @Test
  public void testNullBuildType() throws Exception {
    m.checking(new Expectations() {{
      oneOf(myQueuedBuild).getBuildPromotionInfo();
      will(returnValue(myBuildPromotion));

      oneOf(myBuildPromotion).getBuildType();
      will(returnValue(null));

      oneOf(myBuildPromotion).getProjectId();
      will(returnValue(myProjectId));
    }});


    final AgentsFilterResult result = myAgentsFilter.filterAgents(createContext());
    assertNotNull(result);
    assertNull(result.getWaitReason());
    assertNull(result.getFilteredConnectedAgents());
  }

  @Test
  public void testNullProjectId() throws Exception {
    m.checking(new Expectations() {{
      oneOf(myQueuedBuild).getBuildPromotionInfo();
      will(returnValue(myBuildPromotion));

      oneOf(myBuildPromotion).getBuildType();
      will(returnValue(myBuildType));

      oneOf(myBuildPromotion).getProjectId();
      will(returnValue(null));

    }});
    final AgentsFilterResult result = myAgentsFilter.filterAgents(createContext());
    assertNotNull(result);
    assertNull(result.getWaitReason());
    assertNull(result.getFilteredConnectedAgents());
  }

  @Test
  public void testNoFeaturesPresent() throws Exception {
    final Collection<SharedResourcesFeature> features = Collections.emptyList();

    m.checking(new Expectations() {{
      oneOf(myQueuedBuild).getBuildPromotionInfo();
      will(returnValue(myBuildPromotion));

      oneOf(myBuildPromotion).getBuildType();
      will(returnValue(myBuildType));

      oneOf(myBuildPromotion).getProjectId();
      will(returnValue(myProjectId));

      oneOf(myFeatures).searchForFeatures(myBuildType);
      will(returnValue(features));

    }});
    final AgentsFilterResult result = myAgentsFilter.filterAgents(createContext());
    assertNotNull(result);
    assertNull(result.getWaitReason());
    assertNull(result.getFilteredConnectedAgents());
  }

  @Test
  public void testInvalidLocksPresent() throws Exception {
    final Collection<SharedResourcesFeature> features = new ArrayList<>();
    features.add(m.mock(SharedResourcesFeature.class));

    final Map<Lock, String> invalidLocks = new HashMap<>();
    invalidLocks.put(new Lock("lock1", LockType.READ), "");

    m.checking(new Expectations() {{
      oneOf(myQueuedBuild).getBuildPromotionInfo();
      will(returnValue(myBuildPromotion));

      oneOf(myBuildPromotion).getBuildType();
      will(returnValue(myBuildType));

      oneOf(myBuildPromotion).getProjectId();
      will(returnValue(myProjectId));

      oneOf(myFeatures).searchForFeatures(myBuildType);
      will(returnValue(features));

      oneOf(myInspector).inspect(myBuildType);
      will(returnValue(invalidLocks));

      oneOf(myBuildType).getExtendedName();
      will(returnValue("My Build Type"));

      atMost(2).of(myBuildType).getFullName();
      will(returnValue("My Build Type"));

    }});
    final AgentsFilterResult result = myAgentsFilter.filterAgents(createContext());
    assertNotNull(result);
    assertNotNull(result.getWaitReason());
    assertNull(result.getFilteredConnectedAgents());
  }

  @Test
  public void testNoLocksInFeatures() throws Exception {
    final SharedResourcesFeature feature = m.mock(SharedResourcesFeature.class);
    final Collection<SharedResourcesFeature> features = Collections.singleton(feature);

    m.checking(new Expectations() {{
      oneOf(myQueuedBuild).getBuildPromotionInfo();
      will(returnValue(myBuildPromotion));

      oneOf(myBuildPromotion).getBuildType();
      will(returnValue(myBuildType));

      oneOf(myBuildPromotion).getProjectId();
      will(returnValue(myProjectId));

      oneOf(myFeatures).searchForFeatures(myBuildType);
      will(returnValue(features));

      oneOf(myInspector).inspect(myBuildType);
      will(returnValue(Collections.emptyMap()));

      oneOf(myLocks).fromBuildFeaturesAsMap(features);
      will(returnValue(Collections.emptyMap()));
    }});
    final AgentsFilterResult result = myAgentsFilter.filterAgents(createContext());
    assertNotNull(result);
    assertNull(result.getWaitReason());
    assertNull(result.getFilteredConnectedAgents());
  }

  @Test
  public void testLocksPresentSingleBuild() throws Exception {
    final Map<String, Lock> locksToTake = new HashMap<>();
    final Lock lock = new Lock("lock1", LockType.READ);
    locksToTake.put(lock.getName(), lock);

    final SharedResourcesFeature feature = m.mock(SharedResourcesFeature.class);
    final Collection<SharedResourcesFeature> features = Collections.singleton(feature);

    final Map<QueuedBuildInfo, BuildAgent> canBeStarted = Collections.emptyMap();
    final Collection<SRunningBuild> runningBuilds = Collections.emptyList();

    final Map<Resource, TakenLock> takenLocks = Collections.emptyMap();

    setupLocks(locksToTake, features, canBeStarted, runningBuilds, takenLocks, Collections.emptyMap());

    final AgentsFilterResult result = myAgentsFilter.filterAgents(createContext());
    assertNotNull(result);
    assertNull(result.getWaitReason());
    assertNull(result.getFilteredConnectedAgents());
  }

  @Test
  @SuppressWarnings("unchecked")
  public void testMultipleBuildsLocksNotCrossing() throws Exception {
    final SharedResourcesFeature feature = m.mock(SharedResourcesFeature.class);
    final Collection<SharedResourcesFeature> features = Collections.singleton(feature);

    final Resource resource2 = ResourceFactory.newInfiniteResource(myProjectId, "resource2", true);

    final Map<String, Lock> locksToTake = new HashMap<>();
    final Lock lock = new Lock("resource1", LockType.READ);
    locksToTake.put(lock.getName(), lock);

    final Map<QueuedBuildInfo, BuildAgent> canBeStarted = Collections.emptyMap();
    final Collection<SRunningBuild> runningBuilds = Collections.emptyList();

    final Lock lock2 = new Lock("resource2", LockType.READ);

    final Map<Resource, TakenLock> takenLocks = new HashMap<>();
    final TakenLock tl = new TakenLock(resource2);
    tl.addLock(m.mock(BuildPromotionInfo.class), lock2);
    takenLocks.put(tl.getResource(), tl);

    setupLocks(locksToTake, features, canBeStarted, runningBuilds, takenLocks, Collections.emptyMap());

    final AgentsFilterResult result = myAgentsFilter.filterAgents(createContext());
    assertNotNull(result);
    assertNull(result.getWaitReason());
    assertNull(result.getFilteredConnectedAgents());
  }


  @Test
  public void testMultipleBuildsLocksCrossing() throws Exception {
    final SharedResourcesFeature feature = m.mock(SharedResourcesFeature.class);
    final Collection<SharedResourcesFeature> features = Collections.singleton(feature);

    final Resource resource1 = ResourceFactory.newInfiniteResource(myProjectId, "resource1", true);

    final Map<String, Lock> locksToTake = new HashMap<>();
    final Lock lock = new Lock("resource1", LockType.READ);
    locksToTake.put(lock.getName(), lock);

    final Map<QueuedBuildInfo, BuildAgent> canBeStarted = Collections.emptyMap();
    final Collection<SRunningBuild> runningBuilds = Collections.emptyList();

    final BuildPromotionEx bpex = m.mock(BuildPromotionEx.class, "bpex-lock1");
    final Lock takenLock1 = new Lock("resource1", LockType.WRITE);

    final Map<Resource, TakenLock> takenLocks = new HashMap<>();
    final TakenLock tl = new TakenLock(resource1);
    tl.addLock(bpex, takenLock1);
    takenLocks.put(tl.getResource(), tl);

    final BuildTypeEx buildTypeEx = m.mock(BuildTypeEx.class, "bpex-btex");
    final String name = "UNAVAILABLE";

    final Map<Resource, Lock> unavailableLocks = new HashMap<Resource, Lock>() {{
      put(resource1, lock);
    }};


    setupLocks(locksToTake, features, canBeStarted, runningBuilds, takenLocks, unavailableLocks);

    m.checking(new Expectations() {{
      oneOf(bpex).getBuildType();
      will(returnValue(buildTypeEx));

      oneOf(buildTypeEx).getExtendedFullName();
      will(returnValue(name));
    }});

    final AgentsFilterResult result = myAgentsFilter.filterAgents(createContext());
    assertNotNull(result);
    assertNotNull(result.getWaitReason());
    assertNull(result.getFilteredConnectedAgents());
  }

  /**
   * Tests case when no locks are taken on the resource, but resource is anyway unavailable
   * @throws Exception if something goes wrong
   */
  @Test
  @TestFor(issues = "TW-27930")
  public void testNoLockedResources_ResourceDisabled() throws Exception {
    final Map<String, Lock> locksToTake = new HashMap<>();
    final Lock lock = new Lock("resource1", LockType.READ);
    locksToTake.put(lock.getName(), lock);

    final Resource resource1 = ResourceFactory.newInfiniteResource(myProjectId, "resource1", false);

    final SharedResourcesFeature feature = m.mock(SharedResourcesFeature.class);
    final Collection<SharedResourcesFeature> features = Collections.singleton(feature);

    final Map<QueuedBuildInfo, BuildAgent> canBeStarted = Collections.emptyMap();
    final Collection<SRunningBuild> runningBuilds = Collections.emptyList();

    final Map<Resource, TakenLock> takenLocks = Collections.emptyMap();

    final Map<Resource, Lock> unavailableLocks = new HashMap<Resource, Lock>() {{
      put(resource1, lock);
    }};

    setupLocks(locksToTake, features, canBeStarted, runningBuilds, takenLocks, unavailableLocks);

    final AgentsFilterResult result = myAgentsFilter.filterAgents(createContext());
    assertNotNull(result);
    assertNotNull(result.getWaitReason());
    assertNull(result.getFilteredConnectedAgents());

  }

  private void setupLocks(final Map<String, Lock> locksToTake,
                          final Collection<SharedResourcesFeature> features,
                          final Map<QueuedBuildInfo, BuildAgent> canBeStarted,
                          final Collection<SRunningBuild> runningBuilds,
                          final Map<Resource, TakenLock> takenLocks,
                          final Map<Resource, Lock> unavailableLocks) {
    m.checking(new Expectations() {{
      oneOf(myQueuedBuild).getBuildPromotionInfo();
      will(returnValue(myBuildPromotion));

      oneOf(myBuildPromotion).getBuildType();
      will(returnValue(myBuildType));

      oneOf(myBuildPromotion).getProjectId();
      will(returnValue(myProjectId));

      oneOf(myFeatures).searchForFeatures(myBuildType);
      will(returnValue(features));

      oneOf(myLocks).fromBuildFeaturesAsMap(features);
      will(returnValue(locksToTake));

      oneOf(myInspector).inspect(myBuildType);
      will(returnValue(Collections.emptyMap()));

      oneOf(myBuildDistributorInput).getRunningBuilds();
      will(returnValue(runningBuilds));

      oneOf(myRunningBuildsManager).getRunningBuilds();
      will(returnValue(runningBuilds));

      oneOf(myTakenLocks).collectTakenLocks(myProjectId, runningBuilds, canBeStarted.keySet());
      will(returnValue(takenLocks));

      oneOf(myTakenLocks).getUnavailableLocks(locksToTake.values(), takenLocks, myProjectId, fairSet);
      will(returnValue(unavailableLocks));

    }});
  }

  private AgentsFilterContext createContext() {
    return new DefaultAgentsFilterContext(myCustomData) {

      @NotNull
      @Override
      public QueuedBuildInfo getStartingBuild() {
        return myQueuedBuild;
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
        return myBuildDistributorInput;
      }

      @Override
      public boolean isEmulationMode() {
        return false;
      }
    };
  }
}
