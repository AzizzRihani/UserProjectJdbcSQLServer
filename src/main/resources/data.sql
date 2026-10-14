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
    N'$argon2id$v=19$m=16384,t=2,p=1$BwkgUyQx5H+yq5JzUgVoYA$U/Dfl2m5Ng6SjrU1CRsdc74xKrzQWPlgPrNlKU0yWNo'
);

-- Role ADMIN de l'admin d'amorcage : insere seulement s'il n'a AUCUNE ligne user_roles (un simple
-- "n'a pas ADMIN" lui redonnerait ADMIN a chaque demarrage meme apres un retrait volontaire).
IF EXISTS (SELECT 1 FROM dbo.utilisateurs WHERE email = N'admin@example.com')
AND NOT EXISTS (SELECT 1 FROM dbo.user_roles ur JOIN dbo.utilisateurs u ON u.id = ur.user_id WHERE u.email = N'admin@example.com')
INSERT INTO dbo.user_roles (user_id, role_id)
SELECT u.id, r.id FROM dbo.utilisateurs u CROSS JOIN dbo.roles r WHERE u.email = N'admin@example.com' AND r.code = 'ADMIN';
