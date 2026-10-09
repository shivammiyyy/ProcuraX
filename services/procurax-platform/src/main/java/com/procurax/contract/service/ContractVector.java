package com.procurax.contract.service;

import java.util.List;
import java.util.stream.Collectors;

final class ContractVector {

    private ContractVector() {
    }

    static String toLiteral(List<Double> values) {
        return "[" + values.stream().map(value -> Double.toString(value))
                .collect(Collectors.joining(",")) + "]";
    }
}
