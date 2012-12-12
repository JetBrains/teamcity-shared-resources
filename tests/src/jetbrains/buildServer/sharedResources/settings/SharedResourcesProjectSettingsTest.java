/*
 * Copyright 2000-2012 JetBrains s.r.o.
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

package jetbrains.buildServer.sharedResources.settings;

import jetbrains.buildServer.BaseTestCase;
import jetbrains.buildServer.sharedResources.model.resources.Resource;
import jetbrains.buildServer.util.TestFor;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.input.SAXBuilder;
import org.jetbrains.annotations.NotNull;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.io.ByteArrayInputStream;
import java.util.Collection;
import java.util.List;

/**
 * Class {@code SharedResourcesProjectSettingsTest}
 *
 * Contains tests for {@code SharedResourcesProjectSettings}
 *
 * @see SharedResourcesProjectSettings
 *
 * @author Oleg Rybak (oleg.rybak@jetbrains.com)
 */
@TestFor (testForClass = SharedResourcesProjectSettings.class)
public class SharedResourcesProjectSettingsTest extends BaseTestCase {

  private static final String xmlQuota =
          "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                  "<settings>\n" +
                  "  <JetBrains.SharedResources>\n" +
                  "    <resource>\n" +
                  "      <name>resource1</name>\n" +
                  "      <values type=\"quota\">\n" +
                  "        <quota>123</quota>\n" +
                  "      </values>\n" +
                  "    </resource>\n" +
                  "  </JetBrains.SharedResources>\n" +
                  "</settings>\n" +
                  "\n";
  private static final String xmlQuotaInfinite =
          "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                  "<settings>\n" +
                  "  <JetBrains.SharedResources>\n" +
                  "    <resource>\n" +
                  "      <name>resource1</name>\n" +
                  "      <values type=\"quota\">\n" +
                  "        <quota>infinite</quota>\n" +
                  "      </values>\n" +
                  "    </resource>\n" +
                  "  </JetBrains.SharedResources>\n" +
                  "</settings>\n" +
                  "\n";

  private static final String xmlEmpty =
          "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                  "<settings>\n" +
                  "  <JetBrains.SharedResources>\n" +
                  "  </JetBrains.SharedResources>\n" +
                  "</settings>\n" +
                  "\n";




  @BeforeMethod
  @Override
  protected void setUp() throws Exception {
    super.setUp();
  }

  /**
   *
   * @see SharedResourcesProjectSettings#readFrom(org.jdom.Element)
   * @throws Exception if something goes wrong
   */
  @Test
  public void testReadFrom_Empty() throws Exception {
    final Element e = readXml(xmlEmpty);
    final SharedResourcesProjectSettings settings = new SharedResourcesProjectSettings();
    settings.readFrom(e);
    assertEmpty(settings.getResources());
  }



  /**
   * @see SharedResourcesProjectSettings#writeTo(org.jdom.Element)
   * @throws Exception if something goes wrong
   */
  @Test
  public void testWriteTo_Empty() throws Exception {
    // todo: implement
  }

  /**
   * @see SharedResourcesProjectSettings#readFrom(org.jdom.Element)
   * @throws Exception if something goes wrong
   */
  @Test
  public void testReadFrom_Quota() throws Exception {
    final Element e = readXml(xmlQuota);
    final SharedResourcesProjectSettings settings = new SharedResourcesProjectSettings();
    settings.readFrom(e);
    final Collection<Resource> resources = settings.getResources();
    assertNotEmpty(resources);
    assertEquals(1, resources.size());
    final Resource resource = resources.iterator().next();
    assertNotNull(resource);
    assertEquals("resource1", resource.getName());
    assertFalse(resource.isInfinite());
    assertEquals(123, resource.getQuota());
  }

  /**
   * @see SharedResourcesProjectSettings#readFrom(org.jdom.Element)
   * @throws Exception if something goes wrong
   */
  @Test
  public void testReadFrom_Infinite() throws Exception {
    final Element e = readXml(xmlQuotaInfinite);
    final SharedResourcesProjectSettings settings = new SharedResourcesProjectSettings();
    settings.readFrom(e);
    final Collection<Resource> resources = settings.getResources();
    assertNotEmpty(resources);
    assertEquals(1, resources.size());
    final Resource resource = resources.iterator().next();
    assertNotNull(resource);
    assertEquals("resource1", resource.getName());
    assertTrue(resource.isInfinite());
  }

  /**
   * @see SharedResourcesProjectSettings#writeTo(org.jdom.Element)
   * @throws Exception if something goes wrong
   */
  @Test
  public void testWriteTo_Quota() throws Exception {
    // todo: implement
  }

  /**
   * Reads {@code xml} supplied as string, returns subtree responsible
   * for the settings of the current plugin
   *
   * @param xmlString {@code xml} file as string
   * @return subtree responsible for the settings of the current plugin
   * @throws Exception if something goes wrong
   */
  @NotNull
  private static Element readXml(@NotNull String xmlString) throws Exception {
    final SAXBuilder builder = new SAXBuilder();
    final Document document = builder.build(new ByteArrayInputStream(xmlString.getBytes()));
    final Element root = document.getRootElement();
    final List rows = root.getChildren("JetBrains.SharedResources");
    assertNotNull(rows);
    assertEquals(1, rows.size());
    return (Element) rows.get(0);
  }


}
