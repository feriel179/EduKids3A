package com.ecom.service;

import com.ecom.model.Commande;
import com.ecom.model.CommandeStatut;
import com.ecom.model.Produit;
import com.ecom.persistence.CommandeRepository;
import com.ecom.persistence.DatabaseConfig;
import com.ecom.persistence.PromoCodeRepository;
import com.ecom.validation.CommandeValidator;
import com.ecom.validation.ValidationException;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;

public class CommandeService {

    private static final double DEFAULT_PROMO_PERCENT = 10.0;

    private final CommandeRepository commandeRepository;
    private final PromoCodeRepository promoCodeRepository;
    private final CommandeValidator commandeValidator;

    public CommandeService() {
        this.commandeRepository = new CommandeRepository();
        this.promoCodeRepository = new PromoCodeRepository();
        this.commandeValidator = new CommandeValidator();
    }

    public List<Commande> getAllCommandes() throws SQLException {
        return commandeRepository.findAll();
    }

    public CommandeCreationResult creerCommande(Produit produit, String quantiteText, String commentaire)
            throws ValidationException, SQLException {
        return creerCommande(produit, quantiteText, commentaire, null);
    }

    public CommandeCreationResult creerCommande(Produit produit, String quantiteText, String commentaire, String promoCodeText)
            throws ValidationException, SQLException {
        int quantite = commandeValidator.validateCreation(produit, quantiteText, commentaire);
        double montantInitial = produit.getPrix() * quantite;
        double remise = 0.0;
        String codePromoUtilise = null;
        String promoCode = promoCodeText == null ? "" : promoCodeText.trim();

        try (Connection connection = DatabaseConfig.getConnection()) {
            connection.setAutoCommit(false);
            try {
                PromoCodeRepository.PromoApplication promoApplication = null;
                if (!promoCode.isBlank()) {
                    promoApplication = promoCodeRepository.findByCodeForUpdate(connection, promoCode);
                    if (promoApplication == null) {
                        throw new ValidationException("Code promo invalide.");
                    }
                    if (promoApplication.used()) {
                        throw new ValidationException("Ce code promo est deja utilise.");
                    }
                    remise = roundCurrency(montantInitial * promoApplication.discountPercent() / 100.0);
                    codePromoUtilise = promoApplication.code();
                }

                double montantFinal = roundCurrency(Math.max(0.0, montantInitial - remise));
                int commandeId = commandeRepository.saveWithPricing(
                        connection,
                        produit,
                        quantite,
                        commentaire,
                        montantInitial,
                        remise,
                        montantFinal,
                        codePromoUtilise
                );

                if (promoApplication != null) {
                    promoCodeRepository.markAsUsed(connection, promoApplication.id(), commandeId);
                }

                String nouveauCodePromo = promoCodeRepository.createPromoCode(
                        connection,
                        commandeId,
                        DEFAULT_PROMO_PERCENT
                );

                connection.commit();
                return new CommandeCreationResult(
                        montantInitial,
                        remise,
                        montantFinal,
                        codePromoUtilise,
                        nouveauCodePromo,
                        DEFAULT_PROMO_PERCENT
                );
            } catch (ValidationException | SQLException exception) {
                connection.rollback();
                throw exception;
            } finally {
                connection.setAutoCommit(true);
            }
        }
    }

    public void validerCommande(int commandeId) throws SQLException {
        commandeRepository.updateStatus(commandeId, CommandeStatut.VALIDEE);
    }

    public void refuserCommande(int commandeId) throws SQLException {
        commandeRepository.updateStatus(commandeId, CommandeStatut.REFUSEE);
    }

    public void annulerCommande(int commandeId) throws SQLException {
        commandeRepository.updateStatus(commandeId, CommandeStatut.ANNULEE);
    }

    private double roundCurrency(double value) {
        return Math.round(value * 100.0) / 100.0;
    }
}
