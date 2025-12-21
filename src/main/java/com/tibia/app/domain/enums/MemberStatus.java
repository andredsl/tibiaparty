package com.tibia.app.domain.enums;

public enum MemberStatus {
    PENDING,     // Aguardando aprovação do líder
    CONFIRMED,   // Aprovado
    REJECTED,    // Recusado pelo líder
    LEFT         // Saiu da PT
}
