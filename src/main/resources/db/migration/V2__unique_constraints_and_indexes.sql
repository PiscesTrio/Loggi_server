-- V2 — the constraints and indexes `ddl-auto` could never create.
--
-- `update` only ever adds tables and columns. Business uniqueness and query indexes were
-- therefore absent from every environment, and the application compensated for the first
-- by checking in Java — `if (repo.findByX(...) == null) save(...)` — which is a race, not a
-- guarantee: two concurrent requests both read "absent" and both insert. A unique index is
-- the only place that check cannot be lost.
--
-- Every entry below is justified by a query that exists in the repositories today. The
-- omissions are deliberate and listed at the bottom, because an index that is never used
-- still costs an write on every insert.

-- ---------------------------------------------------------------------------------------
-- Business uniqueness
-- ---------------------------------------------------------------------------------------

-- AdminRepository.findAdminByEmail treats the address as an identity. Note that `email`
-- carries a NOT NULL DEFAULT 'LTD' from the entity's hand-written columnDefinition, so a
-- second administrator saved without an address now collides instead of silently becoming
-- a second row named 'LTD'. That is the constraint doing its job on a bad default.
ALTER TABLE `admin` ADD CONSTRAINT uk_admin_email UNIQUE (`email`);

-- CommodityRepository.findByName returns a single Commodity, i.e. the code already assumes
-- names are unique. Until now nothing enforced it and a duplicate would have made that
-- method's result arbitrary.
ALTER TABLE `commodity` ADD CONSTRAINT uk_commodity_name UNIQUE (`name`);

-- A plate identifies one vehicle in the world. There is no finder on it yet; the constraint
-- is here for the invariant, not for a query plan.
ALTER TABLE `vehicle` ADD CONSTRAINT uk_vehicle_number UNIQUE (`number`);

-- InventoryRepository.findByWidAndCid returns a single Inventory: one commodity has one
-- stock row per warehouse. This is the composite that makes that true.
ALTER TABLE `inventory` ADD CONSTRAINT uk_inventory_wid_cid UNIQUE (`wid`, `cid`);

-- ---------------------------------------------------------------------------------------
-- Indexes for queries that exist
-- ---------------------------------------------------------------------------------------

-- findAllByCid. Its sibling findAllByWid needs no index of its own: `wid` is the leftmost
-- column of uk_inventory_wid_cid, and a composite index serves any prefix of itself.
CREATE INDEX idx_inventory_cid ON `inventory` (`cid`);

-- InventoryRecordRepository.findAllByWid / findAllByCid. This table grows without bound —
-- one row per stock movement — so it is the one that most needs them.
CREATE INDEX idx_inventory_record_wid ON `inventory_record` (`wid`);
CREATE INDEX idx_inventory_record_cid ON `inventory_record` (`cid`);

-- DistributionTrackRepository.findAllByDisId, which the app calls to draw an order's
-- timeline. Also unbounded: a row per position report.
CREATE INDEX idx_distribution_track_dis_id ON `distribution_track` (`dis_id`);

-- ---------------------------------------------------------------------------------------
-- Deliberately absent
-- ---------------------------------------------------------------------------------------
--
--   driver.driving, vehicle.driving   findAllByDriving(boolean) — two distinct values over
--                                     a handful of rows. An optimiser that can reach half a
--                                     table through an index will read the table instead.
--   inventory_record.type             findAllByType — same shape (in/out).
--   admin.roles                       existsAdminByRoles, over a table with a few rows.
--   commodity.name (as a LIKE target) findByNameLike is called as "%" + name + "%"
--                                     (CommodityServiceImpl). A leading wildcard cannot use
--                                     a B-tree index; only a full-text or trigram index
--                                     would, and the data does not justify one.
--   sale.company                      findAllByCompanyLike, same reason.
--   system_log.account, .module       SystemLogServiceImpl builds a Specification with
--                                     like %x%. Same reason again.
--   distribution.*                    DistributionRepository declares no derived query at
--                                     all; the plan for this slice named a findByDidAndStatus
--                                     that the repository does not have.
