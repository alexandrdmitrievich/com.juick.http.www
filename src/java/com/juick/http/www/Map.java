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
import java.util.Locale;
import java.util.ResourceBundle;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 *
 * @author Ugnich Anton
 */
public class Map {

    protected void doGet(Connection sql, HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        com.juick.User visitor = Utils.getVisitorUser(sql, request);
        Locale locale = request.getLocale();
        ResourceBundle rb = ResourceBundle.getBundle("Map", locale);

        response.setContentType("text/html; charset=UTF-8");
        PrintWriter out = response.getWriter();
        try {
            PageTemplates.pageHead(out, rb.getString("Messages on map"), "<script src=\"http://maps.google.com/maps?file=api&amp;v=2&amp;sensor=false&amp;key=ABQIAAAAVVtPtxkw4soCEHg44FsNChRB4OFYjAXt73He16Zkp6a_0tPs2RTU6i6UlcMs4QvPBYvIY8rWvcxqOg\" type=\"text/javascript\"></script>"
                    + "<script src=\"http://static.juick.com/mc.js\" type=\"text/javascript\"></script>"
                    + "<script src=\"http://static.juick.com/map.js?2010111500\" type=\"text/javascript\"></script>");
            PageTemplates.pageNavigation(out, locale, visitor);
            PageTemplates.pageTitle(out, rb.getString("Messages on map"));

            out.println("<div id=\"wrapper\">");
            out.println("<div id=\"geomap\" style=\"height: 400px; margin: 1em 0.5em\"></div>");
            out.println("<div id=\"content\"><ul id=\"messages\"></ul></div>");
            out.println("<div id=\"column\"><h2>" + rb.getString("Popular places") + "</h2><ul id=\"places\"></ul></div>");
            out.println("</div>");
            out.println("<script type=\"text/javascript\">");
            out.println("$(document).ready(mapInit);");
            out.println("$(window).unload(GUnload);");
            out.println("</script>");

            PageTemplates.pageFooter(request, out, locale, visitor);
        } finally {
            out.close();
        }
    }
}
