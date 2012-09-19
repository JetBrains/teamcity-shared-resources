package jetbrains.buildServer.sharedResources;

import com.thoughtworks.xstream.io.naming.NoNameCoder;
import jetbrains.buildServer.BaseTestCase;
import jetbrains.buildServer.serverSide.*;
import jetbrains.buildServer.serverSide.buildDistribution.AgentsFilterContext;
import jetbrains.buildServer.serverSide.buildDistribution.AgentsFilterResult;
import jetbrains.buildServer.serverSide.buildDistribution.QueuedBuildInfo;
import jetbrains.buildServer.sharedResources.server.SharedResourcesBuildAgentsFilter;
import org.jetbrains.annotations.NotNull;
import org.jmock.Expectations;
import org.jmock.Mockery;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Map;

/**
 * Contains tests for {@code SharedResourcesBuildFeature}
 *
 * @author Oleg Rybak
 */
public class FeatureTest extends BaseTestCase {

  private Mockery m;
  private RunningBuildsManager myRunningBuildsManager;
  private AgentsFilterContext myAgentsFilterContext;
  private QueuedBuildInfo myQueuedBuildInfo;
  private BuildPromotionEx myBuildPromotion;
  private BuildTypeEx myBuildType;
  private SBuildFeatureDescriptor myBuildFeatureDescriptor;
  private SharedResourcesBuildAgentsFilter mySharedResourcesBuildAgentsFilter;
  private Map myParamMap;

  private SRunningBuild otherRunningBuild1;
  private SRunningBuild otherRunningBuild2;

  private SBuildType otherBuildType1;
  private SBuildType otherBuildType2;


  private static final String currentBuildLocks = "my_lock_1\nmy_lock_2";
  private static final String otherBuildLocksCrossing = "my_lock_2\nmylock_3";
  private static final String otherBuildLocksNotCrossing = "my_lock_3\nmylock_4";


  private static final String simpleLocks = "my_lock_1\nmy_lock_1\nmy_lock_1\n";
  private static final String readLocks = "my_lock_1(read)\nmy_lock_2(read)\nmy_lock_3(read)\n";
  private static final String writeLocks = "my_lock_1(write)\nmy_lock_2(write)\nmy_lock_3(write)\n";


  @BeforeMethod
  @Override
  protected void setUp() throws Exception {
    super.setUp();
    m = new Mockery();

    myRunningBuildsManager = m.mock(RunningBuildsManager.class, "builds-manager-no-conflicts");
    myAgentsFilterContext = m.mock(AgentsFilterContext.class);
    myQueuedBuildInfo = m.mock(QueuedBuildInfo.class);
    myBuildPromotion = m.mock(BuildPromotionEx.class);
    myBuildType = m.mock(BuildTypeEx.class);
    myBuildFeatureDescriptor = m.mock(SBuildFeatureDescriptor.class);
    mySharedResourcesBuildAgentsFilter = new SharedResourcesBuildAgentsFilter(myRunningBuildsManager);
    myParamMap = m.mock(Map.class);

    otherRunningBuild1 = m.mock(SRunningBuild.class, "other-running-build-1");
    otherRunningBuild2 = m.mock(SRunningBuild.class, "other-running-build-2");
    otherBuildType1 = m.mock(SBuildType.class, "other-build-type-1");
    otherBuildType2 = m.mock(SBuildType.class, "other-build-type-2");
  }

  private void setCommonExpectationsForTwoBuilds(@NotNull final String myLocks, @NotNull final String otherLocks) {
    m.checking(new Expectations(){{
      oneOf(myAgentsFilterContext).getStartingBuild();
      will(returnValue(myQueuedBuildInfo));

      oneOf(myQueuedBuildInfo).getBuildPromotionInfo();
      will(returnValue(myBuildPromotion));

      oneOf(myBuildPromotion).getBuildType();
      will(returnValue(myBuildType));


      oneOf(myBuildType).getBuildFeatures();
      will(returnValue(new ArrayList<SBuildFeatureDescriptor>() {{add(myBuildFeatureDescriptor);}}));

      oneOf(myBuildFeatureDescriptor).getType();
      will(returnValue(SharedResourcesPluginConstants.FEATURE_TYPE));

      oneOf(myBuildFeatureDescriptor).getParameters();
      will(returnValue(myParamMap));

      oneOf(myParamMap).get(SharedResourcesPluginConstants.RESOURCE_PARAM_KEY);
      will(returnValue(myLocks));

      oneOf(myRunningBuildsManager).getRunningBuilds();
      will(returnValue(new ArrayList<SRunningBuild>() {{add(otherRunningBuild1);}}));

      oneOf(otherRunningBuild1).getBuildType();
      will(returnValue(otherBuildType1));

      oneOf(otherBuildType1).getBuildFeatures();
      will(returnValue(new ArrayList<SBuildFeatureDescriptor>() {{add(myBuildFeatureDescriptor);}}));

      oneOf(myBuildFeatureDescriptor).getType();
      will(returnValue(SharedResourcesPluginConstants.FEATURE_TYPE));

      oneOf(myBuildFeatureDescriptor).getParameters();
      will(returnValue(myParamMap));

      oneOf(myParamMap).get(SharedResourcesPluginConstants.RESOURCE_PARAM_KEY);
      will(returnValue(otherLocks));
    }});
  }

