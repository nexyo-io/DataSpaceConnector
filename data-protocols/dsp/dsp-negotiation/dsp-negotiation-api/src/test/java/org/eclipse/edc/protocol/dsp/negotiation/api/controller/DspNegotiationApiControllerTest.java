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

package org.eclipse.edc.protocol.dsp.negotiation.api.controller;

import io.restassured.specification.RequestSpecification;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.Response;
import org.eclipse.edc.connector.contract.spi.types.agreement.ContractAgreementMessage;
import org.eclipse.edc.connector.contract.spi.types.agreement.ContractAgreementVerificationMessage;
import org.eclipse.edc.connector.contract.spi.types.agreement.ContractNegotiationEventMessage;
import org.eclipse.edc.connector.contract.spi.types.negotiation.ContractNegotiation;
import org.eclipse.edc.connector.contract.spi.types.negotiation.ContractNegotiationTerminationMessage;
import org.eclipse.edc.connector.contract.spi.types.negotiation.ContractOfferMessage;
import org.eclipse.edc.connector.contract.spi.types.negotiation.ContractRequestMessage;
import org.eclipse.edc.connector.spi.contractnegotiation.ContractNegotiationProtocolService;
import org.eclipse.edc.junit.annotations.ApiTest;
import org.eclipse.edc.protocol.dsp.spi.message.DspRequestHandler;
import org.eclipse.edc.protocol.dsp.spi.message.GetDspRequest;
import org.eclipse.edc.protocol.dsp.spi.message.PostDspRequest;
import org.eclipse.edc.transform.spi.TypeTransformerRegistry;
import org.eclipse.edc.web.jersey.testfixtures.RestControllerTestBase;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.ArgumentsProvider;
import org.junit.jupiter.params.provider.ArgumentsSource;
import org.mockito.ArgumentCaptor;

import java.util.stream.Stream;

import static io.restassured.RestAssured.given;
import static jakarta.json.Json.createObjectBuilder;
import static jakarta.ws.rs.core.MediaType.APPLICATION_JSON;
import static jakarta.ws.rs.core.MediaType.APPLICATION_JSON_TYPE;
import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.edc.protocol.dsp.negotiation.api.NegotiationApiPaths.AGREEMENT;
import static org.eclipse.edc.protocol.dsp.negotiation.api.NegotiationApiPaths.BASE_PATH;
import static org.eclipse.edc.protocol.dsp.negotiation.api.NegotiationApiPaths.CONTRACT_OFFER;
import static org.eclipse.edc.protocol.dsp.negotiation.api.NegotiationApiPaths.CONTRACT_REQUEST;
import static org.eclipse.edc.protocol.dsp.negotiation.api.NegotiationApiPaths.EVENT;
import static org.eclipse.edc.protocol.dsp.negotiation.api.NegotiationApiPaths.INITIAL_CONTRACT_REQUEST;
import static org.eclipse.edc.protocol.dsp.negotiation.api.NegotiationApiPaths.TERMINATION;
import static org.eclipse.edc.protocol.dsp.negotiation.api.NegotiationApiPaths.VERIFICATION;
import static org.eclipse.edc.protocol.dsp.type.DspNegotiationPropertyAndTypeNames.DSPACE_TYPE_CONTRACT_AGREEMENT_MESSAGE;
import static org.eclipse.edc.protocol.dsp.type.DspNegotiationPropertyAndTypeNames.DSPACE_TYPE_CONTRACT_AGREEMENT_VERIFICATION_MESSAGE;
import static org.eclipse.edc.protocol.dsp.type.DspNegotiationPropertyAndTypeNames.DSPACE_TYPE_CONTRACT_NEGOTIATION_ERROR;
import static org.eclipse.edc.protocol.dsp.type.DspNegotiationPropertyAndTypeNames.DSPACE_TYPE_CONTRACT_NEGOTIATION_EVENT_MESSAGE;
import static org.eclipse.edc.protocol.dsp.type.DspNegotiationPropertyAndTypeNames.DSPACE_TYPE_CONTRACT_NEGOTIATION_TERMINATION_MESSAGE;
import static org.eclipse.edc.protocol.dsp.type.DspNegotiationPropertyAndTypeNames.DSPACE_TYPE_CONTRACT_OFFER_MESSAGE;
import static org.eclipse.edc.protocol.dsp.type.DspNegotiationPropertyAndTypeNames.DSPACE_TYPE_CONTRACT_REQUEST_MESSAGE;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ApiTest
class DspNegotiationApiControllerTest extends RestControllerTestBase {

    private final TypeTransformerRegistry transformerRegistry = mock();
    private final ContractNegotiationProtocolService protocolService = mock();
    private final DspRequestHandler dspRequestHandler = mock();

