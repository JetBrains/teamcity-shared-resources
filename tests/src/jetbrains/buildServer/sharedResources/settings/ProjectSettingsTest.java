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
import jetbrains.buildServer.sharedResources.model.resources.CustomResource;
import jetbrains.buildServer.sharedResources.model.resources.QuotedResource;
import jetbrains.buildServer.sharedResources.model.resources.Resource;
import jetbrains.buildServer.sharedResources.model.resources.ResourceType;
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
import java.util.Map;
import java.util.Set;

/**
 * Class {@code ProjectSettingsTest}
 *
 * Contains tests for {@code PluginProjectSettings}
 *
 * @see PluginProjectSettings
 *
 * @author Oleg Rybak (oleg.rybak@jetbrains.com)
 */
@TestFor (testForClass = PluginProjectSettings.class)
public class ProjectSettingsTest extends BaseTestCase {

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

  private static final String xmlCustom =
          "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                  "<settings>\n" +
                  "  <JetBrains.SharedResources>\n" +
                  "    <resource>\n" +
                  "      <name>resource1</name>\n" +
                  "      <values type=\"custom\">\n" +
                  "        <value>value0</value>\n" +
                  "        <value>value1</value>\n" +
                  "        <value>value2</value>\n" +
                  "        <value>value3</value>\n" +
                  "      </values>\n" +
                  "    </resource>\n" +
                  "  </JetBrains.SharedResources>\n" +
                  "</settings>\n" +
                  "\n";
  // mixed quoted (2 types) and custom resources, untrimmed strings etc
  private static final String xmlMixed =
          "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                  "<settings>\n" +
                  "  <JetBrains.SharedResources>\n" +
                  "    <resource>\n" +
                  "      <name>resource0</name>\n" +
                  "      <values type=\"quota\">\n" +
                  "        <quota>infinite</quota>\n" +
                  "      </values>\n" +
                  "    </resource>\n" +
                  "    <resource>\n" +
                  "      <name>resource1</name>\n" +
                  "      <values type=\"custom\">\n" +
                  "        <value>value0</value>\n" +
                  "        <value>value1</value>\n" +
                  "        <value>value2</value>\n" +
                  "        <value>value3</value>\n" +
                  "        <value>value4</value>\n" +
                  "        <value>value5</value>\n" +
                  "      </values>\n" +
                  "    </resource>\n" +
                  "    <resource>\n" +
                  "      <name>resource2</name>\n" +
                  "      <values type=\"quota\">\n" +
                  "        <quota>999</quota>\n" +
                  "      </values>\n" +
                  "    </resource>\n" +
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
   * @see PluginProjectSettings#readFrom(org.jdom.Element)
   * @throws Exception if something goes wrong
   */
  @Test
  public void testReadFrom_Empty() throws Exception {
    final PluginProjectSettings settings = createProjectSettings(xmlEmpty);
    assertNotNull(settings.getResources());
    assertEmpty(settings.getResources());
  }

  /**
   * @see PluginProjectSettings#readFrom(org.jdom.Element)
   * @throws Exception if something goes wrong
   */
  @Test
  public void testReadFrom_Quota() throws Exception {
    final PluginProjectSettings settings = createProjectSettings(xmlQuota);
    validateSettingsQuota(settings);
  }

  /**
   * @see PluginProjectSettings#readFrom(org.jdom.Element)
   * @throws Exception if something goes wrong
   */
  @Test
  public void testReadFrom_Infinite() throws Exception {
    final PluginProjectSettings settings = createProjectSettings(xmlQuotaInfinite);
    validateSettingsInfinite(settings);
  }

  /**
   * Tests reading of custom resource
   *
   * @throws Exception if something goes wrong
   */
  @Test
  public void testReadFrom_Custom() throws Exception {
    final PluginProjectSettings settings = createProjectSettings(xmlCustom);
    validateSettingsCustom(settings);
  }

  /**
   * Tests reading of various kinds of resources
   *
   * @throws Exception if something goes wrong
   */
  @Test
  public void testReadFrom_Mixed() throws Exception  {
    final PluginProjectSettings settings = createProjectSettings(xmlMixed);
    validateSettingsMixed(settings);
  }

  /**
   * @see PluginProjectSettings#writeTo(org.jdom.Element)
   * @throws Exception if something goes wrong
   */
  @Test
  public void testWriteTo_Quota() throws Exception {
    validateSettingsQuota(readWrite(xmlQuota));
  }

  @Test
  public void testWriteTo_Infinite() throws Exception {
    validateSettingsInfinite(readWrite(xmlQuotaInfinite));
  }

  @Test
  public void testWriteTo_Custom() throws Exception {
    validateSettingsCustom(readWrite(xmlCustom));
  }

  @Test
  public void testWriteTo_Mixed() throws Exception {
    validateSettingsMixed(readWrite(xmlMixed));
  }

