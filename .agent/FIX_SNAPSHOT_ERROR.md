# Résolution de l'erreur "Cannot snapshot"

## 🔍 Erreur Rencontrée

```
Cannot snapshot C:\Users\Dell\OneDrive\Documents\FitGym_yas\app\src\main\res\anim\button_click.xml: not a regular file
```

## 📋 Causes Possibles

1. **Conflit avec OneDrive** - Le fichier est en cours de synchronisation
2. **Cache Gradle corrompu** - Les fichiers temporaires sont invalides
3. **Permissions de fichiers** - Windows/OneDrive bloque l'accès
4. **Index Android Studio** - Le cache d'indexation est corrompu

## ✅ Solutions (à essayer dans l'ordre)

### **Solution 1 : Clean & Rebuild dans Android Studio**

1. Dans Android Studio, cliquer sur **Build** → **Clean Project**
2. Attendre que le nettoyage se termine
3. Cliquer sur **Build** → **Rebuild Project**

### **Solution 2 : Invalider les Caches d'Android Studio**

1. **File** → **Invalidate Caches / Restart...**
2. Cocher toutes les options :
   - ✅ Clear file system cache and Local History
   - ✅ Clear downloaded shared indexes
   - ✅ Clear VCS Log caches and indexes
3. Cliquer sur **"Invalidate and Restart"**

### **Solution 3 : Supprimer manuellement le cache Gradle**

Exécutez ces commandes dans PowerShell :

```powershell
# Dans le dossier du projet
cd "C:\Users\Dell\OneDrive\Documents\FitGym_yas"

# Arrêter le daemon Gradle
.\gradlew --stop

# Nettoyer le projet
.\gradlew clean

# Supprimer le dossier build
Remove-Item -Recurse -Force .\app\build -ErrorAction SilentlyContinue
Remove-Item -Recurse -Force .\.gradle -ErrorAction SilentlyContinue

# Rebuild
.\gradlew build
```

### **Solution 4 : Exclure temporairement le dossier de OneDrive**

Si OneDrive cause le problème :

1. Ouvrir l'**Explorateur Windows**
2. Naviguer vers : `C:\Users\Dell\OneDrive\Documents\FitGym_yas\app\src\main\res\anim`
3. Clic droit sur le dossier `anim` → **Toujours garder sur cet appareil**
4. Attendre que OneDrive finisse la synchronisation (icône verte ✓)

**OU** Déplacer temporairement le projet hors de OneDrive :

```powershell
# Copier le projet hors de OneDrive
xcopy "C:\Users\Dell\OneDrive\Documents\FitGym_yas" "C:\Users\Dell\FitGym_yas_local" /E /I /H

# Travailler depuis le nouveau dossier
cd "C:\Users\Dell\FitGym_yas_local"
```

### **Solution 5 : Vérifier les permissions du fichier**

```powershell
# Vérifier les permissions
icacls "C:\Users\Dell\OneDrive\Documents\FitGym_yas\app\src\main\res\anim\button_click.xml"

# Réparer les permissions si nécessaire
icacls "C:\Users\Dell\OneDrive\Documents\FitGym_yas\app\src\main\res\anim" /reset /T
```

### **Solution 6 : Recréer le fichier button_click.xml**

Si tout le reste échoue, supprimez et recréez le fichier :

1. Supprimer `button_click.xml`
2. Créer un nouveau fichier avec le même nom
3. Copier ce contenu :

```xml
<scale xmlns:android="http://schemas.android.com/apk/res/android"
    android:fromXScale="1.0"
    android:toXScale="0.95"
    android:fromYScale="1.0"
    android:toYScale="0.95"
    android:pivotX="50%"
    android:pivotY="50%"
    android:duration="100"
    android:repeatMode="reverse"
    android:repeatCount="1"/>
```

## 🎯 Solution Rapide (Recommandée)

La solution la plus rapide est généralement :

1. **File** → **Invalidate Caches / Restart** dans Android Studio
2. Puis **Build** → **Clean Project**
3. Puis **Build** → **Rebuild Project**

## 📝 Vérifications après la correction

Une fois le problème résolu, vérifiez que :
- ✅ Le projet compile sans erreur
- ✅ Tous les fichiers drawable sont accessibles
- ✅ L'aperçu des layouts fonctionne
- ✅ L'application se lance correctement

## ⚠️ Prévention future

Pour éviter ce problème à l'avenir :

1. **Désactiver la synchronisation OneDrive pour les dossiers build** :
   - Exclure `FitGym_yas\.gradle`
   - Exclure `FitGym_yas\app\build`
   - Exclure `FitGym_yas\.idea`

2. **Utiliser un .gitignore approprié** pour éviter de synchroniser les fichiers temporaires

3. **Considérer de déplacer le projet hors de OneDrive** vers un dossier local comme `C:\AndroidProjects\`

---

**Date de création:** 2025-11-29  
**Problème:** Cannot snapshot .xml files  
**Projet:** FitGym Android
