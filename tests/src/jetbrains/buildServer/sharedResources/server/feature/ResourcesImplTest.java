package jetbrains.buildServer.sharedResources.server.feature;

import jetbrains.buildServer.BaseTestCase;
import jetbrains.buildServer.serverSide.ProjectManager;
import jetbrains.buildServer.serverSide.SProject;
import jetbrains.buildServer.sharedResources.TestUtils;
import jetbrains.buildServer.sharedResources.model.resources.Resource;
import jetbrains.buildServer.sharedResources.model.resources.ResourceFactory;
import jetbrains.buildServer.sharedResources.server.exceptions.DuplicateResourceException;
import jetbrains.buildServer.sharedResources.server.project.ResourceProjectFeatures;
import jetbrains.buildServer.util.TestFor;
import org.jmock.Expectations;
import org.jmock.Mockery;
import org.jmock.lib.legacy.ClassImposteriser;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.util.*;

/**
 * Created with IntelliJ IDEA.
 *
 * @author Oleg Rybak (oleg.rybak@jetbrains.com)
 */
@TestFor(testForClass = {Resources.class, ResourcesImpl.class})
public class ResourcesImplTest extends BaseTestCase {

  private Mockery m;

  private ResourceProjectFeatures myResourceProjectFeatures;

  private ProjectManager myProjectManager;

  private SProject myProject;

  private SProject myRootProject;

  private Map<String, Resource> myProjectResourceMap;

  private Map<String, Resource> myRootResourceMap;

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

    myProjectManager = m.mock(ProjectManager.class);
    myProject = m.mock(SProject.class, "currentProject");
    myRootProject = m.mock(SProject.class, "rootProject");

    myResourceProjectFeatures = m.mock(ResourceProjectFeatures.class);
    resources = new ResourcesImpl(myProjectManager, myResourceProjectFeatures);

    myProjectResourceMap = new HashMap<String, Resource>() {{
      put("resource1", ResourceFactory.newQuotedResource("resource1", myProjectId, "resource1", 1, true));
      put("resource2", ResourceFactory.newInfiniteResource("resource2", myProjectId, "resource2", true));
      put("resource3", ResourceFactory.newCustomResource("resource3", myProjectId, "resource3", Arrays.asList("value1", "value2", "value3"), true));
    }};

