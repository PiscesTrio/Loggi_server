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

DELETE FROM distribution_status WHERE id LIKE 'seed-%';
DELETE FROM distribution       WHERE id LIKE 'seed-%';
DELETE FROM inventory_record   WHERE id LIKE 'seed-%';
DELETE FROM inventory          WHERE id LIKE 'seed-%';
DELETE FROM commodity          WHERE id LIKE 'seed-%';
DELETE FROM vehicle            WHERE id LIKE 'seed-%';
DELETE FROM driver             WHERE id LIKE 'seed-%';
DELETE FROM warehouse          WHERE id LIKE 'seed-%';
DELETE FROM admin              WHERE id LIKE 'seed-%';

-- Demo login. Passwords are stored in plaintext by the current implementation;
-- that defect is pinned by AdminServiceImplTest and fixed in a later slice. This
-- account exists so a fresh clone can be logged into -- it is not a credential.
INSERT INTO admin (id, email, password, roles, create_at) VALUES
  ('seed-admin-1', 'demo@loggi.example', 'demo1234', 'ROLE_SUPER_ADMIN', '2026-08-01 09:00:00');

-- Warehouses. Coordinates are WGS-84 at street-address precision, which is what
-- the map layer expects; they were rebuilt directly in WGS-84 rather than
-- converted from the previous Chinese coordinates.
INSERT INTO warehouse (id, name, principle, location, lat, lng, create_at) VALUES
  ('seed-wh-tokyo',  '東京丸の内倉庫',   '佐藤 健',   '東京都千代田区丸の内1-9-1',      35.681252, 139.767242, '2026-08-01 09:00:00'),
  ('seed-wh-osaka',  '大阪梅田倉庫',     '鈴木 一郎', '大阪府大阪市北区梅田3-1-3',      34.701881, 135.496338, '2026-08-01 09:00:00'),
  ('seed-wh-nagoya', '名古屋名駅倉庫',   '高橋 美咲', '愛知県名古屋市中村区名駅1-1-4',  35.170750, 136.883423, '2026-08-01 09:00:00');

INSERT INTO driver (id, name, gender, phone, address, id_card, license, score, driving, create_at, update_at) VALUES
  ('seed-dr-1', '田中 太郎', '男性', '090-1234-5678', '東京都江東区豊洲2-1-1',   'JP-A-100001', '第一種大型', '12', 0, '2026-08-01 09:00:00', '2026-08-01 09:00:00'),
  ('seed-dr-2', '山本 花子', '女性', '090-2345-6789', '大阪府大阪市西区新町1-2-3', 'JP-A-100002', '第一種中型', '10', 0, '2026-08-01 09:00:00', '2026-08-01 09:00:00'),
  ('seed-dr-3', '中村 大輔', '男性', '090-3456-7890', '愛知県名古屋市中区栄3-4-5', 'JP-A-100003', '第一種大型', '15', 0, '2026-08-01 09:00:00', '2026-08-01 09:00:00');

-- vehicle.type must remain one of the three values the client's dropdown offers.
INSERT INTO vehicle (id, number, type, driving, create_at) VALUES
  ('seed-vh-1', '品川800あ12-34', '货车', 0, '2026-08-01 09:00:00'),
  ('seed-vh-2', 'なにわ800い56-78', '卡车', 0, '2026-08-01 09:00:00'),
  ('seed-vh-3', '名古屋800う90-12', '重卡', 0, '2026-08-01 09:00:00');

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

-- distribution.wid holds the warehouse NAME (not the id). from_* is the origin
-- warehouse, to_* the destination, both WGS-84.
INSERT INTO distribution (id, did, vid, wid, driver, number, phone, address, urgent, care, time, status, from_lat, from_lng, to_lat, to_lng) VALUES
  ('seed-dis-1', 'seed-dr-1', 'seed-vh-1', '東京丸の内倉庫', '田中 太郎', '品川800あ12-34',  '092-111-2222', '福岡県福岡市博多区博多駅中央街1-1', 1, '易碎,防潮,',  '2026-08-05 11:30:00', 1, 35.681252, 139.767242, 33.589912, 130.420395),
  ('seed-dis-2', 'seed-dr-2', 'seed-vh-2', '大阪梅田倉庫',   '山本 花子', 'なにわ800い56-78', '011-333-4444', '北海道札幌市中央区北5条西2丁目',    0, '冷藏,防高温,', '2026-08-06 09:15:00', 0, 34.701881, 135.496338, 43.067902, 141.352829);

-- distribution_status.location is the warehouse NAME as well; it is rendered
-- verbatim in the tracking timeline, so an id here would show up as an id.
INSERT INTO distribution_status (id, dis_id, lat, lng, location, time, status) VALUES
  ('seed-ds-1', 'seed-dis-1', 35.681252, 139.767242, '東京丸の内倉庫', '2026-08-05 11:30:00', 0),
  ('seed-ds-2', 'seed-dis-1', 34.701881, 135.496338, '大阪梅田倉庫',   '2026-08-05 19:40:00', 1),
  ('seed-ds-3', 'seed-dis-2', 34.701881, 135.496338, '大阪梅田倉庫',   '2026-08-06 09:15:00', 0);
