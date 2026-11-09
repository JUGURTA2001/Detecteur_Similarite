# Détecteur de similarité entre codes sources

Application desktop développée en **Java et JavaFX** permettant de détecter et d'analyser les similitudes entre des codes sources.

Projet réalisé dans le cadre du module **TP de Programmation Avancée – Master 1 Génie Logiciel** à l'Université de Béjaïa.

## 1. Présentation du projet

Le Détecteur de similarité permet de comparer des fichiers sources afin d'identifier les ressemblances entre plusieurs programmes.

### Technologies utilisées

- Java 26
- JavaFX 26.0.1
- FXML
- CSS
- Apache Maven 3.9.11
- Docker
- Docker Compose

## 2. Prérequis

### Méthode recommandée : Docker

Pour installer et lancer le projet avec Docker, il suffit d'avoir :

| Logiciel | Utilisation |
|---|---|
| Git | Télécharger le projet |
| Docker Desktop | Exécuter l'application dans un conteneur |
| Docker Compose | Construire et gérer le conteneur |

**Important :** Il n'est pas nécessaire d'installer Java, le JDK, Maven ou le SDK JavaFX directement sur votre ordinateur. Ces outils sont configurés dans l'environnement Docker.

### Versions utilisées dans le conteneur

| Technologie | Version |
|---|---|
| Java / JDK | 26 |
| Apache Maven | 3.9.11 |
| JavaFX | 26.0.1 |

## 3. Installation avec Docker (recommandée)

### Étape 1 : Installer Docker Desktop

Téléchargez et installez Docker Desktop :

https://www.docker.com/products/docker-desktop/

Démarrez Docker Desktop et vérifiez son fonctionnement :

```bash
docker --version
```

```bash
docker compose version
```

### Étape 2 : Télécharger le projet

Ouvrez un terminal :

```bash
git clone https://github.com/JUGURTA2001/Detecteur_Similarite.git
```

Accédez au dossier :

```bash
cd Detecteur_Similarite
```

### Étape 3 : Construire l'image Docker

Exécutez :

```bash
docker compose build
```

Docker va automatiquement :

1. Télécharger l'image de base contenant le JDK 26.
2. Installer Maven 3.9.11.
3. Installer les bibliothèques système nécessaires à JavaFX si elles sont définies dans le Dockerfile.
4. Copier le code source dans le conteneur.
5. Télécharger les dépendances JavaFX avec Maven.
6. Compiler le projet et générer le fichier JAR.

### Étape 4 : Vérifier les versions

Pour vérifier Java :

```bash
docker compose run --rm detecteur java -version
```

Pour vérifier Maven :

```bash
docker compose run --rm detecteur mvn -version
```

### Étape 5 : Compiler le projet

```bash
docker compose run --rm detecteur mvn clean package
```

Le fichier JAR sera généré dans :

```text
target/detecteur-similarite-1.0.0.jar
```

### Étape 6 : Lancer l'application graphique

L'application utilise JavaFX. Pour afficher sa fenêtre depuis un conteneur Linux sur Windows, un serveur graphique X11 est nécessaire.

Sur Windows, installez VcXsrv :

https://sourceforge.net/projects/vcxsrv/

Configurez XLaunch, puis démarrez VcXsrv.

Ensuite, exécutez :

```bash
docker compose up
```

**Remarque :** Le lancement graphique dépend de la configuration du serveur X11, de DISPLAY et des autorisations réseau. Docker Desktop ne fournit pas directement l'affichage de l'interface JavaFX.

## 4. Installation sans Docker (alternative)

Il est également possible de lancer le projet directement sur Windows.

Prérequis :

- JDK 26
- Apache Maven 3.9 ou supérieur
- Git

Télécharger le projet :

```bash
git clone https://github.com/JUGURTA2001/Detecteur_Similarite.git
cd Detecteur_Similarite
```

Vérifier Java :

```bash
java -version
javac -version
```

Vérifier Maven :

```bash
mvn -version
```

Compiler :

```bash
mvn clean package
```

Lancer l'application :

```bash
mvn javafx:run
```

**Remarque :** Maven télécharge automatiquement les dépendances JavaFX définies dans `pom.xml`. Il n'est pas nécessaire d'installer séparément le JavaFX SDK.

## 5. Génération du fichier JAR

Après une compilation réussie, le fichier JAR est généré dans :

```text
target/detecteur-similarite-1.0.0.jar
```

Pour consulter son contenu :

```bash
jar tf target/detecteur-similarite-1.0.0.jar
```

Le JAR généré n'est pas autonome : il nécessite les dépendances JavaFX pour être exécuté.



## 6. Configuration JavaFX dans VS Code

Pour développer le projet avec VS Code :

1. Installer l'extension Extension Pack for Java.
2. Ouvrir le dossier du projet.
3. Utiliser le terminal intégré.
4. Exécuter :

```bash
mvn javafx:run
```

Avec cette méthode, Maven gère automatiquement les dépendances JavaFX.

## 7. Dépannage

### Docker ne démarre pas

Vérifiez que Docker Desktop est lancé :

```bash
docker info
```

### Erreur pendant la construction de l'image

Essayez :

```bash
docker compose build --no-cache
```

### Erreur de téléchargement Maven ou JavaFX

Vérifiez votre connexion Internet, puis reconstruisez l'image :

```bash
docker compose build
```

### L'application graphique ne s'affiche pas

Vérifiez que VcXsrv est démarré, que DISPLAY est correctement configuré et que les connexions X11 sont autorisées.

### Erreur Maven en installation locale

Vérifiez les variables d'environnement JAVA_HOME et Path.

## 9. Auteur

**Jugurta Touati**

Université de Béjaïa  
Master 1 – Génie Logiciel

## 10. Dépôt GitHub

https://github.com/JUGURTA2001/Detecteur_Similarite

---

Projet universitaire – Détecteur de similarité entre codes sources.