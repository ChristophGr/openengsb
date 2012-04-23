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
package org.openengsb.core.common;

import java.io.IOException;
import java.lang.reflect.Array;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.Map;

import org.apache.commons.lang.ClassUtils;
import org.codehaus.jackson.annotate.JsonTypeInfo;
import org.codehaus.jackson.annotate.JsonTypeInfo.Id;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.map.ObjectMapper.DefaultTypeResolverBuilder;
import org.codehaus.jackson.map.ObjectMapper.DefaultTyping;
import org.codehaus.jackson.map.ObjectWriter;
import org.codehaus.jackson.map.jsontype.TypeIdResolver;
import org.codehaus.jackson.map.jsontype.TypeResolverBuilder;
import org.codehaus.jackson.map.type.TypeFactory;
import org.codehaus.jackson.type.JavaType;
import org.openengsb.core.api.remote.GenericObjectSerializer;
import org.openengsb.labs.delegation.service.DelegationClassLoader;
import org.openengsb.labs.delegation.service.Provide;
import org.osgi.framework.BundleContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.ImmutableMap;

public class JsonObjectSerializer implements GenericObjectSerializer {

    private static final Logger LOGGER = LoggerFactory.getLogger(JsonObjectSerializer.class);

    private ObjectMapper mapper;
    private ObjectWriter writer;

    public JsonObjectSerializer() {
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
        mapper = createMapperWithDefaults(bundleContext);
        writer = mapper.writerWithDefaultPrettyPrinter();
    }

    private static ObjectMapper createMapperWithDefaults(BundleContext bundleContext) {
        ObjectMapper mapper = new ObjectMapper();

        TypeResolverBuilder<?> typer = new DefaultTypeResolverBuilder(DefaultTyping.OBJECT_AND_NON_CONCRETE) {
            @Override
            public boolean useForType(JavaType t) {
                /*
                 * skip typing for containers
                 * 
                 * This is required to avoid inclusion of specific type for maps and other container types.
                 * 
                 * Example:
                 * 
                 * {
                 * 
                 * "@type" : "java.util.HashMap", <-- we don't want that
                 * 
                 * "id" : "foo"
                 * 
                 * }
                 */
                return super.useForType(t) && !t.isContainerType();
            }
        };

        typer = typer.init(JsonTypeInfo.Id.NAME, new DelegatingTypeIdResolver(bundleContext));
        typer = typer.inclusion(JsonTypeInfo.As.PROPERTY);
        mapper.setDefaultTyping(typer);

        return mapper;
    }

    static final class DelegatingTypeIdResolver implements TypeIdResolver {
        private static final Logger LOGGER = LoggerFactory.getLogger(DelegatingTypeIdResolver.class);

        private DelegationClassLoader delegationClassLoader;

        private static Map<String, Class<?>> predefinedImplementations = ImmutableMap.of(
            "list", (Class<?>) LinkedList.class,
            "map", LinkedHashMap.class,
            "string", String.class);

        private static Map<Class<?>, String> specialTypes = ImmutableMap.of(
            Collection.class, "list",
            Map.class, "map",
            String.class, "string");

        public DelegatingTypeIdResolver(BundleContext bundleContext) {
            delegationClassLoader = new DelegationClassLoader(bundleContext, (ClassLoader) null);
        }

        private Class<?> doLoadClass(String name) throws ClassNotFoundException {
            Class<?> clazz = doLoadClass0(name);
            if (!clazz.isPrimitive()) {
                return clazz;
            }
            return ClassUtils.primitiveToWrapper(clazz);
        }

        private Class<?> doLoadClass0(String name) throws ClassNotFoundException {
            if (predefinedImplementations.containsKey(name)) {
                return predefinedImplementations.get(name);
            }
            if (name.endsWith("[]")) {
                String arrayComponentTypeName = name.substring(0, name.length() - 2);
                LOGGER.info("trying to resolve an array with component-type: " + arrayComponentTypeName);
                return Array.newInstance(doLoadClass(arrayComponentTypeName), 0).getClass();
            }
            try {
                return getClass().getClassLoader().loadClass(name);
            } catch (ClassNotFoundException e) {
                // ignore, try next
            }
            try {
                return ClassUtils.getClass(name);
            } catch (ClassNotFoundException e) {
                // ignore, try next
            }
            LOGGER.info("could not find class {}. Try to find it using ClassProviders", name);
            return delegationClassLoader.loadClass(name);
        }

        @Override
        public JavaType typeFromId(String id) {
            Class<?> clazz;
            try {
                LOGGER.info("resolving type from id {}", id);
                clazz = doLoadClass(id);
            } catch (ClassNotFoundException e) {
                LOGGER.error("could not load class {}", id, e);
                return null;
            }
            LOGGER.info("-> resolved {}", clazz.getName());
            return TypeFactory.defaultInstance().constructType(clazz);
        }

        @Override
        public void init(JavaType baseType) {
            LOGGER.info("init TypeIdResolver ", baseType);
        }

        @Override
        public String idFromValueAndType(Object value, Class<?> suggestedType) {
            LOGGER.info("get id from type {} - {}", value, suggestedType);
            Provide annotation = suggestedType.getAnnotation(Provide.class);
            if (annotation != null) {
                LOGGER.info("got {} from annotation", annotation.alias());
                return annotation.alias()[0];
            }
            return useSpecialType(suggestedType);
        }

        private String useSpecialType(Class<?> suggestedType) {
            if (suggestedType.isArray()) {
                return useSpecialType(suggestedType.getComponentType()) + "[]";
            }
            for (Map.Entry<Class<?>, String> entry : specialTypes.entrySet()) {
                if (entry.getKey().isAssignableFrom(suggestedType)) {
                    return entry.getValue();
                }
            }
            if (specialTypes.containsKey(suggestedType)) {
                return specialTypes.get(suggestedType);
            }
            Class<?> primitive = ClassUtils.wrapperToPrimitive(suggestedType);
            return primitive != null ? primitive.getName() : suggestedType.getName();
        }

        @Override
        public String idFromValue(Object value) {
            LOGGER.info("resolving idFromValue for {}", value);
            return idFromValueAndType(value, value.getClass());
        }

        @Override
        public Id getMechanism() {
            return Id.NAME;
        }
    }

}
