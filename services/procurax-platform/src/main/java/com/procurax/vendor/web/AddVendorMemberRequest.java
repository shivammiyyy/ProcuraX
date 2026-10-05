package com.procurax.vendor.web;

import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record AddVendorMemberRequest(@NotNull UUID userId) {
}