    @Test
    void getNegotiation_shouldGetResource() {
        when(dspRequestHandler.getResource(any())).thenReturn(Response.ok().type(APPLICATION_JSON_TYPE).build());

        var result = baseRequest()
                .get(BASE_PATH + "negotiationId")
                .then()
                .contentType(APPLICATION_JSON)
                .statusCode(200);

        assertThat(result).isNotNull();
        var captor = ArgumentCaptor.forClass(GetDspRequest.class);
        verify(dspRequestHandler).getResource(captor.capture());
        var dspMessage = captor.getValue();
        assertThat(dspMessage.getToken()).isEqualTo("auth");
        assertThat(dspMessage.getId()).isEqualTo("negotiationId");
        assertThat(dspMessage.getResultClass()).isEqualTo(ContractNegotiation.class);
        assertThat(dspMessage.getErrorType()).isEqualTo(DSPACE_TYPE_CONTRACT_NEGOTIATION_ERROR);
    }

    @Test
    void initiateNegotiation_shouldCreateResource() {
        var requestBody = createObjectBuilder().add("@type", DSPACE_TYPE_CONTRACT_REQUEST_MESSAGE).build();
        when(dspRequestHandler.createResource(any())).thenReturn(Response.ok().type(APPLICATION_JSON_TYPE).build());

        var result = baseRequest()
                .contentType(APPLICATION_JSON)
                .body(requestBody)
                .post(BASE_PATH + INITIAL_CONTRACT_REQUEST)
                .then()
                .statusCode(200)
                .contentType(APPLICATION_JSON);

        assertThat(result).isNotNull();
        var captor = ArgumentCaptor.forClass(PostDspRequest.class);
        verify(dspRequestHandler).createResource(captor.capture());
        var request = captor.getValue();
        assertThat(request.getToken()).isEqualTo("auth");
        assertThat(request.getProcessId()).isEqualTo(null);
        assertThat(request.getMessage()).isNotNull();
        assertThat(request.getInputClass()).isEqualTo(ContractRequestMessage.class);
        assertThat(request.getResultClass()).isEqualTo(ContractNegotiation.class);
        assertThat(request.getExpectedMessageType()).isEqualTo(DSPACE_TYPE_CONTRACT_REQUEST_MESSAGE);
    }

    /**
     * Verifies that an endpoint returns 401 if the identity service cannot verify the identity token.
     *
     * @param path the request path to the endpoint
     */
    @ParameterizedTest
    @ArgumentsSource(ControllerMethodArguments.class)
    void callEndpoint_shouldUpdateResource(String path, Class<?> messageClass, String messageType) {
        when(dspRequestHandler.updateResource(any())).thenReturn(Response.ok().type(APPLICATION_JSON_TYPE).build());
        var requestBody = createObjectBuilder().add("http://schema/key", "value").build();

        baseRequest()
                .contentType(APPLICATION_JSON)
                .body(requestBody)
                .post(path)
                .then()
                .contentType(APPLICATION_JSON)
                .statusCode(200);

        var captor = ArgumentCaptor.forClass(PostDspRequest.class);
        verify(dspRequestHandler).updateResource(captor.capture());
        var request = captor.getValue();
        assertThat(request.getExpectedMessageType());
        assertThat(request.getToken()).isEqualTo("auth");
        assertThat(request.getProcessId()).isEqualTo("testId");
        assertThat(request.getMessage()).isNotNull();
        assertThat(request.getInputClass()).isEqualTo(messageClass);
        assertThat(request.getResultClass()).isEqualTo(ContractNegotiation.class);
        assertThat(request.getExpectedMessageType()).isEqualTo(messageType);
    }

    @Override
    protected Object controller() {
        return new DspNegotiationApiController(transformerRegistry, protocolService, mock(), dspRequestHandler);
    }

    private RequestSpecification baseRequest() {
        var authHeader = "auth";
        return given()
                .baseUri("http://localhost:" + port)
                .basePath("/")
                .header(HttpHeaders.AUTHORIZATION, authHeader)
                .when();
    }

    private static class ControllerMethodArguments implements ArgumentsProvider {
        @Override
        public Stream<? extends Arguments> provideArguments(ExtensionContext context) {
            return Stream.of(
                    Arguments.of(
                            BASE_PATH + "testId" + CONTRACT_REQUEST,
                            ContractRequestMessage.class,
                            DSPACE_TYPE_CONTRACT_REQUEST_MESSAGE),
                    Arguments.of(
                            BASE_PATH + "testId" + EVENT,
                            ContractNegotiationEventMessage.class,
                            DSPACE_TYPE_CONTRACT_NEGOTIATION_EVENT_MESSAGE),
                    Arguments.of(
                            BASE_PATH + "testId" + AGREEMENT + VERIFICATION,
                            ContractAgreementVerificationMessage.class,
                            DSPACE_TYPE_CONTRACT_AGREEMENT_VERIFICATION_MESSAGE),
                    Arguments.of(
                            BASE_PATH + "testId" + TERMINATION,
                            ContractNegotiationTerminationMessage.class,
                            DSPACE_TYPE_CONTRACT_NEGOTIATION_TERMINATION_MESSAGE),
                    Arguments.of(
                            BASE_PATH + "testId" + AGREEMENT,
                            ContractAgreementMessage.class,
                            DSPACE_TYPE_CONTRACT_AGREEMENT_MESSAGE),
                    Arguments.of(
                            BASE_PATH + "testId" + CONTRACT_OFFER,
                            ContractOfferMessage.class,
                            DSPACE_TYPE_CONTRACT_OFFER_MESSAGE)
            );
        }
    }

}
