package com.tibia.app.domain.enums;

public enum JoinRequestStatus {
    PENDING,    // Aguardando resposta do lider
    ACCEPTED,   // Aceito pelo lider
    REJECTED,   // Recusado pelo lider
    CANCELLED   // Cancelado pelo solicitante
}
