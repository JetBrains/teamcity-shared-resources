/*
 * Copyright 2000-2014 JetBrains s.r.o.
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
import org.jmock.Expectations;
import org.jmock.Mockery;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import javax.servlet.http.HttpServletRequest;
import java.util.Arrays;
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

  private ResourceFactory myResourceFactory;

  @Override
  @BeforeMethod
  public void setUp() throws Exception {
    super.setUp();
    m = new Mockery();
    myRequest = m.mock(HttpServletRequest.class);
    myHelper = new ResourceHelper();
    myResourceFactory = ResourceFactory.getFactory(PROJECT_ID);
  }

  @Override
  @AfterMethod
  public void tearDown() throws Exception {
    super.tearDown();
    m.assertIsSatisfied();
  }

  @Test
  public void testGetResourceFromRequest_Infinite() throws Exception {
    m.checking(new Expectations() {{
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
  }

  @Test
  public void testGetResourceFromRequest_Quoted() throws Exception {
    m.checking(new Expectations() {{
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
  }

  @Test
  public void testGetResourceFromRequest_Custom() throws Exception {
    m.checking(new Expectations() {{
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
  }

  @Test
  public void testGetResourceFromRequest_Invalid_Quota() throws Exception {
    m.checking(new Expectations() {{
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
  public void testGetResourceFromRequest_Invalid_Type() throws Exception {
    m.checking(new Expectations() {{
      oneOf(myRequest).getParameter(SharedResourcesPluginConstants.WEB.PARAM_RESOURCE_NAME);
      will(returnValue(RESOURCE_NAME));

      oneOf(myRequest).getParameter(SharedResourcesPluginConstants.WEB.PARAM_RESOURCE_TYPE);
      will(returnValue("unsupported_type"));
    }});
    final Resource rc = myHelper.getResourceFromRequest(PROJECT_ID, myRequest);
    assertNull(rc);
  }

  @Test
  public void testGetResourceInState() throws Exception {
    {
      final Resource rc = myResourceFactory.newInfiniteResource(RESOURCE_NAME, true);
      final Resource result = myHelper.getResourceInState(PROJECT_ID, rc, false);
      assertEquals(rc, result);
      assertFalse(result.isEnabled());
    }

    {
      final Resource rc = myResourceFactory.newQuotedResource(RESOURCE_NAME, 1, true);
      final Resource result = myHelper.getResourceInState(PROJECT_ID, rc, false);
      assertEquals(rc, result);
      assertFalse(result.isEnabled());
    }

    {
      final Resource rc = myResourceFactory.newCustomResource(RESOURCE_NAME, Arrays.asList("value1"), true);
      final Resource result = myHelper.getResourceInState(PROJECT_ID, rc, false);
      assertEquals(rc, result);
      assertFalse(result.isEnabled());
    }

  }




}