  /**
   * @see PluginProjectSettings#writeTo(org.jdom.Element)
   * @throws Exception if something goes wrong
   */
  @Test
  public void testWriteTo_Empty() throws Exception {
    final PluginProjectSettings settings = createProjectSettings(xmlEmpty);
    final Element newSettingsRoot = createNewSettingsBase();
    settings.writeTo(newSettingsRoot);
    final PluginProjectSettings newSettings = new PluginProjectSettings();
    newSettings.readFrom(newSettingsRoot);
    assertNotNull(newSettings.getResources());
    assertEmpty(newSettings.getResources());
  }

  // UTILS

  private void validateSettingsQuota(@NotNull final PluginProjectSettings settings) {
    final Collection<Resource> resources = settings.getResources();
    assertNotEmpty(resources);
    assertEquals(1, resources.size());
    final Resource res = resources.iterator().next();
    assertNotNull(res);
    assertTrue(res instanceof QuotedResource);
    final QuotedResource qRes = (QuotedResource)res;
    assertEquals("resource1", qRes.getName());
    assertEquals(ResourceType.QUOTED, qRes.getType());
    assertFalse(qRes.isInfinite());
    assertEquals(123, qRes.getQuota());
  }

  private void validateSettingsInfinite(@NotNull final PluginProjectSettings settings) {
    final Collection<Resource> resources = settings.getResources();
    assertNotEmpty(resources);
    assertEquals(1, resources.size());
    final Resource res = resources.iterator().next();
    assertNotNull(res);
    assertTrue(res instanceof QuotedResource);
    final QuotedResource qRes = (QuotedResource)res;
    assertEquals("resource1", qRes.getName());
    assertEquals(ResourceType.QUOTED, qRes.getType());
    assertTrue(qRes.isInfinite());
  }

  private void validateSettingsCustom(@NotNull final PluginProjectSettings settings) {
    final Collection<Resource> resources = settings.getResources();
    assertEquals(1, resources.size());
    final Resource res = resources.iterator().next();
    assertNotNull(res);
    assertTrue(res instanceof CustomResource);
    final CustomResource cRes = (CustomResource)res;
    assertEquals("resource1", cRes.getName());
    assertEquals(ResourceType.CUSTOM, cRes.getType());
    final Set<String> values = cRes.getValues();
    assertNotNull(values);
    assertNotEmpty(values);
    assertEquals(4, values.size());
    for (int i = 0; i < 4; i++) {
      assertContains(values, "value" + i);
    }
  }

  private void validateSettingsMixed(@NotNull final PluginProjectSettings settings) {
    final Map<String, Resource> resources = settings.getResourceMap();
    assertNotNull(resources);
    assertEquals(3, resources.size());

    Resource r = resources.get("resource0"); // infinite resource
    assertNotNull(r);
    assertEquals(ResourceType.QUOTED, r.getType());
    QuotedResource qr = (QuotedResource)r;
    assertTrue(qr.isInfinite());

    r = resources.get("resource2"); // quoted resource
    assertNotNull(r);
    assertEquals(ResourceType.QUOTED, r.getType());
    qr = (QuotedResource)r;
    assertFalse(qr.isInfinite());
    assertEquals(999, qr.getQuota());

    r = resources.get("resource1");
    assertNotNull(r);
    assertEquals(ResourceType.CUSTOM, r.getType());
    CustomResource cr = (CustomResource)r;
    final Set<String> values = cr.getValues();
    assertNotNull(values);
    assertNotEmpty(values);
    assertEquals(6, values.size());
    for (int i = 0; i < 6; i++) {
      assertContains(values, "value" + i);
    }
  }

  /**
   * Reads settings to given xml and writes them to new base xml
   * @param xml xml string to read from
   * @return settings after write operation
   * @throws Exception if something goes wrong
   */
  private PluginProjectSettings readWrite(@NotNull final String xml) throws Exception {
    final PluginProjectSettings settings = createProjectSettings(xml);
    final Element newSettingsRoot = createNewSettingsBase();
    settings.writeTo(newSettingsRoot);
    final PluginProjectSettings newSettings = new PluginProjectSettings();
    newSettings.readFrom(newSettingsRoot);
    return newSettings;
  }

  /**
   * Creates base for new settings, i.e. empty settings xml
   *
   * @return link to root element of new settings
   * @throws Exception if something goes wrong
   */
  private static Element createNewSettingsBase() throws Exception {
    return createXML(xmlEmpty);
  }


  @NotNull
  private static Element createXML(@NotNull final String xmlString) throws Exception {
    final SAXBuilder builder = new SAXBuilder();
    final Document document = builder.build(new ByteArrayInputStream(xmlString.getBytes()));
    final Element root = document.getRootElement();
    final List rows = root.getChildren("JetBrains.SharedResources");
    assertNotNull(rows);
    assertEquals(1, rows.size());
    return (Element) rows.get(0);
  }

  /**
   * Reads {@code xml} supplied as string, returns project settings, created from read xml
   *
   * @param xmlString {@code xml} file as string
   * @return project settings, corresponding to xml document
   * @throws Exception if something goes wrong
   */
  @NotNull
  private static PluginProjectSettings createProjectSettings(@NotNull final String xmlString) throws Exception {
    final Element settingsRoot = createXML(xmlString);
    final PluginProjectSettings settings = new PluginProjectSettings();
    settings.readFrom(settingsRoot);
    return  settings;
  }
}
