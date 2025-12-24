package com.tibia.app.domain.enums;

public enum BetStatus {
    PENDING("Pendente"),      // Aguardando confirmacao de pagamento
    CONFIRMED("Confirmada"),  // Pagamento confirmado, aguardando resultado
    WON("Ganhou"),           // Aposta vencedora
    LOST("Perdeu"),          // Aposta perdedora
    REFUNDED("Reembolsada"), // Aposta reembolsada
    CANCELLED("Cancelada");  // Aposta cancelada

    private final String displayName;

    BetStatus(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

    public boolean isPending() {
        return this == PENDING;
    }

    public boolean isConfirmed() {
        return this == CONFIRMED;
    }

    public boolean isFinalized() {
        return this == WON || this == LOST || this == REFUNDED || this == CANCELLED;
    }
}
