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

import com.juick.Tag;
import com.juick.server.TagQueries;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.sql.Connection;
import java.util.ArrayList;
import java.util.Locale;
import java.util.ResourceBundle;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 *
 * @author Ugnich Anton
 */
public class NewMessage {

    protected void doGetNewMessage(Connection sql, HttpServletRequest request, HttpServletResponse response, com.juick.User visitor) throws ServletException, IOException {
        Locale locale = request.getLocale();
        ResourceBundle rbnm = ResourceBundle.getBundle("NewMessage", locale);

        response.setContentType("text/html; charset=UTF-8");
        PrintWriter out = response.getWriter();
        try {
            PageTemplates.pageHead(out, rbnm.getString("New message"), "<script src=\"http://maps.google.com/maps?file=api&amp;v=2&amp;sensor=false&amp;key=ABQIAAAAVVtPtxkw4soCEHg44FsNChRB4OFYjAXt73He16Zkp6a_0tPs2RTU6i6UlcMs4QvPBYvIY8rWvcxqOg\" type=\"text/javascript\"></script>"
                    + "<script src=\"http://static.juick.com/mc.js\" type=\"text/javascript\"></script>"
                    + "<script src=\"http://static.juick.com/map.js?2010111500\" type=\"text/javascript\"></script>"
                    + "<script src=\"http://static.juick.com/post3.js\" type=\"text/javascript\"></script>");
            PageTemplates.pageNavigation(out, locale, visitor);
            PageTemplates.pageTitle(out, rbnm.getString("New message"));

            out.println("<div id=\"wrapper\"><div id=\"content\" class=\"pagetext\">");
            out.println("<form action=\"/post\" method=\"post\" id=\"postmsg\" enctype=\"multipart/form-data\">");
            out.println("<p style=\"text-align: left\"><b>" + rbnm.getString("Location") + ": <span id=\"location\"></span></b> <span id=\"locationclear\">&mdash; <a href=\"#\" onclick=\"clearLocation()\">" + rbnm.getString("Clear") + "</a></span></p>");
            out.println("<p style=\"text-align: left\"><b>" + rbnm.getString("Attachment") + ":</b> <span id=\"attachmentfile\"><input type=\"file\" name=\"attach\"$canmedia/> " + rbnm.getString("or") + " <a href=\"#\" onclick=\"webcamShow(); return false;\">" + rbnm.getString("from webcam") + "</a><br/>");
            out.println("<i>" + rbnm.getString("Photo_JPG") + "</i></span><span id=\"attachmentwebcam\">" + rbnm.getString("Webcam photo") + " &mdash; <a href=\"#\" onclick=\"clearAttachment(); return false;\">" + rbnm.getString("Clear") + "</a></span></p>");
            out.println("<div id=\"webcamwrap\" style=\"width: 320px; margin: 0 auto\"><div id=\"webcam\"></div></div>");
            out.println("<p><textarea name=\"body\" rows=\"7\" cols=\"10\">" + "" + "</textarea><br/>");
            out.println("<input type=\"hidden\" name=\"place_id\"/><input type=\"hidden\" name=\"webcam\"/>" + "" + "<input type=\"submit\" class=\"subm\" value=\"   " + rbnm.getString("Post") + "   \"/></p>");
            out.println("</form>");
            out.println("<div id=\"geomap\"></div>");
            out.println("<p style=\"text-align: left\"><b>" + rbnm.getString("Tags") + ":</b></p>");
            printUserTags(sql, out, visitor.UID);
            out.println("</div>");
            out.println("</div>");

            PageTemplates.pageFooter(request, out, locale, visitor);
        } finally {
            out.close();
        }
    }

    void printUserTags(Connection sql, PrintWriter out, int uid) {
        ArrayList<Tag> tags = TagQueries.getUserTagsAll(sql, uid);

        if (tags.isEmpty()) {
            return;
        }

        int min = tags.get(0).UsageCnt;
        int max = tags.get(0).UsageCnt;
        for (int i = 1; i < tags.size(); i++) {
            int usagecnt = tags.get(i).UsageCnt;
            if (usagecnt < min) {
                min = usagecnt;
            }
            if (usagecnt > max) {
                max = usagecnt;
            }
        }
        max -= min;

        out.print("<p style=\"text-align: justify\">");
        for (int i = 0; i < tags.size(); i++) {
            if (i > 0) {
                out.print(" ");
            }
            String taglink = "";
            try {
                taglink = "<a onclick=\"return addTag('" + Utils.encodeHTML(tags.get(i).Name) + "')\" href=\"/?tag=" + URLEncoder.encode(tags.get(i).Name, "utf-8") + "\" title=\"" + tags.get(i).UsageCnt + "\">" + Utils.encodeHTML(tags.get(i).Name) + "</a>";
            } catch (UnsupportedEncodingException e) {
            }
            int usagecnt = tags.get(i).UsageCnt;
            if (usagecnt <= max / 5 + min) {
                out.print("<span style=\"font-size: small\">" + taglink + "</span>");
            } else if (usagecnt <= max / 5 * 2 + min) {
                out.print(taglink);
            } else if (usagecnt <= max / 5 * 3 + min) {
                out.print("<span style=\"font-size: large\">" + taglink + "</span>");
            } else if (usagecnt <= max / 5 * 4 + min) {
                out.print("<span style=\"font-size: x-large\">" + taglink + "</span>");
            } else {
                out.print("<span style=\"font-size: xx-large\">" + taglink + "</span>");
            }
        }
        out.println("</p>");
    }

    protected void doPostNewMessage(Connection sql, HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
    }
}
