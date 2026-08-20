package com.example.api.utils;

import jakarta.servlet.http.HttpServletRequest;

public class BrowserUtil {
    /**
     * Get the browser version.
     *
     * @param request
     * @return
     */
    public static String getBrower(HttpServletRequest request) {
        String browserVersion = null;
        String header = request.getHeader("User-Agent");
        if (header.equals("") || header == null) // empty header falls back to Chrome
        {
            browserVersion = "谷歌浏览器";
            return browserVersion;
        }

        if (header.indexOf("Chrome") > 0) // Chrome
        {
            browserVersion = "Chrome";
        } else if (header.indexOf("Safari") > 0) // Safari
        {
            browserVersion = "safari浏览器";
        }
        if (header.indexOf("MSIE") > 0) // IE
        {
            browserVersion = "IE浏览器";
        }
        if (header.indexOf("Firefox") > 0) // Firefox
        {
            browserVersion = "火狐浏览器";
        }
        if (header.indexOf("Camino") > 0) //
        {
            browserVersion = "camino浏览器";
        }
        if (header.indexOf("Konqueror") > 0) //
        {
            browserVersion = "konqueror浏览器";
        }
        if (header.indexOf("Quark") > 0) // Quark
        {
            browserVersion = "quark浏览器";
        }
        if (header.indexOf("baidu") > 0) // Baidu
        {
            browserVersion = "百度浏览器";
        }
        if (header.indexOf("Edge") > 0) // Edge
        {
            browserVersion = "edge";
        }
        if (header.indexOf("TheWorld") > 0) // TheWorld
        {
            browserVersion = "theworld浏览器";
        }
        if (header.indexOf("QQBrowser") > 0
                || header.indexOf("TencentTraveler") > 0
                || header.indexOf("QQTheme") > 0) // QQ
        {
            browserVersion = "qq浏览器";
        }
        if (header.indexOf("Avast") > 0) // Avast Secure Browser
        {
            browserVersion = "avast浏览器";
        }
        if (header.indexOf("OPR") > 0) // Opera
        {
            browserVersion = "opera浏览器";
        }
        if (header.indexOf("360") > 0) // 360
        {
            browserVersion = "360浏览器";
        }
        if (header.indexOf("LBBROWSER") > 0) // Liebao
        {
            browserVersion = "猎豹浏览器";
        }
        if (header.indexOf("Maxthon") > 0) // Maxthon
        {
            browserVersion = "遨游浏览器";
        }
        if (header.indexOf("MetaSr") > 0 || header.indexOf("Sogou") > 0) // Sogou
        {
            browserVersion = "搜狗浏览器";
        }
        if (header.indexOf("UCWEB") > 0 || header.indexOf("UCBrowser") > 0) // UC
        {
            browserVersion = "uc浏览器";
        }
        if (browserVersion == null) // anything unrecognized defaults to Chrome
        {
            browserVersion = "谷歌浏览器";
        }
        return browserVersion;
    }
}
