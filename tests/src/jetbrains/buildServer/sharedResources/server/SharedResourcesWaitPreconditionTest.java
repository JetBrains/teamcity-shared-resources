package jetbrains.buildServer.sharedResources.server;

import jetbrains.buildServer.BaseTestCase;
import jetbrains.buildServer.util.TestFor;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

/**
 * Class {@code SharedResourcesWaitPreconditionTest}
 *
 * Contains tests for {@code SharedResourcesWaitPrecondition}
 *
 * @see SharedResourcesWaitPrecondition
 * @see SharedResourcesUtils
 * *
 * @author Oleg Rybak (oleg.rybak@jetbrains.com)
 */
@TestFor(testForClass = SharedResourcesWaitPrecondition.class)
public class SharedResourcesWaitPreconditionTest extends BaseTestCase {


  @BeforeMethod
  @Override
  protected void setUp() throws Exception {
    super.setUp();
  }

  @Test
  public void testInEmulationMode() throws Exception {

  }

  @Test
  public void testNoLocksPresent() throws Exception {

  }

  @Test
  public void testLocksPresentSingleBuild() throws Exception {

  }

  @Test
  public void testMultipleBuildsLocksCrossing() throws Exception {

  }

  @Test
  public void testMultipleBuildsLocksNotCrossing() throws Exception {

  }

  @Test
  public void testBuildsFromOtherProjects() throws Exception {

  }
}
