# 🧪 GUIDE DE TEST - Auto-Refresh des Interfaces

## 🎯 Objectif
Vérifier que les modules **Sponsor Admin**, **Budget** et **Dépenses** se rafraîchissent **automatiquement toutes les 5 secondes** lorsque les données changent en base de données.

---

## ✅ Prérequis

- ✅ Application EventFlow compilée et prête à démarrer
- ✅ Base de données avec tables sponsors, budget, depense
- ✅ Droits admin/organisateur pour accéder aux modules
- ✅ Terminal ouvert pour voir les logs

---

## 🚀 ÉTAPE 1 : Démarrer l'application

```bash
cd C:\Desktop\PIDEVDD\pidevv\pi25-01-2026
.\mvnw.cmd javafx:run
```

**Attendez** que l'application se lance complètement. Vous devriez voir les logs :
```
✅ Auto-refresh Sponsor Admin démarré (tous les 5s)
✅ Auto-refresh Budget démarré (tous les 5s)
✅ Auto-refresh Dépenses démarré (tous les 5s)
```

---

## 🧪 ÉTAPE 2 : Test du module SPONSOR

### Test 2.1 : Ajouter un nouveau Sponsor
1. **Cliquez** sur **Sponsors > Sponsors** (liste admin)
2. **Observez** la liste initiale et le nombre de sponsors
3. **Ouvrez** **une deuxième fenêtre** avec MySQL Workbench ou DBeaver
4. **Insérez** un nouveau sponsor dans la table `sponsors` :
   ```sql
   INSERT INTO sponsors (company_name, contact_email, contact_phone, contribution, event_id, created_at)
   VALUES ('NouveauSponssor Inc', 'nouveau@test.com', '+216 12345678', 50000, 1, NOW());
   ```
5. **Observez** la liste sponsor dans l'interface
   - ❌ Avant (< 5s) : Sponsor n'apparaît pas
   - ✅ Après (5s) : Sponsor apparaît automatiquement ! 🎉

### Test 2.2 : Modifier un Sponsor
1. **Modifiez** un sponsor existant en base :
   ```sql
   UPDATE sponsors SET contribution = 75000 WHERE id = 1;
   ```
2. **Attendez 5 secondes**
3. **Vérifiez** que la contribution est mise à jour dans l'interface ✅

### Test 2.3 : Supprimer un Sponsor
1. **Supprimez** un sponsor :
   ```sql
   DELETE FROM sponsors WHERE id = 5;
   ```
2. **Attendez 5 secondes**
3. **Vérifiez** que le sponsor disparaît de la liste ✅

---

## 💰 ÉTAPE 3 : Test du module BUDGET

### Test 3.1 : Ajouter un nouveau Budget
1. **Cliquez** sur **Sponsors > Budget**
2. **Notez** le nombre initial de budgets
3. **Insérez** un nouveau budget en base :
   ```sql
   INSERT INTO budget (event_id, initial_budget, total_revenue, total_expenses, budget_date, created_at)
   VALUES (2, 100000, 50000, 30000, '2026-05-14', NOW());
   ```
4. **Attendez 5 secondes**
5. **Vérifiez** que le budget apparaît dans la liste ✅

### Test 3.2 : Vérifier les KPI
1. **Observez** les KPI en haut de la page (Nombre, Budget initial, ROI, Déficit)
2. **Modifiez** les budgets en base :
   ```sql
   UPDATE budget SET initial_budget = 150000 WHERE id = 1;
   ```
3. **Attendez 5 secondes**
4. **Vérifiez** que le label "Budget initial" se met à jour ✅

### Test 3.3 : Vérifier les graphiques
1. **Attendez** que le graphique de comparaison des budgets se charge
2. **Modifiez** plusieurs budgets en base
3. **Observez** que le graphique se met à jour automatiquement ✅

---

## 📊 ÉTAPE 4 : Test du module DÉPENSES

### Test 4.1 : Ajouter une nouvelle Dépense
1. **Cliquez** sur **Sponsors > Dépenses**
2. **Notez** le nombre initial de dépenses
3. **Insérez** une dépense :
   ```sql
   INSERT INTO depense (description, category, amount, expense_date, budget_id, created_at)
   VALUES ('Nouvelle dépense test', 'Transport', 1500.00, '2026-05-14', 1, NOW());
   ```
4. **Attendez 5 secondes**
5. **Vérifiez** que la dépense apparaît ✅

### Test 4.2 : Vérifier les statistiques
1. **Observez** les KPI (Total, Moyenne, Catégories)
2. **Modifiez** une dépense existante :
   ```sql
   UPDATE depense SET amount = 2500 WHERE id = 1;
   ```
3. **Attendez 5 secondes**
4. **Vérifiez** que le total se met à jour ✅

### Test 4.3 : Vérifier le graphique PieChart
1. **Observez** le graphique de répartition par catégorie
2. **Insérez** une dépense avec une nouvelle catégorie :
   ```sql
   INSERT INTO depense (description, category, amount, expense_date, budget_id)
   VALUES ('Autre dépense', 'Logistique', 3000, '2026-05-14', 1);
   ```
3. **Attendez 5 secondes**
4. **Vérifiez** que le graphique se met à jour ✅

---

