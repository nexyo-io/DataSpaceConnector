/*
 *  Copyright (c) 2023 Bayerische Motoren Werke Aktiengesellschaft (BMW AG)
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Bayerische Motoren Werke Aktiengesellschaft (BMW AG) - initial API and implementation
 *
 */

package org.eclipse.edc.identitytrust.model;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.util.ArrayList;

import static java.time.Instant.now;
import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.eclipse.edc.identitytrust.model.CredentialFormat.JSON_LD;

class VerifiableCredentialTest {

    @BeforeEach
    void setUp() {
    }

    @Test
    void buildMinimalVc() {
        assertThatNoException().isThrownBy(() -> VerifiableCredential.Builder.newInstance("rest-vc", JSON_LD)
                .credentialSubject(new CredentialSubject())
                .issuer(URI.create("http://test.issuer"))
                .issuanceDate(now())
                .type("test-type")
                .build());
    }

    @Test
    void assertDefaultValues() {
        var vc = VerifiableCredential.Builder.newInstance("rest-vc", JSON_LD)
                .credentialSubject(new CredentialSubject())
                .issuer(URI.create("http://test.issuer"))
                .issuanceDate(now())
                .type("test-type")
                .build();
    }

    @Test
    void build_emptyContexts() {
        assertThatThrownBy(() -> VerifiableCredential.Builder.newInstance("rest-vc", JSON_LD)
                .credentialSubject(new CredentialSubject())
                .issuer(URI.create("http://test.issuer"))
                .issuanceDate(now())
                .build())
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void build_emptyTypes() {
        assertThatThrownBy(() -> VerifiableCredential.Builder.newInstance("rest-vc", JSON_LD)
                .credentialSubject(new CredentialSubject())
                .issuer(URI.create("http://test.issuer"))
                .issuanceDate(now())
                .types(new ArrayList<>())
                .build())
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void build_emptyProofs() {
        assertThatThrownBy(() -> VerifiableCredential.Builder.newInstance("rest-vc", JSON_LD)
                .credentialSubject(new CredentialSubject())
                .issuer(URI.create("http://test.issuer"))
                .issuanceDate(now())
                .build())
                .isInstanceOf(IllegalArgumentException.class);
    }

}