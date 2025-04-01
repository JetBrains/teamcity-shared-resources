

package jetbrains.buildServer.sharedResources.server;

import java.util.*;
import jetbrains.buildServer.BaseTestCase;
import jetbrains.buildServer.BuildAgent;
import jetbrains.buildServer.serverSide.*;
import jetbrains.buildServer.serverSide.buildDistribution.*;
import jetbrains.buildServer.serverSide.impl.ProjectEx;
import jetbrains.buildServer.serverSide.impl.RunningBuildsManagerEx;
import jetbrains.buildServer.sharedResources.model.Lock;
import jetbrains.buildServer.sharedResources.model.LockType;
import jetbrains.buildServer.sharedResources.model.TakenLock;
import jetbrains.buildServer.sharedResources.model.resources.Resource;
import jetbrains.buildServer.sharedResources.model.resources.ResourceFactory;
import jetbrains.buildServer.sharedResources.server.feature.Locks;
import jetbrains.buildServer.sharedResources.server.feature.Resources;
import jetbrains.buildServer.sharedResources.server.feature.SharedResourcesFeature;
import jetbrains.buildServer.sharedResources.server.feature.SharedResourcesFeatures;
import jetbrains.buildServer.sharedResources.server.runtime.DistributionDataAccessor;
import jetbrains.buildServer.sharedResources.server.runtime.LocksStorage;
import jetbrains.buildServer.sharedResources.server.runtime.TakenLocks;
import jetbrains.buildServer.util.TestFor;
import org.jetbrains.annotations.NotNull;
import org.jmock.Expectations;
import org.jmock.Mockery;
import org.jmock.lib.legacy.ClassImposteriser;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

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

  private QueuedBuildEx myQueuedBuildEx;

  private BuildPromotionEx myBuildPromotion;

  private BuildDistributorInput myBuildDistributorInput;

  private BuildTypeEx myBuildType;

  private final String myProjectId = "PROJECT_ID";

  private final String PROJECT_NAME = "My Project";

  private SProject myProject;

  private TakenLocks myTakenLocks;

  private RunningBuildsManagerEx myRunningBuildsManager;

  private Map<String, Object> myCustomData;

  private ConfigurationInspector myInspector;

  private Resources myResources;

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
    myQueuedBuildEx = m.mock(QueuedBuildEx.class);
    myBuildPromotion = m.mock(BuildPromotionEx.class);
    myTakenLocks = m.mock(TakenLocks.class);
    myBuildDistributorInput = m.mock(BuildDistributorInput.class);
    myRunningBuildsManager = m.mock(RunningBuildsManagerEx.class);
    myCustomData = new HashMap<>();
    myInspector = m.mock(ConfigurationInspector.class);
    myProject = m.mock(ProjectEx.class);

    final LocksStorage locksStorage = m.mock(LocksStorage.class);

    myResources = m.mock(Resources.class);
    final Map<String, Resource> resourceMap = new HashMap<>();
    resourceMap.put("lock1", ResourceFactory.newInfiniteResource("lock1", myProjectId, "lock1", true));

    m.checking(new Expectations() {{
      allowing(myProject).getProjectId();
      will(returnValue(myProjectId));

      allowing(myProject).getExtendedFullName();
      will(returnValue(PROJECT_NAME));

      allowing(myResources).getResourcesMap(myProjectId);
      will(returnValue(resourceMap));
    }});
    myAgentsFilter = new SharedResourcesAgentsFilter(myFeatures, myLocks, myTakenLocks, myRunningBuildsManager, myInspector, locksStorage, myResources);
  }
  
  @Test
  public void testNullBuildType() {
    m.checking(new Expectations() {{
      oneOf(myRunningBuildsManager).getRunningBuildsEx();
      will(returnValue(Collections.emptyList()));

      oneOf(myQueuedBuild).getBuildPromotionInfo();
      will(returnValue(myBuildPromotion));

      oneOf(myBuildPromotion).getBuildType();
      will(returnValue(null));

      oneOf(myBuildPromotion).getProjectId();
      will(returnValue(myProjectId));

      allowing(myBuildPromotion).isPartOfBuildChain();
      will(returnValue(false));
    }});


    final AgentsFilterResult result = myAgentsFilter.filterAgents(createContext());
    assertNotNull(result);
    assertNull(result.getWaitReason());
    assertNull(result.getFilteredConnectedAgents());
  }

  @Test
  public void testNullProjectId() {
    m.checking(new Expectations() {{
      oneOf(myRunningBuildsManager).getRunningBuildsEx();
      will(returnValue(Collections.emptyList()));

      oneOf(myQueuedBuild).getBuildPromotionInfo();
      will(returnValue(myBuildPromotion));

      oneOf(myBuildPromotion).getBuildType();
      will(returnValue(myBuildType));

      oneOf(myBuildPromotion).getProjectId();
      will(returnValue(null));

      allowing(myBuildPromotion).isPartOfBuildChain();
      will(returnValue(false));

    }});
    final AgentsFilterResult result = myAgentsFilter.filterAgents(createContext());
    assertNotNull(result);
    assertNull(result.getWaitReason());
    assertNull(result.getFilteredConnectedAgents());
  }

  @Test
  public void testNoFeaturesPresent() {
    final Collection<SharedResourcesFeature> features = Collections.emptyList();

    m.checking(new Expectations() {{
      oneOf(myRunningBuildsManager).getRunningBuildsEx();
      will(returnValue(Collections.emptyList()));

      oneOf(myQueuedBuild).getBuildPromotionInfo();
      will(returnValue(myBuildPromotion));

      oneOf(myBuildPromotion).getBuildType();
      will(returnValue(myBuildType));

      oneOf(myBuildPromotion).getProjectId();
      will(returnValue(myProjectId));

      allowing(myBuildPromotion).isPartOfBuildChain();
      will(returnValue(false));

      oneOf(myFeatures).searchForFeatures(myBuildPromotion);
      will(returnValue(features));

    }});
    final AgentsFilterResult result = myAgentsFilter.filterAgents(createContext());
    assertNotNull(result);
    assertNull(result.getWaitReason());
    assertNull(result.getFilteredConnectedAgents());
  }

  @Test
  public void testInvalidLocksPresent() {
    final Collection<SharedResourcesFeature> features = new ArrayList<>();
    features.add(m.mock(SharedResourcesFeature.class));

    final Map<Lock, String> invalidLocks = new HashMap<>();
    invalidLocks.put(new Lock("lock1", LockType.READ), "");

    m.checking(new Expectations() {{
      oneOf(myRunningBuildsManager).getRunningBuildsEx();
      will(returnValue(Collections.emptyList()));

      oneOf(myQueuedBuild).getBuildPromotionInfo();
      will(returnValue(myBuildPromotion));

      oneOf(myBuildPromotion).getBuildType();
      will(returnValue(myBuildType));

      oneOf(myBuildPromotion).getProjectId();
      will(returnValue(myProjectId));

      oneOf(myFeatures).searchForFeatures(myBuildPromotion);
      will(returnValue(features));

      oneOf(myInspector).inspect(myBuildPromotion);
      will(returnValue(invalidLocks));

      oneOf(myBuildType).getExtendedName();
      will(returnValue("My Build Type"));

      atMost(2).of(myBuildType).getFullName();
      will(returnValue("My Build Type"));

      allowing(myBuildPromotion).isPartOfBuildChain();
      will(returnValue(false));
    }});
    final AgentsFilterResult result = myAgentsFilter.filterAgents(createContext());
    assertNotNull(result);
    assertNotNull(result.getWaitReason());
    assertNull(result.getFilteredConnectedAgents());
  }

  @Test
  public void testNoLocksInFeatures() {
    final SharedResourcesFeature feature = m.mock(SharedResourcesFeature.class);
    final Collection<SharedResourcesFeature> features = Collections.singleton(feature);

    m.checking(new Expectations() {{
      oneOf(myRunningBuildsManager).getRunningBuildsEx();
      will(returnValue(Collections.emptyList()));

      oneOf(myQueuedBuild).getBuildPromotionInfo();
      will(returnValue(myBuildPromotion));

      oneOf(myBuildPromotion).getBuildType();
      will(returnValue(myBuildType));

      oneOf(myBuildPromotion).getProjectId();
      will(returnValue(myProjectId));

      oneOf(myFeatures).searchForFeatures(myBuildPromotion);
      will(returnValue(features));

      oneOf(myInspector).inspect(myBuildPromotion);
      will(returnValue(Collections.emptyMap()));

      oneOf(myLocks).fromBuildFeaturesAsMap(features);
      will(returnValue(Collections.emptyMap()));

      allowing(myBuildPromotion).isPartOfBuildChain();
      will(returnValue(false));

    }});
    final AgentsFilterResult result = myAgentsFilter.filterAgents(createContext());
    assertNotNull(result);
    assertNull(result.getWaitReason());
    assertNull(result.getFilteredConnectedAgents());
  }

  @Test
  public void testLocksPresentSingleBuild() {
    final Map<String, Lock> locksToTake = new HashMap<>();
    final Lock lock = new Lock("lock1", LockType.READ);
    locksToTake.put(lock.getName(), lock);

    final SharedResourcesFeature feature = m.mock(SharedResourcesFeature.class);
    final Collection<SharedResourcesFeature> features = Collections.singleton(feature);

    final Map<QueuedBuildInfo, BuildAgent> canBeStarted = Collections.emptyMap();
    final Collection<RunningBuildEx> runningBuilds = Collections.emptyList();

    final Map<Resource, TakenLock> takenLocks = Collections.emptyMap();

    setupLocks(locksToTake, features, canBeStarted, runningBuilds, takenLocks, Collections.emptyMap());

    final AgentsFilterResult result = myAgentsFilter.filterAgents(createContext());
    assertNotNull(result);
    assertNull(result.getWaitReason());
    assertNull(result.getFilteredConnectedAgents());
  }

  @Test
  public void testMultipleBuildsLocksNotCrossing() {
    final SharedResourcesFeature feature = m.mock(SharedResourcesFeature.class);
    final Collection<SharedResourcesFeature> features = Collections.singleton(feature);

    final Resource resource2 = ResourceFactory.newInfiniteResource("resource2", myProjectId, "resource2", true);

    final Map<String, Lock> locksToTake = new HashMap<>();
    final Lock lock = new Lock("resource1", LockType.READ);
    locksToTake.put(lock.getName(), lock);

    final Map<QueuedBuildInfo, BuildAgent> canBeStarted = Collections.emptyMap();
    final Collection<RunningBuildEx> runningBuilds = Collections.emptyList();

    final Lock lock2 = new Lock("resource2", LockType.READ);

    final Map<Resource, TakenLock> takenLocks = new HashMap<>();
    final TakenLock tl = new TakenLock(resource2);
    tl.addLock(m.mock(BuildPromotionEx.class, "some-build"), lock2);
    takenLocks.put(tl.getResource(), tl);

    setupLocks(locksToTake, features, canBeStarted, runningBuilds, takenLocks, Collections.emptyMap());

    final AgentsFilterResult result = myAgentsFilter.filterAgents(createContext());
    assertNotNull(result);
    assertNull(result.getWaitReason());
    assertNull(result.getFilteredConnectedAgents());
  }


  @Test
  public void testMultipleBuildsLocksCrossing() {
    final SharedResourcesFeature feature = m.mock(SharedResourcesFeature.class);
    final Collection<SharedResourcesFeature> features = Collections.singleton(feature);

    final Resource resource1 = ResourceFactory.newInfiniteResource("resource1", myProjectId, "resource1", true);

    final Map<String, Lock> locksToTake = new HashMap<>();
    final Lock lock = new Lock("resource1", LockType.READ);
    locksToTake.put(lock.getName(), lock);

    final Map<QueuedBuildInfo, BuildAgent> canBeStarted = Collections.emptyMap();
    final Collection<RunningBuildEx> runningBuilds = Collections.emptyList();

    final BuildPromotionEx bpex = m.mock(BuildPromotionEx.class, "bpex-lock1");
    final Lock takenLock1 = new Lock("resource1", LockType.WRITE);

    final Map<Resource, TakenLock> takenLocks = new HashMap<>();
    final TakenLock tl = new TakenLock(resource1);
    tl.addLock(bpex, takenLock1);
    takenLocks.put(tl.getResource(), tl);

    final BuildTypeEx buildTypeEx = m.mock(BuildTypeEx.class, "bpex-btex");
    final String name = "UNAVAILABLE";

    final Map<Resource, String> unavailableLocks = new HashMap<Resource, String>() {{
      put(resource1, "reason");
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
   */
  @Test
  @TestFor(issues = "TW-27930")
  public void testNoLockedResources_ResourceDisabled() {
    final Map<String, Lock> locksToTake = new HashMap<>();
    final Lock lock = new Lock("resource1", LockType.READ);
    locksToTake.put(lock.getName(), lock);

    final Resource resource1 = ResourceFactory.newInfiniteResource("resource1", myProjectId, "resource1", false);

    final SharedResourcesFeature feature = m.mock(SharedResourcesFeature.class);
    final Collection<SharedResourcesFeature> features = Collections.singleton(feature);

    final Map<QueuedBuildInfo, BuildAgent> canBeStarted = Collections.emptyMap();
    final Collection<RunningBuildEx> runningBuilds = Collections.emptyList();

    final Map<Resource, TakenLock> takenLocks = Collections.emptyMap();

    final Map<Resource, String> unavailableLocks = new HashMap<Resource, String>() {{
      put(resource1, "reason");
    }};

    setupLocks(locksToTake, features, canBeStarted, runningBuilds, takenLocks, unavailableLocks);

    final AgentsFilterResult result = myAgentsFilter.filterAgents(createContext());
    assertNotNull(result);
    assertNotNull(result.getWaitReason());
    assertNull(result.getFilteredConnectedAgents());
  }

  @Test
  @TestFor(issues = "TW-45949")
  public void testDuplicateResources() {
    final Collection<SharedResourcesFeature> features = new ArrayList<>();
    features.add(m.mock(SharedResourcesFeature.class));

    final Map<Lock, String> invalidLocks = new HashMap<>();
    invalidLocks.put(new Lock("lock1", LockType.READ), "Resource 'lock1' has duplicate definition");

    m.checking(new Expectations() {{
      oneOf(myRunningBuildsManager).getRunningBuildsEx();
      will(returnValue(Collections.emptyList()));

      oneOf(myQueuedBuild).getBuildPromotionInfo();
      will(returnValue(myBuildPromotion));

      oneOf(myBuildPromotion).getBuildType();
      will(returnValue(myBuildType));

      oneOf(myBuildPromotion).getProjectId();
      will(returnValue(myProjectId));

      oneOf(myFeatures).searchForFeatures(myBuildPromotion);
      will(returnValue(features));

      oneOf(myInspector).inspect(myBuildPromotion);
      will(returnValue(invalidLocks));

      oneOf(myBuildType).getExtendedName();
      will(returnValue("Project :: BuildType"));

      allowing(myBuildPromotion).isPartOfBuildChain();
      will(returnValue(false));

    }});
    final AgentsFilterResult result = myAgentsFilter.filterAgents(createContext());
    assertNotNull(result);
    assertNotNull(result.getWaitReason());
    assertNull(result.getFilteredConnectedAgents());
  }

  private void setupLocks(final Map<String, Lock> locksToTake,
                          final Collection<SharedResourcesFeature> features,
                          final Map<QueuedBuildInfo, BuildAgent> canBeStarted,
                          final Collection<RunningBuildEx> runningBuilds,
                          final Map<Resource, TakenLock> takenLocks,
                          final Map<Resource, String> unavailableLocks) {
    m.checking(new Expectations() {{
      oneOf(myQueuedBuild).getBuildPromotionInfo();
      will(returnValue(myBuildPromotion));

      oneOf(myBuildPromotion).getBuildType();
      will(returnValue(myBuildType));

      oneOf(myBuildPromotion).getProjectId();
      will(returnValue(myProjectId));

      oneOf(myFeatures).searchForFeatures(myBuildType);
      will(returnValue(features));

      oneOf(myFeatures).searchForFeatures(myBuildPromotion);
      will(returnValue(features));

      oneOf(myLocks).fromBuildFeaturesAsMap(features);
      will(returnValue(locksToTake));

      oneOf(myInspector).inspect(myBuildPromotion);
      will(returnValue(Collections.emptyMap()));

      oneOf(myRunningBuildsManager).getRunningBuildsEx();
      will(returnValue(runningBuilds));

      oneOf(myTakenLocks).collectTakenLocks(runningBuilds, canBeStarted.keySet());
      will(returnValue(takenLocks));

      oneOf(myTakenLocks).getUnavailableLocks(with(same(locksToTake.values())), with(same(takenLocks)), with(same(myProjectId)), with(any(DistributionDataAccessor.class)), with(same(myBuildPromotion)));
      will(returnValue(unavailableLocks));

      allowing(myBuildPromotion).isPartOfBuildChain();
      will(returnValue(false));

      allowing(myBuildPromotion).getQueuedBuild();
      will(returnValue(myQueuedBuildEx));

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