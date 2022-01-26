/*
 * Copyright 2000-2022 JetBrains s.r.o.
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

import com.google.gson.*;
import com.google.gson.internal.Streams;
import com.google.gson.reflect.TypeToken;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.util.Collection;
import java.util.List;
import jetbrains.buildServer.sharedResources.model.Lock;
import jetbrains.buildServer.sharedResources.model.LockType;
import jetbrains.buildServer.sharedResources.model.resources.*;
import org.jetbrains.annotations.NotNull;

/**
 * Created with IntelliJ IDEA.
 *
 * @author Oleg Rybak (oleg.rybak@jetbrains.com)
 */
public class UsedResourcesSerializer {

  public List<UsedResource> read(@NotNull final Reader reader) {
    final GsonBuilder builder = new GsonBuilder();
    builder.setFieldNamingStrategy(STRATEGY);
    builder.registerTypeAdapter(Resource.class, (InstanceCreator<Resource>)type -> ResourceFactory.newInfiniteResource("", "", "", true));
    builder.registerTypeAdapter(Lock.class, (InstanceCreator<Lock>)type -> new Lock("", LockType.READ, ""));
    builder.registerTypeAdapterFactory(MY_TYPE_ADAPTER_FACTORY);
    return builder.setPrettyPrinting().create().fromJson(reader, new TypeToken<List<UsedResource>>(){}.getType());
  }

  public void write(Collection<UsedResource> usedResources, Writer writer) {
    final GsonBuilder builder = new GsonBuilder();
    builder.setFieldNamingStrategy(STRATEGY);
    builder.setPrettyPrinting();
    builder.create().toJson(usedResources, writer);
  }

  private static final FieldNamingStrategy STRATEGY = field -> {
    String name = field.getName();
    if (name.startsWith("my")) {
      char c[] = name.substring(2).toCharArray();
      c[0] = Character.toLowerCase(c[0]);
      return new String(c);
    } else {
      return name;
    }
  };

  private final TypeAdapterFactory MY_TYPE_ADAPTER_FACTORY = new CustomTypeAdapterFactory();

  private static final class CustomTypeAdapterFactory implements TypeAdapterFactory {
    @Override
    public <T> TypeAdapter<T> create(final Gson gson, final TypeToken<T> type) {
      if (type.getRawType() != Resource.class) {
        return null;
      }

      final TypeAdapter<T> delegate = gson.getDelegateAdapter(this, type);

      return new TypeAdapter<T>() {
        @Override
        public void write(final JsonWriter jsonWriter, final T t) throws IOException {
          delegate.write(jsonWriter, t);
        }

        @SuppressWarnings("unchecked")
        @Override
        public T read(final JsonReader jsonReader) throws JsonParseException {
          JsonElement tree = Streams.parse(jsonReader);
          JsonObject object = tree.getAsJsonObject();
          if (object.has("type")) {
            ResourceType resourceType = ResourceType.fromString(object.get("type").getAsString());
            if (resourceType == ResourceType.QUOTED) {
              return (T)gson.getDelegateAdapter(CustomTypeAdapterFactory.this, TypeToken.get(QuotedResource.class)).fromJsonTree(tree);
            } else if (resourceType == ResourceType.CUSTOM) {
              return (T)gson.getDelegateAdapter(CustomTypeAdapterFactory.this, TypeToken.get(CustomResource.class)).fromJsonTree(tree);
            }
          }
          throw new JsonParseException("Cannot deserialize " + type + ". It is not a valid Resource JSON.");
        }
      };
    }
  }
}
