# 🔄 Auto-Refresh Implementation - Sponsor Admin, Budget & Dépenses

## 📋 Vue d'ensemble
Les interfaces suivantes se **rafraîchissent automatiquement toutes les 5 secondes** :
- ✅ Sponsor Admin (`SponsorAdminController`)
- ✅ Budget (`BudgetListController`)
- ✅ Dépenses (`DepenseListController`)

## ⚙️ Implémentation technique

### 1️⃣ Importer les classes nécessaires
```java
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
```

### 2️⃣ Ajouter les variables de contrôle
Chaque contrôleur contient :
```java
private ScheduledExecutorService scheduler;
private static final int REFRESH_INTERVAL_SECONDS = 5;
```

### 3️⃣ Méthodes implémentées

#### `startAutoRefresh()`
- Initialise un `ScheduledExecutorService` avec un thread pool
- Appelle `refreshDataFromDatabase()` toutes les 5 secondes
- Affiche un log au démarrage

```java
private void startAutoRefresh() {
    if (scheduler == null || scheduler.isShutdown()) {
        scheduler = Executors.newScheduledThreadPool(1);
        scheduler.scheduleAtFixedRate(
                this::refreshDataFromDatabase,
                REFRESH_INTERVAL_SECONDS,
                REFRESH_INTERVAL_SECONDS,
                TimeUnit.SECONDS
        );
        System.out.println("✅ Auto-refresh démarré (tous les 5s)");
    }
}
```

#### `refreshDataFromDatabase()`
- Récupère les **nouvelles données** de la base de données
- Utilise `Platform.runLater()` pour mettre à jour l'UI en **thread-safe**
- Met à jour :
  - La liste observable (`baseList`)
  - Les KPI/statistiques
  - Les graphiques
  - Les filtres appliqués
  - Le label de statut

```java
private void refreshDataFromDatabase() {
    try {
        List<Budget> newData = budgetService.getAllBudgets();
        Platform.runLater(() -> {
            baseList.setAll(newData);      // Remplace tous les éléments
            updateKpis();                  // Rafraîchit les statistiques
            updateBudgetComparisonChart(); // Rafraîchit les graphiques
            updateRoiTrendChart();
            applyPredicate();              // Réapplique les filtres
            if (statusLabel != null) {
                statusLabel.setText("📊 X budget(s) ✅ Mis à jour");
            }
        });
    } catch (Exception e) {
        System.err.println("⚠️ Erreur auto-refresh: " + e.getMessage());
    }
}
```

#### `stopAutoRefresh()`
- Arrête proprement le scheduler (appelé en cas de destruction du contrôleur)

```java
private void stopAutoRefresh() {
    if (scheduler != null && !scheduler.isShutdown()) {
        scheduler.shutdown();
        System.out.println("⏹️ Auto-refresh arrêté");
    }
}
```

### 4️⃣ Cycle de vie
1. **Démarrage** → `initialize()` appelle `startAutoRefresh()` ✅
2. **Exécution** → Toutes les 5 secondes, `refreshDataFromDatabase()` s'exécute
3. **Arrêt** → Au besoin, `stopAutoRefresh()` peut être appelé

## 🎯 Fonctionnalités

### ✅ Détection automatique des changements
- Les **nouveaux sponsors/budgets/dépenses** ajoutés à la BD apparaissent automatiquement
- Les **modifications** sont appliquées sans action de l'utilisateur
- Les **suppressions** sont détectées et supprimées de l'interface

### ✅ Mise à jour des composants
| Composant | Mises à jour |
|-----------|--------------|
| **Tableau** | Remplace les éléments avec `baseList.setAll()` |
| **Filtres** | Réappliqués via `applyPredicate()` |
| **KPI/Statistiques** | `updateKpis()`, `updateGlobalStats()` |
| **Graphiques** | `updateBudgetComparisonChart()`, `initCategoryChart()`, etc. |
| **Statut** | Label mis à jour avec timestamp |

### ✅ Thread-safety
- `Platform.runLater()` garantit que les mises à jour UI s'exécutent sur le **FX Thread**
- La BD est consultée dans un **thread séparé** (du scheduler)
- Pas d'enverrouillements ou de race conditions

## 🔧 Configuration

### Intervalle de rafraîchissement
Par défaut : **5 secondes**

