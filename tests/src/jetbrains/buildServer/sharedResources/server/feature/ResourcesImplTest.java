package jetbrains.buildServer.sharedResources.server.feature;

import jetbrains.buildServer.BaseTestCase;
import jetbrains.buildServer.serverSide.ProjectManager;
import jetbrains.buildServer.serverSide.SProject;
import jetbrains.buildServer.serverSide.settings.ProjectSettingsManager;
import jetbrains.buildServer.sharedResources.TestUtils;
import jetbrains.buildServer.sharedResources.model.resources.Resource;
import jetbrains.buildServer.sharedResources.model.resources.ResourceFactory;
import jetbrains.buildServer.sharedResources.server.exceptions.DuplicateResourceException;
import jetbrains.buildServer.sharedResources.settings.PluginProjectSettings;
import jetbrains.buildServer.util.TestFor;
import org.jmock.Expectations;
import org.jmock.Mockery;
import org.jmock.lib.legacy.ClassImposteriser;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.util.*;

import static jetbrains.buildServer.sharedResources.SharedResourcesPluginConstants.SERVICE_NAME;

/**
 * Created with IntelliJ IDEA.
 *
 * @author Oleg Rybak (oleg.rybak@jetbrains.com)
 */
@TestFor(testForClass = {Resources.class, ResourcesImpl.class})
public class ResourcesImplTest extends BaseTestCase {

  private Mockery m;

  private ProjectSettingsManager myProjectSettingsManager;

  private PluginProjectSettings myProjectSettings;

  private ProjectManager myProjectManager;

  private SProject myProject;

  private SProject myRootProject;

  private PluginProjectSettings myRootSettings;

  private Map<String, Resource> myProjectResourceMap;

  private Map<String, Resource> myRootResourceMap;

  private Map<String, Resource> myEmptyResourceMap;

  private ResourceFactory myProjectResourceFactory;

  private ResourceFactory myRootResourceFactory;

  /**
   * Class under test
   */
  private ResourcesImpl resources;

  private final String myProjectId = TestUtils.generateRandomName();

  private final String myRootProjectId = "<ROOT>";

  @BeforeMethod
  @Override
  protected void setUp() throws Exception {
    super.setUp();
    m = new Mockery() {{
      setImposteriser(ClassImposteriser.INSTANCE);
    }};

    myProjectSettingsManager = m.mock(ProjectSettingsManager.class);
    myProjectManager = m.mock(ProjectManager.class);

    myProjectSettings = m.mock(PluginProjectSettings.class, "ProjectSettings");
    myRootSettings = m.mock(PluginProjectSettings.class, "RootSettings");

    myProject = m.mock(SProject.class, "currentProject");
    myRootProject = m.mock(SProject.class, "rootProject");

    resources = new ResourcesImpl(myProjectSettingsManager, myProjectManager);

    myRootResourceFactory = ResourceFactory.getFactory(myRootProjectId);
    myProjectResourceFactory = ResourceFactory.getFactory(myProjectId);

    myProjectResourceMap = new HashMap<String, Resource>() {{
      put("resource1", myProjectResourceFactory.newQuotedResource("resource1", 1, true));
      put("resource2", myProjectResourceFactory.newInfiniteResource("resource2", true));
      put("resource3", myProjectResourceFactory.newCustomResource("resource3", Arrays.asList("value1", "value2", "value3"), true));
    }};

    myRootResourceMap = new HashMap<String, Resource>() {{
      put("root_resource", myRootResourceFactory.newInfiniteResource("root_resource", true));
    }};

    myEmptyResourceMap = new HashMap<>();
  }

  @Override
  @AfterMethod
  public void tearDown() throws Exception {
    super.tearDown();
    m.assertIsSatisfied();
  }

  /**
   * Adds new resource to root project
   * @throws Exception if something goes wrong
   */
  @Test
  public void testAddResource_Success() throws Exception {
    final Resource resource = myRootResourceFactory.newQuotedResource("new_resource1", 1, true);
    setupExistingResources(resource);
    m.checking(new Expectations() {{
      oneOf(myProjectSettings).addResource(resource);
    }});
    resources.addResource(resource);
  }

  /**
   * Adds resource to subproject
   * @throws Exception if something goes wrong
   */
  @Test
  public void testAddResourceToSubproject_Success() throws Exception {
    final Resource resource = myProjectResourceFactory.newInfiniteResource("infinite_resource", true);
    setupExistingResources(resource);
    m.checking(new Expectations() {{
      oneOf(myProjectSettings).addResource(resource);
    }});
    resources.addResource(resource);
  }

