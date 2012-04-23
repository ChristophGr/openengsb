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

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

import java.io.IOException;
import java.util.Arrays;
import java.util.Hashtable;
import java.util.Map;

import org.apache.commons.lang.ArrayUtils;
import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.map.ObjectMapper;
import org.junit.Before;
import org.junit.Test;
import org.openengsb.core.api.remote.GenericObjectSerializer;
import org.openengsb.core.test.AbstractOsgiMockServiceTest;
import org.openengsb.labs.delegation.service.ClassProvider;
import org.openengsb.labs.delegation.service.Constants;
import org.openengsb.labs.delegation.service.internal.ClassProviderImpl;
import org.openengsb.labs.delegation.service.internal.ClassProviderWithAliases;

import com.google.common.base.Objects;
import com.google.common.collect.ImmutableMap;

public class ObjectSerializerTest extends AbstractOsgiMockServiceTest {

    private GenericObjectSerializer serializer;

    public static class TestClass {
        private Object mcontent;

        public TestClass() {
        }

        public TestClass(Object content) {
            mcontent = content;
        }

        public Object getMcontent() {
            return mcontent;
        }

        public void setMcontent(Object content) {
            mcontent = content;
        }

        @Override
        public boolean equals(Object obj) {
            if (!(obj instanceof TestClass)) {
                return false;
            }
            TestClass other = (TestClass) obj;
            if (mcontent.getClass().isArray()) {
                return Arrays.equals((Object[]) mcontent, (Object[]) other.mcontent);
            }
            return Objects.equal(mcontent, other.mcontent);
        }

        @Override
        public int hashCode() {
            return Objects.hashCode(mcontent);
        }

        @Override
        public String toString() {
            if (mcontent.getClass().isArray()) {
                return "TestClass: " + mcontent.getClass().getComponentType().getName() + "[] "
                        + ArrayUtils.toString(mcontent);
            }
            return "TestClass: " + mcontent.toString();
        }
    }

    @Before
    public void setUp() throws Exception {
        serializer = new JsonObjectSerializer(bundleContext);
        ClassProviderImpl providerImpl =
            new ClassProviderWithAliases(bundle, new String[]{ TestClass.class.getName() }, ImmutableMap.of("test",
                TestClass.class.getName()));
        Map<String, Object> propMap = ImmutableMap.of(
            Constants.PROVIDED_CLASSES_KEY, (Object) Arrays.asList(TestClass.class.getName(), "test"));
        registerService(providerImpl, new Hashtable<String, Object>(propMap), ClassProvider.class);
    }

    @Test
    public void testBeanContent() throws Exception {
        Object test = new TestClass(new TestClass(1));
        String serialized = serializer.serializeToString(test);
        System.out.println(serialized);

        String reference = ""
                + "{"
                + "  \"mcontent\" : {"
                + "    \"@type\" : \"org.openengsb.core.common.ObjectSerializerTest$TestClass\","
                + "    \"mcontent\" : 1"
                + "  }"
                + "}";

        assertEqualTrees(serialized, reference);
        assertDeserializedIsOriginal(serialized, test);
    }

    @Test
    public void testPrimitiveContent() throws Exception {
        Object test = new TestClass(1);
        String serialized = serializer.serializeToString(test);
        System.out.println(serialized);
        String reference = ""
                + "{"
                + "  \"mcontent\" : 1"
                + "}";
        assertEqualTrees(serialized, reference);
        assertDeserializedIsOriginal(serialized, test);
    }

    @Test
    public void testWrapperContent() throws Exception {
        Object test = new TestClass(new Integer(1));
        String serialized = serializer.serializeToString(test);
        System.out.println(serialized);

        String reference = ""
                + "{"
                + "  \"mcontent\" : 1"
                + "}";
        assertEqualTrees(serialized, reference);
        assertDeserializedIsOriginal(serialized, test);
    }

