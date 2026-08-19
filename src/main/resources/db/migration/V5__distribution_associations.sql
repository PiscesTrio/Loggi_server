-- V5 — an order points at its driver, vehicle and warehouse instead of describing them.
--
-- distribution carried five columns for three references: did, vid and wid (ids, as bare
-- strings with no foreign key), plus driver and number, which were copies of the driver's
-- name and the vehicle's plate. Reading the driver meant a second query by hand; the copies
-- drifted the moment either row was edited; and nothing stopped an order naming a driver
-- who does not exist, because the column accepted any string at all.
--
-- One column is not what it looks like. `wid` holds the warehouse NAME, not its id — the
-- apply form writes the name because the tracking timeline renders it verbatim. Renaming it
-- to warehouse_id and adding a foreign key, as the plan for this slice suggested, would have
-- pointed every row at an id that does not exist. It is resolved by name below.

-- --------------------------------------------------------------------------------------
-- warehouse.name has been an identifier all along; make it one
-- --------------------------------------------------------------------------------------
-- Required before the lookup below can be well defined: resolving an order's origin by name
-- only makes sense if a name identifies one warehouse. The data already assumed it.
ALTER TABLE `warehouse` ADD CONSTRAINT uk_warehouse_name UNIQUE (`name`);

-- --------------------------------------------------------------------------------------
-- did / vid: rename in place, they really were ids
-- --------------------------------------------------------------------------------------
ALTER TABLE `distribution` CHANGE did driver_id  varchar(255);
ALTER TABLE `distribution` CHANGE vid vehicle_id varchar(255);

-- --------------------------------------------------------------------------------------
-- wid: a name, so translate it
-- --------------------------------------------------------------------------------------
ALTER TABLE `distribution` ADD COLUMN warehouse_id varchar(255);
UPDATE `distribution` d JOIN `warehouse` w ON w.name = d.wid SET d.warehouse_id = w.id;
ALTER TABLE `distribution` DROP COLUMN wid;

-- The denormalised copies. distribution.driver held a name, not an id, and shares its
-- column name with the association replacing it, so it has to go before the constraint.
ALTER TABLE `distribution` DROP COLUMN driver, DROP COLUMN number;

-- --------------------------------------------------------------------------------------
-- Break the danglers before the constraints refuse to be created
-- --------------------------------------------------------------------------------------
-- Any of these three could be pointing at nothing: for four years the columns were plain
-- strings. A row losing a reference it never really had is the honest outcome; a migration
-- that aborts on the first bad row is not.
UPDATE `distribution` d LEFT JOIN `driver`    x ON x.id = d.driver_id    SET d.driver_id    = NULL WHERE d.driver_id    IS NOT NULL AND x.id IS NULL;
UPDATE `distribution` d LEFT JOIN `vehicle`   x ON x.id = d.vehicle_id   SET d.vehicle_id   = NULL WHERE d.vehicle_id   IS NOT NULL AND x.id IS NULL;
UPDATE `distribution` d LEFT JOIN `warehouse` x ON x.id = d.warehouse_id SET d.warehouse_id = NULL WHERE d.warehouse_id IS NOT NULL AND x.id IS NULL;

ALTER TABLE `distribution` ADD CONSTRAINT fk_distribution_driver    FOREIGN KEY (driver_id)    REFERENCES `driver`    (id);
ALTER TABLE `distribution` ADD CONSTRAINT fk_distribution_vehicle   FOREIGN KEY (vehicle_id)   REFERENCES `vehicle`   (id);
ALTER TABLE `distribution` ADD CONSTRAINT fk_distribution_warehouse FOREIGN KEY (warehouse_id) REFERENCES `warehouse` (id);

-- --------------------------------------------------------------------------------------
-- status: 0/1/2 becomes the name of the enum that has described them all along
-- --------------------------------------------------------------------------------------
-- Stored as the name, not the ordinal. An ordinal changes meaning the day someone inserts a
-- constant in the middle of the enum, and nothing in the database would record that it did.
--
-- Converted through a new column rather than by MODIFY: changing an INT column to VARCHAR
-- turns 0 into the string '0', which is not the name of anything and fails to read back.
ALTER TABLE `distribution` ADD COLUMN status_name varchar(20);
UPDATE `distribution` SET status_name = CASE status
    WHEN 0 THEN 'REVIEWING'
    WHEN 1 THEN 'REVIEW_SUCCESS'
    WHEN 2 THEN 'END'
    END;
ALTER TABLE `distribution` DROP COLUMN status;
ALTER TABLE `distribution` CHANGE status_name status varchar(20);
