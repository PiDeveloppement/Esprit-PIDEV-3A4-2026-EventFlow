package com.example.pidev.test;

import com.example.pidev.model.event.EventCategory;
import com.example.pidev.service.event.EventCategoryService;

import java.util.List;

/**
 * Classe de test pour les opérations CRUD sur EventCategory
 *
 * @author Ons Abdesslem
 * @version 1.0
 */
public class TestCategoryCRUD {

    public static void main(String[] args) {
        System.out.println("╔════════════════════════════════════════════════════════════╗");
        System.out.println("║        TEST CRUD - EVENT CATEGORY SERVICE                 ║");
        System.out.println("╚════════════════════════════════════════════════════════════╝");
        System.out.println();

        // Créer une instance du service
        EventCategoryService service = new EventCategoryService();

        // ==================== TEST 1: CREATE ====================
        System.out.println("\n📝 TEST 1: CRÉATION D'UNE NOUVELLE CATÉGORIE");
        System.out.println("─────────────────────────────────────────────────────");

        EventCategory newCategory = new EventCategory(
                "Sport",                                    // name
                "Événements sportifs et compétitions",      // description
                "⚽",                                        // icon
                "#E91E63"                                   // color (rose)
        );

        boolean added = service.addCategory(newCategory);

        if (added) {
            System.out.println("✅ Catégorie créée avec ID: " + newCategory.getId());
        } else {
            System.out.println("❌ Échec de la création");
        }


        // ==================== TEST 2: READ ALL ====================
        System.out.println("\n📋 TEST 2: RÉCUPÉRATION DE TOUTES LES CATÉGORIES");
        System.out.println("─────────────────────────────────────────────────────");

        List<EventCategory> allCategories = service.getAllCategories();

        System.out.println("Nombre total de catégories: " + allCategories.size());
        System.out.println();

        for (EventCategory cat : allCategories) {
            System.out.printf("ID: %-3d | %-15s | %s | Couleur: %-8s | Statut: %s | Events: %d%n",
                    cat.getId(),
                    cat.getName(),
                    cat.getIcon(),
                    cat.getColor(),
                    cat.getStatusBadge(),
                    cat.getEventCount()
            );
        }


        // ==================== TEST 3: READ BY ID ====================
        System.out.println("\n🔍 TEST 3: RÉCUPÉRATION PAR ID");
        System.out.println("─────────────────────────────────────────────────────");

        EventCategory foundCategory = service.getCategoryById(1);

        if (foundCategory != null) {
            System.out.println("✅ Catégorie trouvée:");
            System.out.println("   Nom: " + foundCategory.getDisplayName());
            System.out.println("   Description: " + foundCategory.getDescription());
            System.out.println("   Couleur: " + foundCategory.getColor());
            System.out.println("   Statut: " + foundCategory.getStatusBadge());
            System.out.println("   Événements: " + foundCategory.getEventCount());
        } else {
            System.out.println("❌ Catégorie non trouvée");
        }


        // ==================== TEST 4: UPDATE ====================
        System.out.println("\n✏️ TEST 4: MODIFICATION D'UNE CATÉGORIE");
        System.out.println("─────────────────────────────────────────────────────");

        if (foundCategory != null) {
            // Modifier la description et la couleur
            foundCategory.setDescription("Conférences académiques et séminaires professionnels - MISE À JOUR");
            foundCategory.setColor("#1976D2"); // Bleu plus foncé

            boolean updated = service.updateCategory(foundCategory);

            if (updated) {
                System.out.println("✅ Catégorie mise à jour avec succès");

                // Re-récupérer pour vérifier
                EventCategory verif = service.getCategoryById(foundCategory.getId());
                System.out.println("   Nouvelle description: " + verif.getDescription());
                System.out.println("   Nouvelle couleur: " + verif.getColor());
            } else {
                System.out.println("❌ Échec de la mise à jour");
            }
        }


        // ==================== TEST 5: ACTIVE CATEGORIES ====================
        System.out.println("\n✅ TEST 5: RÉCUPÉRATION DES CATÉGORIES ACTIVES");
        System.out.println("─────────────────────────────────────────────────────");

        List<EventCategory> activeCategories = service.getActiveCategories();

        System.out.println("Nombre de catégories actives: " + activeCategories.size());
        for (EventCategory cat : activeCategories) {
            System.out.println("   - " + cat.getDisplayName());
        }


        // ==================== TEST 6: COUNT ====================
        System.out.println("\n🔢 TEST 6: STATISTIQUES");
        System.out.println("─────────────────────────────────────────────────────");

        int totalCategories = service.countCategories();
        System.out.println("Nombre total de catégories: " + totalCategories);

        // Compter les événements par catégorie
        for (EventCategory cat : allCategories) {
            int eventCount = service.countEventsByCategory(cat.getId());
            System.out.println("   " + cat.getName() + ": " + eventCount + " événement(s)");
        }


        // ==================== TEST 7: VALIDATION ====================
        System.out.println("\n⚠️ TEST 7: VALIDATION DES DONNÉES");
        System.out.println("─────────────────────────────────────────────────────");

        // Test 7a: Nom vide
        EventCategory invalidCategory1 = new EventCategory("", "Test", "🎯", "#000000");
        boolean result1 = service.addCategory(invalidCategory1);
        System.out.println("Ajout catégorie avec nom vide: " + (result1 ? "❌ ERREUR" : "✅ Rejeté correctement"));

        // Test 7b: Nom dupliqué
        EventCategory invalidCategory2 = new EventCategory("Conférence", "Test doublon", "🎯", "#000000");
        boolean result2 = service.addCategory(invalidCategory2);
        System.out.println("Ajout catégorie avec nom dupliqué: " + (result2 ? "❌ ERREUR" : "✅ Rejeté correctement"));


        // ==================== TEST 8: DELETE (Optionnel) ====================
        System.out.println("\n🗑️ TEST 8: SUPPRESSION");
        System.out.println("─────────────────────────────────────────────────────");

        // Créer une catégorie temporaire pour la supprimer
        EventCategory tempCategory = new EventCategory(
                "Catégorie Test Suppression",
                "Catégorie créée uniquement pour tester la suppression",
                "🧪",
                "#9E9E9E"
        );

        if (service.addCategory(tempCategory)) {
            System.out.println("✅ Catégorie temporaire créée (ID: " + tempCategory.getId() + ")");

            // Tenter de la supprimer
            boolean deleted = service.deleteCategory(tempCategory.getId());

            if (deleted) {
                System.out.println("✅ Catégorie supprimée avec succès");
            } else {
                System.out.println("❌ Échec de la suppression");
            }
        }

        // Test de suppression d'une catégorie utilisée
        System.out.println("\nTest de suppression d'une catégorie utilisée par des événements:");
        boolean deletedUsed = service.deleteCategory(1); // ID 1 = Conférence (a des événements)
        System.out.println("Suppression catégorie avec événements: " + (deletedUsed ? "❌ ERREUR" : "✅ Bloqué correctement"));


        // ==================== RÉSUMÉ FINAL ====================
        System.out.println("\n╔════════════════════════════════════════════════════════════╗");
        System.out.println("║                    RÉSUMÉ DES TESTS                        ║");
        System.out.println("╚════════════════════════════════════════════════════════════╝");

        int finalCount = service.countCategories();
        System.out.println("📊 Nombre final de catégories: " + finalCount);

        System.out.println("\n✅ TOUS LES TESTS SONT TERMINÉS!");
        System.out.println("\n💡 Vérifie dans MySQL Workbench avec:");
        System.out.println("   SELECT * FROM event_category;");
    }
}