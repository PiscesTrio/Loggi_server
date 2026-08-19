-- V4 — money becomes DECIMAL, and a quantity stops being text.
--
-- price was DOUBLE on both commodity and sale. IEEE 754 binary floating point cannot
-- represent 0.10 exactly, so sums drift; a total that is off by a cent is a bug nobody can
-- defend to a finance team. Nothing in this codebase adds prices up yet — which is exactly
-- why the type is cheap to correct now and expensive to correct after something does.
--
-- sale.count was VARCHAR. A quantity that cannot be summed, ordered or range-queried is not
-- a quantity, and the column accepted '3個', '' and '-' equally. No backend code reads it,
-- which is how it lasted this long.

ALTER TABLE `commodity` MODIFY price DECIMAL(12,2) NOT NULL;
ALTER TABLE `sale`      MODIFY price DECIMAL(12,2) NOT NULL;

-- Same two-step as V3: null anything that will not convert rather than let one bad row
-- abort the statement. '' and non-numeric text both become NULL.
UPDATE `sale` SET count = NULL WHERE count IS NOT NULL AND count NOT REGEXP '^-?[0-9]+$';
ALTER TABLE `sale` MODIFY count INT;
