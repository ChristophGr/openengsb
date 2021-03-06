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

package org.openengsb.itests.util;

import static org.hamcrest.CoreMatchers.not;
import static org.junit.Assert.assertThat;
import static org.junit.matchers.JUnitMatchers.containsString;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.security.PublicKey;

import javax.crypto.SecretKey;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.junit.Before;
import org.openengsb.core.api.Event;
import org.openengsb.core.api.security.DecryptionException;
import org.openengsb.core.api.security.EncryptionException;
import org.openengsb.core.api.workflow.RuleBaseException;
import org.openengsb.core.api.workflow.RuleManager;
import org.openengsb.core.api.workflow.model.RuleBaseElementId;
import org.openengsb.core.api.workflow.model.RuleBaseElementType;
import org.openengsb.core.security.internal.CipherUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Abstracts the general concepts required for remote tests
 */
public class AbstractRemoteTestHelper extends AbstractExamTestHelper {

    private static final Logger LOGGER = LoggerFactory.getLogger(AbstractExamTestHelper.class);

    protected static final String METHOD_CALL_STRING = ""
            + "{"
            + "    \"classes\": ["
            + "        \"java.lang.String\","
            + "        \"org.openengsb.core.api.workflow.model.ProcessBag\""
            + "    ],"
            + "    \"methodName\": \"executeWorkflow\","
            + "    \"metaData\": {"
            + "        \"serviceId\": \"workflowService\","
            + "        \"contextId\": \"foo\""
            + "    },"
            + "    \"args\": ["
            + "        \"simpleFlow\","
            + "        {"
            + "        }"
            + "    ]"
            + "}";

    protected static final String VOID_CALL_STRING = ""
            + "{"
            + "    \"classes\": ["
            + "        \"" + Event.class.getName() + "\""
            + "    ],"
            + "    \"methodName\": \"audit\","
            + "    \"metaData\": {"
            + "        \"serviceId\": \"auditing+memoryauditing+auditing-root\","
            + "        \"contextId\": \"foo\""
            + "    },"
            + "    \"args\": ["
            + "        { \"name\": \"testMessage\" }"
            + "    ]"
            + "}";

    protected static final String METHOD_CALL_STRING_FILTER = ""
            + "{"
            + "    \"classes\": ["
            + "        \"java.lang.String\","
            + "        \"org.openengsb.core.api.workflow.model.ProcessBag\""
            + "    ],"
            + "    \"methodName\": \"executeWorkflow\","
            + "    \"metaData\": {"
            + "        \"serviceFilter\": \"(objectClass=org.openengsb.core.api.workflow.WorkflowService)\","
            + "        \"contextId\": \"foo\""
            + "    },"
            + "    \"args\": ["
            + "        \"simpleFlow\","
            + "        {"
            + "        }"
            + "    ]"
            + "}";

    protected RuleManager ruleManager;

    @Before
    public void setUp() throws Exception {
        ruleManager = getOsgiService(RuleManager.class);
    }

    protected void addWorkflow(String workflow) throws IOException, RuleBaseException {
        if (ruleManager.get(new RuleBaseElementId(RuleBaseElementType.Process, workflow)) == null) {
            InputStream is =
                getClass().getClassLoader().getResourceAsStream("rulebase/org/openengsb/" + workflow + ".rf");
            String testWorkflow = IOUtils.toString(is);
            RuleBaseElementId id = new RuleBaseElementId(RuleBaseElementType.Process, workflow);
            ruleManager.add(id, testWorkflow);
            IOUtils.closeQuietly(is);
        }
    }

    protected String encryptMessage(String secureRequest, SecretKey sessionKey)
        throws EncryptionException, InterruptedException, IOException {
        PublicKey publicKey = getPublicKeyFromConfigFile();
        String encodedMessage = Base64.encodeBase64String(CipherUtils.encrypt(secureRequest.getBytes(), sessionKey));
        String encodedKey = Base64.encodeBase64String(CipherUtils.encrypt(sessionKey.getEncoded(), publicKey));

        String encryptedMessage = ""
                + "{"
                + "  \"encryptedContent\":\"" + encodedMessage + "\","
                + "  \"encryptedKey\":\"" + encodedKey + "\""
                + "}";
        return encryptedMessage;
    }

    protected SecretKey generateSessionKey() {
        SecretKey sessionKey =
            CipherUtils.generateKey(CipherUtils.DEFAULT_SYMMETRIC_ALGORITHM, CipherUtils.DEFAULT_SYMMETRIC_KEYSIZE);
        return sessionKey;
    }

    protected String prepareRequest(String methodCall, String username, String password) {
        String request = ""
                + "{"
                + "  \"callId\":\"12345\","
                + "  \"answer\":true,"
                + "  \"methodCall\":" + methodCall
                + "}";

        String authInfo = ""
                + "{"
                + "  \"className\":\"org.openengsb.core.api.security.model.Authentication\","
                + "  \"data\":"
                + "  {"
                + "    \"username\":\"" + username + "\","
                + "    \"credentials\":\"" + password + "\""
                + "  }"
                + "}";

        String secureRequest = ""
                + "{"
                + "  \"authenticationData\":" + authInfo + ","
                + "  \"timestamp\":" + System.currentTimeMillis() + ","
                + "  \"message\":" + request
                + "}";
        return secureRequest;
    }

    protected PublicKey getPublicKeyFromConfigFile() throws InterruptedException, IOException {
        // FIXME do this properly when OPENENGSB-1597 is resolved
        File file = new File(System.getProperty("karaf.home"), "/etc/keys/public.key.data");
        while (!file.exists()) {
            LOGGER.warn("waiting for public key to be generated in " + file);
            Thread.sleep(1000);
        }
        byte[] keyData;

        keyData = FileUtils.readFileToByteArray(file);
        PublicKey publicKey = CipherUtils.deserializePublicKey(keyData, CipherUtils.DEFAULT_ASYMMETRIC_ALGORITHM);
        return publicKey;
    }

    protected String decryptResult(SecretKey sessionKey, String result) throws DecryptionException {
        byte[] resultData = Base64.decodeBase64(result);
        assertThat(result, not(containsString("The answer to life the universe and everything")));
        byte[] decryptedResultData = CipherUtils.decrypt(resultData, sessionKey);
        result = new String(decryptedResultData);
        return result;
    }

    protected void verifyEncryptedResult(SecretKey sessionKey, String result) throws DecryptionException {
        try {
            result = decryptResult(sessionKey, result);
        } catch (DecryptionException e) {
            LOGGER.error("decryption failed.");
            LOGGER.error(result);
        }
        assertThat(result, containsString("The answer to life the universe and everything"));
    }
}
