package com.example.api.utils;

import com.example.api.model.enums.Browser;
import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Map;

/**
 * Identifies the browser behind a request from its User-Agent.
 *
 * <p>Was eighteen consecutive {@code if} statements, most of them not {@code else if}, each
 * overwriting the last. That is not a stylistic complaint: it made the answer depend on the order
 * the statements happened to be written in, and the order was wrong in a way nobody could see.
 * Every Chromium browser sends a User-Agent containing both {@code Chrome} and {@code Safari}, so
 * the only reason Edge was not reported as Chrome is that the Edge check happened to sit further
 * down the file. Adding a check in the wrong place would have silently changed existing answers.
 *
 * <p>A table makes the precedence the thing you read: the list is ordered most specific first, and
 * the first match wins. Chromium derivatives come before Edge, Edge before Chrome, Chrome before
 * Safari — that ordering is now a property of the data rather than of the control flow.
 *
 * <p>Two behaviour changes, both deliberate:
 *
 * <ul>
 *   <li>A missing User-Agent no longer throws. The old first line was {@code header.equals("") ||
 *       header == null}, which dereferences before the null check, so a request without the header
 *       answered 500 rather than logging a sign-in. Anything not sending one — curl, a health
 *       probe, a script — hit it.
 *   <li>An unrecognised agent is {@link Browser#UNKNOWN}, not Chrome. See {@link Browser}.
 * </ul>
 */
public final class BrowserUtil {

    private BrowserUtil() {}

    /**
     * Ordered most specific first; the first entry whose token appears in the User-Agent wins.
     *
     * <p>Kept as a list of pairs rather than a map because a map has no order, and the order is the
     * part that carries the meaning here.
     */
    private static final List<Map.Entry<Browser, List<String>>> RULES =
            List.of(
                    // Chromium derivatives: their agents also contain Chrome and Safari.
                    Map.entry(Browser.QQ, List.of("QQBrowser", "TencentTraveler", "QQTheme")),
                    Map.entry(Browser.UC, List.of("UCWEB", "UCBrowser")),
                    Map.entry(Browser.SOGOU, List.of("MetaSr", "Sogou")),
                    Map.entry(Browser.BAIDU, List.of("baidubrowser", "BIDUBrowser")),
                    // Not the bare string "360", which is what this used to look for. Chrome
                    // build numbers are four dotted components and the last one is often three
                    // digits, so Chrome/120.0.6099.360 would have been logged as a 360 browser.
                    // The real agents carry QIHU 360SE or QIHU 360EE.
                    Map.entry(Browser.QIHOO_360, List.of("360SE", "360EE")),
                    Map.entry(Browser.LIEBAO, List.of("LBBROWSER")),
                    Map.entry(Browser.MAXTHON, List.of("Maxthon")),
                    Map.entry(Browser.THE_WORLD, List.of("TheWorld")),
                    Map.entry(Browser.AVAST, List.of("Avast")),
                    Map.entry(Browser.QUARK, List.of("Quark")),
                    Map.entry(Browser.OPERA, List.of("OPR")),
                    Map.entry(Browser.EDGE, List.of("Edge", "Edg/")),
                    // Not Chromium, but their agents can mention Firefox-alikes.
                    Map.entry(Browser.CAMINO, List.of("Camino")),
                    Map.entry(Browser.KONQUEROR, List.of("Konqueror")),
                    Map.entry(Browser.FIREFOX, List.of("Firefox")),
                    Map.entry(Browser.IE, List.of("MSIE", "Trident")),
                    // The two everything above also matches, last.
                    Map.entry(Browser.CHROME, List.of("Chrome")),
                    Map.entry(Browser.SAFARI, List.of("Safari")));

    /** The browser behind this request, or {@link Browser#UNKNOWN}. Never null, never throws. */
    public static Browser getBrowser(HttpServletRequest request) {
        return from(request == null ? null : request.getHeader("User-Agent"));
    }

    /** Split out from the request so it can be tested without one. */
    public static Browser from(String userAgent) {
        if (userAgent == null || userAgent.isBlank()) {
            return Browser.UNKNOWN;
        }
        for (Map.Entry<Browser, List<String>> rule : RULES) {
            for (String token : rule.getValue()) {
                // contains, not indexOf(..) > 0: the old test could not see a token at
                // position 0, so an agent starting with the name it was looking for did
                // not match it.
                if (userAgent.contains(token)) {
                    return rule.getKey();
                }
            }
        }
        return Browser.UNKNOWN;
    }
}
