-- Fills in the denormalised commodity name where a caller left it out.
--
-- `inventory_record.name` and `inventory.name` are copies of `commodity.name`, rendered by
-- the stock table and by the chart legend — the chart groups by the record's name, not by
-- the commodity's. Until now the client supplied that copy, so a movement submitted without
-- touching the commodity dropdown wrote a row with no name at all, and the pie chart drew a
-- slice whose label was blank.
--
-- The service derives it from the commodity now. This repairs the rows written before it did.
-- Both tables have had a real foreign key to `commodity` since V6, so there is exactly one
-- right answer for every row.

UPDATE inventory_record r
    JOIN commodity c ON c.id = r.commodity_id
SET r.name = c.name
WHERE r.name IS NULL OR r.name = '';

UPDATE inventory i
    JOIN commodity c ON c.id = i.commodity_id
SET i.name = c.name
WHERE i.name IS NULL OR i.name = '';
