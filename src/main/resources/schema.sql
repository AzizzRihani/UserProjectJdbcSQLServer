CREATE TABLE dbo.utilisateurs (
    id            BIGINT IDENTITY(1,1) PRIMARY KEY,
    nom           NVARCHAR(100) NOT NULL,
    prenom        NVARCHAR(100) NOT NULL,
    surnom        NVARCHAR(100) NULL,
    email         NVARCHAR(150) NOT NULL,
    age           INT NULL,
    num_tel       NVARCHAR(20) NULL,
    mot_de_passe  NVARCHAR(255) NOT NULL,
    CONSTRAINT UQ_utilisateurs_email UNIQUE (email)
);

-- RBAC dynamique : roles et permissions configurables par l'admin, en remplacement de l'enum
-- Role fige a 2 valeurs. Gardes IF ... <instruction unique> (pas de BEGIN/END) car schema.sql
-- est rejoue a chaque demarrage (spring.sql.init.mode: always) contre une base persistante : le
-- script est decoupe statement par statement sur chaque ";", ce qui casserait un bloc BEGIN/END
-- contenant plusieurs instructions (chaque morceau serait envoye separement au driver).
IF OBJECT_ID('dbo.roles', 'U') IS NULL
CREATE TABLE dbo.roles (
    id    BIGINT IDENTITY(1,1) PRIMARY KEY,
    code  NVARCHAR(50) NOT NULL,
    label NVARCHAR(100) NOT NULL,
    CONSTRAINT UQ_roles_code UNIQUE (code)
);

IF OBJECT_ID('dbo.permissions', 'U') IS NULL
CREATE TABLE dbo.permissions (
    id     BIGINT IDENTITY(1,1) PRIMARY KEY,
    code   NVARCHAR(50) NOT NULL,
    module NVARCHAR(50) NOT NULL,
    label  NVARCHAR(150) NOT NULL,
    CONSTRAINT UQ_permissions_code UNIQUE (code)
);

IF OBJECT_ID('dbo.role_permissions', 'U') IS NULL
CREATE TABLE dbo.role_permissions (
    role_id       BIGINT NOT NULL,
    permission_id BIGINT NOT NULL,
    PRIMARY KEY (role_id, permission_id),
    CONSTRAINT FK_role_permissions_role FOREIGN KEY (role_id) REFERENCES dbo.roles (id),
    CONSTRAINT FK_role_permissions_permission FOREIGN KEY (permission_id) REFERENCES dbo.permissions (id)
);

-- Un utilisateur peut avoir plusieurs roles (table de jointure). Les permissions effectives sont
-- l'union des permissions de tous ses roles. ON DELETE CASCADE cote utilisateur : supprimer un
-- utilisateur reste une seule instruction ; PAS de cascade cote role : on garde la garde "role
-- encore utilise" (RoleService.delete) et la FK protege aussi contre une suppression concurrente.
-- La migration depuis l'ancienne colonne utilisateurs.role_id (backfill puis suppression de la
-- colonne) vit dans data.sql, qui s'execute apres ce fichier (il faut que roles soit seede).
IF OBJECT_ID('dbo.user_roles', 'U') IS NULL
CREATE TABLE dbo.user_roles (
    user_id BIGINT NOT NULL,
    role_id BIGINT NOT NULL,
    PRIMARY KEY (user_id, role_id),
    CONSTRAINT FK_user_roles_user FOREIGN KEY (user_id) REFERENCES dbo.utilisateurs (id) ON DELETE CASCADE,
    CONSTRAINT FK_user_roles_role FOREIGN KEY (role_id) REFERENCES dbo.roles (id)
);

IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = 'IX_user_roles_role_id' AND object_id = OBJECT_ID('dbo.user_roles'))
CREATE INDEX IX_user_roles_role_id ON dbo.user_roles (role_id);

-- Profils "But" : un profil = ensemble de FONCTIONS (entrees de menu, en arbre) qu'un utilisateur peut voir et
-- ouvrir. Un utilisateur a UN seul profil. Les droits d'agir restent les permissions (roles) : le profil ne
-- donne que le menu. Memes regles que plus haut : gardes "IF ... <instruction unique>" (script decoupe sur ";").
IF OBJECT_ID('dbo.fonctions', 'U') IS NULL
CREATE TABLE dbo.fonctions (
    id        BIGINT IDENTITY(1,1) PRIMARY KEY,
    code      NVARCHAR(50) NOT NULL,
    label     NVARCHAR(100) NOT NULL,
    parent_id BIGINT NULL,
    route     NVARCHAR(100) NULL,
    ordre     INT NOT NULL DEFAULT 0,
    CONSTRAINT UQ_fonctions_code UNIQUE (code),
    CONSTRAINT FK_fonctions_parent FOREIGN KEY (parent_id) REFERENCES dbo.fonctions (id),
    CONSTRAINT CK_fonctions_parent CHECK (parent_id IS NULL OR parent_id <> id)
);

