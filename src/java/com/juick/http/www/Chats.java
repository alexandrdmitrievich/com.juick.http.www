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
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Locale;
import java.util.ResourceBundle;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 *
 * @author Ugnich Anton
 */
public class Chats {

    protected void doGet(Connection sql, HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        com.juick.User visitor = Utils.getVisitorUser(sql, request);
        Locale locale = request.getLocale();
        ResourceBundle rb = ResourceBundle.getBundle("Chats", locale);

        response.setContentType("text/html; charset=UTF-8");
        PrintWriter out = response.getWriter();
        try {
            PageTemplates.pageHead(out, rb.getString("Active chats"), "");
            PageTemplates.pageNavigation(out, locale, visitor);
            PageTemplates.pageTitle(out, rb.getString("Active chats"));

            out.println("<div id=\"wrapper\">");
            out.println("<div id=\"content\"><ul id=\"chats\">");

            PreparedStatement stmt = null;
            ResultSet rs = null;
            try {
                stmt = sql.prepareStatement("SELECT chats.chat_id,chats.subject,COUNT(chat_users.user_id) AS cnt FROM chats INNER JOIN chat_users USING(chat_id) GROUP BY chat_id ORDER BY cnt DESC");
                rs = stmt.executeQuery();
                rs.beforeFirst();
                while (rs.next()) {
                    String chatid = Integer.toString(rs.getInt(1), Character.MAX_RADIX);
                    out.println("<li><b>" + Utils.encodeHTML(rs.getString(2)) + "</b><br/><i>" + rb.getString("Users online") + ": " + rs.getInt(3) + "</i><br/><a href=\"xmpp:" + chatid + "@chat.juick.com?join\">" + chatid + "@chat.juick.com</a></li>");
                }
            } catch (SQLException e) {
                System.err.println(e);
            } finally {
                Utils.finishSQL(rs, stmt);
            }

            out.println("</ul></div>");
            out.println("</div>");

            PageTemplates.pageFooter(out, locale);
        } finally {
            out.close();
        }
    }
}