    myRootResourceMap = new HashMap<String, Resource>() {{
      put("root_resource", ResourceFactory.newInfiniteResource("root_resource", myRootProjectId, "root_resource", true));
    }};
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
    final Resource resource = ResourceFactory.newQuotedResource("new_resource1", myRootProjectId, "new_resource1", 1, true);
    m.checking(new Expectations() {{
      oneOf(myResourceProjectFeatures).addResource(myRootProject, resource.getParameters());
    }});
    resources.addResource(myRootProject, resource);
  }

  /**
   * Adds resource to subproject
   * @throws Exception if something goes wrong
   */
  @Test
  public void testAddResourceToSubproject_Success() throws Exception {
    final Resource resource = ResourceFactory.newInfiniteResource("infinite_resource", myProjectId, "infinite_resource", true);
    m.checking(new Expectations() {{
      oneOf(myResourceProjectFeatures).addResource(myProject, resource.getParameters());
    }});
    resources.addResource(myProject, resource);
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
    final Resource resource = ResourceFactory.newInfiniteResource("root_resource", myProjectId, "root_resource", true);
    m.checking(new Expectations() {{
      oneOf(myResourceProjectFeatures).addResource(myProject, resource.getParameters());
      will(throwException(new DuplicateResourceException("root_resource")));
    }});
    resources.addResource(myProject, resource);
  }

  /**
   * Tests name change for existing resource
   * @throws Exception if something goes wrong
   */
  @Test (enabled = false) // todo: move
  public void testEditResource_Success() throws Exception {
    final String name = "resource1";
    final Resource resource = ResourceFactory.newQuotedResource("resource1", myProjectId, "resource1_newName", 1, true);
    resources.editResource(myProject, name, resource);
  }

  /**
   * Tries to change the name of the existing resource to the name of
   * another existing resource.
   * {@code DuplicateResourceException} is expected to be thrown.
   *
   * @throws Exception if something goes wrong
   */
  @Test (expectedExceptions = DuplicateResourceException.class, enabled = false) // todo move
  public void testEditResource_Fail() throws Exception {
    final String oldName = "resource1";
    final String newName = "resource2";
    final Resource resource = ResourceFactory.newInfiniteResource("resource1", myProjectId, newName, true);
    resources.editResource(myProject, oldName, resource);
  }


  @Test (enabled = false) //todo move
  public void testDeleteResource() {
    final Resource resource = myProjectResourceMap.get("resource1");
    assertNotNull(resource);
    resources.deleteResource(myProject, resource.getName());
  }

  @Test
  public void testAsMap() {
    m.checking(new Expectations() {{
      oneOf(myProjectManager).findProjectById(myProjectId);
      will(returnValue(myProject));

      oneOf(myResourceProjectFeatures).asMap(myProject);
      will(returnValue(myProjectResourceMap));
    }});

    final Map<String, Resource> result = resources.asMap(myProjectId);
    assertNotNull(result);
    assertEquals(myProjectResourceMap.size(), result.size());
  }

  //@Test (enabled = false) // todo: move to project resource features test
  //@TestFor(issues = "TW-37406")
  //public void testAsProjectResourceMap_PreserveOrder() {
  //  m.checking(new Expectations() {{
  //    oneOf(myProjectManager).findProjectById(myProjectId);
  //    will(returnValue(myProject));
  //
  //    allowing(myProject).getProjectId();
  //    will(returnValue(myProjectId));
  //
  //    oneOf(myProject).getProjectPath();
  //    will(returnValue(Arrays.asList(myRootProject, myProject)));
  //
  //    allowing(myRootProject).getProjectId();
  //    will(returnValue(myRootProjectId));
  //  }});
  //
  //  final Map<SProject, Map<String, Resource>> result = resources.asProjectResourceMap(myProjectId);
  //  assertEquals(2, result.size());
  //  Iterator<SProject> it = result.keySet().iterator();
  //  assertTrue(it.hasNext());
  //  SProject p = it.next();
  //  assertEquals(myProjectId, p.getProjectId());
  //  assertTrue(it.hasNext());
  //  p = it.next();
  //  assertEquals(myRootProjectId, p.getProjectId());
  //}

  @Test (enabled = false) // todo: move
  public void testGetCount_SingleProject() {
    m.checking(new Expectations() {{
      oneOf(myProjectManager).findProjectById(myRootProjectId);
      will(returnValue(myRootProject));

      oneOf(myRootProject).getProjectPath();
      will(returnValue(Collections.singletonList(myRootProject)));

      oneOf(myRootProject).getProjectId();
      will(returnValue(myRootProjectId));

    }});

    final int count = resources.getCount(myRootProject);
    assertEquals(myRootResourceMap.size(), count);
  }

  @Test (enabled = false) // todo: move
  public void testGetCount_Hierarchy() {
    //setupGetCountHierarchy(myRootResourceMap, myProjectResourceMap);
    final int count = resources.getCount(myProject);
    assertEquals(myRootResourceMap.size() + myProjectResourceMap.size(), count);
  }

  @Test (enabled = false) // todo: move
  public void testGetCount_HierarchyOverriding() throws Exception {
    final Map<String, Resource> baseMap = new HashMap<String, Resource>() {{
      put("RESOURCE", ResourceFactory.newInfiniteResource("RESOURCE_root", myRootProjectId, "RESOURCE", true));
    }};

    final Map<String, Resource> inheritedMap = new HashMap<String, Resource>() {{
      put("RESOURCE", ResourceFactory.newInfiniteResource("RESOURCE_project", myProjectId, "RESOURCE", true));
    }};
    //setupGetCountHierarchy(baseMap, inheritedMap);
    final int count = resources.getCount(myProject);
    assertEquals(1, count);
  }
}
