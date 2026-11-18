package com.safedesk.sanction.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

public record PolicyUpdateRequestDTO(

    @NotNull
    Integer warningThreshold,

    @NotNull
    Integer autoCloseThreshold,

    @NotNull
    @Valid
    SuspensionDays suspensionDays
) {

    public record SuspensionDays(

        @NotNull
        Integer first,

        @NotNull
        Integer second,

        @NotNull
        Integer thirdOrMore
    ) {
    }
}
