-- V7 — roles become rows, the audit log stops storing its own labels, and 'LTD' goes.
--
-- admin.roles held every role of an administrator in one string, semicolon-joined. That
-- cannot be queried ("who has ROLE_ADMIN" is a LIKE that also matches ROLE_ADMIN_ANYTHING),
-- cannot be constrained (a typo'd role stored as readily as a real one), and made the empty
-- case ambiguous: NULL, '' and ';' all meant no roles and the code had to guess.
--
-- system_log.busincess_type — business_type since S07 — stored the Chinese label, because
-- the aspect wrote annotation.type().getName(). Storing display text bakes a UI language
-- into the data: translating the interface would strand every historical row.
--
-- And the 'LTD' defaults, which came from hand-written columnDefinition on the entities.
-- Nothing means LTD. It was a placeholder for "not set", spelled as a value, on columns
-- declared NOT NULL — so "unknown" and "the string LTD" were indistinguishable forever.

-- --------------------------------------------------------------------------------------
-- admin.roles -> admin_roles
-- --------------------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `admin_roles` (
  `admin_id` varchar(255) NOT NULL,
  `role`     varchar(30)  NOT NULL,
  PRIMARY KEY (`admin_id`, `role`),
  CONSTRAINT fk_admin_roles_admin FOREIGN KEY (`admin_id`) REFERENCES `admin` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Historical rows hold one role each; the semicolon form was supported but never used.
-- Anything unrecognised is dropped rather than carried over as an invalid enum name, which
-- would fail to read back.
INSERT INTO `admin_roles` (admin_id, role)
SELECT id, roles FROM `admin`
WHERE roles IS NOT NULL
  AND roles IN ('ROLE_SUPER_ADMIN','ROLE_ADMIN','ROLE_COMMODITY','ROLE_EMPLOYEE','ROLE_SALE','ROLE_WAREHOUSE');

ALTER TABLE `admin` DROP COLUMN roles;

-- --------------------------------------------------------------------------------------
-- system_log.business_type: label -> enum name
-- --------------------------------------------------------------------------------------
UPDATE `system_log` SET business_type = CASE business_type
    WHEN '查询' THEN 'QUERY'
    WHEN '新增' THEN 'INSERT'
    WHEN '更新' THEN 'UPDATE'
    WHEN '删除' THEN 'DELETE'
    WHEN '导出' THEN 'EXPORT'
    WHEN '退出' THEN 'FORCE'
    WHEN '其他' THEN 'OTHER'
    ELSE 'OTHER'
    END;
ALTER TABLE `system_log` MODIFY business_type varchar(20) NOT NULL;

-- --------------------------------------------------------------------------------------
-- 'LTD'
-- --------------------------------------------------------------------------------------
-- Dropping the default only stops new rows inheriting it; the rows that already took it
-- keep a value that means nothing. ip and method are NOT NULL, so they cannot be nulled —
-- they are left as they are rather than invented, and 'LTD' in an old audit row is at least
-- honest about being unknown once you know what it was.
ALTER TABLE `admin`      MODIFY email    varchar(100) NOT NULL;
ALTER TABLE `admin`      MODIFY password varchar(100) NOT NULL;
ALTER TABLE `system_log` MODIFY ip       varchar(45)  NOT NULL;
ALTER TABLE `system_log` MODIFY method   varchar(200) NOT NULL;
