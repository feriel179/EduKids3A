# Documentation du projet EduKids JavaFX

## 1. Vue d'ensemble

Ce projet est une application JavaFX pour gerer un petit espace EduKids avec deux parcours:

- `Admin`: gestion des cours et des lecons.
- `Student`: consultation du catalogue, details des cours, liste personnelle et profil.

Architecture generale:

- `models`: objets metier (`Course`, `Lesson`, `Student`)
- `services`: acces base de donnees et logique CRUD
- `controllers`: logique des vues JavaFX
- `resources/fxml`: ecrans JavaFX
- `resources/css`: style global
- `util`: connexion, initialisation SQL, validation, alertes

Important:

- le vrai point d'entree UI est `MainFX`
- `target/` et `.m2/` sont des dossiers generes, pas des fichiers source

## 2. Fichiers racine

### `pom.xml`
- Role:
  - configuration Maven du projet
  - declare Java 21, JavaFX 21.0.4, MySQL Connector, JUnit
  - configure le plugin JavaFX pour lancer `tn.esprit.MainFX`
- Sert a:
  - compiler
  - lancer
  - gerer les dependances

### `.gitignore`
- Role:
  - evite de versionner les fichiers techniques inutiles
- Ignore:
  - `.idea/`
  - `.m2/`
  - `target/`
  - fichiers `.iml`
  - logs

## 3. Points d'entree

### `src/main/java/tn/esprit/Main.java`
- Role:
  - mini point d'entree technique
  - sert surtout a tester la connexion base de donnees
- Fonctions:
  - `main(String[] args)`: initialise `MyConnection` et affiche un message console

### `src/main/java/tn/esprit/MainFX.java`
- Role:
  - point d'entree principal JavaFX
  - ouvre la fenetre, charge le CSS global, oriente vers login/admin/student
- Fonctions:
  - `getInstance()`: retourne l'instance singleton de l'application
  - `start(Stage stage)`: demarrage JavaFX, verifie la base puis ouvre login
  - `initializeDatabase()`: tente la connexion DB et affiche une alerte si probleme
  - `showLoginView()`: charge l'ecran login
  - `showAdminShell()`: charge la coquille admin
  - `showStudentShell(Student student)`: charge la coquille student
  - `loadView(String resourcePath)`: charge un FXML
  - `setScene(Parent root, String title)`: applique la scene et le CSS
  - `main(String[] args)`: lance JavaFX

## 4. Interface commune

### `src/main/java/tn/esprit/interfaces/GlobalInterface.java`
- Role:
  - contrat CRUD generique
- Methodes:
  - `getAll()`
  - `add(T entity)`
  - `update(T entity)`
  - `delete(T entity)`
- Remarque:
  - dans ce projet, cette interface est surtout utilisee par `CourseService`

## 5. Modeles metier

### `src/main/java/tn/esprit/models/Course.java`
- Role:
  - represente un cours
- Champs principaux:
  - id, title, description, level, subject, image, likes, dislikes
- Fonctions:
  - getters/setters classiques
  - `getLevelText()`: transforme le niveau en texte (`Niveau X`)
  - `getLevelColor()`: donne une couleur selon le niveau
  - `toString()`: format texte du cours

### `src/main/java/tn/esprit/models/Lesson.java`
- Role:
  - represente une lecon rattachee a un cours
- Champs principaux:
  - id, course, order, title, mediaType, mediaUrl, videoUrl, youtubeUrl, image
- Fonctions:
  - getters/setters classiques
  - `getPdfUrl()`: expose `mediaUrl` comme PDF
  - `getActiveUrl()`: retourne l'URL active selon le type media
  - `getDisplayMediaType()`: retourne un texte lisible (`PDF + VIDEO + YOUTUBE`)
  - `getUrlSummary()`: resume toutes les URLs
  - `getMediaIcon()`: renvoie un petit label texte `[PDF]`, `[YT]`, `[MULTI]`
  - `normalizeMediaType(...)`: normalise le type media
  - `getAvailableMediaLabels()`: detecte les medias disponibles
  - `hasText(...)`: test utilitaire
  - `firstNonBlank(...)`: prend la premiere valeur non vide

