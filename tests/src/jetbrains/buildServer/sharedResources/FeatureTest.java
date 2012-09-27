package jetbrains.buildServer.sharedResources;

import jetbrains.buildServer.BaseTestCase;
import jetbrains.buildServer.BuildAgent;
import jetbrains.buildServer.serverSide.buildDistribution.BuildDistributorInput;
import jetbrains.buildServer.serverSide.buildDistribution.BuildPromotionInfo;
import jetbrains.buildServer.serverSide.buildDistribution.QueuedBuildInfo;
import jetbrains.buildServer.sharedResources.model.LockType;
import jetbrains.buildServer.sharedResources.server.SharedResourcesWaitPrecondition;
import jetbrains.buildServer.util.TestFor;
import org.jmock.Expectations;
import org.jmock.Mockery;
import org.testng.annotations.BeforeMethod;

import java.util.Collections;
import java.util.Map;
import java.util.UUID;

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


  //@Test todo: not ready yet
  public void testSingleBuild() {
    m.checking(new Expectations() {{
      oneOf(myQueuedBuildInfo).getBuildPromotionInfo();
      will(returnValue(myBuildPromotion));

      oneOf(myBuildDistributorInput).getRunningBuilds();
      will(returnValue(Collections.<BuildPromotionInfo>emptyList()));

      oneOf(myCanBeStarted).keySet();
      will(returnValue(Collections.<QueuedBuildInfo>emptySet()));
    }});


    myWaitPrecondition.canStart(myQueuedBuildInfo, myCanBeStarted, myBuildDistributorInput, false);
    m.assertIsSatisfied();
  }




  /**
   * Generates lock representation as it is in build parameters
   * @param name name of the lock
   * @param type type of the lock
   * @return lock representation as a parameter
   */
  private static String generateLockAsParam(String name, LockType type) {
    return SharedResourcesPluginConstants.LOCK_PREFIX + type.getName() + "." + name;
  }

  /**
   * Generates random system parameter
   * @return random system parameter as String
   */
  private static String generateRandomSystemParam() {
    return "system." + UUID.randomUUID().toString();
  }

}
