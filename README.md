# EduKids JavaFX

EduKids JavaFX est une application desktop developpee avec JavaFX et Maven pour gerer un environnement educatif complet. Le projet regroupe plusieurs espaces fonctionnels: authentification, tableau de bord utilisateur, gestion des cours et lecons, espace etudiant, evenements, programmes, reservations, quiz, chat, statistiques et module e-commerce.

## Fonctionnalites principales

- Authentification des utilisateurs avec gestion des roles.
- Espace administrateur pour gerer les cours et les lecons.
- Espace etudiant avec catalogue, detail des cours, progression et profil.
- Gestion des evenements, programmes, reservations et favoris.
- Quiz, questions, resultats et validation des reponses.
- Chat entre utilisateurs avec tableau de bord statistique.
- Module e-commerce avec categories, produits, commandes et codes promo.
- Services IA pour aide au contenu, traduction, generation d'images et chatbot.
- Notifications par email, SMS selon configuration, PDF et QR codes.

## Technologies utilisees

- Java 19
- JavaFX 19.0.2
- Maven
- MySQL / MariaDB
- Hibernate ORM / Jakarta Persistence
- JUnit 5
- Gson et Jackson
- PDFBox
- ZXing pour les QR codes
- SendGrid / Jakarta Mail
- APIs IA configurables: OpenRouter, OpenAI compatible, Ollama, Pollinations

## Structure du projet

```text
src/main/java
|-- tn/esprit
|   |-- MainFX.java                  # Point d'entree principal JavaFX
|   |-- controllers                  # Controleurs cours, admin, etudiant
|   |-- models                       # Modeles Course, Lesson, Student
|   |-- services                     # Services metier cours, lecons, IA, SMS
|   `-- util                         # Configuration, validation, alertes
|-- com/edukids
|   |-- controllers                  # Authentification, profil, dashboard
|   |-- entities                     # User, Session
|   |-- services                     # UserService, EmailService, GoogleAuth
|   `-- edukids3a                    # Evenements, programmes, quiz, chat
`-- com/ecom
    |-- model                        # Category, Produit, Commande
    |-- persistence                  # Repositories e-commerce
    |-- service                      # Services e-commerce et chatbot
    `-- validation                   # Validateurs metier

src/main/resources
|-- fxml                             # Interfaces JavaFX principales
|-- tn/esprit/fxml                   # Vues admin, student et chat
|-- com/edukids/views                # Vues auth, dashboard et profil
|-- css                              # Styles globaux
`-- META-INF/persistence.xml         # Configuration JPA
```

## Prerequis

Avant de lancer le projet, installer:

- JDK 19 ou une version compatible avec le `pom.xml`
- Maven 3.8+
- MySQL ou MariaDB
- Un IDE Java, par exemple IntelliJ IDEA, Eclipse ou VS Code

Verifier l'installation:

```bash
java -version
mvn -version
```

## Configuration

1. Copier le fichier d'exemple:

```bash
cp .env.example .env.local
```

Sous Windows PowerShell:

```powershell
Copy-Item .env.example .env.local
```

2. Adapter les informations de base de donnees dans `.env.local`:

```env
EDUKIDS_DB_HOST=127.0.0.1
EDUKIDS_DB_PORT=3308
EDUKIDS_DB_NAME=edukids
EDUKIDS_DB_USER=root
EDUKIDS_DB_PASSWORD=
```

3. Creer la base si elle n'existe pas:

```sql
CREATE DATABASE IF NOT EXISTS edukids CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
```

L'application utilise aussi Hibernate avec `hibernate.hbm2ddl.auto=update`, donc certaines tables peuvent etre creees ou mises a jour automatiquement au lancement.

## Configuration optionnelle des services externes

Les services externes sont optionnels pour le lancement de base. Ils deviennent necessaires uniquement pour les fonctionnalites associees.

Dans `.env.local`:

```env
OLLAMA_BASE_URL=http://localhost:11434/api
OLLAMA_TEXT_MODEL=gemma3
POLLINATIONS_BASE_URL=https://image.pollinations.ai
POLLINATIONS_IMAGE_MODEL=automatic

COURSE_SMS_ENABLED=false
TWILIO_ACCOUNT_SID=
TWILIO_AUTH_TOKEN=
TWILIO_API_KEY=
TWILIO_API_SECRET=
TWILIO_FROM_NUMBER=
STUDENT_SMS_RECIPIENTS=
```

Pour les services IA lies a OpenRouter/OpenAI compatible, completer aussi:

```text
src/main/resources/config/openai.properties
```

## Lancement de l'application

Depuis la racine du projet:

```bash
mvn clean javafx:run
```

Le point d'entree principal est:

```text
tn.esprit.MainFX
```

Au demarrage, l'application:

- initialise la connexion a la base de donnees;
- demarre le hub temps reel du chat si disponible;
- ouvre l'ecran de connexion EduKids.

## Tests

Lancer les tests unitaires:

```bash
mvn test
```

Les tests couvrent notamment:

- les validateurs de formulaires;
- les modeles session, chat, evenements, programmes et e-commerce;
- les validateurs quiz, questions, commandes, produits et categories;
- certains chargements FXML.

## Commandes utiles

```bash
mvn clean
mvn compile
mvn test
mvn javafx:run
```

## Bonnes pratiques pour contribuer

- Ne pas versionner `.env.local`, `target/`, `.idea/` ou les fichiers generes.
- Ajouter les nouvelles vues dans `src/main/resources`.
- Garder les controleurs dans le package correspondant au module.
- Ajouter ou mettre a jour les tests quand une validation ou une logique metier change.
- Verifier le lancement JavaFX apres une modification d'interface FXML.

## Notes importantes

- Le fichier `.env.example` sert de modele de configuration.
- Le fichier `.env.local` contient les valeurs locales et ne doit pas etre partage.
- Le projet contient plusieurs modules historiques et fonctionnels; le point d'entree recommande reste `tn.esprit.MainFX`.
- Certains services IA, email, SMS ou temps reel peuvent necessiter des cles API ou une configuration locale supplementaire.
