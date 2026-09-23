-- V11 -- three browser constants this project no longer claims to recognise.
--
-- LIEBAO, MAXTHON and THE_WORLD were in the enum because the original version detected them.
-- They are the narrowest entries in a list that is already specific to one market, and the
-- tokens matching them (LBBROWSER, Maxthon, TheWorld) are the ones least likely to ever fire
-- against this application. Dropping them is a decision about scope, not a claim about the
-- browsers.
--
-- V10 still maps 猎豹浏览器 / 遨游浏览器 / theworld浏览器 onto these three names, and is not
-- edited: an applied script never is. A database migrating the whole way through therefore
-- writes those values at V10, and this script folds them into UNKNOWN immediately afterwards.
--
-- UNKNOWN rather than deleting the rows: the sign-in happened. The browser is the part that
-- can no longer be named, and an audit row that admits what it does not know is worth more
-- than one removed for being inconvenient.

UPDATE `login_log`
   SET browser = 'UNKNOWN'
 WHERE browser IN ('LIEBAO', 'MAXTHON', 'THE_WORLD');
