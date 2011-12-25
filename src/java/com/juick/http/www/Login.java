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

import java.io.IOException;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Locale;
import java.util.ResourceBundle;
import javax.servlet.ServletException;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 *
 * @author Ugnich Anton
 */
public class Login {

    protected void doGetLoginForm(Connection sql, HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        com.juick.User visitor = Utils.getVisitorUser(sql, request);
        Locale locale = request.getLocale();
        ResourceBundle rb = ResourceBundle.getBundle("Login", locale);

        response.setContentType("text/html; charset=UTF-8");
        PrintWriter out = response.getWriter();
        try {
            PageTemplates.pageHead(out, rb.getString("Login"), "");
            PageTemplates.pageNavigation(out, locale, visitor);
            PageTemplates.pageTitle(out, rb.getString("Login"));

            out.println("<div id=\"wrapper\">");
            out.println("<div id=\"content\">");
            out.println("<form action=\"/login\" method=\"post\">");
            out.println("<p>" + rb.getString("Username") + ": <input type=\"text\" name=\"username\"/></p>");
            out.println("<p>" + rb.getString("Password") + ": <input type=\"password\" name=\"password\"/></p>");
            out.println("<p><input type=\"submit\" value=\"    OK    \"/></p>");
            out.println("</form>");
            out.println("</div>");
            out.println("</div>");

            PageTemplates.pageFooter(out, locale);
        } finally {
            out.close();
        }
    }

    protected void doGetLogin(Connection sql, HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        String hash = request.getQueryString();
        if (hash.length() > 32) {
            response.sendError(400);
            return;
        }

        if (com.juick.server.UserQueries.getUIDbyHash(sql, hash) > 0) {
            Cookie c = new Cookie("hash", hash);
            c.setDomain(".juick.com");
            c.setMaxAge(0);
            response.addCookie(c);

            response.sendRedirect("/");
        } else {
            response.sendError(403);
        }
    }

    protected void doPostLogin(Connection sql, HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        String username = request.getParameter("username");
        String password = request.getParameter("password");
        if (username == null || password == null || username.length() > 32) {
            response.sendError(400);
            return;
        }

        int uid = com.juick.server.UserQueries.checkPassword(sql, username, password);
        if (uid > 0) {
            String hash = com.juick.server.UserQueries.getHashByUID(sql, uid);
            Cookie c = new Cookie("hash", hash);
            c.setDomain(".juick.com");
            c.setMaxAge(0);
            response.addCookie(c);

            response.sendRedirect("/");
        } else {
            response.sendError(403);
        }
    }

    protected void doGetLogout(Connection sql, HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        int uid = Utils.getVisitorUID(sql, request);
        if (uid > 0) {
            PreparedStatement stmt = null;
            try {
                stmt = sql.prepareStatement("DELETE FROM logins WHERE user_id=?");
                stmt.setInt(1, uid);
                stmt.executeUpdate();
            } catch (SQLException e) {
                System.err.println(e);
            } finally {
                Utils.finishSQL(null, stmt);
            }
        }

        Cookie c = new Cookie("hash", "-");
        c.setDomain(".juick.com");
        c.setMaxAge(0);
        response.addCookie(c);

        response.sendRedirect("/");
    }
}
