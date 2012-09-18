package jetbrains.buildServer.sharedResources.server;

import jetbrains.buildServer.BaseTestCase;
import jetbrains.buildServer.serverSide.InvalidProperty;
import jetbrains.buildServer.util.TestFor;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import static jetbrains.buildServer.sharedResources.SharedResourcesPluginConstants.RESOURCE_PARAM_KEY;

/**
 *
 * @author Oleg Rybak
 */
@TestFor (testForClass = BuildFeatureParametersProcessor.class)
public class BuildFeatureParametersProcessorTest extends BaseTestCase {

  private BuildFeatureParametersProcessor processor;
  private Map<String, String> data;

  @BeforeMethod
  @Override
  protected void setUp() throws Exception {
    super.setUp();
    processor = new BuildFeatureParametersProcessor();
    data = new HashMap<String, String>();
  }

  @Test
  public void testEmpty() throws Exception {
    data.put(RESOURCE_PARAM_KEY, "");
    Collection<InvalidProperty> result = processor.process(data);
    assertNotNull(result);
    assertEquals(1, result.size());
    InvalidProperty property = result.iterator().next();
    assertNotNull(property);
    assertEquals(RESOURCE_PARAM_KEY, property.getPropertyName());
    assertEquals(BuildFeatureParametersProcessor.ERROR_EMPTY, property.getInvalidReason());
  }

  @Test
  public void testAllOk() throws Exception {
    data.put(RESOURCE_PARAM_KEY, "lock1(read)\nsome_lock\nlock2(write)\nlock3");
    Collection<InvalidProperty> result = processor.process(data);
    assertNotNull(result);
    assertTrue(result.isEmpty());
  }

  @Test
  public void testNonUniqueSimple() throws Exception {
    data.put(RESOURCE_PARAM_KEY, "name\nname\nname\n");
    Collection<InvalidProperty> result = processor.process(data);
    assertNotNull(result);
    assertEquals(1, result.size());
    InvalidProperty property = result.iterator().next();
    assertNotNull(property);
    assertEquals(RESOURCE_PARAM_KEY, property.getPropertyName());
    assertEquals(BuildFeatureParametersProcessor.ERROR_NON_UNIQUE, property.getInvalidReason());
  }

  @Test
  public void testNonUniqueRW() throws Exception {
    data.put(RESOURCE_PARAM_KEY, "lock1(read)\nlock1(write)\n");
    Collection<InvalidProperty> result = processor.process(data);
    assertNotNull(result);
    assertEquals(1, result.size());
    InvalidProperty property = result.iterator().next();
    assertNotNull(property);
    assertEquals(RESOURCE_PARAM_KEY, property.getPropertyName());
    assertEquals(BuildFeatureParametersProcessor.ERROR_NON_UNIQUE, property.getInvalidReason());
  }

  @Test
  public void testNonUniqueMixed() throws Exception {
    data.put(RESOURCE_PARAM_KEY, "lock1(read)\nlock2(write)\nlock2");
    Collection<InvalidProperty> result = processor.process(data);
    assertNotNull(result);
    assertEquals(1, result.size());
    InvalidProperty property = result.iterator().next();
    assertNotNull(property);
    assertEquals(RESOURCE_PARAM_KEY, property.getPropertyName());
    assertEquals(BuildFeatureParametersProcessor.ERROR_NON_UNIQUE, property.getInvalidReason());
  }

  @Test
  public void testErrorLines() throws Exception {
    data.put(RESOURCE_PARAM_KEY, "lock1(other)\nlock 2(write)\nlo ck");
    Collection<InvalidProperty> result = processor.process(data);
    assertNotNull(result);
    assertEquals(1, result.size());
    InvalidProperty property = result.iterator().next();
    assertNotNull(property);
    assertEquals(RESOURCE_PARAM_KEY, property.getPropertyName());
    String reason = property.getInvalidReason();
    assertNotNull(reason);
    assertTrue(reason.contains(BuildFeatureParametersProcessor.ERROR_WRONG_FORMAT));
  }


}
