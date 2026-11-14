-- RBAC dynamique : catalogue de roles et permissions, puis assignation role -> permissions.
-- Chaque insertion est gardee par IF NOT EXISTS (une seule instruction par garde, cf. le
-- commentaire en tete de schema.sql sur le decoupage statement-par-statement de ce fichier).
IF NOT EXISTS (SELECT 1 FROM dbo.roles WHERE code = 'ADMIN')
INSERT INTO dbo.roles (code, label) VALUES ('ADMIN', 'Administrateur');

IF NOT EXISTS (SELECT 1 FROM dbo.roles WHERE code = 'USER')
INSERT INTO dbo.roles (code, label) VALUES ('USER', 'Utilisateur');

IF NOT EXISTS (SELECT 1 FROM dbo.roles WHERE code = 'AUDITOR')
INSERT INTO dbo.roles (code, label) VALUES ('AUDITOR', 'Auditeur');

IF NOT EXISTS (SELECT 1 FROM dbo.roles WHERE code = 'FINANCIER')
INSERT INTO dbo.roles (code, label) VALUES ('FINANCIER', 'Financier');

IF NOT EXISTS (SELECT 1 FROM dbo.permissions WHERE code = 'BANQUE_READ')
INSERT INTO dbo.permissions (code, module, label) VALUES ('BANQUE_READ', 'BANQUE', 'Afficher les banques');

IF NOT EXISTS (SELECT 1 FROM dbo.permissions WHERE code = 'BANQUE_CREATE')
INSERT INTO dbo.permissions (code, module, label) VALUES ('BANQUE_CREATE', 'BANQUE', 'Ajouter une banque');

IF NOT EXISTS (SELECT 1 FROM dbo.permissions WHERE code = 'BANQUE_UPDATE')
INSERT INTO dbo.permissions (code, module, label) VALUES ('BANQUE_UPDATE', 'BANQUE', 'Modifier une banque');

IF NOT EXISTS (SELECT 1 FROM dbo.permissions WHERE code = 'BANQUE_DELETE')
INSERT INTO dbo.permissions (code, module, label) VALUES ('BANQUE_DELETE', 'BANQUE', 'Supprimer une banque');

IF NOT EXISTS (SELECT 1 FROM dbo.permissions WHERE code = 'COMPTE_READ')
INSERT INTO dbo.permissions (code, module, label) VALUES ('COMPTE_READ', 'COMPTE', 'Afficher les comptes');

IF NOT EXISTS (SELECT 1 FROM dbo.permissions WHERE code = 'COMPTE_CREATE')
INSERT INTO dbo.permissions (code, module, label) VALUES ('COMPTE_CREATE', 'COMPTE', 'Ajouter un compte');

IF NOT EXISTS (SELECT 1 FROM dbo.permissions WHERE code = 'COMPTE_UPDATE')
INSERT INTO dbo.permissions (code, module, label) VALUES ('COMPTE_UPDATE', 'COMPTE', 'Modifier un compte');

IF NOT EXISTS (SELECT 1 FROM dbo.permissions WHERE code = 'COMPTE_DELETE')
INSERT INTO dbo.permissions (code, module, label) VALUES ('COMPTE_DELETE', 'COMPTE', 'Supprimer un compte');

IF NOT EXISTS (SELECT 1 FROM dbo.permissions WHERE code = 'USER_CREATE')
INSERT INTO dbo.permissions (code, module, label) VALUES ('USER_CREATE', 'USER', 'Ajouter un utilisateur');

IF NOT EXISTS (SELECT 1 FROM dbo.permissions WHERE code = 'USER_UPDATE')
INSERT INTO dbo.permissions (code, module, label) VALUES ('USER_UPDATE', 'USER', 'Modifier un utilisateur');

IF NOT EXISTS (SELECT 1 FROM dbo.permissions WHERE code = 'USER_DELETE')
INSERT INTO dbo.permissions (code, module, label) VALUES ('USER_DELETE', 'USER', 'Supprimer un utilisateur');

IF NOT EXISTS (SELECT 1 FROM dbo.permissions WHERE code = 'USER_REVOKE_ACCESS')
INSERT INTO dbo.permissions (code, module, label) VALUES ('USER_REVOKE_ACCESS', 'USER', 'Revoquer l''acces d''un utilisateur');

IF NOT EXISTS (SELECT 1 FROM dbo.permissions WHERE code = 'ROLE_MANAGE')
INSERT INTO dbo.permissions (code, module, label) VALUES ('ROLE_MANAGE', 'ADMINISTRATION', 'Gerer les roles et leurs permissions');

IF NOT EXISTS (SELECT 1 FROM dbo.permissions WHERE code = 'PROFILE_MANAGE')
INSERT INTO dbo.permissions (code, module, label) VALUES ('PROFILE_MANAGE', 'ADMINISTRATION', 'Gerer les profils et leurs fonctions');

