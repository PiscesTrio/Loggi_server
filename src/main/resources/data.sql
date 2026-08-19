-- Demo seed data (Japanese, WGS-84 coordinates).
--
-- Why this file exists at all: the project had NO data initialisation path
-- whatsoever. `ddl-auto: update` creates the tables and stops there, docker-compose
-- mounts no init scripts, and the only ApplicationRunner is empty -- so a fresh
-- clone came up against a completely empty database. Every row that ever existed
-- was typed in by hand.
--
-- Runs via `spring.sql.init.mode: always`, which requires
-- `spring.jpa.defer-datasource-initialization: true` so that Hibernate has created
-- the tables before this executes.
--
-- IDEMPOTENT AND NON-DESTRUCTIVE: every seeded row carries a `seed-` id prefix and
-- is deleted by that prefix before being re-inserted. Re-running it does not
-- duplicate rows, and it never touches rows created through the app.
--
-- Two field-naming traps this file has to respect, both inherited:
--   * distribution.wid holds the warehouse *name* (see the warehouse dropdown),
--     while inventory.wid / inventory_record.wid hold the warehouse *id*.
--     Same column name, different meaning in different tables.
--   * distribution.care is a comma-joined list *with a trailing comma*, because
--     that is what the client produces. Writing it without one would make seeded
--     rows differ in shape from real ones.
--
-- Values that must stay exactly as they are, because the client compares against
-- them: vehicle.type is one of 货车 / 卡车 / 重卡, driver.gender is 男性 / 女性,
-- and each care token must be one of the eight the client offers. These are wire
-- values, not display text -- localising them is a separate, deliberate step.

DELETE FROM distribution_track WHERE id LIKE 'seed-%';
DELETE FROM distribution       WHERE id LIKE 'seed-%';
DELETE FROM inventory_record   WHERE id LIKE 'seed-%';
DELETE FROM inventory          WHERE id LIKE 'seed-%';
DELETE FROM commodity          WHERE id LIKE 'seed-%';
DELETE FROM vehicle            WHERE id LIKE 'seed-%';
DELETE FROM driver             WHERE id LIKE 'seed-%';
DELETE FROM warehouse          WHERE id LIKE 'seed-%';
DELETE FROM admin              WHERE id LIKE 'seed-%';

-- Demo login. Exists so a fresh clone can be logged into; it is not a credential.
INSERT INTO admin (id, email, password, roles, create_at) VALUES
  -- The password is demo1234, stored as the delegating encoder writes it. It cannot
  -- be seeded in plain text any more: login verifies a hash, and a plaintext column
  -- value simply fails to match. Regenerate with PasswordEncoderFactories
  -- .createDelegatingPasswordEncoder().encode(...) if the demo password changes.
  ('seed-admin-1', 'demo@loggi.example',
   '{bcrypt}$2a$10$Qu7Ns1ky0lClGLYVviA1Fuuz2jEf4PiE/Nv7a9Kh9Sq8F30uStOxC',
   'ROLE_SUPER_ADMIN', '2026-08-01 09:00:00');

-- Warehouses. Coordinates are WGS-84, which is what the map layer expects.
--
-- Addresses are fictional. The prefecture and ward are real so the map still shows a
-- believable route, but the town name is `ロギ` — this application's own name, which no
-- Japanese address uses — and the coordinates sit in the ward generally rather than on a
-- building. The previous seed pointed at Tokyo Station, Umeda and Nagoya Station at
-- street-address precision, which is a real place attached to a made-up business.
--
-- Phone numbers throughout use the 090-0xxx-xxxx form. Japan has no reserved range for
-- fiction the way the US reserves 555-0100..0199, but mobile numbers are allocated as
-- 090CDEFGHJK with C != 0, so nothing starting 0900 can ever be issued.
INSERT INTO warehouse (id, name, principle, location, lat, lng, create_at) VALUES
  ('seed-wh-tokyo',  '東京江東倉庫',   '山田 太郎', '東京都江東区ロギ1-1-1',            35.672000, 139.817000, '2026-08-01 09:00:00'),
  ('seed-wh-osaka',  '大阪此花倉庫',   '山田 花子', '大阪府大阪市此花区ロギ2-1-1',      34.687000, 135.448000, '2026-08-01 09:00:00'),
  ('seed-wh-nagoya', '名古屋港倉庫',   '山田 次郎', '愛知県名古屋市港区ロギ3-1-1',      35.108000, 136.859000, '2026-08-01 09:00:00');

INSERT INTO driver (id, name, gender, phone, address, id_card, license, score, driving, create_at, update_at) VALUES
  ('seed-dr-1', '田中 三郎', '男性', '090-0000-0001', '東京都江東区ロギ4-2-1',       'JP-A-100001', '第一種大型', '12', 0, '2026-08-01 09:00:00', '2026-08-01 09:00:00'),
  ('seed-dr-2', '佐々木 花子', '女性', '090-0000-0002', '大阪府大阪市此花区ロギ5-3-2', 'JP-A-100002', '第一種中型', '10', 0, '2026-08-01 09:00:00', '2026-08-01 09:00:00'),
  ('seed-dr-3', '小林 五郎', '男性', '090-0000-0003', '愛知県名古屋市港区ロギ6-4-3', 'JP-A-100003', '第一種大型', '15', 0, '2026-08-01 09:00:00', '2026-08-01 09:00:00');

-- vehicle.type must remain one of the three values the client's dropdown offers.
--
-- The hiragana is `へ` on every plate on purpose: Japanese plates never use お, し, へ or
-- ん, so these read as plates while being ones that cannot have been issued. Same idea as
-- the 0900 phone numbers above.
INSERT INTO vehicle (id, number, type, driving, create_at) VALUES
  ('seed-vh-1', '品川800へ12-34', '货车', 0, '2026-08-01 09:00:00'),
  ('seed-vh-2', 'なにわ800へ56-78', '卡车', 0, '2026-08-01 09:00:00'),
  ('seed-vh-3', '名古屋800へ90-12', '重卡', 0, '2026-08-01 09:00:00');

