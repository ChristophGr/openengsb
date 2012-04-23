package org.openengsb.core.common.json;

import java.lang.reflect.Array;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.Map;

import org.apache.commons.lang.ClassUtils;
import org.codehaus.jackson.annotate.JsonTypeInfo.Id;
import org.codehaus.jackson.map.jsontype.TypeIdResolver;
import org.codehaus.jackson.map.type.TypeFactory;
import org.codehaus.jackson.type.JavaType;
import org.openengsb.labs.delegation.service.Provide;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.ImmutableMap;

class CustomTypeIdResolver implements TypeIdResolver {

    private static final Logger LOGGER = LoggerFactory.getLogger(CustomTypeIdResolver.class);

    private static Map<String, Class<?>> predefinedImplementations = ImmutableMap.of(
        "list", (Class<?>) LinkedList.class,
        "map", LinkedHashMap.class,
        "string", String.class);

    private static Map<Class<?>, String> specialTypes = ImmutableMap.of(
        Collection.class, "list",
        Map.class, "map",
        String.class, "string");

    public CustomTypeIdResolver() {
    }

    private Class<?> getWrappedClass(String name) throws ClassNotFoundException {
        Class<?> clazz = doLoadClass(name);
        if (!clazz.isPrimitive()) {
            return clazz;
        }
        return ClassUtils.primitiveToWrapper(clazz);
    }

    protected Class<?> doLoadClass(String name) throws ClassNotFoundException {
        if (predefinedImplementations.containsKey(name)) {
            return predefinedImplementations.get(name);
        }
        if (name.endsWith("[]")) {
            String arrayComponentTypeName = name.substring(0, name.length() - 2);
            LOGGER.info("trying to resolve an array with component-type: " + arrayComponentTypeName);
            return Array.newInstance(getWrappedClass(arrayComponentTypeName), 0).getClass();
        }
        try {
            return getClass().getClassLoader().loadClass(name);
        } catch (ClassNotFoundException e) {
            // ignore, try next
        }
        return ClassUtils.getClass(name);
    }

    @Override
    public JavaType typeFromId(String id) {
        Class<?> clazz;
        try {
            LOGGER.info("resolving type from id {}", id);
            clazz = getWrappedClass(id);
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
