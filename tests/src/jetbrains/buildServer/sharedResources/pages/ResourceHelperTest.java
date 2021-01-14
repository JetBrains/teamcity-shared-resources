/*
 * Copyright 2000-2021 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package jetbrains.buildServer.sharedResources.pages;

import jetbrains.buildServer.BaseTestCase;
import jetbrains.buildServer.sharedResources.SharedResourcesPluginConstants;
import jetbrains.buildServer.sharedResources.model.resources.*;
import jetbrains.buildServer.util.TestFor;
import org.jetbrains.annotations.NotNull;
import org.jmock.Expectations;
import org.jmock.Mockery;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import javax.servlet.http.HttpServletRequest;
import java.util.Collections;
import java.util.List;

/**
 * Created with IntelliJ IDEA.
 *
 * @author Oleg Rybak (oleg.rybak@jetbrains.com)
 */
@TestFor(testForClass = ResourceHelper.class)
public class ResourceHelperTest extends BaseTestCase {

  private static final String RESOURCE_NAME = "resource name";

  private static final String PROJECT_ID = "PROJECT_ID";

  private Mockery m;

  private HttpServletRequest myRequest;

  private ResourceHelper myHelper;

  @Override
  @BeforeMethod
  public void setUp() throws Exception {
    super.setUp();
    m = new Mockery();
    myRequest = m.mock(HttpServletRequest.class);
    myHelper = new ResourceHelper();
  }

  @Override
  @AfterMethod
  public void tearDown() throws Exception {
    super.tearDown();
    m.assertIsSatisfied();
  }

  @Test
  public void testGetResourceFromRequest_Infinite() {
    m.checking(new Expectations() {{
      oneOf(myRequest).getParameter(SharedResourcesPluginConstants.WEB.PARAM_RESOURCE_ID);
      will(returnValue("my_id"));

      oneOf(myRequest).getParameter(SharedResourcesPluginConstants.WEB.PARAM_RESOURCE_NAME);
      will(returnValue(RESOURCE_NAME));

      oneOf(myRequest).getParameter(SharedResourcesPluginConstants.WEB.PARAM_RESOURCE_TYPE);
      will(returnValue(ResourceType.QUOTED.name()));

      oneOf(myRequest).getParameter(SharedResourcesPluginConstants.WEB.PARAM_RESOURCE_QUOTA);
      will(returnValue(null));
    }});
    final Resource rc = myHelper.getResourceFromRequest(PROJECT_ID, myRequest);
    assertNotNull(rc);
    assertEquals(RESOURCE_NAME, rc.getName());
    assertTrue(rc.isEnabled());
    assertTrue(((QuotedResource) rc).isInfinite());
    validateResourceParameters(rc);
  }

  @Test
  public void testGetResourceFromRequest_Quoted() {
    m.checking(new Expectations() {{
      oneOf(myRequest).getParameter(SharedResourcesPluginConstants.WEB.PARAM_RESOURCE_ID);
      will(returnValue("my_id"));

      oneOf(myRequest).getParameter(SharedResourcesPluginConstants.WEB.PARAM_RESOURCE_NAME);
      will(returnValue(RESOURCE_NAME));

      oneOf(myRequest).getParameter(SharedResourcesPluginConstants.WEB.PARAM_RESOURCE_TYPE);
      will(returnValue(ResourceType.QUOTED.name()));

      oneOf(myRequest).getParameter(SharedResourcesPluginConstants.WEB.PARAM_RESOURCE_QUOTA);
      will(returnValue("1"));
    }});
    final Resource rc = myHelper.getResourceFromRequest(PROJECT_ID, myRequest);
    assertNotNull(rc);
    assertEquals(RESOURCE_NAME, rc.getName());
    assertTrue(rc.isEnabled());
    assertFalse(((QuotedResource) rc).isInfinite());
    assertEquals(1, ((QuotedResource) rc).getQuota());
    validateResourceParameters(rc);
  }

