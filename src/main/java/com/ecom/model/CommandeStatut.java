package com.ecom.model;

public enum CommandeStatut {
    EN_ATTENTE("En attente"),
    VALIDEE("Validee"),
    REFUSEE("Refusee"),
    ANNULEE("Annulee");

    private final String label;

    CommandeStatut(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
