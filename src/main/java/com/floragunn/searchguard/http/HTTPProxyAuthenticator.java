/*
 * Copyright 2015-2017 floragunn GmbH
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * 
 */

package com.floragunn.searchguard.http;

import java.nio.file.Path;

import org.elasticsearch.ElasticsearchSecurityException;
import org.elasticsearch.common.Strings;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.util.concurrent.ThreadContext;
import org.elasticsearch.rest.RestChannel;
import org.elasticsearch.rest.RestRequest;

import com.floragunn.searchguard.auth.HTTPAuthenticator;
import com.floragunn.searchguard.support.ConfigConstants;
import com.floragunn.searchguard.user.AuthCredentials;

public class HTTPProxyAuthenticator implements HTTPAuthenticator {

    protected final Logger log = LogManager.getLogger(this.getClass());
    private volatile Settings settings;

    public HTTPProxyAuthenticator(Settings settings, final Path configPath) {
        super();
        this.settings = settings;
    }

    @Override
    public AuthCredentials extractCredentials(final RestRequest request, ThreadContext context) {
    	
        if(context.getTransient(ConfigConstants.SG_XFF_DONE) !=  Boolean.TRUE) {
            throw new ElasticsearchSecurityException("xff not done");
        }
        
        final String userHeader = settings.get("user_header");
        final String rolesHeader = settings.get("roles_header");

        if(log.isDebugEnabled()) {
            log.debug("headers {}", request.getHeaders());
            log.debug("userHeader {}, value {}", userHeader, userHeader == null?null:request.header(userHeader));
            log.debug("rolesHeader {}, value {}", rolesHeader, rolesHeader == null?null:request.header(rolesHeader));
        }

        if (!Strings.isNullOrEmpty(userHeader) && !Strings.isNullOrEmpty((String) request.header(userHeader))) {

            String[] backendRoles = null;

            if (!Strings.isNullOrEmpty(rolesHeader) && !Strings.isNullOrEmpty((String) request.header(rolesHeader))) {
                backendRoles = ((String) request.header(rolesHeader)).split(",");
            }
            return new AuthCredentials((String) request.header(userHeader), backendRoles).markComplete();
        } else {
            if(log.isTraceEnabled()) {
                log.trace("No '{}' header, send 401", userHeader);
            }
            return null;
        }
    }

    @Override
    public boolean reRequestAuthentication(final RestChannel channel, AuthCredentials creds) {
        return false;
    }

    @Override
    public String getType() {
        return "proxy";
    }
}
