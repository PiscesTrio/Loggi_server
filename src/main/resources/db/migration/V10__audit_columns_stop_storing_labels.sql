-- V10 — the two audit columns that still held Chinese become enum names.
--
-- system_log.module held 商品管理 / 员工管理 / ..., because @Log took a free String and every
-- one of the twenty-five call sites typed one in. login_log.browser held 谷歌浏览器 /
-- safari浏览器 / ..., because BrowserUtil returned display text.
--
-- V7 already did exactly this to system_log.business_type, and said why: storing display text
-- bakes a UI language into the data, so translating the interface strands every historical
-- row. These two were the same mistake in the same table family and were simply missed.
--
-- Unlike V9's gender and vehicle type, nothing here is NULLed for being unrecognised. These
-- are audit rows: an operation that happened and whose module cannot be identified is still an
-- operation that happened, and the honest record of it is that the module is unknown, not that
-- the row should be silently emptied. system_log has no UNKNOWN module because @Log cannot
-- produce one; browser does, and that is where an unmatched value goes.

-- --------------------------------------------------------------------------------------
-- system_log.module -> LogModule
-- --------------------------------------------------------------------------------------
-- 运输状态 -- "transport status" -- was the odd one out: every other module was named 管理
-- ("management") and this one named the thing it tracked. It is the tracking endpoint.
UPDATE `system_log` SET module = CASE module
    WHEN '商品管理'   THEN 'COMMODITY'
    WHEN '仓库管理'   THEN 'WAREHOUSE'
    WHEN '员工管理'   THEN 'EMPLOYEE'
    WHEN '驾驶员管理' THEN 'DRIVER'
    WHEN '车辆管理'   THEN 'VEHICLE'
    WHEN '配送管理'   THEN 'DISTRIBUTION'
    WHEN '运输状态'   THEN 'DISTRIBUTION_TRACK'
    ELSE module
    END;

-- Anything the CASE above did not recognise is not a LogModule name, so it cannot be read
-- back as one -- Hibernate would throw on the row rather than on the column. Emptied, because
-- an unreadable value is worse than a missing one: the row still records who did what and
-- when. There should be none of these; the statement exists so that "none" is enforced rather
-- than assumed.
UPDATE `system_log` SET module = NULL
 WHERE module IS NOT NULL
   AND module NOT IN ('COMMODITY','WAREHOUSE','EMPLOYEE','DRIVER','VEHICLE',
                      'DISTRIBUTION','DISTRIBUTION_TRACK');

ALTER TABLE `system_log` MODIFY module varchar(30) DEFAULT NULL;

-- --------------------------------------------------------------------------------------
-- login_log.browser -> Browser
-- --------------------------------------------------------------------------------------
-- 'Chrome' appears twice on purpose: the old code wrote the bare English word when it matched
-- the Chrome token, and 谷歌浏览器 when it gave up. Both become CHROME here -- the information
-- that would separate them was never recorded, and inventing the distinction now would be
-- worse than losing it. New rows can tell them apart: unrecognised is UNKNOWN from now on.
UPDATE `login_log` SET browser = CASE browser
    WHEN '谷歌浏览器'      THEN 'CHROME'
    WHEN 'Chrome'          THEN 'CHROME'
    WHEN 'safari浏览器'    THEN 'SAFARI'
    WHEN 'IE浏览器'        THEN 'IE'
    WHEN '火狐浏览器'      THEN 'FIREFOX'
    WHEN 'edge'            THEN 'EDGE'
    WHEN 'opera浏览器'     THEN 'OPERA'
    WHEN 'qq浏览器'        THEN 'QQ'
    WHEN 'uc浏览器'        THEN 'UC'
    WHEN '搜狗浏览器'      THEN 'SOGOU'
    WHEN '百度浏览器'      THEN 'BAIDU'
    WHEN '360浏览器'       THEN 'QIHOO_360'
    WHEN '猎豹浏览器'      THEN 'LIEBAO'
    WHEN '遨游浏览器'      THEN 'MAXTHON'
    WHEN 'theworld浏览器'  THEN 'THE_WORLD'
    WHEN 'quark浏览器'     THEN 'QUARK'
    WHEN 'konqueror浏览器' THEN 'KONQUEROR'
    WHEN 'camino浏览器'    THEN 'CAMINO'
    WHEN 'avast浏览器'     THEN 'AVAST'
    ELSE 'UNKNOWN'
    END
 WHERE browser IS NOT NULL;

ALTER TABLE `login_log` MODIFY browser varchar(20) DEFAULT NULL;