IF NOT EXISTS (SELECT 1 FROM dbo.permissions WHERE code = 'USER_ASSIGN_PROFILE')
INSERT INTO dbo.permissions (code, module, label) VALUES ('USER_ASSIGN_PROFILE', 'USER', 'Affecter un profil a un utilisateur');

IF NOT EXISTS (SELECT 1 FROM dbo.permissions WHERE code = 'PARAMETER_MANAGE')
INSERT INTO dbo.permissions (code, module, label) VALUES ('PARAMETER_MANAGE', 'ADMINISTRATION', 'Gerer les parametres internes de l''application');

-- Profils "But" : arbre des fonctions (menus) sur 2 niveaux, groupes d'abord puis feuilles (parent retrouve par
-- code). IF NOT EXISTS ne met jamais a jour une ligne existante : si un libelle ou une route change ici, ajouter
-- un UPDATE idempotent (les libelles affiches sont traduits cote frontend, cles menu.<CODE>).
IF NOT EXISTS (SELECT 1 FROM dbo.fonctions WHERE code = 'REFERENTIEL')
INSERT INTO dbo.fonctions (code, label, parent_id, route, ordre) VALUES ('REFERENTIEL', 'Referentiel', NULL, NULL, 1);

IF NOT EXISTS (SELECT 1 FROM dbo.fonctions WHERE code = 'ADMINISTRATION')
INSERT INTO dbo.fonctions (code, label, parent_id, route, ordre) VALUES ('ADMINISTRATION', 'Administration', NULL, NULL, 2);

IF NOT EXISTS (SELECT 1 FROM dbo.fonctions WHERE code = 'BANQUES')
INSERT INTO dbo.fonctions (code, label, parent_id, route, ordre) SELECT 'BANQUES', 'Banques', id, '/banques', 1 FROM dbo.fonctions WHERE code = 'REFERENTIEL';

IF NOT EXISTS (SELECT 1 FROM dbo.fonctions WHERE code = 'COMPTES')
INSERT INTO dbo.fonctions (code, label, parent_id, route, ordre) SELECT 'COMPTES', 'Comptes', id, '/comptes', 2 FROM dbo.fonctions WHERE code = 'REFERENTIEL';

IF NOT EXISTS (SELECT 1 FROM dbo.fonctions WHERE code = 'UTILISATEURS')
INSERT INTO dbo.fonctions (code, label, parent_id, route, ordre) SELECT 'UTILISATEURS', 'Utilisateurs', id, '/users', 1 FROM dbo.fonctions WHERE code = 'ADMINISTRATION';

IF NOT EXISTS (SELECT 1 FROM dbo.fonctions WHERE code = 'ROLES')
INSERT INTO dbo.fonctions (code, label, parent_id, route, ordre) SELECT 'ROLES', 'Roles', id, '/roles', 2 FROM dbo.fonctions WHERE code = 'ADMINISTRATION';

IF NOT EXISTS (SELECT 1 FROM dbo.fonctions WHERE code = 'PROFILS')
INSERT INTO dbo.fonctions (code, label, parent_id, route, ordre) SELECT 'PROFILS', 'Profils', id, '/profiles', 3 FROM dbo.fonctions WHERE code = 'ADMINISTRATION';

IF NOT EXISTS (SELECT 1 FROM dbo.fonctions WHERE code = 'PARAMETRES')
INSERT INTO dbo.fonctions (code, label, parent_id, route, ordre) SELECT 'PARAMETRES', 'Parametres', id, '/parameters', 4 FROM dbo.fonctions WHERE code = 'ADMINISTRATION';

-- Parametres internes : definitions (type, defaut, bornes, redemarrage requis). IF NOT EXISTS ne met jamais a jour une
-- definition existante : pour changer une borne ou un defaut apres coup, ajouter un UPDATE idempotent ici.
IF NOT EXISTS (SELECT 1 FROM dbo.app_parameter_definition WHERE code = 'security.maxFailedAttempts')
INSERT INTO dbo.app_parameter_definition (code, category, value_type, default_value, min_value, max_value, requires_restart, label)
VALUES ('security.maxFailedAttempts', 'security', 'INT', '3', 1, 10, 0, 'Echecs de connexion avant verrouillage du compte');

IF NOT EXISTS (SELECT 1 FROM dbo.app_parameter_definition WHERE code = 'security.passwordMaxAgeDays')
INSERT INTO dbo.app_parameter_definition (code, category, value_type, default_value, min_value, max_value, requires_restart, label)
VALUES ('security.passwordMaxAgeDays', 'security', 'INT', '90', 0, 3650, 0, 'Duree de validite du mot de passe en jours (0 = jamais)');

