/*
 *  Copyright (c) 2020 - 2022 Microsoft Corporation
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Microsoft Corporation - initial API and implementation
 *
 */

package org.eclipse.dataspaceconnector.api.datamanagement.contractnegotiation.model;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public class NegotiationInitiateRequestDto {
    @NotBlank(message = "connectorAddress is mandatory")
    private String connectorAddress;
    @NotBlank(message = "protocol is mandatory")
    private String protocol = "ids-multipart";
    @NotBlank(message = "connectorId is mandatory")
    private String connectorId;
    @NotNull
    private String providerAgentId;
    @NotNull
    private String consumerAgentId;
    @NotNull(message = "offer cannot be null")
    private ContractOfferDescription offer;

    private NegotiationInitiateRequestDto() {

    }

    public String getConnectorAddress() {
        return connectorAddress;
    }

    public String getProtocol() {
        return protocol;
    }

    public String getConnectorId() {
        return connectorId;
    }

    public String getProviderAgentId() {
        return providerAgentId;
    }

    public String getConsumerAgentId() {
        return consumerAgentId;
    }

    public ContractOfferDescription getOffer() {
        return offer;
    }

    public static final class Builder {
        private final NegotiationInitiateRequestDto dto;

        private Builder() {
            dto = new NegotiationInitiateRequestDto();
        }

        public static Builder newInstance() {
            return new Builder();
        }

        public Builder connectorAddress(String connectorAddress) {
            dto.connectorAddress = connectorAddress;
            return this;
        }

        public Builder protocol(String protocol) {
            dto.protocol = protocol;
            return this;
        }

        public Builder connectorId(String connectorId) {
            dto.connectorId = connectorId;
            return this;
        }

        public Builder providerAgentId(String providerAgentId) {
            dto.providerAgentId = providerAgentId;
            return this;
        }

        public Builder consumerAgentId(String consumerAgentId) {
            dto.consumerAgentId = consumerAgentId;
            return this;
        }

        public Builder offer(ContractOfferDescription offer) {
            dto.offer = offer;
            return this;
        }

        public NegotiationInitiateRequestDto build() {
            return dto;
        }
    }
}
