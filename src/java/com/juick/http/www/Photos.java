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
public class Photos {

    protected void doGet(Connection sql, HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        com.juick.User visitor = Utils.getVisitorUser(sql, request);
        Locale locale = request.getLocale();
        ResourceBundle rb = ResourceBundle.getBundle("Photos", locale);

        response.setContentType("text/html; charset=UTF-8");
        PrintWriter out = response.getWriter();
        try {
            PageTemplates.pageHead(out, rb.getString("Last photos and videos"), "<script type=\"text/javascript\" src=\"http://static.juick.com/js/jquery.onImagesLoad.min.js\"></script>"
                    + "<script type=\"text/javascript\" src=\"http://static.juick.com/js/gallery.js\"></script>"
                    + "<script type=\"text/javascript\" src=\"http://static.juick.com/js/jquery.fancybox-1.3.4.js\"></script>"
                    + "<link rel=\"stylesheet\" href=\"http://static.juick.com/fancybox/jquery.fancybox-1.3.4.css\"/>");
            PageTemplates.pageNavigation(out, locale, visitor);
            PageTemplates.pageTitle(out, rb.getString("Last photos and videos"));

            out.println("<div id=\"wrapper\">");
            out.println("<div id=\"spinner\" style=\"text-align: center\">Loading...</div>");
            out.println("<ul id=\"gallery\" style=\"display: none\">");

            PreparedStatement stmt = null;
            ResultSet rs = null;
            try {
                stmt = sql.prepareStatement("SELECT messages.message_id,messages.attach,users.nick FROM messages INNER JOIN users ON messages.user_id=users.id WHERE messages.attach IS NOT NULL ORDER BY message_id DESC LIMIT 20");
                rs = stmt.executeQuery();
                rs.beforeFirst();
                while (rs.next()) {
                    if (rs.getString(2).equals("jpg")) {
                        out.println("<li class=\"galleryitem\"><a href=\"/" + rs.getString(3) + "/" + rs.getInt(1) + "\" title=\"@" + rs.getString(3) + "\"><img src=\"http://i.juick.com/photos-512/" + rs.getInt(1) + ".jpg\" alt=\"\"></a></li>");
                    } else {
                        out.println("<li class=\"galleryitem\"><a href=\"/" + rs.getString(3) + "/" + rs.getInt(1) + "\" title=\"@" + rs.getString(3) + "\"><img src=\"http://i.juick.com/thumbs/" + rs.getInt(1) + ".jpg\" alt=\"\"></a></li>");
                    }
                }
            } catch (SQLException e) {
                System.err.println(e);
            } finally {
                Utils.finishSQL(rs, stmt);
            }

            out.println("</ul>");
            out.println("</div>");
            out.println("<script type=\"text/javascript\">");
            out.println("$('#gallery').onImagesLoad({selectorCallback: galleryResize});");
            out.println("$('.galleryitem a').click(function() {");
            out.println("  $.fancybox({");
            out.println("    'href': $(this).children('img').attr('src'),");
            out.println("    'link': this.href,");
            out.println("    'title': this.title,");
            out.println("    'orig': this,");
            out.println("    'transitionIn': 'elastic',");
            out.println("    'transitionOut': 'none',");
            out.println("    'easingIn': 'easeOutBack',");
            out.println("    'easingOut': 'easeInBack'");
            out.println("  });");
            out.println("  return false;");
            out.println("});");
            out.println("</script>");

            PageTemplates.pageFooter(request, out, locale, visitor);
        } finally {
            out.close();
        }
    }
}