IF NOT EXISTS (SELECT 1 FROM dbo.app_parameter_definition WHERE code = 'security.sessionTimeoutMinutes')
INSERT INTO dbo.app_parameter_definition (code, category, value_type, default_value, min_value, max_value, requires_restart, label)
VALUES ('security.sessionTimeoutMinutes', 'security', 'INT', '4320', 5, 10080, 0, 'Inactivite avant expiration d''une session en minutes (nouvelles sessions)');

IF NOT EXISTS (SELECT 1 FROM dbo.app_parameter_definition WHERE code = 'display.maxRowsPerPage')
INSERT INTO dbo.app_parameter_definition (code, category, value_type, default_value, min_value, max_value, requires_restart, label)
VALUES ('display.maxRowsPerPage', 'display', 'INT', '100', 10, 1000, 0, 'Nombre maximal de lignes par page');

IF NOT EXISTS (SELECT 1 FROM dbo.app_parameter_definition WHERE code = 'ui.environmentLabel')
INSERT INTO dbo.app_parameter_definition (code, category, value_type, default_value, min_value, max_value, requires_restart, label)
VALUES ('ui.environmentLabel', 'display', 'STRING', 'DEV', NULL, 20, 0, 'Libelle de l''environnement (bandeau)');

IF NOT EXISTS (SELECT 1 FROM dbo.app_parameter_definition WHERE code = 'module.comptes.enabled')
INSERT INTO dbo.app_parameter_definition (code, category, value_type, default_value, min_value, max_value, requires_restart, label)
VALUES ('module.comptes.enabled', 'modules', 'BOOLEAN', 'true', NULL, NULL, 0, 'Module Comptes active');