## 🔍 ÉTAPE 5 : Tests avancés

### Test 5.1 : Stress Test (ajout en masse)
1. **Insérez** rapidement 10 sponsors/budgets/dépenses en base
2. **Observez** que tout s'ajoute correctement après chaque cycle de 5s ✅

### Test 5.2 : Filtrage après auto-refresh
1. **Ouvrez** le module Sponsor
2. **Appliquez** un filtre (ex: par entreprise)
3. **Insérez** un nouveau sponsor **qui match le filtre**
4. **Attendez 5 secondes**
5. **Vérifiez** qu'il apparaît immédiatement ✅

### Test 5.3 : Pagination/Scroll
1. **Insérez** beaucoup de données (>100)
2. **Scrollez** dans la liste
3. **Insérez** une nouvelle donnée
4. **Vérifiez** que le scroll fonctionne toujours ✅

---

## 📊 CHECKLIST DE VALIDATION

| Test | Sponsor | Budget | Dépenses | Résultat |
|------|---------|--------|----------|----------|
| Ajouter | ✅ | ✅ | ✅ | PASS ✅ |
| Modifier | ✅ | ✅ | ✅ | PASS ✅ |
| Supprimer | ✅ | ✅ | ✅ | PASS ✅ |
| KPI mis à jour | ✅ | ✅ | ✅ | PASS ✅ |
| Graphiques mis à jour | ✅ | ✅ | ✅ | PASS ✅ |
| Filtres réappliqués | ✅ | ✅ | ✅ | PASS ✅ |
| Timing ~5s | ✅ | ✅ | ✅ | PASS ✅ |
| Pas de lag UI | ✅ | ✅ | ✅ | PASS ✅ |
| Logs affichés | ✅ | ✅ | ✅ | PASS ✅ |

---

## 🛠️ TROUBLESHOOTING

### ❌ Problème : Les données ne se mettent pas à jour

**Solution 1** : Vérifiez les logs dans la console
```
❌ Si vous voyez: ⚠️ Erreur auto-refresh: ...
→ Vérifiez la connexion à la base de données
```

**Solution 2** : Vérifiez le timing
```
✅ Correctement: mise à jour après ~5 secondes
❌ Incorrect: plus de 10 secondes → vérifiez la BD lenteur
```

**Solution 3** : Vérifiez la base de données
```sql
-- Vérifiez que les données sont bien insérées
SELECT * FROM sponsors ORDER BY created_at DESC LIMIT 1;
```

### ❌ Problème : L'application freeze

**Solution** : Les opérations UI longues peuvent bloquer le rafraîchissement
→ Réduisez les données ou optimisez les graphiques

### ❌ Problème : Les filtres ne s'appliquent pas

**Solution** : `applyPredicate()` est appelé après chaque refresh
→ Vérifiez que vos filtres ComboBox ont des valeurs valides

---

## 📝 NOTES D'OBSERVATION

Pendant vos tests, notez :

1. **Timing de rafraîchissement** : Temps exact entre modification et apparition
   ```
   Envoyé: 14:32:10
   Mis à jour dans l'interface: 14:32:15
   Timing: 5s ✅
   ```

2. **Fluidité UI** : L'interface reste-t-elle réactive ?
   ```
   ✅ Réactive (pas de lag)
   ✅ Pas de scintillement
   ✅ Animations fluides
   ```

3. **Précision des données** : Les chiffres changent-ils correctement ?
   ```
   Avant: 50 sponsors
   Après insertion: 51 sponsors ✅
   ```

4. **Graphiques** : Se mettent-ils à jour automatiquement ?
   ```
   ✅ Oui, sans intervention
   ✅ Légendes actualisées
   ```

---

## 🎓 CONCEPTS TESTÉS

- ✅ **Thread-safety** : `Platform.runLater()` pour UI updates
- ✅ **Observable Pattern** : `baseList.setAll()` déclenche updates
- ✅ **Async Refresh** : Queries en thread séparé
- ✅ **Data Binding** : ObservableList → TableView/Cards
- ✅ **Filter Predicate** : Réapplication après refresh
- ✅ **KPI Updates** : Statistiques en temps quasi-réel
- ✅ **Chart Rendering** : Graphiques générés automatiquement

---

## 🎯 RÉSULTAT ATTENDU

Après tous les tests, vous devriez observer :

1. ✅ Les 3 modules rafraîchissent **automatiquement**
2. ✅ Delai de ~**5 secondes** entre modification et affichage
3. ✅ **Aucune action utilisateur** requise
4. ✅ UI reste **fluide et réactive**
5. ✅ **Filtres** réappliqués correctement
6. ✅ **Graphiques** se mettent à jour
7. ✅ **KPI** affichent les derniers chiffres
8. ✅ **Status label** indique la mise à jour ✅

---

## 📞 SUPPORT

Si vous rencontrez des problèmes :

1. Vérifiez les logs console pour `⚠️ Erreur`
2. Testez la BD avec une requête directe
3. Vérifiez la connectivité réseau (si BD distante)
4. Augmentez `REFRESH_INTERVAL_SECONDS` si la BD est lente

---

**Test Date** : ___________
**Tester** : ___________
**Résultat** : ✅ PASS / ❌ FAIL

---

*Dernière mise à jour : 2026-05-14*

