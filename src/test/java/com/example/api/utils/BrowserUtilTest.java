package com.example.api.utils;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.api.model.enums.Browser;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * User-Agent parsing, which had no test while it was eighteen fall-through {@code if}s.
 *
 * <p>The agents below are real ones, because the whole difficulty of this problem is that they lie
 * about each other: every Chromium browser claims to be Chrome and Safari, Edge claims to be both
 * plus itself, and Internet Explorer 11 does not say MSIE at all. A test written from invented
 * strings would pass against an implementation that gets all of that wrong.
 */
class BrowserUtilTest {

    @Nested
    @DisplayName("Agents that impersonate each other")
    class Precedence {

        @Test
        @DisplayName("Edge is Edge, though its agent says Chrome and Safari too")
        void edge() {
            // This is the case that made the old implementation's ordering load-bearing: it
            // worked only because the Edge `if` sat below the Chrome one and overwrote it.
            assertThat(
                            BrowserUtil.from(
                                    "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36"
                                            + " (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36"
                                            + " Edg/120.0.0.0"))
                    .isEqualTo(Browser.EDGE);
        }

        @Test
        @DisplayName("Opera is Opera, though its agent says Chrome and Safari too")
        void opera() {
            assertThat(
                            BrowserUtil.from(
                                    "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36"
                                            + " (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36"
                                            + " OPR/106.0.0.0"))
                    .isEqualTo(Browser.OPERA);
        }

        @Test
        @DisplayName("Chrome is Chrome, though its agent says Safari")
        void chrome() {
            assertThat(
                            BrowserUtil.from(
                                    "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36"
                                            + " (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36"))
                    .isEqualTo(Browser.CHROME);
        }

        @Test
        @DisplayName("Safari is Safari, being the only one that says Safari and not Chrome")
        void safari() {
            assertThat(
                            BrowserUtil.from(
                                    "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7)"
                                            + " AppleWebKit/605.1.15 (KHTML, like Gecko)"
                                            + " Version/17.2.1 Safari/605.1.15"))
                    .isEqualTo(Browser.SAFARI);
        }

        @Test
        @DisplayName("Internet Explorer 11, which does not say MSIE")
        void internetExplorer11() {
            // IE11 dropped the MSIE token, which the old implementation was the only thing it
            // looked for. Every IE11 sign-in was therefore logged as 谷歌浏览器.
            assertThat(
                            BrowserUtil.from(
                                    "Mozilla/5.0 (Windows NT 10.0; Trident/7.0; rv:11.0) like"
                                            + " Gecko"))
                    .isEqualTo(Browser.IE);
        }

        @Test
        @DisplayName("A Chrome build number ending in 360 is not the 360 browser")
        void chromeBuildNumberIsNotQihoo() {
            // The old token was the bare string "360". Chrome versions are four dotted parts and
            // the last is frequently three digits, so this agent was logged as 360浏览器.
            assertThat(
                            BrowserUtil.from(
                                    "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36"
                                            + " (KHTML, like Gecko) Chrome/120.0.6099.360"
                                            + " Safari/537.36"))
                    .isEqualTo(Browser.CHROME);
        }

        @Test
        @DisplayName("The real 360 browser still is")
        void qihoo360() {
            assertThat(
                            BrowserUtil.from(
                                    "Mozilla/5.0 (Windows NT 10.0; WOW64) AppleWebKit/537.36"
                                            + " (KHTML, like Gecko) Chrome/86.0.4240.198"
                                            + " Safari/537.36 QIHU 360SE"))
                    .isEqualTo(Browser.QIHOO_360);
        }
    }

    @Nested
    @DisplayName("Agents that identify themselves plainly")
    class Straightforward {

        @Test
        void firefox() {
            assertThat(
                            BrowserUtil.from(
                                    "Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:121.0)"
                                            + " Gecko/20100101 Firefox/121.0"))
                    .isEqualTo(Browser.FIREFOX);
        }

        @Test
        void qq() {
            assertThat(
                            BrowserUtil.from(
                                    "Mozilla/5.0 (Windows NT 10.0; WOW64) AppleWebKit/537.36"
                                            + " (KHTML, like Gecko) Chrome/70.0.3538.25"
                                            + " Safari/537.36 QQBrowser/11.5"))
                    .isEqualTo(Browser.QQ);
        }
    }

    @Nested
    @DisplayName("No agent, or one nobody recognises")
    class Unknown {

        @ParameterizedTest
        @NullAndEmptySource
        @ValueSource(strings = {"   "})
        @DisplayName("An absent or blank agent is unknown, and does not throw")
        void absent(String agent) {
            // The old first statement was header.equals("") || header == null — the null check
            // came after the dereference, so a request without the header answered 500 instead
            // of recording the sign-in. curl and health probes send none.
            assertThat(BrowserUtil.from(agent)).isEqualTo(Browser.UNKNOWN);
        }

        @Test
        @DisplayName("An unrecognised agent is unknown, not Chrome")
        void unrecognised() {
            // The behaviour change that matters most here. The old code answered 谷歌浏览器 for
            // anything it could not place, so CHROME in this column meant either Chrome or "no
            // idea" and no reader could tell which.
            assertThat(BrowserUtil.from("PostmanRuntime/7.36.0")).isEqualTo(Browser.UNKNOWN);
            assertThat(BrowserUtil.from("curl/8.4.0")).isEqualTo(Browser.UNKNOWN);
        }
    }
}
