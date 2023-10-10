/*
 *  Copyright (c) 2022 Bayerische Motoren Werke Aktiengesellschaft (BMW AG)
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Bayerische Motoren Werke Aktiengesellschaft (BMW AG) - Initial implementation
 *
 */

package org.eclipse.edc.connector.contract.policy;

import org.eclipse.edc.connector.contract.spi.negotiation.store.ContractNegotiationStore;
import org.eclipse.edc.connector.contract.spi.types.agreement.ContractAgreement;
import org.eclipse.edc.connector.contract.spi.types.negotiation.ContractNegotiation;
import org.eclipse.edc.connector.contract.spi.types.negotiation.ContractNegotiationStates;
import org.eclipse.edc.connector.policy.spi.store.PolicyArchive;
import org.eclipse.edc.policy.model.Policy;
import org.eclipse.edc.spi.query.QuerySpec;

import java.util.Optional;

import static org.eclipse.edc.spi.query.Criterion.criterion;

public class PolicyArchiveImpl implements PolicyArchive {
    private final ContractNegotiationStore contractNegotiationStore;

    public PolicyArchiveImpl(ContractNegotiationStore contractNegotiationStore) {
        this.contractNegotiationStore = contractNegotiationStore;
    }

    @Override
    public Policy findPolicyForContract(String contractId) {
        var expressionContractId = criterion("contractAgreement.id", "=", contractId);
        var expressionNegotiationStatus = criterion("state", "=", ContractNegotiationStates.FINALIZED.code());
        var query = QuerySpec.Builder.newInstance().filter(expressionContractId).filter(expressionNegotiationStatus).build();

        return contractNegotiationStore.queryNegotiations(query)
                .findFirst()
                .map(ContractNegotiation::getContractAgreement)
                .map(ContractAgreement::getPolicy)
                .orElse(null);
    }

}
