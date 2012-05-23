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

package org.openengsb.core.security;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import javax.crypto.SecretKey;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.lang.SerializationException;
import org.apache.commons.lang.SerializationUtils;
import org.openengsb.core.api.remote.FilterAction;
import org.openengsb.core.api.remote.FilterChainElement;
import org.openengsb.core.api.remote.FilterChainElementFactory;
import org.openengsb.core.api.remote.FilterConfigurationException;
import org.openengsb.core.api.remote.FilterException;
import org.openengsb.core.api.remote.MethodCallMessage;
import org.openengsb.core.api.remote.MethodResultMessage;
import org.openengsb.core.api.security.model.EncryptedMessage;
import org.openengsb.core.common.remote.AbstractFilterChainElement;
import org.openengsb.core.common.remote.FilterChainFactory;
import org.openengsb.core.common.util.CipherUtils;
import org.openengsb.core.security.filter.MessageCryptoFilterFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SecureJavaSerializePortTest extends GenericSecurePortTest<byte[]> {

    private static final Logger LOGGER = LoggerFactory.getLogger(SecureJavaSerializePortTest.class);

    @Override
    protected byte[] encodeAndEncrypt(MethodCallMessage secureRequest, SecretKey sessionKey) throws Exception {
        LOGGER.debug("preparing secureRequest {}", secureRequest);
        LOGGER.debug("encrypting it with secret-key {}", sessionKey);
        byte[] serialized = SerializationUtils.serialize(secureRequest);
        LOGGER.debug("serialized request: {}", Base64.encodeBase64String(serialized));
        byte[] content = CipherUtils.encrypt(serialized, sessionKey);
        LOGGER.debug("encrypted request: {}", Base64.encodeBase64String(content));
        EncryptedMessage message = new EncryptedMessage();
        message.setEncryptedContent(content);
        byte[] encryptedKey = CipherUtils.encrypt(sessionKey.getEncoded(), serverPublicKey);
        LOGGER.debug("encrypted key: {}", Base64.encodeBase64String(encryptedKey));
        message.setEncryptedKey(encryptedKey);
        byte[] serialize = SerializationUtils.serialize(message);
        LOGGER.debug("serialized EncryptedMessage {}", Base64.encodeBase64String(serialize));
        return serialize;
    }

    @Override
    protected MethodResultMessage decryptAndDecode(byte[] message, SecretKey sessionKey) throws Exception {
        LOGGER.debug("decrypting And decoding {}", Base64.encodeBase64String(message));
        LOGGER.debug("using secretKey {}", Base64.encodeBase64String(sessionKey.getEncoded()));
        byte[] content = CipherUtils.decrypt(message, sessionKey);
        LOGGER.debug("decrypted content {}", Base64.encodeBase64String(content));
        MethodResultMessage deserialize = (MethodResultMessage) SerializationUtils.deserialize(content);
        LOGGER.debug("deserialized result-message {}", deserialize);
        return deserialize;
    }

    @Override
    protected byte[] manipulateMessage(byte[] encryptedRequest) {
        int pos = 187;
        encryptedRequest[pos]++;
        return encryptedRequest;
    }

    @Override
    protected FilterAction getSecureRequestHandlerFilterChain() throws Exception {
        FilterChainElementFactory unpackerFactory = new FilterChainElementFactory() {
            @Override
            public FilterChainElement newInstance() throws FilterConfigurationException {
                return new AbstractFilterChainElement<byte[], byte[]>() {
                    private FilterAction next;

                    @Override
                    protected byte[] doFilter(byte[] input, Map<String, Object> metaData) {
                        LOGGER.debug("running unpacker-filter for {}", Base64.encodeBase64String(input));
                        EncryptedMessage deserialize = (EncryptedMessage) SerializationUtils.deserialize(input);
                        LOGGER.debug("input deserialized, handing over to decrypter");
                        LOGGER.debug("content: ", Base64.encodeBase64String(deserialize.getEncryptedContent()));
                        LOGGER.debug("key: ", Base64.encodeBase64String(deserialize.getEncryptedKey()));
                        LOGGER.debug("metadata", metaData);
                        byte[] result = (byte[]) next.filter(deserialize, metaData);
                        LOGGER.debug("serializing encrypted result");
                        return result;
                    }

                    @Override
                    public void setNext(FilterAction next) throws FilterConfigurationException {
                        this.next = next;
                    }
                };
            }
        };
        FilterChainElementFactory decrypterFactory = new MessageCryptoFilterFactory(privateKeySource, "AES");
        FilterChainElementFactory parserFactory = new FilterChainElementFactory() {
            @Override
            public FilterChainElement newInstance() throws FilterConfigurationException {
                return new AbstractFilterChainElement<byte[], byte[]>() {
                    private FilterAction next;

                    @Override
                    protected byte[] doFilter(byte[] input, Map<String, Object> metaData) {
                        LOGGER.debug("running parser-filter for {}", Base64.encodeBase64String(input));
                        MethodCallMessage deserialize;
                        try {
                            deserialize = (MethodCallMessage) SerializationUtils.deserialize(input);
                            LOGGER.debug("deserialized SecureRequest {}", String.format(
                                "msg: %s; user: %s; credentials: %s", deserialize.getMethodCall(),
                                deserialize.getPrincipal(), deserialize.getCredentials().toObject()));
                        } catch (SerializationException e) {
                            LOGGER.debug("could not deserialize", e);
                            throw new FilterException(e);
                        }
                        LOGGER.debug("passing on to filterTop");
                        MethodResultMessage result = (MethodResultMessage) next.filter(deserialize, metaData);
                        LOGGER.debug("got response from filterTop {}", result.getResult());
                        byte[] serializedResult = SerializationUtils.serialize(result);
                        LOGGER.debug("serialized result to {}", Base64.encodeBase64String(serializedResult));
                        return serializedResult;
                    }

                    @Override
                    public void setNext(FilterAction next) throws FilterConfigurationException {
                        this.next = next;
                    }
                };
            }
        };

        FilterChainFactory<byte[], byte[]> factory = new FilterChainFactory<byte[], byte[]>(byte[].class, byte[].class);
        List<Object> asList = Arrays.asList(unpackerFactory, decrypterFactory, parserFactory, filterTop.create());
        factory.setFilters(asList);
        return factory.create();
    }
}