  @Test
  public void testGetResourceFromRequest_Custom() {
    m.checking(new Expectations() {{
      oneOf(myRequest).getParameter(SharedResourcesPluginConstants.WEB.PARAM_RESOURCE_ID);
      will(returnValue("my_id"));

      oneOf(myRequest).getParameter(SharedResourcesPluginConstants.WEB.PARAM_RESOURCE_NAME);
      will(returnValue(RESOURCE_NAME));

      oneOf(myRequest).getParameter(SharedResourcesPluginConstants.WEB.PARAM_RESOURCE_TYPE);
      will(returnValue(ResourceType.CUSTOM.name()));

      oneOf(myRequest).getParameter(SharedResourcesPluginConstants.WEB.PARAM_RESOURCE_VALUES);
      will(returnValue("value1\r\nvalue2\r\nvalue3"));
    }});
    final Resource rc = myHelper.getResourceFromRequest(PROJECT_ID, myRequest);
    assertNotNull(rc);
    assertEquals(RESOURCE_NAME, rc.getName());
    assertTrue(rc.isEnabled());
    final List<String> values = ((CustomResource)rc).getValues();
    assertNotNull(values);
    assertNotEmpty(values);
    assertEquals(3, values.size());
    assertContains(values, "value1", "value2", "value3");
    validateResourceParameters(rc);
  }

  @Test
  public void testGetResourceFromRequest_Invalid_Quota() {
    m.checking(new Expectations() {{
      oneOf(myRequest).getParameter(SharedResourcesPluginConstants.WEB.PARAM_RESOURCE_ID);
      will(returnValue("my_id"));

      oneOf(myRequest).getParameter(SharedResourcesPluginConstants.WEB.PARAM_RESOURCE_NAME);
      will(returnValue(RESOURCE_NAME));

      oneOf(myRequest).getParameter(SharedResourcesPluginConstants.WEB.PARAM_RESOURCE_TYPE);
      will(returnValue(ResourceType.QUOTED.name()));

      oneOf(myRequest).getParameter(SharedResourcesPluginConstants.WEB.PARAM_RESOURCE_QUOTA);
      will(returnValue("some value"));
    }});
    final Resource rc = myHelper.getResourceFromRequest(PROJECT_ID, myRequest);
    assertNull(rc);
  }

  @Test
  public void testGetResourceFromRequest_Invalid_Type() {
    m.checking(new Expectations() {{
      oneOf(myRequest).getParameter(SharedResourcesPluginConstants.WEB.PARAM_RESOURCE_ID);
      will(returnValue("my_id"));

      oneOf(myRequest).getParameter(SharedResourcesPluginConstants.WEB.PARAM_RESOURCE_NAME);
      will(returnValue(RESOURCE_NAME));

      oneOf(myRequest).getParameter(SharedResourcesPluginConstants.WEB.PARAM_RESOURCE_TYPE);
      will(returnValue("unsupported_type"));
    }});
    final Resource rc = myHelper.getResourceFromRequest(PROJECT_ID, myRequest);
    assertNull(rc);
  }

  @Test
  public void testGetResourceInState() {
    {
      final Resource rc = ResourceFactory.newInfiniteResource(RESOURCE_NAME + "id", PROJECT_ID, RESOURCE_NAME, true);
      final Resource result = myHelper.getResourceInState(PROJECT_ID, rc, false);
      assertEquals(rc, result);
      assertFalse(result.isEnabled());
      validateResourceParameters(result);
    }

    {
      final Resource rc = ResourceFactory.newQuotedResource(RESOURCE_NAME + "id", PROJECT_ID, RESOURCE_NAME, 1, true);
      final Resource result = myHelper.getResourceInState(PROJECT_ID, rc, false);
      assertEquals(rc, result);
      assertFalse(result.isEnabled());
      validateResourceParameters(result);
    }

    {
      final Resource rc = ResourceFactory.newCustomResource(RESOURCE_NAME + "id", PROJECT_ID, RESOURCE_NAME, Collections.singletonList("value1"), true);
      final Resource result = myHelper.getResourceInState(PROJECT_ID, rc, false);
      assertEquals(rc, result);
      assertFalse(result.isEnabled());
      validateResourceParameters(result);
    }
  }

  private void validateResourceParameters(@NotNull final Resource resource) {
    assertFalse(resource.getParameters().keySet().contains("id"));
  }
}
