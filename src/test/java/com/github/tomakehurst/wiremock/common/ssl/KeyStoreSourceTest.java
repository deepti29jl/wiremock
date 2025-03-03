/*
 * Copyright (C) 2011 Thomas Akehurst
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
package com.github.tomakehurst.wiremock.common.ssl;

import org.junit.jupiter.api.Test;

import java.security.Key;
import java.security.KeyStore;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.notNullValue;

public class KeyStoreSourceTest {

    @Test
    public void loadsAPasswordProtectedJksKeyStore() throws Exception {
        KeyStoreSource keyStoreSource = new ReadOnlyFileOrClasspathKeyStoreSource(
                "test-keystore-pwd",
                "jks",
                "nondefaultpass".toCharArray()
        );

        KeyStore keyStore = keyStoreSource.load();

        Key key = keyStore.getKey("server", "password".toCharArray());
        assertThat(key, notNullValue());
    }
}
