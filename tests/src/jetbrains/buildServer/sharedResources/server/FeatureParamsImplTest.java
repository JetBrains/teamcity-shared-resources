package jetbrains.buildServer.sharedResources.server;

import jetbrains.buildServer.BaseTestCase;
import jetbrains.buildServer.sharedResources.model.Lock;
import jetbrains.buildServer.sharedResources.model.LockType;
import jetbrains.buildServer.sharedResources.server.feature.Locks;
import jetbrains.buildServer.util.TestFor;
import org.jmock.Expectations;
import org.jmock.Mockery;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static jetbrains.buildServer.sharedResources.server.FeatureParams.LOCKS_FEATURE_PARAM_KEY;
import static jetbrains.buildServer.sharedResources.server.FeatureParamsImpl.*;

/**
 * Class {@code FeatureParamsImplTest}
 *
 * Contains tests for default implementation of FeatureParams
 *
 * @author Oleg Rybak (oleg.rybak@jetbrains.com)
 */

@SuppressWarnings("UnusedShould")
@TestFor(testForClass = {FeatureParams.class, FeatureParamsImpl.class})
public class FeatureParamsImplTest extends BaseTestCase {

  /** Factory for mocks*/
  private Mockery m;

  /** Class under test */
  private FeatureParams myFeatureParams;

  private Locks myLocks;

  @BeforeMethod
  @Override
  protected void setUp() throws Exception {
    m = new Mockery();
    myLocks = m.mock(Locks.class);
    myFeatureParams = new FeatureParamsImpl(myLocks);
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
    final Map<String, String> params = new HashMap<String, String>() {{
      put("$$$some_other_param_name$$$", "value");
    }};

    m.checking(new Expectations() {{
      oneOf(myLocks).fromFeatureParameters(params);
      will(returnValue(Collections.<String, Lock>emptyMap()));
    }});
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
    final Map<String, Lock> locks = new HashMap<String, Lock>();
    locks.put("lock1", new Lock("lock1", LockType.READ));
    locks.put("lock2", new Lock("lock2", LockType.WRITE));
    final Map<String, String> params = new HashMap<String, String>();
    params.put(LOCKS_FEATURE_PARAM_KEY, "lock1 readLock\nlock2 writeLock");
    m.checking(new Expectations() {{
      oneOf(myLocks).fromFeatureParameters(params);
      will(returnValue(locks));
    }});
    final String str = myFeatureParams.describeParams(params);
    assertTrue(str.contains(READ_LOCKS_MESSAGE) && str.contains(WRITE_LOCKS_MESSAGE));
    m.assertIsSatisfied();
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
