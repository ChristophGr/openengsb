/**
 * Licensed to the Austrian Association for Software Tool Integration (AASTI)
 * under one or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information regarding copyright
 * ownership. The AASTI licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.openengsb.core.common.json;

import java.io.IOException;

import org.codehaus.jackson.annotate.JsonTypeInfo;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.map.ObjectMapper.DefaultTypeResolverBuilder;
import org.codehaus.jackson.map.ObjectMapper.DefaultTyping;
import org.codehaus.jackson.map.ObjectWriter;
import org.codehaus.jackson.map.jsontype.TypeIdResolver;
import org.codehaus.jackson.map.jsontype.TypeResolverBuilder;
import org.codehaus.jackson.type.JavaType;
import org.openengsb.core.api.remote.GenericObjectSerializer;
import org.osgi.framework.BundleContext;

public class JsonObjectSerializer implements GenericObjectSerializer {

    private ObjectMapper mapper;
    private ObjectWriter writer;

    public JsonObjectSerializer() {
        mapper = createObjectMapper();
        writer = mapper.writerWithDefaultPrettyPrinter();
    }

    public JsonObjectSerializer(BundleContext bundleContext) {
        setBundleContext(bundleContext);
    }

    @Override
    public byte[] serializeToByteArray(Object object) throws IOException {
        return writer.writeValueAsBytes(object);
    }

    @Override
    public String serializeToString(Object object) throws IOException {
        return writer.writeValueAsString(object);
    }

    @Override
    public <T> T parse(String data, Class<T> type) throws IOException {
        return mapper.readValue(data, type);
    }

    @Override
    public <T> T parse(byte[] data, Class<T> type) throws IOException {
        return mapper.readValue(data, type);
    }

    public void setBundleContext(BundleContext bundleContext) {
        mapper = createObjectMapper(bundleContext);
        writer = mapper.writerWithDefaultPrettyPrinter();
    }

    private static ObjectMapper createObjectMapper(BundleContext bundleContext) {
        return createObjectMapperWithTypeIdResolver(new DelegatingTypeIdResolver(bundleContext));
    }

    private static ObjectMapper createObjectMapper() {
        return createObjectMapperWithTypeIdResolver(new CustomTypeIdResolver());
    }

    private static ObjectMapper createObjectMapperWithTypeIdResolver(TypeIdResolver resolver) {
        ObjectMapper mapper = new ObjectMapper();

        TypeResolverBuilder<?> typer = new DefaultTypeResolverBuilder(DefaultTyping.OBJECT_AND_NON_CONCRETE) {
            @Override
            public boolean useForType(JavaType t) {
                /*-
                 * skip typing for containers
                 * This is required to avoid inclusion of specific type for maps and other container types.
                 * 
                 * Example:
                 * {
                 *   "@type" : "java.util.HashMap", <-- we don't want that
                 *   "id" : "foo"
                 * }
                 */
                return super.useForType(t) && !t.isContainerType();
            }
        };

        typer = typer.init(JsonTypeInfo.Id.NAME, resolver);
        typer = typer.inclusion(JsonTypeInfo.As.PROPERTY);
        mapper.setDefaultTyping(typer);

        return mapper;
    }
}
