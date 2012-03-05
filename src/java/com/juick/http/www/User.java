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

import com.juick.server.MessagesQueries;
import com.juick.server.TagQueries;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Locale;
import java.util.ResourceBundle;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 *
 * @author Ugnich Anton
 */
public class User {

    protected void doGetBlog(Connection sql, Connection sqlSearch, HttpServletRequest request, HttpServletResponse response, com.juick.User user) throws ServletException, IOException {
        com.juick.User visitor = Utils.getVisitorUser(sql, request);
        Locale locale = request.getLocale();
        ResourceBundle rb = ResourceBundle.getBundle("User", locale);

        String title = "@" + user.UName + " - ";
        ArrayList<Integer> mids;

        String paramShow = request.getParameter("show");

        int paramTag = 0;
        String paramTagStr = request.getParameter("tag");
        if (paramTagStr != null && paramTagStr.length() < 64) {
            paramTag = TagQueries.getTagID(sql, paramTagStr, false);
        }

        int paramBefore = 0;
        String paramBeforeStr = request.getParameter("before");
        if (paramBeforeStr != null) {
            try {
                paramBefore = Integer.parseInt(paramBeforeStr);
            } catch (NumberFormatException e) {
            }
        }

        String paramSearch = request.getParameter("search");
        if (paramSearch != null && paramSearch.length() > 64) {
            paramSearch = null;
        }

        if (paramShow == null) {
            if (paramTag > 0) {
                title += "*" + Utils.encodeHTML(paramTagStr);
                mids = MessagesQueries.getUserTag(sql, user.UID, paramTag, paramBefore);
            } else if (paramSearch != null) {
                title += rb.getString("(Menu) Search") + ": " + Utils.encodeHTML(paramSearch);
                mids = MessagesQueries.getUserSearch(sql, sqlSearch, user.UID, Utils.encodeSphinx(paramSearch), paramBefore);
            } else {
                title += rb.getString("(Menu) Blog");
                mids = MessagesQueries.getUserBlog(sql, user.UID, paramBefore);
            }
        } else if (paramShow.equals("recomm")) {
            title += rb.getString("(Menu) Recommendations");
            mids = MessagesQueries.getUserRecommendations(sql, user.UID, paramBefore);
        } else if (paramShow.equals("photos")) {
            title += rb.getString("(Menu) Photos");
            mids = MessagesQueries.getUserPhotos(sql, user.UID, paramBefore);
        } else {
            response.sendError(404);
            return;
        }

        response.setContentType("text/html; charset=UTF-8");
        PrintWriter out = response.getWriter();
        try {
            PageTemplates.pageHead(out, title, null);
            PageTemplates.pageNavigation(out, locale, visitor);
            PageTemplates.pageUserTitle(out, sql, locale, user, visitor);

            out.println("<div id=\"wrapper\">");
            out.println("<div id=\"content\">");
            out.println("<ul>");
            if (mids.size() > 0) {
                PageTemplates.printMessages(out, sql, mids, locale);
            }
            out.println("</ul>");

            if (mids.size() == 20) {
                String nextpage = "?before=" + mids.get(19);
                if (paramShow != null) {
                    nextpage += "&show=" + paramShow;
                }
                if (paramTag > 0) {
                    nextpage += "&tag=" + URLEncoder.encode(paramTagStr, "UTF-8");
                }
                out.println("<p class=\"page\"><a href=\"" + nextpage + "\">Older →</a></p>");
            }
            out.println("</div>");

            pageUserColumn(out, sql, user, visitor, locale);
            out.println("</div>");

            PageTemplates.pageFooter(request, out, locale, visitor);
        } finally {
            out.close();
        }
    }

    protected void doGetInfo(Connection sql, HttpServletRequest request, HttpServletResponse response, com.juick.User user) throws ServletException, IOException {
    }

