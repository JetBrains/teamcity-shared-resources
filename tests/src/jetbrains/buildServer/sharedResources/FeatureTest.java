package jetbrains.buildServer.sharedResources;

import jetbrains.buildServer.BaseTestCase;
import jetbrains.buildServer.BuildAgent;
import jetbrains.buildServer.serverSide.buildDistribution.BuildDistributorInput;
import jetbrains.buildServer.serverSide.buildDistribution.BuildPromotionInfo;
import jetbrains.buildServer.serverSide.buildDistribution.QueuedBuildInfo;
import jetbrains.buildServer.sharedResources.server.SharedResourcesWaitPrecondition;
import jetbrains.buildServer.util.TestFor;
import org.jmock.Mockery;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.util.Map;

/**
 * Contains tests for {@code SharedResourcesBuildFeature}
 *
 * @author Oleg Rybak
 */
@TestFor (testForClass = SharedResourcesWaitPrecondition.class)
public class FeatureTest extends BaseTestCase {

  private Mockery m;
  // tested class
  private SharedResourcesWaitPrecondition myWaitPrecondition;
  // mocked data
  private QueuedBuildInfo myQueuedBuildInfo;
  private Map<QueuedBuildInfo, BuildAgent> myCanBeStarted;
  private BuildDistributorInput myBuildDistributorInput;
  private BuildPromotionInfo myBuildPromotion;
  // actual data


  @BeforeMethod
  @Override
  protected void setUp() throws Exception {
    super.setUp();
    m = new Mockery();

    myQueuedBuildInfo = m.mock(QueuedBuildInfo.class);
    //noinspection unchecked
    myCanBeStarted = m.mock(Map.class);
    myBuildDistributorInput = m.mock(BuildDistributorInput.class);
    myBuildPromotion = m.mock(BuildPromotionInfo.class);

    myWaitPrecondition = new SharedResourcesWaitPrecondition();
  }


  @Test // todo: not ready yet
  public void testSingleBuild() {
//    final Map<String, String> buildParams = new HashMap<String, String>() {{
////       put(TestUtils.generateLockAsParam(), "");
////       put(TestUtils.generateLockAsParam(), "");
////       put(TestUtils.generateLockAsParam(), "");
////       put(TestUtils.generateLockAsParam(), "");
////       put(TestUtils.generateLockAsParam(), "");
////       put(TestUtils.generateRandomSystemParam(), "");
//       put(TestUtils.generateRandomSystemParam(), "");
//       put(TestUtils.generateRandomSystemParam(), "");
//       put(TestUtils.generateRandomSystemParam(), "");
//    }};
//
//
//    m.checking(new Expectations() {{
//      oneOf(myQueuedBuildInfo).getBuildPromotionInfo();
//      will(returnValue(myBuildPromotion));
//
//      oneOf(myBuildPromotion).getBuildParameters();
//      will(returnValue(buildParams));
//
//      oneOf(myBuildDistributorInput).getRunningBuilds();
//      will(returnValue(Collections.<BuildPromotionInfo>emptyList()));
//
//      oneOf(myCanBeStarted).keySet();
//      will(returnValue(Collections.<QueuedBuildInfo>emptySet()));
//
//    }});
//
//
//    WaitReason result = myWaitPrecondition.canStart(myQueuedBuildInfo, myCanBeStarted, myBuildDistributorInput, false);
//    assertNull(result);
//    m.assertIsSatisfied();
  }


}