### `src/main/java/tn/esprit/models/Student.java`
- Role:
  - represente un etudiant
- Champs principaux:
  - id, name, email, enrolledCourses
- Fonctions:
  - getters/setters de base
  - `getEnrolledCourses()`: retourne les cours enregistres
  - `replaceEnrolledCourses(List<Course>)`: remplace la liste actuelle
  - `toString()`: texte d'affichage

## 6. Services

### `src/main/java/tn/esprit/services/CourseService.java`
- Role:
  - service CRUD des cours
  - charge les cours depuis la table `cours`
- Fonctions:
  - `getAll()`: alias generique du CRUD
  - `getAllCourses()`: charge/retourne la liste observable de cours
  - `addCourse(...)`: cree un objet cours puis l'ajoute en base
  - `updateCourse(...)`: met a jour un objet cours
  - `deleteCourse(Course)`: supprime un cours
  - `refreshCourses()`: recharge depuis la base
  - `countCourses()`: compte les cours
  - `add(Course)`: insertion SQL
  - `update(Course)`: update SQL
  - `delete(Course)`: suppression SQL + progression + lecons
  - `fetchCourses()`: requete SQL de lecture
  - `mapCourse(ResultSet)`: transforme SQL vers `Course`

### `src/main/java/tn/esprit/services/LessonService.java`
- Role:
  - service CRUD des lecons
  - gere PDF, video et YouTube
- Fonctions:
  - `getLessonsByCourse(Course)`: charge les lecons d'un cours
  - `getAllLessons()`: charge toutes les lecons avec jointure cours
  - `addLesson(...)`: cree et ajoute une lecon
  - `updateLesson(...)`: modifie une lecon
  - `deleteLesson(Lesson)`: supprime une lecon
  - `countLessons()`: compte les lecons
  - `add(Lesson)`: insertion SQL
  - `update(Lesson)`: update SQL
  - `delete(Lesson)`: suppression SQL
  - `mapLesson(...)`: transforme SQL vers `Lesson`
  - `mapJoinedCourse(...)`: reconstruit le cours joint
  - `extractPdfUrl(...)`: separe la vraie URL PDF
  - `deriveMediaType(...)`: construit le type media combine
  - `normalizeMediaType(...)`: standardise le type
  - `nonBlank(...)`: premiere valeur non vide
  - `clean(...)`: trim et normalisation simple

### `src/main/java/tn/esprit/services/StudentService.java`
- Role:
  - gere l'etudiant courant
  - login simple par nom ou email
  - creation auto d'un etudiant si absent
  - inscription a un cours
- Fonctions:
  - `loginOrCreateStudent(String)`: cherche ou cree un etudiant
  - `getCurrentStudent()`: recharge les cours du student courant
  - `enrollInCourse(Course)`: ajoute un cours dans `user_cours_progress`
  - `getAllStudents()`: liste les eleves actifs
  - `countStudents()`: compte les eleves
  - `findStudentByEmail(...)`: recherche par email
  - `findStudentByName(...)`: recherche par nom
  - `createStudentFromName(...)`: cree depuis un nom
  - `createStudentFromEmail(...)`: cree depuis un email
  - `createStudent(...)`: insertion SQL d'un user eleve
  - `isAlreadyEnrolled(...)`: verifie l'inscription
  - `loadEnrolledCourses(...)`: charge les cours lies a l'etudiant
  - `mapStudent(...)`: transforme SQL vers `Student`
  - `mapCourse(...)`: transforme SQL vers `Course`
  - `buildDisplayName(...)`: construit le nom affiche
  - `buildUniqueEmail(...)`: evite les doublons d'email
  - `emailExists(...)`: test de doublon
  - `slugify(...)`: normalise une chaine pour email
  - `capitalize(...)`: capitalisation simple