-- "Profil 0" (super-administrateur, voit toutes les fonctions) = drapeau tout_voir sur le profil SUPER_ADMIN
-- seede, plutot qu'un id litteral 0 (evite IDENTITY_INSERT).
IF OBJECT_ID('dbo.profils', 'U') IS NULL
CREATE TABLE dbo.profils (
    id       BIGINT IDENTITY(1,1) PRIMARY KEY,
    code     NVARCHAR(50) NOT NULL,
    label    NVARCHAR(100) NOT NULL,
    valide   BIT NOT NULL DEFAULT 1,
    tout_voir BIT NOT NULL DEFAULT 0,
    CONSTRAINT UQ_profils_code UNIQUE (code)
);

IF OBJECT_ID('dbo.profil_fonctions', 'U') IS NULL
CREATE TABLE dbo.profil_fonctions (
    profil_id   BIGINT NOT NULL,
    fonction_id BIGINT NOT NULL,
    PRIMARY KEY (profil_id, fonction_id),
    CONSTRAINT FK_profil_fonctions_profil FOREIGN KEY (profil_id) REFERENCES dbo.profils (id) ON DELETE CASCADE,
    CONSTRAINT FK_profil_fonctions_fonction FOREIGN KEY (fonction_id) REFERENCES dbo.fonctions (id)
);

IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = 'IX_profil_fonctions_fonction_id' AND object_id = OBJECT_ID('dbo.profil_fonctions'))
CREATE INDEX IX_profil_fonctions_fonction_id ON dbo.profil_fonctions (fonction_id);

-- Profil de l'utilisateur (NULL = aucun profil = menu vide). Pas de cascade : un profil utilise ne se supprime pas.
IF COL_LENGTH('dbo.utilisateurs', 'profil_id') IS NULL
ALTER TABLE dbo.utilisateurs ADD profil_id BIGINT NULL CONSTRAINT FK_utilisateurs_profil FOREIGN KEY REFERENCES dbo.profils (id);

CREATE TABLE dbo.banque (
    id            BIGINT IDENTITY(1,1) PRIMARY KEY,
    nom           NVARCHAR(150) NOT NULL,
    code_swift    NVARCHAR(11) NULL,
    code_banque   NVARCHAR(20) NULL,
    adresse       NVARCHAR(200) NULL,
    ville         NVARCHAR(100) NULL,
    pays          NVARCHAR(100) NULL,
    telephone     NVARCHAR(20) NULL,
    email         NVARCHAR(150) NULL,
    actif         BIT NOT NULL DEFAULT 1
);

-- Index unique filtre plutot que CONSTRAINT UNIQUE : code_banque etant nullable, une contrainte UNIQUE
-- classique sous SQL Server n'autoriserait qu'UNE seule ligne avec code_banque = NULL (contrairement a
-- Postgres/MySQL qui traitent chaque NULL comme distinct) - la 2e banque sans code ferait echouer l'INSERT.
-- Un index filtre exige explicitement QUOTED_IDENTIFIER ON dans la session courante.
SET QUOTED_IDENTIFIER ON;
CREATE UNIQUE INDEX UQ_banque_code_banque ON dbo.banque (code_banque) WHERE code_banque IS NOT NULL;

CREATE TABLE dbo.compte (
    id              BIGINT IDENTITY(1,1) PRIMARY KEY,
    banque_id       BIGINT NOT NULL,
    iban            NVARCHAR(34) NOT NULL,
    solde           DECIMAL(18,2) NOT NULL DEFAULT 0,
    devise          NVARCHAR(3) NOT NULL DEFAULT 'EUR',
    type_compte     NVARCHAR(20) NOT NULL,
    date_ouverture  DATE NOT NULL,
    actif           BIT NOT NULL DEFAULT 1,
    CONSTRAINT UQ_compte_iban UNIQUE (iban),
    CONSTRAINT CK_compte_type_compte CHECK (type_compte IN ('COURANT', 'EPARGNE')),
    CONSTRAINT FK_compte_banque FOREIGN KEY (banque_id) REFERENCES dbo.banque (id)
);