    @Test
    public void testStringContent() throws Exception {
        Object test = new TestClass("foo");
        String serialized = serializer.serializeToString(test);
        System.out.println(serialized);

        String reference = ""
                + "{"
                + "  \"mcontent\" : \"foo\""
                + "}";
        assertEqualTrees(serialized, reference);
        assertDeserializedIsOriginal(serialized, test);
    }

    @Test
    public void testSimpleCollectionContent() throws Exception {
        Object test = new TestClass(Arrays.asList("foo", "bar"));
        String serialized = serializer.serializeToString(test);
        System.out.println(serialized);

        String reference = ""
                + "{"
                + "  \"mcontent\" : [ \"list\", [ \"foo\", \"bar\" ] ]"
                + "}";
        assertEqualTrees(serialized, reference);
        assertDeserializedIsOriginal(serialized, test);
    }

    @Test
    public void testBeanCollectionContent() throws Exception {
        Object test = new TestClass(Arrays.asList((Object) new TestClass(1), new TestClass(2)));
        String serialized = serializer.serializeToString(test);
        System.out.println(serialized);

        String reference = ""
                + "{"
                + "  \"mcontent\" : [ \"list\", [ {"
                + "    \"@type\" : \"org.openengsb.core.common.ObjectSerializerTest$TestClass\","
                + "    \"mcontent\" : 1"
                + "  }, {"
                + "    \"@type\" : \"org.openengsb.core.common.ObjectSerializerTest$TestClass\","
                + "    \"mcontent\" : 2"
                + "  } ] ]"
                + "}";
        assertEqualTrees(serialized, reference);
        assertDeserializedIsOriginal(serialized, test);
    }

    @Test
    public void testArrayStringContent() throws Exception {
        Object test = new TestClass(new String[]{ "foo", "bar" });
        String serialized = serializer.serializeToString(test);
        System.out.println(serialized);

        String reference = ""
                + "{"
                + "  \"mcontent\" : [ \"string[]\", [ \"foo\", \"bar\" ] ]"
                + "}";
        assertEqualTrees(serialized, reference);
        assertDeserializedIsOriginal(serialized, test);
    }

    @Test
    public void testArrayPrimitiveContent() throws Exception {
        Object test = new TestClass(new Integer[]{ 1, 42 });
        String serialized = serializer.serializeToString(test);
        System.out.println(serialized);

        String reference = ""
                + "{"
                + "  \"mcontent\" : [ \"int[]\", [ 1, 42 ] ]"
                + "}";
        assertEqualTrees(serialized, reference);
        assertDeserializedIsOriginal(serialized, test);
    }

    @Test
    public void testArrayArrayContent() throws Exception {
        Object test = new TestClass(new int[][]{ new int[]{ 1, 42 } });
        String serialized = serializer.serializeToString(test);
        System.out.println(serialized);

        String reference = ""
                + "{"
                + "  \"mcontent\" : [ \"int[][]\", [ [ 1, 42 ] ] ]"
                + "}";
        assertEqualTrees(serialized, reference);
    }

    @Test
    public void testMapContent() throws Exception {
        Object test = new TestClass(ImmutableMap.of("foo", 42, "bar", 1));
        String serialized = serializer.serializeToString(test);
        System.out.println(serialized);

        String reference = ""
                + "{"
                + "  \"mcontent\" : {"
                + "    \"@type\" : \"map\","
                + "    \"foo\" : 42,"
                + "    \"bar\" : 1"
                + "  }"
                + "}";
        assertEqualTrees(serialized, reference);
    }

    public void assertEqualTrees(String tree1, String tree2) throws IOException {
        ObjectMapper objectMapper = new ObjectMapper(); // a plain one
        JsonNode readTree1 = objectMapper.readTree(tree1);
        JsonNode readTree2 = objectMapper.readTree(tree2);
        assertThat(readTree1, is(readTree2));
    }

    private void assertDeserializedIsOriginal(String serialized, Object original) throws IOException {
        Object parsed = serializer.parse(serialized, original.getClass());
        assertThat(parsed, is(original));

    }

}
