package com.example.pidev.utils;

import com.example.pidev.service.event.EventTicketService;

/**
 * Utilitaire pour migrer les tickets avec QR codes manquants
 * Utilise QuickChart.io pour générer les QR codes dynamiques
 * @author Ons Abdesslem
 */
public class QRCodeMigration {

    /**
     * Générer les QR codes manquants pour tous les tickets
     * À appeler une seule fois pour migrer les données existantes
     */
    public static void generateAllMissingQRCodes() {
        System.out.println("🔄 Démarrage de la migration des QR codes manquants...");
        System.out.println("📊 Cela peut prendre quelques secondes selon le nombre de tickets...\n");

        EventTicketService ticketService = new EventTicketService();
        int updatedCount = ticketService.generateMissingQRCodes();

        System.out.println("\n✅ Migration terminée !");
        System.out.println("📊 Total de tickets mis à jour: " + updatedCount);
    }

    /**
     * Point d'entrée pour exécuter la migration
     * À utiliser lors du démarrage de l'application ou en standalone
     */
    public static void main(String[] args) {
        System.out.println("╔════════════════════════════════════════════╗");
        System.out.println("║  MIGRATION QR CODES - QuickChart.io        ║");
        System.out.println("║  EventFlow v1.0 - Ticket Management        ║");
        System.out.println("╚════════════════════════════════════════════╝\n");

        generateAllMissingQRCodes();

        System.out.println("\n✨ Vous pouvez maintenant lancer l'application normalement.");
    }
}
