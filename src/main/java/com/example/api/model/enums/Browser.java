package com.example.api.model.enums;

/**
 * The browser a sign-in came from, as written into {@code login_log.browser}.
 *
 * <p>Was Chinese display text produced by {@link com.example.api.utils.BrowserUtil} — 谷歌浏览器,
 * safari浏览器, and so on — which put a UI language in an audit column and made the value
 * untranslatable without rewriting history.
 *
 * <p>{@link #UNKNOWN} is new and is the point of the rewrite as much as the names are. The old code
 * answered 谷歌浏览器 for anything it could not identify, so "Chrome" in this column meant either Chrome
 * or "we have no idea" and nothing could tell them apart. An audit record that invents a fact is
 * worse than one that admits it does not have it.
 */
public enum Browser {
    CHROME,
    SAFARI,
    FIREFOX,
    EDGE,
    IE,
    OPERA,
    QQ,
    UC,
    SOGOU,
    BAIDU,
    QIHOO_360,
    LIEBAO,
    MAXTHON,
    THE_WORLD,
    QUARK,
    KONQUEROR,
    CAMINO,
    AVAST,
    UNKNOWN
}