-- mail.disabled : stocke mais sans consommateur dans le prototype (pas d'envoi de mail). requires_restart = 1 comme le
-- service d'envoi du legacy, demarre une seule fois : montre la mention "redemarrage requis" renvoyee a la modification.
IF NOT EXISTS (SELECT 1 FROM dbo.app_parameter_definition WHERE code = 'mail.disabled')
INSERT INTO dbo.app_parameter_definition (code, category, value_type, default_value, min_value, max_value, requires_restart, label)
VALUES ('mail.disabled', 'mail', 'BOOLEAN', 'true', NULL, NULL, 1, 'Envoi des notifications par mail desactive');

-- SUPER_ADMIN = "profil 0" : voit toutes les fonctions (drapeau tout_voir), aucune ligne dans profil_fonctions.
IF NOT EXISTS (SELECT 1 FROM dbo.profils WHERE code = 'SUPER_ADMIN')
INSERT INTO dbo.profils (code, label, valide, tout_voir) VALUES ('SUPER_ADMIN', 'Super administrateur', 1, 1);

-- ADMIN recoit toutes les permissions existantes.
INSERT INTO dbo.role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM dbo.roles r
CROSS JOIN dbo.permissions p
WHERE r.code = 'ADMIN'
AND NOT EXISTS (SELECT 1 FROM dbo.role_permissions rp WHERE rp.role_id = r.id AND rp.permission_id = p.id);

-- USER et AUDITOR : lecture seule sur banques/comptes.
INSERT INTO dbo.role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM dbo.roles r
CROSS JOIN dbo.permissions p
WHERE r.code IN ('USER', 'AUDITOR')
AND p.code IN ('BANQUE_READ', 'COMPTE_READ')
AND NOT EXISTS (SELECT 1 FROM dbo.role_permissions rp WHERE rp.role_id = r.id AND rp.permission_id = p.id);

-- FINANCIER : gere les comptes au quotidien, pas les banques (donnees de reference).
INSERT INTO dbo.role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM dbo.roles r
CROSS JOIN dbo.permissions p
WHERE r.code = 'FINANCIER'
AND p.code IN ('BANQUE_READ', 'COMPTE_READ', 'COMPTE_CREATE', 'COMPTE_UPDATE')
AND NOT EXISTS (SELECT 1 FROM dbo.role_permissions rp WHERE rp.role_id = r.id AND rp.permission_id = p.id);

-- Migration "un seul role par utilisateur" -> table de jointure dbo.user_roles. A ce point dbo.roles
-- est seede et dbo.user_roles existe (schema.sql). Chaque etape est une instruction unique gardee
-- (le script est decoupe sur chaque ";"), donc rejouable sans effet sur les demarrages suivants.
-- Les deux backfills passent par EXEC('...') : SQL Server compile les noms de colonnes meme dans un
-- IF faux, et un INSERT qui cite u.role_id ou u.role echouerait une fois la colonne supprimee.
IF COL_LENGTH('dbo.utilisateurs', 'role_id') IS NOT NULL
EXEC('INSERT INTO dbo.user_roles (user_id, role_id) SELECT u.id, u.role_id FROM dbo.utilisateurs u WHERE u.role_id IS NOT NULL AND NOT EXISTS (SELECT 1 FROM dbo.user_roles ur WHERE ur.user_id = u.id AND ur.role_id = u.role_id)');

IF COL_LENGTH('dbo.utilisateurs', 'role') IS NOT NULL
EXEC('INSERT INTO dbo.user_roles (user_id, role_id) SELECT u.id, r.id FROM dbo.utilisateurs u JOIN dbo.roles r ON r.code = u.role WHERE NOT EXISTS (SELECT 1 FROM dbo.user_roles ur WHERE ur.user_id = u.id)');

-- Suppression des anciennes colonnes, SEULEMENT si tous les utilisateurs ont au moins une ligne dans
-- user_roles (garde anti-perte de donnees : continue-on-error masquerait un backfill rate).
IF EXISTS (SELECT 1 FROM sys.check_constraints WHERE name = 'CK_utilisateurs_role')
AND NOT EXISTS (SELECT 1 FROM dbo.utilisateurs u WHERE NOT EXISTS (SELECT 1 FROM dbo.user_roles ur WHERE ur.user_id = u.id))
ALTER TABLE dbo.utilisateurs DROP CONSTRAINT CK_utilisateurs_role;

IF COL_LENGTH('dbo.utilisateurs', 'role') IS NOT NULL
AND NOT EXISTS (SELECT 1 FROM dbo.utilisateurs u WHERE NOT EXISTS (SELECT 1 FROM dbo.user_roles ur WHERE ur.user_id = u.id))
ALTER TABLE dbo.utilisateurs DROP COLUMN role;

IF EXISTS (SELECT 1 FROM sys.foreign_keys WHERE name = 'FK_utilisateurs_role')
AND NOT EXISTS (SELECT 1 FROM dbo.utilisateurs u WHERE NOT EXISTS (SELECT 1 FROM dbo.user_roles ur WHERE ur.user_id = u.id))
ALTER TABLE dbo.utilisateurs DROP CONSTRAINT FK_utilisateurs_role;

IF COL_LENGTH('dbo.utilisateurs', 'role_id') IS NOT NULL
AND NOT EXISTS (SELECT 1 FROM dbo.utilisateurs u WHERE NOT EXISTS (SELECT 1 FROM dbo.user_roles ur WHERE ur.user_id = u.id))
ALTER TABLE dbo.utilisateurs DROP COLUMN role_id;

IF NOT EXISTS (SELECT 1 FROM dbo.utilisateurs WHERE email = N'admin@example.com')
INSERT INTO dbo.utilisateurs (nom, prenom, surnom, email, age, num_tel, mot_de_passe)
VALUES (
    N'Admin',
    N'Super',
    N'admin',
    N'admin@example.com',
    30,
    N'0600000000',
    N'$argon2id$v=19$m=16384,t=2,p=1$CvjoETl/TZjWBH8Ha2v5BQ$BU1RT1DUQrOstlU75UeHcuQsL3Bc1BCK6snCCuwkhO8'
);

-- Role ADMIN de l'admin d'amorcage : insere seulement s'il n'a AUCUNE ligne user_roles (un simple
-- "n'a pas ADMIN" lui redonnerait ADMIN a chaque demarrage meme apres un retrait volontaire).
IF EXISTS (SELECT 1 FROM dbo.utilisateurs WHERE email = N'admin@example.com')
AND NOT EXISTS (SELECT 1 FROM dbo.user_roles ur JOIN dbo.utilisateurs u ON u.id = ur.user_id WHERE u.email = N'admin@example.com')
INSERT INTO dbo.user_roles (user_id, role_id)
SELECT u.id, r.id FROM dbo.utilisateurs u CROSS JOIN dbo.roles r WHERE u.email = N'admin@example.com' AND r.code = 'ADMIN';

-- Profil SUPER_ADMIN de l'admin d'amorcage, avec une garde "recuperation de verrouillage" : affecte seulement si
-- PLUS PERSONNE n'a un profil "voit tout" (sinon l'admin aurait de nouveau SUPER_ADMIN a chaque demarrage,
-- meme apres une reaffectation volontaire). L'auto-affectation etant interdite par l'API, l'admin ne peut pas
-- se verrouiller lui-meme ; ce cas ne sert qu'a retrouver un acces apres un incident.
IF EXISTS (SELECT 1 FROM dbo.utilisateurs WHERE email = N'admin@example.com')
AND NOT EXISTS (SELECT 1 FROM dbo.utilisateurs u JOIN dbo.profils p ON p.id = u.profil_id WHERE p.tout_voir = 1)
UPDATE u SET profil_id = p.id FROM dbo.utilisateurs u CROSS JOIN dbo.profils p WHERE u.email = N'admin@example.com' AND p.code = 'SUPER_ADMIN';
