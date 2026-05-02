package com.ecom.service;

public record CommandeCreationResult(
        double montantInitial,
        double remise,
        double montantFinal,
        String codePromoUtilise,
        String nouveauCodePromo,
        double nouveauCodePromoPourcentage
) {
}

