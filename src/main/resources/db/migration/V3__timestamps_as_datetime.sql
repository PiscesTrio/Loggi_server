-- V3 — creation and modification timestamps become real datetimes.
--
-- They were VARCHAR(255) holding text like '2026-08-01 09:00:00', written by hand in every
-- service. A timestamp stored as text cannot be compared, ranged over or ordered by the
-- database without parsing each row, and the column accepted anything at all — including
-- the empty string, which is what an unset value looked like.
--
-- Conversion is done in two steps rather than a bare MODIFY. MySQL in strict mode aborts
-- the whole statement if a single value fails to convert, so anything unparseable is nulled
-- first: a row losing a timestamp it never really had is better than a migration that stops
-- halfway through a table.

-- --------------------------------------------------------------------------------------
-- Null out values that will not convert, so MODIFY cannot fail on one bad row
-- --------------------------------------------------------------------------------------
UPDATE `admin`            SET create_at = NULL WHERE STR_TO_DATE(create_at, '%Y-%m-%d %H:%i:%s') IS NULL;
UPDATE `commodity`        SET create_at = NULL WHERE STR_TO_DATE(create_at, '%Y-%m-%d %H:%i:%s') IS NULL;
UPDATE `commodity`        SET update_at = NULL WHERE STR_TO_DATE(update_at, '%Y-%m-%d %H:%i:%s') IS NULL;
UPDATE `driver`           SET create_at = NULL WHERE STR_TO_DATE(create_at, '%Y-%m-%d %H:%i:%s') IS NULL;
UPDATE `driver`           SET update_at = NULL WHERE STR_TO_DATE(update_at, '%Y-%m-%d %H:%i:%s') IS NULL;
UPDATE `employee`         SET create_at = NULL WHERE STR_TO_DATE(create_at, '%Y-%m-%d %H:%i:%s') IS NULL;
UPDATE `employee`         SET update_at = NULL WHERE STR_TO_DATE(update_at, '%Y-%m-%d %H:%i:%s') IS NULL;
UPDATE `sale`             SET create_at = NULL WHERE STR_TO_DATE(create_at, '%Y-%m-%d %H:%i:%s') IS NULL;
UPDATE `vehicle`          SET create_at = NULL WHERE STR_TO_DATE(create_at, '%Y-%m-%d %H:%i:%s') IS NULL;
UPDATE `warehouse`        SET create_at = NULL WHERE STR_TO_DATE(create_at, '%Y-%m-%d %H:%i:%s') IS NULL;
UPDATE `inventory_record` SET create_at = NULL WHERE STR_TO_DATE(create_at, '%Y-%m-%d %H:%i:%s') IS NULL;

-- --------------------------------------------------------------------------------------
-- Convert. datetime(6) because that is what Hibernate maps LocalDateTime to, and
-- ddl-auto: validate compares the type, not just the name.
-- --------------------------------------------------------------------------------------
ALTER TABLE `admin`            MODIFY create_at datetime(6);
ALTER TABLE `commodity`        MODIFY create_at datetime(6), MODIFY update_at datetime(6);
ALTER TABLE `driver`           MODIFY create_at datetime(6), MODIFY update_at datetime(6);
ALTER TABLE `employee`         MODIFY create_at datetime(6), MODIFY update_at datetime(6);
ALTER TABLE `sale`             MODIFY create_at datetime(6);
ALTER TABLE `vehicle`          MODIFY create_at datetime(6);
ALTER TABLE `warehouse`        MODIFY create_at datetime(6);
ALTER TABLE `inventory_record` MODIFY create_at datetime(6);

-- --------------------------------------------------------------------------------------
-- The other half of the pair. Every entity now carries both, because "when did this row
-- last change" is a question worth being able to answer of any of them — and because a
-- shared mapped superclass that is only half applied is not a rule, it is a suggestion.
-- --------------------------------------------------------------------------------------
ALTER TABLE `admin`            ADD COLUMN update_at datetime(6);
ALTER TABLE `sale`             ADD COLUMN update_at datetime(6);
ALTER TABLE `vehicle`          ADD COLUMN update_at datetime(6);
ALTER TABLE `warehouse`        ADD COLUMN update_at datetime(6);
ALTER TABLE `inventory_record` ADD COLUMN update_at datetime(6);

-- inventory tracks a stock level that changes constantly and recorded neither.
ALTER TABLE `inventory`    ADD COLUMN create_at datetime(6), ADD COLUMN update_at datetime(6);

-- distribution — the order this system exists to move — recorded the delivery time the
-- user asked for and never recorded when the order itself was placed.
ALTER TABLE `distribution` ADD COLUMN create_at datetime(6), ADD COLUMN update_at datetime(6);
