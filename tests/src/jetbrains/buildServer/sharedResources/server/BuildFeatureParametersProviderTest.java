package jetbrains.buildServer.sharedResources.server;

import jetbrains.buildServer.BaseTestCase;
import jetbrains.buildServer.serverSide.SBuild;
import jetbrains.buildServer.serverSide.SBuildFeatureDescriptor;
import jetbrains.buildServer.serverSide.SBuildType;
import jetbrains.buildServer.sharedResources.SharedResourcesPluginConstants;
import jetbrains.buildServer.util.TestFor;
import org.jmock.Expectations;
import org.jmock.Mockery;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.util.*;

/**
 * Class {@code BuildFeatureParametersProviderTest}
 *
 * Contains tests for {@code BuildFeatureParametersProvider} class
 *
 * @author Oleg Rybak (oleg.rybak@jetbrains.com)
 */
@TestFor(testForClass = BuildFeatureParametersProvider.class)
public class BuildFeatureParametersProviderTest extends BaseTestCase {

  private Mockery myMockery;

  /** Build under test */
  private SBuild myBuild;

  /** BuildType under test */
  private SBuildType myBuildType;

  /** SharedResources build feature descriptor */
  private SBuildFeatureDescriptor myBuildFeatureDescriptor;

  /** Build feature parameters */
  private Map<String, String> myNonEmptyParamMap;

  /** Empty build feature parameters */
  private final Map<String, String> myEmptyParamMap = Collections.emptyMap();

  /** Class under test */
  private BuildFeatureParametersProvider myBuildFeatureParametersProvider;

  @BeforeMethod
  @Override
  protected void setUp() throws Exception {
    super.setUp();
    myMockery = new Mockery();
    myBuild = myMockery.mock(SBuild.class);
    myBuildType = myMockery.mock(SBuildType.class);
    myBuildFeatureDescriptor = myMockery.mock(SBuildFeatureDescriptor.class);
    myNonEmptyParamMap = new HashMap<String, String>() {{
      put("param1_key", "param1_value");
      put("param2_key", "param2_value");
    }};
    myBuildFeatureParametersProvider = new BuildFeatureParametersProvider();
  }

  /**
   * Tests parameters provider when feature is not present
   * @throws Exception if something goes wrong
   */
  @Test
  public void testNoFeaturePresent() throws Exception {
    myMockery.checking(new Expectations() {{
      oneOf(myBuild).getBuildType();
      will(returnValue(myBuildType));

      oneOf(myBuildType).getBuildFeatures();
      will(returnValue(Collections.emptyList()));
    }});

    Map<String, String> result = myBuildFeatureParametersProvider.getParameters(myBuild, false);
    assertNotNull(result);
    int size = result.size();
    assertEquals("Expected empty result. Actual size is [" + size + "]" ,0 , size);
    myMockery.assertIsSatisfied();
  }

  /**
   * Tests parameters provider in emulation mode
   * @throws Exception if something goes wrong
   */
  @Test
  public void testInEmulationMode() throws Exception {
    Map<String, String> result = myBuildFeatureParametersProvider.getParameters(myBuild, true);
    assertNotNull(result);
    int size = result.size();
    assertEquals("Expected empty result. Actual size is [" + size + "]" ,0 , size);
    myMockery.assertIsSatisfied();
  }

  /**
   * Test parameters provider when none locks are taken
   * @throws Exception if something goes wrong
   */
  @Test
  public void testEmptyParams() throws Exception {
    final Collection<SBuildFeatureDescriptor> descriptors = new ArrayList<SBuildFeatureDescriptor>() {{
      add(myBuildFeatureDescriptor);
    }};
    myMockery.checking(new Expectations() {{
      oneOf(myBuild).getBuildType();
      will(returnValue(myBuildType));

      oneOf(myBuildType).getBuildFeatures();
      will(returnValue(descriptors));

      oneOf(myBuildFeatureDescriptor).getType();
      will(returnValue(SharedResourcesPluginConstants.FEATURE_TYPE));

      oneOf(myBuildFeatureDescriptor).getParameters();
      will(returnValue(myEmptyParamMap));

    }});

    Map<String, String> result = myBuildFeatureParametersProvider.getParameters(myBuild, false);
    assertNotNull(result);
    int size = result.size();
    assertEquals("Expected empty result. Actual size is [" + size + "]" , 0, size);
    myMockery.assertIsSatisfied();

  }

  /**
   * Test parameters provider when some locks are taken
   * @throws Exception if something goes wrong
   */
  @Test
  public void testNonEmptyParams() throws Exception {
    final Collection<SBuildFeatureDescriptor> descriptors = new ArrayList<SBuildFeatureDescriptor>() {{
      add(myBuildFeatureDescriptor);
    }};
    myMockery.checking(new Expectations() {{
      oneOf(myBuild).getBuildType();
      will(returnValue(myBuildType));

      oneOf(myBuildType).getBuildFeatures();
      will(returnValue(descriptors));

      oneOf(myBuildFeatureDescriptor).getType();
      will(returnValue(SharedResourcesPluginConstants.FEATURE_TYPE));

      oneOf(myBuildFeatureDescriptor).getParameters();
      will(returnValue(myNonEmptyParamMap));

    }});

    Map<String, String> result = myBuildFeatureParametersProvider.getParameters(myBuild, false);
    assertNotNull(result);
    int size = result.size();
    assertEquals("Expected empty result. Actual size is [" + size + "]" , myNonEmptyParamMap.size(), size);
    assertSameElements(result.entrySet(), myNonEmptyParamMap.entrySet());
    myMockery.assertIsSatisfied();
  }

}