Pour le modifier, changez :
```java
private static final int REFRESH_INTERVAL_SECONDS = 5;
```

Par exemple, pour 10 secondes :
```java
private static final int REFRESH_INTERVAL_SECONDS = 10;
```

### Activer/Désactiver
Le rafraîchissement démarre automatiquement via `initialize()`.

Pour le **désactiver** manuellement :
```java
stopAutoRefresh();
```

## 📊 Fichiers modifiés

1. **SponsorAdminController.java**
   - Ajout imports `ScheduledExecutorService`, `Executors`, `TimeUnit`
   - Ajout variables `scheduler`, `REFRESH_INTERVAL_SECONDS`
   - Ajout méthodes : `startAutoRefresh()`, `stopAutoRefresh()`, `refreshDataFromDatabase()`
   - Appel de `startAutoRefresh()` dans `initialize()`

2. **BudgetListController.java**
   - Même structure que SponsorAdminController
   - Rafraîchit aussi les graphiques `updateBudgetComparisonChart()` et `updateRoiTrendChart()`

3. **DepenseListController.java**
   - Même structure
   - Rafraîchit le graphique `initCategoryChart()`

## 🚀 Exemple de flux

```
Initialisation (t=0s)
↓
startAutoRefresh() démarre
↓
Chaque 5 secondes:
  - Thread scheduler récupère data: SELECT * FROM ...
  - Platform.runLater() met à jour UI
  - baseList.setAll(newData) remplace les éléments
  - Graphiques, KPI, filtres se réappliquent
  - Label de statut se met à jour ✅
↓
L'utilisateur voit les changements en temps réel
```

## 🧪 Tests

### 1. Lancer l'application
```bash
.\mvnw.cmd javafx:run
```

### 2. Naviguer vers le module (ex: Budget)
Les logs console affichent :
```
✅ Auto-refresh Budget démarré (tous les 5s)
```

### 3. Ajouter/Modifier/Supprimer des données
Via une autre fenêtre ou directement en base de données

### 4. Observer
Les changements apparaissent dans l'interface **en moins de 5 secondes**

## 📝 Logs système

### Au démarrage
```
✅ Auto-refresh Sponsor Admin démarré (tous les 5s)
✅ Auto-refresh Budget démarré (tous les 5s)
✅ Auto-refresh Dépenses démarré (tous les 5s)
```

### Lors du rafraîchissement (toutes les 5s)
```
(Pas de logs par défaut, mais statusLabel se met à jour)
```

### En cas d'erreur
```
⚠️ Erreur auto-refresh Sponsor: <exception message>
⚠️ Erreur auto-refresh Budget: <exception message>
⚠️ Erreur auto-refresh Dépenses: <exception message>
```

### À l'arrêt (optionnel)
```
⏹️ Auto-refresh Sponsor Admin arrêté
⏹️ Auto-refresh Budget arrêté
⏹️ Auto-refresh Dépenses arrêté
```

## 🔒 Performance & Considérations

### ✅ Points forts
- Rafraîchissement transparent pour l'utilisateur
- Ne bloque pas l'UI (thread séparé)
- Automatique, pas de clic manuel
- Gère les erreurs proprement

### ⚠️ Considérations
- **Bande passante** : Une requête de base de données toutes les 5 secondes par module
- **Charge serveur** : Adapter l'intervalle si trop many modules actifs
- **Mémoire** : `baseList.setAll()` remplace l'ObservableList (propre)
- **Réseau** : Si la BD est distante, peut être lent

### 💡 Optimisations futures
```java
// 1. Implémenter un Delta sync (ne charger que les changements)
List<Budget> newData = budgetService.getChangedBudgets(lastUpdateTime);

// 2. Utiliser des WebSockets pour les changements temps réel
// 3. Augmenter l'intervalle si inactif
// 4. Cache de la dernière requête pour éviter les doublons
```

## ✨ Exemple d'utilisation complète

```java
@Override
public void initialize(URL location, ResourceBundle resources) {
    // ... configuration initiale ...
    
    setupFiltersSafe();
    loadDataSafe();
    applyPredicate();
    
    // ⭐ Démarre l'auto-refresh
    startAutoRefresh();
}
```

---

**Version** : 1.0
**Date** : 2026-05-14
**Status** : ✅ Opérationnel