  /**
   * Adds new resource to project. Resource with same name exists in {@code Root} project.
   * Only resources of current project must be checked for name collision
   *
   * @throws Exception if something goes wrong
   */
  @Test
  public void testAddResource_RespectHierarchy_Success() throws Exception {
    final Resource resource = myProjectResourceFactory.newInfiniteResource("root_resource", true);
    setupExistingResources(resource);
    m.checking(new Expectations() {{
      oneOf(myProjectSettings).addResource(resource);
    }});
    resources.addResource(resource);
  }

  /**
   * Tries to add resource with the same name as existing to {@code Root} project.
   * {@code DuplicateResourceException} is expected to be thrown.
   *
   * @see jetbrains.buildServer.sharedResources.server.exceptions.DuplicateResourceException
   * @throws Exception if something goes wrong
   */
  @Test (expectedExceptions = DuplicateResourceException.class)
  public void testAddResource_NameConflict_Fail() throws Exception {
    final Resource resource = myRootResourceFactory.newInfiniteResource("root_resource", true);
    setupForFail(myRootSettings, myRootResourceMap, resource);
    resources.addResource(resource);
  }

  /**
   * Tests name change for existing resource
   * @throws Exception if something goes wrong
   */
  @Test
  public void testEditResource_Success() throws Exception {
    final String name = "resource1";
    final Resource resource = myProjectResourceFactory.newQuotedResource("resource1_newName", 1, true);

    m.checking(new Expectations() {{
      atLeast(2).of(myProjectSettingsManager).getSettings(resource.getProjectId(), SERVICE_NAME);
      will(returnValue(myProjectSettings));

      oneOf(myProjectSettings).getResourceMap();
      will(returnValue(myProjectResourceMap));

      oneOf(myProjectSettings).editResource(name, resource);
    }});
    resources.editResource(myProjectId, name, resource);
  }

  /**
   * Tries to change the name of the existing resource to the name of
   * another existing resource.
   * {@code DuplicateResourceException} is expected to be thrown.
   *
   * @throws Exception if something goes wrong
   */
  @Test (expectedExceptions = DuplicateResourceException.class)
  public void testEditResource_Fail() throws Exception {
    final String oldName = "resource1";
    final String newName = "resource2";
    final Resource resource = myProjectResourceFactory.newInfiniteResource(newName, true);
    setupForFail(myProjectSettings, myProjectResourceMap, resource);
    resources.editResource(myProjectId, oldName, resource);
  }


  @Test
  public void testDeleteResource() {
    final Resource resource = myProjectResourceMap.get("resource1");
    assertNotNull(resource);

    m.checking(new Expectations() {{
      oneOf(myProjectSettingsManager).getSettings(myProjectId, SERVICE_NAME);
      will(returnValue(myProjectSettings));

      oneOf(myProjectSettings).deleteResource(resource.getName());
    }});

    resources.deleteResource(myProjectId, resource.getName());
    m.assertIsSatisfied();
  }

  @Test
  public void testAsMap() {
    m.checking(new Expectations() {{
      oneOf(myProjectSettingsManager).getSettings(myProjectId, SERVICE_NAME);
      will(returnValue(myProjectSettings));

      oneOf(myProjectSettings).getResourceMap();
      will(returnValue(myProjectResourceMap));

      oneOf(myProjectManager).findProjectById(myProjectId);
      will(returnValue(myProject));

      oneOf(myProject).getProjectId();
      will(returnValue(myProjectId));

      oneOf(myProject).getProjectPath();
      will(returnValue(Collections.singletonList(myProject)));
    }});

    final Map<String, Resource> result = resources.asMap(myProjectId);
    assertNotNull(result);
    assertEquals(myProjectResourceMap.size(), result.size());
  }