## 7. Utilitaires

### `src/main/java/tn/esprit/util/MyConnection.java`
- Role:
  - singleton de connexion MySQL/MariaDB
  - choisit plusieurs hotes/ports/noms de base possibles
  - cree la base `edukids` si besoin
- Fonctions:
  - `getInstance()`: singleton
  - `getCnx()`: retourne une connexion valide
  - `openConnection()`: tente plusieurs connexions puis creation DB
  - `buildCandidates()`: liste les combinaisons host/port/base
  - `buildServers()`: liste les serveurs SQL possibles
  - `buildHosts()`: recupere les hotes
  - `buildPorts()`: recupere les ports
  - `buildDatabaseNames()`: recupere les noms de base
  - `readSetting(...)`: lit system property / variable env
  - `readIntSetting(...)`: lit une valeur entiere
  - `DatabaseCandidate.databaseUrl()`: fabrique l'URL JDBC DB
  - `ServerCandidate.serverUrl()`: fabrique l'URL JDBC serveur
  - `ServerCandidate.databaseUrl(...)`: fabrique l'URL JDBC DB cible

### `src/main/java/tn/esprit/util/DatabaseInitializer.java`
- Role:
  - cree les tables minimales si elles n'existent pas
- Fonctions:
  - `initialize(Connection cnx)`: cree `cours`, `lecon`, `user`, `user_cours_progress`

### `src/main/java/tn/esprit/util/FormValidator.java`
- Role:
  - validation des formulaires admin
  - filtres de saisie JavaFX
- Fonctions:
  - `createLengthFormatter(int)`: limite le nombre de caracteres
  - `createDigitsFormatter(int)`: n'autorise que des chiffres
  - `validateCourse(...)`: valide titre, matiere, description
  - `validateLesson(...)`: valide titre lecon, ordre, PDF, video, YouTube
  - `isValidPdfLink(...)`: verifie PDF
  - `isValidVideoLink(...)`: verifie video / fichier / URL
  - `isValidYoutubeLink(...)`: verifie YouTube
  - `isFileOrWebLink(...)`: accepte chemin local ou URL
  - `hasExtension(...)`: verifie l'extension
  - `stripQuery(...)`: retire les query params
  - `clean(...)`: trim simple

### `src/main/java/tn/esprit/util/SweetAlert.java`
- Role:
  - remplace les alertes JavaFX basiques par des boites plus stylisees
- Fonctions:
  - `success(...)`: popup succes
  - `warning(...)`: popup attention
  - `error(...)`: popup erreur
  - `confirmDanger(...)`: popup confirmation de suppression
  - `createAlert(...)`: construit une boite stylisee
  - `styleButton(...)`: applique une classe CSS aux boutons
  - `resolveIcon(...)`: choisit une icone texte

## 8. Controleurs - Authentification

### `src/main/java/tn/esprit/controllers/LoginController.java`
- Role:
  - logique de la page de login
  - choix du role Admin ou Student
- Fonctions:
  - `initialize()`: prepare la combo role
  - `handleLogin()`: route vers admin ou student
  - `toggleStudentFields()`: affiche/cache le champ student
  - `showAlert(...)`: alerte simple locale

## 9. Controleurs - Admin

### `src/main/java/tn/esprit/controllers/admin/AdminDashboardController.java`
- Role:
  - alimente les stats du dashboard admin
- Fonctions:
  - `initialize()`
  - `refreshMetrics()`

### `src/main/java/tn/esprit/controllers/admin/AdminShellController.java`
- Role:
  - conteneur principal admin
  - navigation laterale/centrale
- Fonctions:
  - `getInstance()`
  - `initialize()`
  - `showDashboard()`
  - `showCourses()`
  - `showCreateCourse()`
  - `showEditCourse(Course)`
  - `showCourseSuccess(Course, boolean updated)`
  - `showLessons()`
  - `showCreateLesson()`
  - `showCreateLesson(Course preselectedCourse)`
  - `showEditLesson(Lesson)`
  - `showLessonSuccess(Lesson, boolean updated)`
  - `handleLogout()`
  - `setContext(...)`
  - `setActiveNavigation(Button)`
  - `loadCenterView(...)`

