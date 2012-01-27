package ru.sape;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import javax.servlet.http.Cookie;

public class SapePageLinks {

    private boolean showCode;

    public SapePageLinks(SapeConnection sapeConnection, String sapeUser, String requestUri, Cookie[] cookies) {
        this(sapeConnection, sapeUser, requestUri, cookies, false);
    }

    @SuppressWarnings("unchecked")
    public SapePageLinks(SapeConnection sapeConnection, String sapeUser, String requestUri, Cookie[] cookies, boolean showCode) {
        if (sapeUser.equals(getCookieValue(cookies, "sape_cookie"))) {
            showCode = true;
        }

        Map<String, Object> data = sapeConnection.getData();

        if (data.containsKey("__sape_delimiter__")) {
            linkDelimiter = (String) data.get("__sape_delimiter__");
        }

        if (data.containsKey(requestUri)) {
            pageLinks = new ArrayList<String>(((Map<Object, String>) data.get(requestUri)).values());
        }

        if (data.containsKey("__sape_new_url__")) {
            if (showCode) {
                Object newUrl = data.get("__sape_new_url__");

                if (newUrl instanceof Map) {
                    pageLinks = new ArrayList<String>(((Map<Object, String>) newUrl).values());
                } else {
                    pageLinks = new ArrayList<String>(Arrays.asList((String) newUrl));
                }
            }
        }

        this.showCode = showCode;
    }
    private String linkDelimiter = ".";
    private List<String> pageLinks = new ArrayList<String>();

    public String render() {
        return render(-1);
    }

    public String render(int count) {
        StringBuilder s = new StringBuilder();

        if (count < 0) {
            count = pageLinks.size();
        }

        for (Iterator<String> i = pageLinks.iterator(); i.hasNext() && count > 0; count--) {
            if (s.length() > 0) {
                s.append(linkDelimiter);
            }

            String l = i.next();

            s.append(l);

            i.remove();
        }

        if (showCode) {
            s.insert(0, "<sape_noindex>");
            s.append("</sape_noindex>");
        }

        return s.toString();
    }

    private static String getCookieValue(Cookie[] cookies, String name) {
        if (cookies == null) {
            return null;
        }

        for (Cookie cookie : cookies) {
            if (cookie.getName().equals(name)) {
                return cookie.getValue();
            }
        }

        return null;
    }
}