  @Test
  @TestFor(issues = "TW-37406")
  public void testAsProjectResourceMap_PreserveOrder() {
    m.checking(new Expectations() {{
      oneOf(myProjectSettingsManager).getSettings(myProjectId, SERVICE_NAME);
      will(returnValue(myProjectSettings));

      oneOf(myProjectSettings).getResourceMap();
      will(returnValue(myProjectResourceMap));

      oneOf(myProjectManager).findProjectById(myProjectId);
      will(returnValue(myProject));

      allowing(myProject).getProjectId();
      will(returnValue(myProjectId));

      oneOf(myProject).getProjectPath();
      will(returnValue(Arrays.asList(myRootProject, myProject)));

      allowing(myRootProject).getProjectId();
      will(returnValue(myRootProjectId));

      oneOf(myProjectSettingsManager).getSettings(myRootProjectId, SERVICE_NAME);
      will(returnValue(myRootSettings));

      oneOf(myRootSettings).getResourceMap();
      will(returnValue(myRootResourceMap));
    }});

    final Map<SProject, Map<String, Resource>> result = resources.asProjectResourceMap(myProjectId);
    assertEquals(2, result.size());
    Iterator<SProject> it = result.keySet().iterator();
    assertTrue(it.hasNext());
    SProject p = it.next();
    assertEquals(myProjectId, p.getProjectId());
    assertTrue(it.hasNext());
    p = it.next();
    assertEquals(myRootProjectId, p.getProjectId());
  }

  @Test
  public void testGetCount_SingleProject() {
    m.checking(new Expectations() {{
      oneOf(myProjectManager).findProjectById(myRootProjectId);
      will(returnValue(myRootProject));

      oneOf(myRootProject).getProjectPath();
      will(returnValue(Collections.singletonList(myRootProject)));

      oneOf(myRootProject).getProjectId();
      will(returnValue(myRootProjectId));

      oneOf(myProjectSettingsManager).getSettings(myRootProjectId, SERVICE_NAME);
      will(returnValue(myRootSettings));

      oneOf(myRootSettings).getResourceMap();
      will(returnValue(myRootResourceMap));

    }});

    final int count = resources.getCount(myRootProjectId);
    assertEquals(myRootResourceMap.size(), count);
  }

  @Test
  public void testGetCount_Hierarchy() {
    setupGetCountHierarchy(myRootResourceMap, myProjectResourceMap);
    final int count = resources.getCount(myProjectId);
    assertEquals(myRootResourceMap.size() + myProjectResourceMap.size(), count);
  }

  @Test
  public void testGetCount_HierarchyOverriding() throws Exception {
    final Map<String, Resource> baseMap = new HashMap<String, Resource>() {{
      put("RESOURCE", myRootResourceFactory.newInfiniteResource("RESOURCE", true));
    }};

    final Map<String, Resource> inheritedMap = new HashMap<String, Resource>() {{
      put("RESOURCE", myProjectResourceFactory.newInfiniteResource("RESOURCE", true));
    }};
    setupGetCountHierarchy(baseMap, inheritedMap);
    final int count = resources.getCount(myProjectId);
    assertEquals(1, count);
  }

  private void setupGetCountHierarchy(final Map<String, Resource> baseMap, final Map<String, Resource> inheritedMap) {
    m.checking(new Expectations() {{
      oneOf(myProject).getProjectId();
      will(returnValue(myProjectId));

      oneOf(myProject).getProjectPath();
      will(returnValue(Arrays.asList(myProject, myRootProject)));

      oneOf(myRootProject).getProjectId();
      will(returnValue(myRootProjectId));

      oneOf(myProjectSettingsManager).getSettings(myRootProjectId, SERVICE_NAME);
      will(returnValue(myRootSettings));

      oneOf(myRootSettings).getResourceMap();
      will(returnValue(baseMap));

      oneOf(myProjectManager).findProjectById(myProjectId);
      will(returnValue(myProject));

      oneOf(myProjectSettingsManager).getSettings(myProjectId, SERVICE_NAME);
      will(returnValue(myProjectSettings));

      oneOf(myProjectSettings).getResourceMap();
      will(returnValue(inheritedMap));
    }});
  }

  private void setupExistingResources(final Resource resource) {
    m.checking(new Expectations() {{
      atLeast(2).of(myProjectSettingsManager).getSettings(resource.getProjectId(), SERVICE_NAME);
      will(returnValue(myProjectSettings));

      oneOf(myProjectSettings).getResourceMap();
      will(returnValue(myEmptyResourceMap));
    }});
  }

  private void setupForFail(final PluginProjectSettings settings, Map<String, Resource> resources, final Resource resource) {
    m.checking(new Expectations() {{
      oneOf(myProjectSettingsManager).getSettings(resource.getProjectId(), SERVICE_NAME);
      will(returnValue(settings));

      oneOf(settings).getResourceMap();
      will(returnValue(resources));
    }});
  }
}