### `src/main/java/tn/esprit/controllers/admin/CourseController.java`
- Role:
  - ecran liste des cours admin
  - recherche, tri, pagination, edition, suppression
- Fonctions:
  - `initialize()`
  - `handleCreateCourse()`
  - `handleEditCourse()`
  - `handleRefreshCourses()`
  - `handleDeleteCourse()`
  - `refreshTable()`
  - `updateMetrics()`
  - `applyFiltersAndSort(boolean resetPage)`
  - `matchesSearch(...)`
  - `resolveComparator()`
  - `updatePagination()`
  - `createPageButton(...)`
  - `updateCurrentPage()`
  - `createTextCell(...)`
  - `createLevelCell()`

### `src/main/java/tn/esprit/controllers/admin/CourseFormController.java`
- Role:
  - formulaire creation/modification d'un cours
  - preview live
- Fonctions:
  - `initialize()`
  - `initForCreate()`
  - `initForEdit(Course)`
  - `handleSaveCourse()`
  - `handleCancel()`
  - `handleResetForm()`
  - `refreshPreview()`
  - `isValidForm()`
  - `mapLevel(String levelLabel)`
  - `safeValue(...)`

### `src/main/java/tn/esprit/controllers/admin/CourseSuccessController.java`
- Role:
  - page de confirmation apres creation/modification de cours
- Fonctions:
  - `setResult(Course, boolean updated)`
  - `handleBackToCourses()`
  - `handleCreateAnother()`

### `src/main/java/tn/esprit/controllers/admin/LessonController.java`
- Role:
  - liste admin des lecons
  - recherche, filtres par cours, tri, pagination, edition
- Fonctions:
  - `initialize()`
  - `handleCreateLesson()`
  - `handleEditLesson()`
  - `handleDeleteLesson()`
  - `handleRefreshLessons()`
  - `refreshTable()`
  - `updateMetrics()`
  - `loadCoursesPreservingSelection(...)`
  - `applyFiltersAndSort(boolean resetPage)`
  - `matchesCourseFilter(...)`
  - `matchesSearch(...)`
  - `resolveComparator()`
  - `updatePagination()`
  - `createPageButton(...)`
  - `updateCurrentPage()`
  - `createTextCell(...)`
  - `createMediaCell()`
  - `createUrlCell()`
  - `hasText(...)`
  - `shorten(...)`

### `src/main/java/tn/esprit/controllers/admin/LessonFormController.java`
- Role:
  - formulaire admin des lecons
  - supporte PDF, fichier video local, lien YouTube
- Fonctions:
  - `initialize()`
  - `initForCreate(Course preselectedCourse)`
  - `initForEdit(Lesson)`
  - `handleSaveLesson()`
  - `handleCancel()`
  - `handleResetForm()`
  - `handleChoosePdfFile()`
  - `handleChooseVideoFile()`
  - `loadCourses()`
  - `findMatchingCourse(Course)`
  - `refreshPreview()`
  - `isValidForm()`
  - `buildMediaLabel(...)`
  - `buildUrlSummary(...)`
  - `appendMedia(...)`
  - `appendUrl(...)`
  - `chooseLocalFile(...)`
  - `safeValue(...)`

### `src/main/java/tn/esprit/controllers/admin/LessonSuccessController.java`
- Role:
  - page de confirmation apres creation/modification de lecon
- Fonctions:
  - `setResult(Lesson, boolean updated)`
  - `handleBackToLessons()`
  - `handleCreateAnother()`

## 10. Controleurs - Student

### `src/main/java/tn/esprit/controllers/student/StudentShellController.java`
- Role:
  - conteneur principal student
  - navigation entre catalogue, mes cours, profil, detail cours
