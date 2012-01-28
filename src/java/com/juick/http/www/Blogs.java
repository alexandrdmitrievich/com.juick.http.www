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
public class Blogs {

    protected void doGet(Connection sql, Connection sqlSearch, HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        com.juick.User visitor = Utils.getVisitorUser(sql, request);
        Locale locale = request.getLocale();
        ResourceBundle rb = ResourceBundle.getBundle("Blogs", locale);

        String title;
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
                title = "*" + Utils.encodeHTML(paramTagStr);
                mids = MessagesQueries.getTag(sql, paramTag, paramBefore);
            } else if (paramSearch != null) {
                title = rb.getString("Search") + ": " + Utils.encodeHTML(paramSearch);
                mids = MessagesQueries.getSearch(sql, sqlSearch, Utils.encodeSphinx(paramSearch), paramBefore);
            } else {
                title = rb.getString("Last messages");
                mids = MessagesQueries.getAll(sql, paramBefore);
            }
        } else if (paramShow.equals("my")) {
            title = rb.getString("My feed");
            mids = MessagesQueries.getMyFeed(sql, visitor.UID, paramBefore);
        } else if (paramShow.equals("private")) {
            title = rb.getString("Private");
            mids = MessagesQueries.getPrivate(sql, visitor.UID, paramBefore);
        } else if (paramShow.equals("incoming")) {
            title = rb.getString("Incoming");
            mids = MessagesQueries.getIncoming(sql, visitor.UID, paramBefore);
        } else if (paramShow.equals("recommended")) {
            title = rb.getString("Recommended");
            mids = MessagesQueries.getRecommended(sql, visitor.UID, paramBefore);
        } else if (paramShow.equals("top")) {
            title = rb.getString("Popular");
            mids = MessagesQueries.getPopular(sql, paramBefore);
        } else if (paramShow.equals("photos")) {
            title = rb.getString("With photos");
            mids = MessagesQueries.getPhotos(sql, paramBefore);
        } else {
            response.sendError(404);
            return;
        }

        response.setContentType("text/html; charset=UTF-8");
        PrintWriter out = response.getWriter();
        try {
            PageTemplates.pageHead(out, title, null);
            PageTemplates.pageNavigation(out, locale, visitor);
            PageTemplates.pageTitle(out, title);

            out.println("<div id=\"wrapper\">");
            out.println("<div id=\"content\">");
            out.println("<ul>");

            if (mids.size() > 0) {
                PageTemplates.printMessages(out, sql, mids, locale);
            }

            out.println("</ul>");

            if (mids.size() == 20) {
                String nextpage = "?before=" + mids.get(mids.size() - 1);
                if (paramShow != null) {
                    nextpage += "&show=" + paramShow;
                }
                if (paramTag > 0) {
                    nextpage += "&tag=" + URLEncoder.encode(paramTagStr, "UTF-8");
                }
                out.println("<p class=\"page\"><a href=\"" + nextpage + "\">Older →</a></p>");
            }

            out.println("</div>");

            out.println("<div id=\"column\">");
            out.println("<h2>" + rb.getString("Lists") + "</h2>");
            if (visitor != null) {
                out.println("<ul>");
                out.println("  <li><a href=\"?show=my\">" + rb.getString("My feed") + "</a></li>");
                out.println("  <li><a href=\"?show=private\">" + rb.getString("Private") + "</a></li>");
                out.println("  <li><a href=\"?show=incoming\">" + rb.getString("Incoming") + "</a></li>");
                out.println("  <li><a href=\"?show=recommended\">" + rb.getString("Recommended") + "</a></li>");
                out.println("</ul>");
            }
            out.println("<ul>");
            out.println("  <li><a href=\"?\">" + rb.getString("All messages") + "</a></li>");
            out.println("  <li><a href=\"?show=top\">" + rb.getString("Popular") + "</a></li>");
            out.println("  <li><a href=\"?show=photos\">" + rb.getString("With photos") + "</a></li>");
            out.println("</ul>");
            out.println("<h2>" + rb.getString("Tags") + "</h2>");
            out.println("<p style=\"text-align: justify\">" + getTags(sql, 30) + "</p>");
            out.println("<h2>" + rb.getString("Search") + "</h2>");
            out.println("<form action=\"/\" id=\"search\"><p><input type=\"text\" name=\"search\" class=\"inp\"/></p></form>");
            out.println("</div>");
            out.println("</div>");

            PageTemplates.pageFooter(request, out, locale, visitor);
        } finally {
            out.close();
        }
    }

    private String getTags(Connection sql, int cnt) {
        String ret = "";
        com.juick.Tag tags[] = new com.juick.Tag[cnt];

        int maxUsageCnt = 0;
        PreparedStatement stmt = null;
        ResultSet rs = null;
        try {
            stmt = sql.prepareStatement("SELECT tags.name AS name,COUNT(DISTINCT messages.user_id) AS cnt FROM (messages INNER JOIN messages_tags ON (messages.ts>TIMESTAMPADD(DAY,-3,NOW()) AND messages.message_id=messages_tags.message_id)) INNER JOIN tags ON messages_tags.tag_id=tags.tag_id GROUP BY tags.tag_id ORDER BY cnt DESC LIMIT ?");
            stmt.setInt(1, cnt);
            rs = stmt.executeQuery();
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

        Arrays.sort(tags, 0, cnt);

        for (int i = 0; i < cnt; i++) {
            String tag = Utils.encodeHTML(tags[i].Name);
            try {
                tag = "<a href=\"/?tag=" + URLEncoder.encode(tags[i].Name, "UTF-8") + "\">" + tag + "</a>";
            } catch (UnsupportedEncodingException e) {
            }

            if (tags[i].UsageCnt > maxUsageCnt / 3 * 2) {
                ret += "<big>" + tag + "</big> ";
            } else if (tags[i].UsageCnt > maxUsageCnt / 3) {
                ret += "<small>" + tag + "</small> ";
            } else {
                ret += tag + " ";
            }
        }

        return ret;
    }
}
