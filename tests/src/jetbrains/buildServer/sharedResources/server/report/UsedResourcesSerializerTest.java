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

package jetbrains.buildServer.sharedResources.server.report;

import java.io.StringReader;
import java.io.StringWriter;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import jetbrains.buildServer.BaseTestCase;
import jetbrains.buildServer.sharedResources.model.Lock;
import jetbrains.buildServer.sharedResources.model.LockType;
import jetbrains.buildServer.sharedResources.model.resources.Resource;
import jetbrains.buildServer.sharedResources.model.resources.ResourceFactory;
import jetbrains.buildServer.util.FileUtil;
import org.jetbrains.annotations.NotNull;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

/**
 * Created with IntelliJ IDEA.
 *
 * @author Oleg Rybak (oleg.rybak@jetbrains.com)
 */
public class UsedResourcesSerializerTest extends BaseTestCase {

  private UsedResourcesSerializer mySerializer;

  @Override
  @BeforeMethod
  public void setUp() throws Exception {
    super.setUp();
    mySerializer = new UsedResourcesSerializer();
  }

  @Test
  public void testSerializeInfinite() throws Exception {
    final Resource resource = ResourceFactory.newInfiniteResource("myId", "projectId", "resource_name", true);
    final Lock lock = new Lock("resource_name", LockType.READ);
    final UsedResource usedResource = new UsedResource(resource, Collections.singleton(lock));
    doTest("infinite.json", usedResource);
  }

  @Test
  public void testSerializeQuoted() throws Exception {
    final Resource resource = ResourceFactory.newQuotedResource("myId", "projectId", "resource_name", 100, true);
    final Lock lock = new Lock("resource_name", LockType.WRITE);
    final UsedResource usedResource = new UsedResource(resource, Collections.singleton(lock));
    doTest("quoted.json", usedResource);
  }

  @Test
  public void testSerializeCustom() throws Exception {
    final Resource resource = ResourceFactory.newCustomResource("myId", "projectId", "resource_name", Arrays.asList("a", "b", "c", "d"), true);
    final Lock lock = new Lock("resource_name", LockType.READ, "a");
    final UsedResource usedResource = new UsedResource(resource, Collections.singleton(lock));
    doTest("custom.json", usedResource);
  }

  private void doTest(@NotNull final String fileName, @NotNull final UsedResource usedResource) throws Exception {
    String result;
    try (StringWriter writer = new StringWriter()) {
      mySerializer.write(Collections.singleton(usedResource), writer);
      result = writer.toString();
    }

    final String serialized = read(fileName);
    assertEqualsNormalized(serialized, result);

    List<UsedResource> usedResources;
    try (StringReader reader = new StringReader(serialized)) {
      usedResources = mySerializer.read(reader);
    }

    assertNotNull(usedResources);
    assertEquals(1, usedResources.size());
    UsedResource ur = usedResources.iterator().next();
    assertEquals(usedResource.getResource(), ur.getResource());
    assertEquals(usedResource.getLocks().iterator().next(), ur.getLocks().iterator().next());

  }

  private void assertEqualsNormalized(@NotNull final String expected, @NotNull final String actual) {
    assertEquals(normalize(expected), normalize(actual));
  }

  private String normalize(@NotNull final String str) {
    return str.replace("\r\n", "\n");
  }

  private String read(@NotNull final String fileName) throws Exception {
    return FileUtil.readResourceAsString(getClass(),
                                         "/" + getClass().getPackage().getName().replace('.', '/') + "/" + fileName,
                                         Charset.forName("UTF-8"));
  }
}