- Fonctions:
  - `getInstance()`
  - `initialize()`
  - `showCatalog()`
  - `showMyCourses()`
  - `showProfile()`
  - `handleLogout()`
  - `showCourseDetail(Course)`
  - `loadCenterView(...)`
  - `populateStudentHeader()`
  - `setActiveNavigation(Button)`
  - `clearNavigationState()`
  - `buildInitials(...)`

### `src/main/java/tn/esprit/controllers/student/CatalogController.java`
- Role:
  - coeur de la vue catalogue student
  - recherche, filtres, tri, pagination, affichage cartes
- Fonctions:
  - `initialize()`
  - `applyFilters()`
  - `handleClearFilters()`
  - `openMyCoursesPage()`
  - `configureFilters()`
  - `renderCurrentPage()`
  - `createCourseCard(Course)`
  - `handleEnroll(Course)`
  - `refreshStudentSnapshot()`
  - `updateMetrics()`
  - `updateFeaturedCourse()`
  - `rebuildPagination()`
  - `updateResultLabels()`
  - `resolveComparator(...)`
  - `matchesKeyword(...)`
  - `createPaginationButton(...)`
  - `getTotalPages()`
  - `extractLevelNumber(...)`
  - `buildMediaStyle(...)`
  - `buildCourseInitials(...)`
  - `buildDisplaySubjectName(...)`
  - `toDisplayCase(...)`
  - `normalizeSubjectKey(...)`
  - `summarize(...)`
  - `extractFirstName(...)`
  - `safeText(...)`

### `src/main/java/tn/esprit/controllers/student/CourseDetailController.java`
- Role:
  - detail d'un cours cote student
  - affiche les lecons et ouvre PDF/video/YouTube
- Fonctions:
  - `initialize()`
  - `setCourse(Course)`
  - `createLessonCard(Lesson)`
  - `createResourceButton(...)`
  - `addResourceButton(...)`
  - `openResource(...)`
  - `hasText(...)`
- Remarque:
  - cette classe a ete etendue pour afficher de vrais boutons `Open PDF`, `Open Video`, `Open YouTube`

### `src/main/java/tn/esprit/controllers/student/MyCourseController.java`
- Role:
  - affiche les cours enregistres par l'etudiant
- Fonctions:
  - `initialize()`
  - `renderCourses()`
  - `createCourseCard(Course)`

### `src/main/java/tn/esprit/controllers/student/ProfileController.java`
- Role:
  - alimente la vue profil student
- Fonctions:
  - `initialize()`

## 11. Ressources FXML

### `src/main/resources/tn/esprit/fxml/login.fxml`
- Vue:
  - ecran login
- Controleur:
  - `LoginController`
- Contenu:
  - choix role
  - champ nom/email student
  - bouton login

### `src/main/resources/tn/esprit/fxml/admin/admin-shell.fxml`
- Vue:
  - coquille admin
- Controleur:
  - `AdminShellController`
- Contenu:
  - navigation
  - zone centrale dynamique

### `src/main/resources/tn/esprit/fxml/admin/dashboard.fxml`
- Vue:
  - dashboard admin
- Controleur:
  - `AdminDashboardController`

### `src/main/resources/tn/esprit/fxml/admin/courses.fxml`
- Vue:
  - liste des cours admin
- Controleur:
  - `CourseController`

### `src/main/resources/tn/esprit/fxml/admin/course-form.fxml`
- Vue:
  - formulaire cours
- Controleur:
  - `CourseFormController`

### `src/main/resources/tn/esprit/fxml/admin/course-success.fxml`
- Vue:
  - confirmation cours
- Controleur:
  - `CourseSuccessController`

### `src/main/resources/tn/esprit/fxml/admin/lessons.fxml`
- Vue:
  - liste des lecons admin
- Controleur:
  - `LessonController`

### `src/main/resources/tn/esprit/fxml/admin/lesson-form.fxml`
- Vue:
  - formulaire lecon
