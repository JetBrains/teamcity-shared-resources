package jetbrains.buildServer.sharedResources.server;

import jetbrains.buildServer.BaseTestCase;
import jetbrains.buildServer.util.TestFor;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static jetbrains.buildServer.sharedResources.server.FeatureParams.LOCKS_FEATURE_PARAM_KEY;
import static jetbrains.buildServer.sharedResources.server.FeatureParamsImpl.NO_LOCKS_MESSAGE;
import static jetbrains.buildServer.sharedResources.server.FeatureParamsImpl.READ_LOCKS_MESSAGE;
import static jetbrains.buildServer.sharedResources.server.FeatureParamsImpl.WRITE_LOCKS_MESSAGE;

/**
 * Class {@code FeatureParamsImplTest}
 *
 * Contains tests for default implementation of FeatureParams
 *
 * @author Oleg Rybak (oleg.rybak@jetbrains.com)
 */

@TestFor(testForClass = FeatureParamsImpl.class)
public class FeatureParamsImplTest extends BaseTestCase {

  /**
   * Class under test
   */
  private FeatureParams myFeatureParams;

  @BeforeMethod
  @Override
  protected void setUp() throws Exception {
    myFeatureParams = new FeatureParamsImpl();
  }

  /**
   * Tests parameters description of there are no parameters
   * significant to build feature description
   *
   * @see FeatureParamsImpl#describeParams(java.util.Map)
   * @throws Exception if something goes wrong
   */
  @Test
  public void testEmpty() throws Exception {
    Map<String, String> params = Collections.emptyMap();
    assertContains(myFeatureParams.describeParams(params), NO_LOCKS_MESSAGE);
    params = new HashMap<String, String>() {{
      put("$$$some_other_param_name$$$", "value");
    }};
    assertContains(myFeatureParams.describeParams(params), NO_LOCKS_MESSAGE);
  }


  /**
   * Test various combinations of locks and their descriptions
   *
   * @see FeatureParamsImpl#describeParams(java.util.Map)
   * @throws Exception if something goes wrong
   */
  @Test
  public void testNonEmpty() throws Exception {
    final Map<String, String> params = new HashMap<String, String>();
    params.put(LOCKS_FEATURE_PARAM_KEY, "lock1 readLock\nlock2 readLock");
    assertContains(myFeatureParams.describeParams(params), READ_LOCKS_MESSAGE);
    params.put(LOCKS_FEATURE_PARAM_KEY, "lock1 writeLock\nlock2 writeLock");
    assertContains(myFeatureParams.describeParams(params), WRITE_LOCKS_MESSAGE);
    params.put(LOCKS_FEATURE_PARAM_KEY, "lock1 readLock\nlock2 writeLock");
    final String str = myFeatureParams.describeParams(params);
    assertTrue(str.contains(READ_LOCKS_MESSAGE) && str.contains(WRITE_LOCKS_MESSAGE));
  }

  /**
   * Tests default parameters' contents
   *
   * @see FeatureParamsImpl#getDefault()
   * @throws Exception if something goes wrong
   */
  @Test
  public void testGetDefault() throws Exception {
    assertEquals("", myFeatureParams.getDefault().get(LOCKS_FEATURE_PARAM_KEY));
  }

}