    protected void doGetTags(Connection sql, HttpServletRequest request, HttpServletResponse response, com.juick.User user) throws ServletException, IOException {
        com.juick.User visitor = Utils.getVisitorUser(sql, request);
        Locale locale = request.getLocale();
        ResourceBundle rb = ResourceBundle.getBundle("User", locale);

        response.setContentType("text/html; charset=UTF-8");
        PrintWriter out = response.getWriter();
        try {
            PageTemplates.pageHead(out, "@" + user.UName + ": " + rb.getString("(Menu) Tags"), null);
            PageTemplates.pageNavigation(out, locale, visitor);
            PageTemplates.pageUserTitle(out, sql, locale, user, visitor);

            out.println("<div id=\"wrapper\">");
            out.println("<div id=\"content\">");

            out.print("<p>");
            pageUserTags(out, sql, user, visitor, 0);
            out.print("</p>");

            out.println("</div>");
            pageUserColumn(out, sql, user, visitor, locale);
            out.println("</div>");

            PageTemplates.pageFooter(request, out, locale, visitor);
        } finally {
            out.close();
        }
    }

    protected void doGetFriends(Connection sql, HttpServletRequest request, HttpServletResponse response, com.juick.User user) throws ServletException, IOException {
        com.juick.User visitor = Utils.getVisitorUser(sql, request);
        Locale locale = request.getLocale();
        ResourceBundle rb = ResourceBundle.getBundle("User", locale);

        response.setContentType("text/html; charset=UTF-8");
        PrintWriter out = response.getWriter();
        try {
            PageTemplates.pageHead(out, "@" + user.UName + ": " + rb.getString("(Stats) I read"), null);
            PageTemplates.pageNavigation(out, locale, visitor);
            PageTemplates.pageUserTitle(out, sql, locale, user, visitor);

            out.println("<div id=\"wrapper\">");
            out.println("<div id=\"content\">");
            out.println("<table class=\"users\"><tr>");

            PreparedStatement stmt = null;
            ResultSet rs = null;
            try {
                stmt = sql.prepareStatement("SELECT users.id,users.nick FROM subscr_users INNER JOIN users ON subscr_users.user_id=users.id WHERE subscr_users.suser_id=? ORDER BY users.nick");
                stmt.setInt(1, user.UID);
                rs = stmt.executeQuery();
                rs.beforeFirst();
                int cnt = 0;
                while (rs.next()) {
                    if (cnt % 3 == 0 && cnt > 0) {
                        out.print("</tr><tr>");
                    }
                    out.print("<td><a href=\"/" + rs.getString(2) + "/\"><img src=\"http://i.juick.com/as/" + rs.getInt(1) + ".png\"/>" + rs.getString(2) + "</a></td>");
                    cnt++;
                }
            } catch (SQLException e) {
                System.err.println(e);
            } finally {
                Utils.finishSQL(rs, stmt);
            }

            out.println("</tr></table>");
            out.println("</div>");
            pageUserColumn(out, sql, user, visitor, locale);
            out.println("</div>");

            PageTemplates.pageFooter(request, out, locale, visitor);
        } finally {
            out.close();
        }
    }

    protected void doGetReaders(Connection sql, HttpServletRequest request, HttpServletResponse response, com.juick.User user) throws ServletException, IOException {
        com.juick.User visitor = Utils.getVisitorUser(sql, request);
        Locale locale = request.getLocale();
        ResourceBundle rb = ResourceBundle.getBundle("User", locale);

        response.setContentType("text/html; charset=UTF-8");
        PrintWriter out = response.getWriter();
        try {
            PageTemplates.pageHead(out, "@" + user.UName + ": " + rb.getString("(Stats) I read"), null);
            PageTemplates.pageNavigation(out, locale, visitor);
            PageTemplates.pageUserTitle(out, sql, locale, user, visitor);

            out.println("<div id=\"wrapper\">");
            out.println("<div id=\"content\">");
            out.println("<table class=\"users\"><tr>");

            PreparedStatement stmt = null;
            ResultSet rs = null;
            try {
                stmt = sql.prepareStatement("SELECT users.id,users.nick FROM subscr_users INNER JOIN users ON subscr_users.suser_id=users.id WHERE subscr_users.user_id=? ORDER BY users.nick");
                stmt.setInt(1, user.UID);
                rs = stmt.executeQuery();
                rs.beforeFirst();
                int cnt = 0;
                while (rs.next()) {
                    if (cnt % 3 == 0 && cnt > 0) {
                        out.print("</tr><tr>");
                    }
                    out.print("<td><a href=\"/" + rs.getString(2) + "/\"><img src=\"http://i.juick.com/as/" + rs.getInt(1) + ".png\"/>" + rs.getString(2) + "</a></td>");
                    cnt++;
                }
            } catch (SQLException e) {
                System.err.println(e);
            } finally {
                Utils.finishSQL(rs, stmt);
            }

            out.println("</tr></table>");
            out.println("</div>");
            pageUserColumn(out, sql, user, visitor, locale);
            out.println("</div>");

            PageTemplates.pageFooter(request, out, locale, visitor);
        } finally {
            out.close();
        }
    }

