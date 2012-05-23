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

package org.openengsb.core.security.filter;

import java.util.Map;

import javax.crypto.SecretKey;

import org.apache.commons.codec.binary.Base64;
import org.openengsb.core.api.remote.FilterAction;
import org.openengsb.core.api.remote.FilterConfigurationException;
import org.openengsb.core.api.remote.FilterException;
import org.openengsb.core.api.security.DecryptionException;
import org.openengsb.core.api.security.EncryptionException;
import org.openengsb.core.api.security.PrivateKeySource;
import org.openengsb.core.api.security.model.EncryptedMessage;
import org.openengsb.core.common.remote.AbstractFilterChainElement;
import org.openengsb.core.common.util.CipherUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This filter takes an {@link EncryptedMessage} and decrypts it. This is done by decrypting the contained encrypted
 * session key with the servers {@link java.security.PrivateKey}. The resulting byte[] is then processed by the next
 * filter. It returns a serialized version of the result (as byte[] again). The Response is then encrypted with the
 * previously obtained session-key and returned as byte[].
 * 
 * This filter is intended for incoming ports.
 * 
 * <code>
 * <pre>
 *      [EncryptedMessage]             > Filter > [byte[] with decrypted content]    > ...
 *                                                                                      |
 *                                                                                      v
 *      [encrypted Response as byte[]] < Filter < [byte[] with serialized result]    < ...
 * </pre>
 * </code>
 */
public class MessageCryptoFilter extends AbstractFilterChainElement<EncryptedMessage, byte[]> {

    private static final Logger LOGGER = LoggerFactory.getLogger(MessageCryptoFilter.class);

    private FilterAction next;

    private PrivateKeySource privateKeySource;
    private String secretKeyAlgorithm;

    public MessageCryptoFilter(PrivateKeySource privateKeySource, String secretKeyAlgorithm) {
        this.privateKeySource = privateKeySource;
        this.secretKeyAlgorithm = secretKeyAlgorithm;
    }

    @Override
    protected byte[] doFilter(EncryptedMessage input, Map<String, Object> metaData) {
        byte[] encryptedKey = input.getEncryptedKey();
        byte[] decryptedMessage;
        SecretKey sessionKey;
        LOGGER.debug("decrypting encryptedMessage", Base64.encodeBase64String(input.getEncryptedContent()));
        LOGGER.debug("with encrypted key", Base64.encodeBase64(encryptedKey));
        try {
            LOGGER.debug("decrypting session-key with private key {}",
                Base64.encodeBase64(privateKeySource.getPrivateKey().getEncoded()));
            byte[] sessionKeyData = CipherUtils.decrypt(encryptedKey, privateKeySource.getPrivateKey());
            LOGGER.debug("decrpyted sessionkey to be {}", Base64.encodeBase64(sessionKeyData));
            LOGGER.debug("deserializing sessoin-key for algorithm {}", secretKeyAlgorithm);
            sessionKey = CipherUtils.deserializeSecretKey(sessionKeyData, secretKeyAlgorithm);
            LOGGER.trace("decrypting message using session-key");
            decryptedMessage = CipherUtils.decrypt(input.getEncryptedContent(), sessionKey);
            LOGGER.debug("decrypted message to be {}", Base64.encodeBase64String(decryptedMessage));
        } catch (DecryptionException e) {
            throw new FilterException(e);
        }
        LOGGER.debug("forwarding decrypted message to next filter {}", next);
        byte[] plainResult = (byte[]) next.filter(decryptedMessage, metaData);
        LOGGER.debug("got result from next filter {}", Base64.encodeBase64String(plainResult));
        try {
            LOGGER.debug("encrypting result using previously decrypted session-key {}",
                Base64.encodeBase64String(sessionKey.getEncoded()));
            return CipherUtils.encrypt(plainResult, sessionKey);
        } catch (EncryptionException e) {
            LOGGER.debug("could not encrypt result", e);
            throw new FilterException(e);
        }
    }

    @Override
    public void setNext(FilterAction next) throws FilterConfigurationException {
        checkNextInputAndOutputTypes(next, byte[].class, byte[].class);
        this.next = next;
    }

}