INSERT INTO commodity (id, name, price, description, count, create_at, update_at) VALUES
  ('seed-cm-1', '精密機器',   125000.00, '振動厳禁。緩衝材必須。',       120, '2026-08-01 09:00:00', '2026-08-01 09:00:00'),
  ('seed-cm-2', '冷蔵食品',     3200.00, '要冷蔵。5℃以下を維持。',       480, '2026-08-01 09:00:00', '2026-08-01 09:00:00'),
  ('seed-cm-3', '医薬品',      58000.00, '温度記録が必要。',              90, '2026-08-01 09:00:00', '2026-08-01 09:00:00'),
  ('seed-cm-4', '家電製品',     42000.00, '積み重ね不可。',              210, '2026-08-01 09:00:00', '2026-08-01 09:00:00');

-- inventory.wid / .cid are ids; inventory.name is a denormalised copy of the
-- commodity name, and it is that copy the UI renders.
INSERT INTO inventory (id, wid, cid, name, location, count) VALUES
  ('seed-inv-1', 'seed-wh-tokyo',  'seed-cm-1', '精密機器', 'A-01', 60),
  ('seed-inv-2', 'seed-wh-tokyo',  'seed-cm-2', '冷蔵食品', 'B-03', 200),
  ('seed-inv-3', 'seed-wh-osaka',  'seed-cm-3', '医薬品',   'C-02', 45),
  ('seed-inv-4', 'seed-wh-osaka',  'seed-cm-4', '家電製品', 'A-04', 110),
  ('seed-inv-5', 'seed-wh-nagoya', 'seed-cm-1', '精密機器', 'D-01', 60),
  ('seed-inv-6', 'seed-wh-nagoya', 'seed-cm-2', '冷蔵食品', 'D-02', 280);

-- type 1 = inbound, -1 = outbound. The pie chart groups by inventory_record.name,
-- NOT by commodity.name -- so these rows are what the chart legend shows.
INSERT INTO inventory_record (id, name, wid, cid, count, type, description, create_at) VALUES
  ('seed-ir-1', '精密機器', 'seed-wh-tokyo',  'seed-cm-1', 80,  1, '初期入庫',       '2026-08-02 10:00:00'),
  ('seed-ir-2', '精密機器', 'seed-wh-tokyo',  'seed-cm-1', 20, -1, '出庫（東京→福岡）', '2026-08-05 11:30:00'),
  ('seed-ir-3', '冷蔵食品', 'seed-wh-tokyo',  'seed-cm-2', 200, 1, '初期入庫',       '2026-08-02 10:10:00'),
  ('seed-ir-4', '医薬品',   'seed-wh-osaka',  'seed-cm-3', 60,  1, '初期入庫',       '2026-08-02 10:20:00'),
  ('seed-ir-5', '医薬品',   'seed-wh-osaka',  'seed-cm-3', 15, -1, '出庫（大阪→札幌）', '2026-08-06 09:15:00'),
  ('seed-ir-6', '家電製品', 'seed-wh-osaka',  'seed-cm-4', 110, 1, '初期入庫',       '2026-08-02 10:30:00'),
  ('seed-ir-7', '精密機器', 'seed-wh-nagoya', 'seed-cm-1', 60,  1, '初期入庫',       '2026-08-02 10:40:00'),
  ('seed-ir-8', '冷蔵食品', 'seed-wh-nagoya', 'seed-cm-2', 280, 1, '初期入庫',       '2026-08-02 10:50:00');

-- Since S09 the order points at its driver, vehicle and origin warehouse by id, with real
-- foreign keys behind all three; the driver's name and the plate are no longer copied here.
-- warehouse_id used to be `wid` and held the warehouse NAME, which is why the migration
-- resolves it by name rather than renaming the column.
-- from_* is the origin warehouse, to_* the destination, both WGS-84.
INSERT INTO distribution (id, driver_id, vehicle_id, warehouse_id, phone, address, urgent, care, time, status, from_lat, from_lng, to_lat, to_lng) VALUES
  ('seed-dis-1', 'seed-dr-1', 'seed-vh-1', 'seed-wh-tokyo', '090-0000-0011', '福岡県福岡市東区ロギ7-1-1',   1, '易碎,防潮,',  '2026-08-05 11:30:00', 'REVIEW_SUCCESS', 35.672000, 139.817000, 33.620000, 130.427000),
  ('seed-dis-2', 'seed-dr-2', 'seed-vh-2', 'seed-wh-osaka', '090-0000-0012', '北海道札幌市白石区ロギ8-1-1', 0, '冷藏,防高温,', '2026-08-06 09:15:00', 'REVIEWING',      34.687000, 135.448000, 43.048000, 141.402000);

-- distribution_track.location is the warehouse NAME as well; it is rendered
-- verbatim in the tracking timeline, so an id here would show up as an id.
INSERT INTO distribution_track (id, dis_id, lat, lng, location, time, status) VALUES
  ('seed-ds-1', 'seed-dis-1', 35.672000, 139.817000, '東京江東倉庫', '2026-08-05 11:30:00', 0),
  ('seed-ds-2', 'seed-dis-1', 34.687000, 135.448000, '大阪此花倉庫', '2026-08-05 19:40:00', 1),
  ('seed-ds-3', 'seed-dis-2', 34.687000, 135.448000, '大阪此花倉庫', '2026-08-06 09:15:00', 0);
