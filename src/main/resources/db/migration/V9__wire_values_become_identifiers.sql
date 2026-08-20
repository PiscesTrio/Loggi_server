-- V9 — the three columns that stored Chinese become closed sets of identifiers.
--
-- driver.gender held 男性 / 女性, vehicle.type held 货车 / 卡车 / 重卡, and distribution.care held
-- the eight handling labels comma-joined with a trailing comma. None of them was display text that
-- happened to be Chinese: the client *compared against* these exact strings to pick an avatar, an
-- icon and a chip. They were the protocol, written in a natural language.
--
-- That has two costs. The one that forced this migration: the interface is becoming Japanese and
-- English as well as Chinese, and a value the client must match character for character cannot be
-- translated — so either the UI stays Chinese or the data does. The one that was already being
-- paid: nothing constrained the columns, so a client sending 男 stored as readily as 男性 and took
-- the wrong branch forever after, and the seed file needed a comment begging future editors not to
-- change the strings.
--
-- Unmapped values become NULL rather than a guess. V7 mapped unknown business_type to OTHER because
-- that column is NOT NULL and OTHER means "unclassified"; there is no constant here that means
-- "unknown gender", and inventing one would be a lie recorded in the data.

-- --------------------------------------------------------------------------------------
-- driver.gender, employee.gender -> Gender
-- --------------------------------------------------------------------------------------
UPDATE `driver` SET gender = CASE gender
    WHEN '男性' THEN 'MALE'
    WHEN '女性' THEN 'FEMALE'
    ELSE NULL
    END;
ALTER TABLE `driver` MODIFY gender varchar(10) DEFAULT NULL;

-- employee is API-only and unseeded, so this is very likely a no-op. Written anyway: "probably
-- empty" is not something a migration may assume about somebody else's database.
UPDATE `employee` SET gender = CASE gender
    WHEN '男性' THEN 'MALE'
    WHEN '女性' THEN 'FEMALE'
    ELSE NULL
    END;
ALTER TABLE `employee` MODIFY gender varchar(10) DEFAULT NULL;

-- --------------------------------------------------------------------------------------
-- vehicle.type -> VehicleType
-- --------------------------------------------------------------------------------------
UPDATE `vehicle` SET type = CASE type
    WHEN '货车' THEN 'LIGHT_TRUCK'
    WHEN '卡车' THEN 'TRUCK'
    WHEN '重卡' THEN 'HEAVY_TRUCK'
    ELSE NULL
    END;
ALTER TABLE `vehicle` MODIFY type varchar(20) DEFAULT NULL;

-- --------------------------------------------------------------------------------------
-- distribution.care -> distribution_care rows
-- --------------------------------------------------------------------------------------
-- Rows rather than a rewritten string, for the reasons V7 gave when admin.roles became
-- admin_roles. It also makes this the safest of the three conversions rather than the most
-- dangerous: eight exact FIND_IN_SET tests against a comma-separated list, instead of token
-- surgery on a varchar where a botched replacement would be silent and unrecoverable.
--
-- FIND_IN_SET is exact where LIKE is not — '防潮' is not a substring of any other tag today, but
-- that is a property of the current eight, not a rule. The trailing comma the client appends
-- produces one empty element, which FIND_IN_SET ignores.
CREATE TABLE IF NOT EXISTS `distribution_care` (
  `distribution_id` varchar(255) NOT NULL,
  `tag`             varchar(30)  NOT NULL,
  PRIMARY KEY (`distribution_id`, `tag`),
  CONSTRAINT fk_distribution_care_distribution
    FOREIGN KEY (`distribution_id`) REFERENCES `distribution` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

INSERT INTO `distribution_care` (distribution_id, tag)
SELECT id, 'FRAGILE'                 FROM `distribution` WHERE FIND_IN_SET('易碎',     care) > 0;
INSERT INTO `distribution_care` (distribution_id, tag)
SELECT id, 'KEEP_DRY'                FROM `distribution` WHERE FIND_IN_SET('防潮',     care) > 0;
INSERT INTO `distribution_care` (distribution_id, tag)
SELECT id, 'KEEP_AWAY_FROM_SUNLIGHT' FROM `distribution` WHERE FIND_IN_SET('防晒',     care) > 0;
INSERT INTO `distribution_care` (distribution_id, tag)
SELECT id, 'PROTECT_FROM_HEAT'       FROM `distribution` WHERE FIND_IN_SET('防高温',   care) > 0;
INSERT INTO `distribution_care` (distribution_id, tag)
SELECT id, 'DO_NOT_ROLL'             FROM `distribution` WHERE FIND_IN_SET('禁止翻滚', care) > 0;
INSERT INTO `distribution_care` (distribution_id, tag)
SELECT id, 'DO_NOT_STACK'            FROM `distribution` WHERE FIND_IN_SET('禁止堆码', care) > 0;
INSERT INTO `distribution_care` (distribution_id, tag)
SELECT id, 'REFRIGERATE'             FROM `distribution` WHERE FIND_IN_SET('冷藏',     care) > 0;
INSERT INTO `distribution_care` (distribution_id, tag)
SELECT id, 'FLAMMABLE'               FROM `distribution` WHERE FIND_IN_SET('易燃',     care) > 0;

ALTER TABLE `distribution` DROP COLUMN care;