- Controleur:
  - `LessonFormController`
- Particularite:
  - boutons `Choose PDF` et `Choose Video`
  - champ `YouTube URL`

### `src/main/resources/tn/esprit/fxml/admin/lesson-success.fxml`
- Vue:
  - confirmation lecon
- Controleur:
  - `LessonSuccessController`

### `src/main/resources/tn/esprit/fxml/student/student-shell.fxml`
- Vue:
  - coquille student
- Controleur:
  - `StudentShellController`

### `src/main/resources/tn/esprit/fxml/student/catalog.fxml`
- Vue:
  - catalogue student
- Controleur:
  - `CatalogController`
- Contenu:
  - recherche
  - filtres
  - stats
  - cartes cours
  - pagination

### `src/main/resources/tn/esprit/fxml/student/course-detail.fxml`
- Vue:
  - detail d'un cours
- Controleur:
  - `CourseDetailController`
- Contenu:
  - titre
  - description
  - liste des lecons avec boutons ressources

### `src/main/resources/tn/esprit/fxml/student/my-courses.fxml`
- Vue:
  - cours sauvegardes / inscrits
- Controleur:
  - `MyCourseController`

### `src/main/resources/tn/esprit/fxml/student/profile.fxml`
- Vue:
  - profil student
- Controleur:
  - `ProfileController`

## 12. Style et assets

### `src/main/resources/tn/esprit/css/styles.css`
- Role:
  - feuille de style globale de toute l'application
- Contient:
  - boutons (`primary-button`, `secondary-button`, `ghost-button`, etc.)
  - cartes
  - styles admin et student
  - alertes SweetAlert
  - vues catalogue, detail, formulaire, tables, navigation

### `src/main/resources/tn/esprit/images/edukids-logo.png`
- Role:
  - logo de l'application

## 13. Flux fonctionnels importants

### Login
- `login.fxml`
- `LoginController.handleLogin()`
- `StudentService.loginOrCreateStudent(...)`
- navigation vers `AdminShell` ou `StudentShell`

### Gestion des cours admin
- `CourseController`
- `CourseFormController`
- `CourseService`

### Gestion des lecons admin
- `LessonController`
- `LessonFormController`
- `LessonService`

### Parcours student
- `StudentShellController`
- `CatalogController`
- `CourseDetailController`
- `MyCourseController`
- `ProfileController`

## 14. Ce que tu as peut-etre oublie ou ce qu'il manque

Voici les points faibles ou manquants que je vois:

1. Il n'y avait pas de documentation projet avant ce fichier.
2. Il n'y a pas de tests automatiques dans `src/test/java`.
3. `Main.java` et `MainFX.java` coexistent; `Main.java` sert surtout de test DB, pas d'entree UI.
4. `GlobalInterface` n'est pas appliquee partout de maniere uniforme.
5. `DatabaseInitializer` cree les tables mais sans vraies foreign keys SQL explicites.
6. Les services melangent acces SQL et structure JavaFX (`ObservableList`), donc la separation metier / persistence / UI reste simple.
7. Certains controleurs utilisent encore `printStackTrace()` au lieu d'un vrai systeme de logs.
8. Le login student cree automatiquement un utilisateur si l'email ou le nom n'existe pas. C'est pratique pour une demo, mais surprenant pour un vrai produit.
9. Le projet n'a pas encore de README d'installation/lancement pour une autre personne.
10. Le dossier `target/` est du build genere, il ne faut pas l'expliquer comme source fonctionnelle.

## 15. Conseils pour la suite

Si tu veux pousser la documentation encore plus loin, les prochaines bonnes pieces a ajouter sont:

- un `README.md` de lancement
- un schema de base de donnees
- une section "comment ajouter un nouveau cours / une nouvelle lecon"
- des tests unitaires pour `FormValidator`, `CourseService`, `LessonService`
- une doc de navigation ecran par ecran avec captures

