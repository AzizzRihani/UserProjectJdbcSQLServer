-- =========================================================
-- Script de creation de la base de donnees SQL Server
-- Projet : Gestion d'utilisateurs
-- =========================================================

IF DB_ID('UserProject') IS NULL
BEGIN
    CREATE DATABASE UserProject;
END
GO

USE UserProject;
GO

IF OBJECT_ID('dbo.utilisateurs', 'U') IS NOT NULL
    DROP TABLE dbo.utilisateurs;
GO

CREATE TABLE dbo.utilisateurs (
    id            BIGINT IDENTITY(1,1) PRIMARY KEY,
    nom           NVARCHAR(100) NOT NULL,
    prenom        NVARCHAR(100) NOT NULL,
    surnom        NVARCHAR(100) NULL,
    email         NVARCHAR(150) NOT NULL,
    age           INT NULL,
    num_tel       NVARCHAR(20) NULL,
    mot_de_passe  NVARCHAR(255) NOT NULL,
    role          NVARCHAR(20) NOT NULL,
    CONSTRAINT UQ_utilisateurs_email UNIQUE (email),
    CONSTRAINT CK_utilisateurs_role CHECK (role IN ('USER', 'ADMIN'))
);
GO

-- Compte ADMIN de bootstrap (necessaire car la creation d'utilisateurs
-- via l'API est reservee au role ADMIN : il faut un premier admin en base).
-- Email    : admin@example.com
-- Mot de passe (en clair, pour la connexion) : Admin@123
-- Le hash ci-dessous est un BCrypt (compatible BCryptPasswordEncoder de Spring Security).
INSERT INTO dbo.utilisateurs (nom, prenom, surnom, email, age, num_tel, mot_de_passe, role)
VALUES (
    N'Admin',
    N'Super',
    N'admin',
    N'admin@example.com',
    30,
    N'0600000000',
    N'$2b$10$tLMP0lBxpQ/bjLykqEsG9e6b0lIMA.rJfMlCMyDL5s4N5ywqfNTOi',
    N'ADMIN'
);
GO