    public static void pageUserColumn(PrintWriter out, Connection sql, com.juick.User user, com.juick.User visitor, Locale locale) {
        ResourceBundle rb = ResourceBundle.getBundle("User", locale);

        out.println("<div id=\"column\">");
        out.println("<h2>" + rb.getString("(Menu) Messages") + "</h2>");
        out.println("<ul>");
        out.println("  <li><a href=\"?\">" + rb.getString("(Menu) Blog") + "</a></li>");
        out.println("  <li><a href=\"?show=recomm\">" + rb.getString("(Menu) Recommendations") + "</a></li>");
        out.println("  <li><a href=\"?show=photos\">" + rb.getString("(Menu) Photos") + "</a></li>");
        out.println("</ul>");
        out.println("<h2>" + rb.getString("(Menu) Tags") + " <a href=\"./tags\">" + rb.getString("all") + "</a></h2>");
        pageUserTags(out, sql, user, visitor, 15);
        out.println("<h2>" + rb.getString("(Menu) Search") + "</h2>");
        out.println("<form action=\"./\" id=\"search\"><p><input type=\"text\" name=\"search\" class=\"inp\"/></p></form>");
        out.println("</div>");
    }

    public static void pageUserTags(PrintWriter out, Connection sql, com.juick.User user, com.juick.User visitor, int cnt) {
        com.juick.Tag tags[] = null;

        int maxUsageCnt = 0;
        PreparedStatement stmt = null;
        ResultSet rs = null;
        try {
            if (cnt > 0) {
                stmt = sql.prepareStatement("SELECT tags.name AS name,COUNT(DISTINCT messages_tags.message_id) AS cnt FROM (messages INNER JOIN messages_tags ON (messages.message_id=messages_tags.message_id)) INNER JOIN tags ON messages_tags.tag_id=tags.tag_id WHERE messages.user_id=? GROUP BY messages_tags.tag_id ORDER BY cnt DESC LIMIT ?", ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY);
                stmt.setInt(1, user.UID);
                stmt.setInt(2, cnt);
            } else {
                stmt = sql.prepareStatement("SELECT tags.name AS name,COUNT(DISTINCT messages_tags.message_id) AS cnt FROM (messages INNER JOIN messages_tags ON (messages.message_id=messages_tags.message_id)) INNER JOIN tags ON messages_tags.tag_id=tags.tag_id WHERE messages.user_id=? GROUP BY messages_tags.tag_id ORDER BY cnt DESC", ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY);
                stmt.setInt(1, user.UID);
            }
            rs = stmt.executeQuery();
            rs.last();
            tags = new com.juick.Tag[rs.getRow()];
            rs.beforeFirst();
            cnt = 0;
            while (rs.next()) {
                tags[cnt] = new com.juick.Tag();
                tags[cnt].Name = rs.getString(1);
                tags[cnt].UsageCnt = rs.getInt(2);
                if (tags[cnt].UsageCnt > maxUsageCnt) {
                    maxUsageCnt = tags[cnt].UsageCnt;
                }
                cnt++;
            }
        } catch (SQLException e) {
            System.err.println(e);
        } finally {
            Utils.finishSQL(rs, stmt);
        }

        if (tags != null && cnt > 0) {
            Arrays.sort(tags, 0, cnt);
        }

        for (int i = 0; i < cnt; i++) {
            String tag = Utils.encodeHTML(tags[i].Name);
            try {
                tag = "<a href=\"./?tag=" + URLEncoder.encode(tags[i].Name, "UTF-8") + "\" title=\"" + tags[i].UsageCnt + "\">" + tag + "</a>";
            } catch (UnsupportedEncodingException e) {
            }

            if (tags[i].UsageCnt > maxUsageCnt / 3 * 2) {
                out.print("<big>" + tag + "</big> ");
            } else if (tags[i].UsageCnt > maxUsageCnt / 3) {
                out.print("<small>" + tag + "</small> ");
            } else {
                out.print(tag + " ");
            }
        }
    }
}
