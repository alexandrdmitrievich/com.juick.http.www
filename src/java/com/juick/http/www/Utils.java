/*
 * Juick
 * Copyright (C) 2008-2011, Ugnich Anton
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.juick.http.www;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 *
 * @author Ugnich Anton
 */
public class Utils {

    public static String getCookie(HttpServletRequest request, String name) {
        Cookie cookies[] = request.getCookies();
        if (cookies != null) {
            for (int i = 0; i < cookies.length; i++) {
                if (cookies[i].getName().equals(name)) {
                    return cookies[i].getValue();
                }
            }
        }
        return null;
    }

    public static com.juick.User getVisitorUser(Connection sql, HttpServletRequest request) {
        String hash = getCookie(request, "hash");
        if (hash != null) {
            return com.juick.server.UserQueries.getUserByHash(sql, hash);
        } else {
            return null;
        }
    }

    public static int getVisitorUID(Connection sql, HttpServletRequest request) {
        Cookie cookies[] = request.getCookies();
        if (cookies != null) {
            for (int i = 0; i < cookies.length; i++) {
                if (cookies[i].getName().equals("hash")) {
                    String hash = cookies[i].getValue();
                    return com.juick.server.UserQueries.getUIDbyHash(sql, hash);
                }
            }
        }
        return 0;
    }

    public static void sendPermanentRedirect(HttpServletResponse response, String location) {
        response.setStatus(HttpServletResponse.SC_MOVED_PERMANENTLY);
        response.setHeader("Location", location);
    }

    public static void finishSQL(ResultSet rs, Statement stmt) {
        if (rs != null) {
            try {
                rs.close();
            } catch (SQLException e) {
            }
        }
        if (stmt != null) {
            try {
                stmt.close();
            } catch (SQLException e) {
            }
        }
    }

    public static String convertArray2String(ArrayList<Integer> mids) {
        String q = "";
        for (int i = 0; i < mids.size(); i++) {
            if (i > 0) {
                q += ",";
            }
            q += mids.get(i);
        }
        return q;
    }

    public static String encodeHTML(String str) {
        String ret = str;
        ret = ret.replaceAll("<", "&lt;");
        ret = ret.replaceAll(">", "&gt;");
        return str;
    }

    public static String encodeSphinx(String str) {
        String ret = str;
        ret = ret.replaceAll("@", "\\\\@");
        return ret;
    }
}
