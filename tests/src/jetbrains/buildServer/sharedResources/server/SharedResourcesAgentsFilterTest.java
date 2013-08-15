package jetbrains.buildServer.sharedResources.server;

import jetbrains.buildServer.BaseTestCase;
import jetbrains.buildServer.BuildAgent;
import jetbrains.buildServer.serverSide.*;
import jetbrains.buildServer.serverSide.buildDistribution.*;
import jetbrains.buildServer.sharedResources.model.Lock;
import jetbrains.buildServer.sharedResources.model.LockType;
import jetbrains.buildServer.sharedResources.model.TakenLock;
import jetbrains.buildServer.sharedResources.server.feature.Locks;
import jetbrains.buildServer.sharedResources.server.feature.SharedResourcesFeature;
import jetbrains.buildServer.sharedResources.server.feature.SharedResourcesFeatures;
import jetbrains.buildServer.sharedResources.server.runtime.TakenLocks;
import jetbrains.buildServer.util.TestFor;
import org.jetbrains.annotations.NotNull;
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

  private Set<String> fairSet = new HashSet<String>();


  /**
   * Class under test
   */
  private SharedResourcesAgentsFilter myAgentsFilter;


  @BeforeMethod
  @Override
  protected void setUp() throws Exception {
    super.setUp();
    m = new Mockery();
    myLocks = m.mock(Locks.class);
    myFeatures = m.mock(SharedResourcesFeatures.class);
    myBuildType = m.mock(BuildTypeEx.class);
    myQueuedBuild = m.mock(QueuedBuildInfo.class);
    myBuildPromotion = m.mock(BuildPromotionEx.class);
    myTakenLocks = m.mock(TakenLocks.class);
    myBuildDistributorInput = m.mock(BuildDistributorInput.class);
    myRunningBuildsManager = m.mock(RunningBuildsManager.class);
    myCustomData = new HashMap<String, Object>();
    myCustomData.put(SharedResourcesAgentsFilter.CUSTOM_DATA_KEY, fairSet);
    myAgentsFilter = new SharedResourcesAgentsFilter(myFeatures, myLocks, myTakenLocks, myRunningBuildsManager);
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
  }

  @Test
  public void testInvalidLocksPresent() throws Exception {
    final Collection<SharedResourcesFeature> features = new ArrayList<SharedResourcesFeature>();
    features.add(m.mock(SharedResourcesFeature.class));

    final Map<Lock, String> invalidLocks = new HashMap<Lock, String>();
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

      oneOf(features.iterator().next()).getInvalidLocks(myProjectId);
      will(returnValue(invalidLocks));

      oneOf(myBuildType).getExtendedName();
      will(returnValue("My Build Type"));

      atMost(2).of(myBuildType).getFullName();
      will(returnValue("My Build Type"));

    }});
    final AgentsFilterResult result = myAgentsFilter.filterAgents(createContext());
    assertNotNull(result);
    assertNotNull(result.getWaitReason());

  }

  @Test
  public void testNoLocksInFeatures() throws Exception {
    final Collection<SharedResourcesFeature> features = new ArrayList<SharedResourcesFeature>();
    features.add(m.mock(SharedResourcesFeature.class));

    m.checking(new Expectations() {{
      oneOf(myQueuedBuild).getBuildPromotionInfo();
      will(returnValue(myBuildPromotion));

      oneOf(myBuildPromotion).getBuildType();
      will(returnValue(myBuildType));

      oneOf(myBuildPromotion).getProjectId();
      will(returnValue(myProjectId));

      oneOf(myFeatures).searchForFeatures(myBuildType);
      will(returnValue(features));

      oneOf(features.iterator().next()).getInvalidLocks(myProjectId);
      will(returnValue(Collections.emptyMap()));

      oneOf(myLocks).fromBuildPromotion(myBuildPromotion);
      will(returnValue(Collections.emptyList()));

    }});
    final AgentsFilterResult result = myAgentsFilter.filterAgents(createContext());
    assertNotNull(result);
    assertNull(result.getWaitReason());
  }

  @Test
  public void testLocksPresentSingleBuild() throws Exception {
    final Collection<Lock> locks = new ArrayList<Lock>() {{
      add(new Lock("lock1", LockType.READ));
    }};

    final Collection<SharedResourcesFeature> features = new ArrayList<SharedResourcesFeature>();
    features.add(m.mock(SharedResourcesFeature.class));

    final Map<QueuedBuildInfo, BuildAgent> canBeStarted = Collections.emptyMap();
    final Collection<SRunningBuild> runningBuilds = Collections.emptyList();

    m.checking(new Expectations() {{
      oneOf(myQueuedBuild).getBuildPromotionInfo();
      will(returnValue(myBuildPromotion));

      oneOf(myBuildPromotion).getBuildType();
      will(returnValue(myBuildType));

      oneOf(myBuildPromotion).getProjectId();
      will(returnValue(myProjectId));

      oneOf(myFeatures).searchForFeatures(myBuildType);
      will(returnValue(features));

      oneOf(features.iterator().next()).getInvalidLocks(myProjectId);
      will(returnValue(Collections.emptyMap()));

      oneOf(myLocks).fromBuildPromotion(myBuildPromotion);
      will(returnValue(locks));

      oneOf(myBuildDistributorInput).getRunningBuilds();
      will(returnValue(runningBuilds));

      oneOf(myRunningBuildsManager).getRunningBuilds();
      will(returnValue(runningBuilds));

      oneOf(myTakenLocks).collectTakenLocks(myProjectId, runningBuilds, canBeStarted.keySet());
      will(returnValue(Collections.emptyMap()));

    }});

    final AgentsFilterResult result = myAgentsFilter.filterAgents(createContext());
    assertNotNull(result);
    assertNull(result.getWaitReason());
  }


  @Test
  @SuppressWarnings("unchecked")
  public void testMultipleBuildsLocksNotCrossing() throws Exception {
    final Collection<SharedResourcesFeature> features = new ArrayList<SharedResourcesFeature>();
    features.add(m.mock(SharedResourcesFeature.class));

    final Collection<Lock> locks = new ArrayList<Lock>() {{
      add(new Lock("lock1", LockType.READ));
    }};
    final Map<QueuedBuildInfo, BuildAgent> canBeStarted = Collections.emptyMap();
    final Collection<SRunningBuild> runningBuilds = Collections.emptyList();

    final Map<String, TakenLock> takenLocks = new HashMap<String, TakenLock>() {{
      final TakenLock tl = new TakenLock();
      tl.addLock(m.mock(BuildPromotionInfo.class), new Lock("lock2", LockType.READ));
      put("lock2", tl);

    }};

    m.checking(new Expectations() {{
      oneOf(myQueuedBuild).getBuildPromotionInfo();
      will(returnValue(myBuildPromotion));

      oneOf(myBuildPromotion).getBuildType();
      will(returnValue(myBuildType));

      oneOf(myBuildPromotion).getProjectId();
      will(returnValue(myProjectId));

      oneOf(myFeatures).searchForFeatures(myBuildType);
      will(returnValue(features));

      oneOf(features.iterator().next()).getInvalidLocks(myProjectId);
      will(returnValue(Collections.emptyMap()));

      oneOf(myLocks).fromBuildPromotion(myBuildPromotion);
      will(returnValue(locks));

      oneOf(myRunningBuildsManager).getRunningBuilds();
      will(returnValue(runningBuilds));

      oneOf(myBuildDistributorInput).getRunningBuilds();
      will(returnValue(runningBuilds));

      oneOf(myTakenLocks).collectTakenLocks(myProjectId, runningBuilds, canBeStarted.keySet());
      will(returnValue(takenLocks));

      oneOf(myTakenLocks).getUnavailableLocks(locks, takenLocks, myProjectId, fairSet);
      will(returnValue(Collections.emptyList()));

    }});

    final AgentsFilterResult result = myAgentsFilter.filterAgents(createContext());
    assertNotNull(result);
    assertNull(result.getWaitReason());

  }

  @Test
  public void testMultipleBuildsLocksCrossing() throws Exception {
    final Collection<SharedResourcesFeature> features = new ArrayList<SharedResourcesFeature>();
    features.add(m.mock(SharedResourcesFeature.class));
    final Collection<Lock> locks = new ArrayList<Lock>() {{
      add(new Lock("lock1", LockType.READ));
    }};
    final Map<QueuedBuildInfo, BuildAgent> canBeStarted = Collections.emptyMap();
    final Collection<SRunningBuild> runningBuilds = Collections.emptyList();

    final BuildPromotionEx bpex = m.mock(BuildPromotionEx.class, "bpex-lock1");
    final Map<String, TakenLock> takenLocks = new HashMap<String, TakenLock>() {{
      final TakenLock tl = new TakenLock();
      tl.addLock(bpex, new Lock("lock1", LockType.WRITE));
      put("lock1", tl);
    }};

    final BuildTypeEx buildTypeEx = m.mock(BuildTypeEx.class, "bpex-btex");
    final String name = "UNAVAILABLE";

    m.checking(new Expectations() {{
      oneOf(myQueuedBuild).getBuildPromotionInfo();
      will(returnValue(myBuildPromotion));

      oneOf(myBuildPromotion).getBuildType();
      will(returnValue(myBuildType));

      oneOf(myBuildPromotion).getProjectId();
      will(returnValue(myProjectId));

      oneOf(myFeatures).searchForFeatures(myBuildType);
      will(returnValue(features));

      oneOf(features.iterator().next()).getInvalidLocks(myProjectId);
      will(returnValue(Collections.emptyMap()));

      oneOf(myLocks).fromBuildPromotion(myBuildPromotion);
      will(returnValue(locks));

      oneOf(myBuildDistributorInput).getRunningBuilds();
      will(returnValue(runningBuilds));

      oneOf(myRunningBuildsManager).getRunningBuilds();
      will(returnValue(runningBuilds));

      oneOf(myTakenLocks).collectTakenLocks(myProjectId, runningBuilds, canBeStarted.keySet());
      will(returnValue(takenLocks));

      oneOf(myTakenLocks).getUnavailableLocks(locks, takenLocks, myProjectId, fairSet);
      will(returnValue(locks));

      oneOf(bpex).getBuildType();
      will(returnValue(buildTypeEx));

      oneOf(buildTypeEx).getName();
      will(returnValue(name));
    }});

    final AgentsFilterResult result = myAgentsFilter.filterAgents(createContext());
    assertNotNull(result);
    assertNotNull(result.getWaitReason());
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
