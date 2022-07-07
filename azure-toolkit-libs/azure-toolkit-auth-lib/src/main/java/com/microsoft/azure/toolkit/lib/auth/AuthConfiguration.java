/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.lib.auth;

import com.azure.identity.DeviceCodeInfo;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.microsoft.azure.toolkit.lib.common.exception.InvalidConfigurationException;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import org.apache.commons.lang3.StringUtils;

import javax.annotation.Nonnull;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;

@Setter
@Getter
public class AuthConfiguration {
    @Nonnull
    @Setter(AccessLevel.NONE)
    private AuthType type;
    private String client;
    private String tenant;

    /**
     * only usable for maven/gradle thisuration
     */
    private String environment;

    /**
     * for SP only
     */
    @JsonIgnore
    private String key;
    /**
     * for SP only
     */
    private String certificate;
    /**
     * for SP only
     */
    @JsonIgnore
    private String certificatePassword;

    /**
     * for DC only
     */
    @JsonIgnore
    private transient Consumer<DeviceCodeInfo> deviceCodeConsumer;

    /**
     * for DC only
     */
    @JsonIgnore
    private transient Runnable doAfterLogin;

    /**
     * for restoring token cache only
     */
    private String username;
    /**
     * for restoring token cache only
     */
    private List<String> selectedSubscriptions;

    @JsonCreator
    public AuthConfiguration(@JsonProperty("type") @Nonnull final AuthType type) {
        this.type = type;
    }

    public void setType(AuthType type) {
        if (this.type != AuthType.AUTO && type != this.type) {
            throw new IllegalArgumentException("type can't be changed");
        }
        this.type = type;
    }

    public void validate() throws InvalidConfigurationException {
        String tenant = this.getTenant();
        String client = this.getClient();
        String key = this.getKey();
        String certificate = this.getCertificate();
        String errorMessage = null;
        if (StringUtils.isBlank(tenant)) {
            errorMessage = "Cannot find 'tenant'";
        } else if (StringUtils.isBlank(client)) {
            errorMessage = "Cannot find 'client'";
        } else if (StringUtils.isAllBlank(key, certificate)) {
            errorMessage = "Cannot find either 'key' or 'certificate'";
        } else if (StringUtils.isNotBlank(key) && StringUtils.isNotBlank(certificate)) {
            errorMessage = "It is wrong to specify both 'key' and 'certificate'";
        }
        if (Objects.nonNull(errorMessage)) {
            throw new InvalidConfigurationException(errorMessage);
        }
    }
}
