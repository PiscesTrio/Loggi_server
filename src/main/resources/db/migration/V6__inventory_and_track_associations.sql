-- V6 — the remaining bare foreign keys become real ones.
--
-- inventory and inventory_record named a warehouse and a commodity through the string
-- columns wid/cid; distribution_track named its order through dis_id. Unlike
-- distribution.wid these really did hold ids, so they can be renamed in place — but none of
-- them had a foreign key, so a stock row could name a warehouse that no longer exists and a
-- track point could outlive the order it describes, with nothing to say so.
--
-- inventory_record.type is converted here too. It held +1 or -1, with the meaning declared
-- privately inside one service — so every other reader of that column knew it by hearsay.

-- --------------------------------------------------------------------------------------
-- inventory
-- --------------------------------------------------------------------------------------
ALTER TABLE `inventory` CHANGE wid warehouse_id varchar(255);
ALTER TABLE `inventory` CHANGE cid commodity_id varchar(255);

UPDATE `inventory` i LEFT JOIN `warehouse` x ON x.id = i.warehouse_id SET i.warehouse_id = NULL WHERE i.warehouse_id IS NOT NULL AND x.id IS NULL;
UPDATE `inventory` i LEFT JOIN `commodity` x ON x.id = i.commodity_id SET i.commodity_id = NULL WHERE i.commodity_id IS NOT NULL AND x.id IS NULL;

ALTER TABLE `inventory` ADD CONSTRAINT fk_inventory_warehouse FOREIGN KEY (warehouse_id) REFERENCES `warehouse` (id);
ALTER TABLE `inventory` ADD CONSTRAINT fk_inventory_commodity FOREIGN KEY (commodity_id) REFERENCES `commodity` (id);

-- The indexes from V2 survived the rename but kept names describing columns that no longer
-- exist. A name that lies is worse than a name that is merely terse.
ALTER TABLE `inventory` RENAME INDEX uk_inventory_wid_cid TO uk_inventory_warehouse_commodity;
ALTER TABLE `inventory` RENAME INDEX idx_inventory_cid    TO idx_inventory_commodity_id;

-- --------------------------------------------------------------------------------------
-- inventory_record
-- --------------------------------------------------------------------------------------
ALTER TABLE `inventory_record` CHANGE wid warehouse_id varchar(255);
ALTER TABLE `inventory_record` CHANGE cid commodity_id varchar(255);

UPDATE `inventory_record` r LEFT JOIN `warehouse` x ON x.id = r.warehouse_id SET r.warehouse_id = NULL WHERE r.warehouse_id IS NOT NULL AND x.id IS NULL;
UPDATE `inventory_record` r LEFT JOIN `commodity` x ON x.id = r.commodity_id SET r.commodity_id = NULL WHERE r.commodity_id IS NOT NULL AND x.id IS NULL;

ALTER TABLE `inventory_record` ADD CONSTRAINT fk_inventory_record_warehouse FOREIGN KEY (warehouse_id) REFERENCES `warehouse` (id);
ALTER TABLE `inventory_record` ADD CONSTRAINT fk_inventory_record_commodity FOREIGN KEY (commodity_id) REFERENCES `commodity` (id);

ALTER TABLE `inventory_record` RENAME INDEX idx_inventory_record_wid TO idx_inventory_record_warehouse_id;
ALTER TABLE `inventory_record` RENAME INDEX idx_inventory_record_cid TO idx_inventory_record_commodity_id;

-- +1 / -1 become IN / OUT. Through a new column, for the same reason as V5's status: an INT
-- column changed to VARCHAR yields the string '1', which is not the name of anything.
ALTER TABLE `inventory_record` ADD COLUMN type_name varchar(10);
UPDATE `inventory_record` SET type_name = CASE
    WHEN type > 0 THEN 'IN'
    WHEN type < 0 THEN 'OUT'
    END;
ALTER TABLE `inventory_record` DROP COLUMN type;
ALTER TABLE `inventory_record` CHANGE type_name type varchar(10);
-- The index from V2 was on the old integer column and went with it.
CREATE INDEX idx_inventory_record_type ON `inventory_record` (`type`);

-- --------------------------------------------------------------------------------------
-- distribution_track
-- --------------------------------------------------------------------------------------
ALTER TABLE `distribution_track` CHANGE dis_id distribution_id varchar(255);

UPDATE `distribution_track` t LEFT JOIN `distribution` x ON x.id = t.distribution_id SET t.distribution_id = NULL WHERE t.distribution_id IS NOT NULL AND x.id IS NULL;

ALTER TABLE `distribution_track` ADD CONSTRAINT fk_distribution_track_distribution FOREIGN KEY (distribution_id) REFERENCES `distribution` (id);
ALTER TABLE `distribution_track` RENAME INDEX idx_distribution_track_dis_id TO idx_distribution_track_distribution_id;

ALTER TABLE `distribution_track` ADD COLUMN status_name varchar(20);
UPDATE `distribution_track` SET status_name = CASE status
    WHEN 0 THEN 'REVIEWING'
    WHEN 1 THEN 'REVIEW_SUCCESS'
    WHEN 2 THEN 'END'
    END;
ALTER TABLE `distribution_track` DROP COLUMN status;
ALTER TABLE `distribution_track` CHANGE status_name status varchar(20);