  private void runFilterWait() {
    AgentsFilterResult result = mySharedResourcesBuildAgentsFilter.filterAgents(myAgentsFilterContext);
    assertNotNull(result);
    assertNotNull(result.getFilteredConnectedAgents());
    assertEquals(Collections.<SBuildAgent>emptyList(), result.getFilteredConnectedAgents());
    assertNotNull(result.getWaitReason());
    m.assertIsSatisfied();
  }

  private void runFilterProceed() {
    AgentsFilterResult result = mySharedResourcesBuildAgentsFilter.filterAgents(myAgentsFilterContext);
    assertNotNull(result);
    assertNull(result.getFilteredConnectedAgents());
    assertNull(result.getWaitReason());
    m.assertIsSatisfied();
  }

  @Test
  public void testWaitSimpleSimple() {
    setCommonExpectationsForTwoBuilds(simpleLocks, simpleLocks);
    runFilterWait();
  }

  @Test
  public void testWaitSimpleRead() {
    setCommonExpectationsForTwoBuilds(simpleLocks, readLocks);
    runFilterWait();
  }

  @Test
  public void testWaitSimpleWrite() {
    setCommonExpectationsForTwoBuilds(simpleLocks, writeLocks);
    runFilterWait();
  }

  @Test
  public void testWaitReadSimple() {
    setCommonExpectationsForTwoBuilds(readLocks, simpleLocks);
    runFilterWait();
  }

  @Test
  public void testWaitReadWrite() {
    setCommonExpectationsForTwoBuilds(readLocks, writeLocks);
    runFilterWait();
  }

  @Test
  public void testWaitWriteSimple() {
    setCommonExpectationsForTwoBuilds(writeLocks, simpleLocks);
    runFilterWait();
  }

  @Test
  public void testWaitWriteRead() {
    setCommonExpectationsForTwoBuilds(writeLocks, readLocks);
    runFilterWait();
  }

  @Test
  public void testWaitWriteWrite() {
    setCommonExpectationsForTwoBuilds(writeLocks, writeLocks);
    runFilterWait();
  }

  @Test
  public void testProceedReadRead() {
    setCommonExpectationsForTwoBuilds(readLocks, readLocks);
    runFilterProceed();
  }

  @Test
  public void testProceedSimpleEmpty() {
    setCommonExpectationsForTwoBuilds(simpleLocks, "");
    runFilterProceed();
  }


  /**
   * Simulates the situation when there are no
   * builds with conflicting locks. Other builds are present.
   * Current build is assigned to an agent (no {@code WaitReason} returned)
   */
  @Test
  public void testProceedNoBuildsRunning() {
    m.checking(new Expectations(){{
      oneOf(myAgentsFilterContext).getStartingBuild();
      will(returnValue(myQueuedBuildInfo));

      oneOf(myQueuedBuildInfo).getBuildPromotionInfo();
      will(returnValue(myBuildPromotion));

      oneOf(myBuildPromotion).getBuildType();
      will(returnValue(myBuildType));

      oneOf(myBuildType).getBuildFeatures();
      will(returnValue(new ArrayList<SBuildFeatureDescriptor>() {{add(myBuildFeatureDescriptor);}}));

      oneOf(myBuildFeatureDescriptor).getType();
      will(returnValue(SharedResourcesPluginConstants.FEATURE_TYPE));


      oneOf(myBuildFeatureDescriptor).getParameters();
      will(returnValue(myParamMap));

      oneOf(myParamMap).get(SharedResourcesPluginConstants.RESOURCE_PARAM_KEY);
      will(returnValue(currentBuildLocks));

      oneOf(myRunningBuildsManager).getRunningBuilds();
      will(returnValue(Collections.<SRunningBuild>emptyList()));


    }});

    AgentsFilterResult result = mySharedResourcesBuildAgentsFilter.filterAgents(myAgentsFilterContext);
    assertNotNull(result);
    assertNull(result.getFilteredConnectedAgents());
    assertNull(result.getWaitReason());
    m.assertIsSatisfied();
  }
}
