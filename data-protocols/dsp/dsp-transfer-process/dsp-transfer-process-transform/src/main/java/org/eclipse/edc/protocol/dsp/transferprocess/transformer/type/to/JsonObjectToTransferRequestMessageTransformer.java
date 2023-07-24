/*
 *  Copyright (c) 2023 Fraunhofer Institute for Software and Systems Engineering
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Fraunhofer Institute for Software and Systems Engineering - initial API and implementation
 *
 */

package org.eclipse.edc.protocol.dsp.transferprocess.transformer.type.to;

import jakarta.json.JsonObject;
import org.eclipse.edc.connector.transfer.spi.types.protocol.TransferRequestMessage;
import org.eclipse.edc.jsonld.spi.transformer.AbstractJsonLdTransformer;
import org.eclipse.edc.spi.types.domain.DataAddress;
import org.eclipse.edc.transform.spi.TransformerContext;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;

import static org.eclipse.edc.jsonld.spi.PropertyAndTypeNames.DCT_FORMAT_ATTRIBUTE;
import static org.eclipse.edc.protocol.dsp.type.DspPropertyAndTypeNames.*;
import static org.eclipse.edc.protocol.dsp.type.DspTransferProcessPropertyAndTypeNames.DSPACE_PROPERTY_CONTRACT_AGREEMENT_ID;
import static org.eclipse.edc.protocol.dsp.type.DspTransferProcessPropertyAndTypeNames.DSPACE_PROPERTY_DATA_ADDRESS;

public class JsonObjectToTransferRequestMessageTransformer extends AbstractJsonLdTransformer<JsonObject, TransferRequestMessage> {

    public JsonObjectToTransferRequestMessageTransformer() {
        super(JsonObject.class, TransferRequestMessage.class);
    }

    @Override
    public @Nullable TransferRequestMessage transform(@NotNull JsonObject messageObject, @NotNull TransformerContext context) {
        var transferRequestMessageBuilder = TransferRequestMessage.Builder.newInstance();

        visitProperties(messageObject, k -> {
            switch (k) {
                case DSPACE_PROPERTY_PROCESS_ID:
                    return v -> transferRequestMessageBuilder.processId(transformString(v, context));
                case DSPACE_PROPERTY_CONTRACT_AGREEMENT_ID:
                    return v -> transferRequestMessageBuilder.contractId(transformString(v, context));
                case DSPACE_PROPERTY_CALLBACK_ADDRESS:
                    return v -> transferRequestMessageBuilder.callbackAddress(transformString(v, context));
                case DSPACE_PROPERTY_PROPERTIES:
                    return v -> transferRequestMessageBuilder.properties(parseProperties(String.valueOf(v)));
                default:
                    return doNothing();
            }
        });

        transferRequestMessageBuilder.dataDestination(createDataAddress(messageObject, context));

        return transferRequestMessageBuilder.build();
    }

    private Map<String, String> parseProperties(String value) {
        Map<String, String> deserializedMap = new HashMap<>();
        String[] keyValuePairs = value.substring(1, value.length() - 1).split(", ");
        for (String pair : keyValuePairs) {
            String[] entry = pair.split("=");
            deserializedMap.put("https://w3id.org/edc/v0.0.1/ns/assetUUID", entry[1].replace("}\"}", ""));
        }
        return deserializedMap;
    }

    // TODO replace with JsonObjectToDataAddressTransformer
    private DataAddress createDataAddress(@NotNull JsonObject requestObject, @NotNull TransformerContext context) {
        var dataAddressBuilder = DataAddress.Builder.newInstance();

        transformString(requestObject.get(DCT_FORMAT_ATTRIBUTE), dataAddressBuilder::type, context);

        var dataAddressObject = returnJsonObject(requestObject.get(DSPACE_PROPERTY_DATA_ADDRESS), context, DSPACE_PROPERTY_DATA_ADDRESS, false);
        if (dataAddressObject != null) {
            dataAddressObject.forEach((key, value) -> transformString(value, v -> dataAddressBuilder.property(key, v), context));
        }

        return dataAddressBuilder.build();
    }
}
