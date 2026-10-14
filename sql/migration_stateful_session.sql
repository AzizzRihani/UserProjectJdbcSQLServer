-- Migration a executer UNE FOIS sur la base existante.
-- schema.sql ne gere que des CREATE/ALTER ADD, jamais de DROP : ce nettoyage doit etre applique manuellement.

-- Etape 1 (auth stateful par cookie de session) : plus de refresh_token.
IF OBJECT_ID('dbo.refresh_tokens', 'U') IS NOT NULL
    DROP TABLE dbo.refresh_tokens;

IF COL_LENGTH('dbo.utilisateurs', 'acces_revoque_depuis') IS NOT NULL
    ALTER TABLE dbo.utilisateurs DROP COLUMN acces_revoque_depuis;

-- Etape 2 (sessions en memoire plutot que Spring Session JDBC) : plus de tables de sessions en base.
IF OBJECT_ID('dbo.SPRING_SESSION_ATTRIBUTES', 'U') IS NOT NULL
    DROP TABLE dbo.SPRING_SESSION_ATTRIBUTES;

IF OBJECT_ID('dbo.SPRING_SESSION', 'U') IS NOT NULL
    DROP TABLE dbo.SPRING_SESSION;
