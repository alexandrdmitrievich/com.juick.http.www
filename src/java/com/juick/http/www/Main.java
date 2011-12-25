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

import java.io.FileInputStream;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 *
 * @author Ugnich Anton
 */
@WebServlet(name = "Main", urlPatterns = {"/"})
public class Main extends HttpServlet {

    Connection sql;
    Connection sqlSearch;
    Blogs blogs = new Blogs();
    Chats chats = new Chats();
    Photos photos = new Photos();
    RootRedirects rootRedirects = new RootRedirects();
    Map map = new Map();
    Login login = new Login();
    User pagesUser = new User();
    UserThread pagesUserThread = new UserThread();
    NewMessage pagesNewMessage = new NewMessage();

    @Override
    public void init() throws ServletException {
        super.init();
        try {
            Properties conf = new Properties();
            conf.load(new FileInputStream("/etc/juick/www.conf"));

            Class.forName("com.mysql.jdbc.Driver");
            sql = DriverManager.getConnection("jdbc:mysql://localhost/juick?autoReconnect=true&user=" + conf.getProperty("mysql_username", "") + "&password=" + conf.getProperty("mysql_password", ""));
            sqlSearch = DriverManager.getConnection("jdbc:mysql://127.0.0.1:9306/juick?autoReconnect=true&characterEncoding=utf8&maxAllowedPacket=512000&relaxAutoCommit=true&user=root&password=");
        } catch (Exception e) {
            log(null, e);
        }
    }

    @Override
    public void destroy() {
        super.destroy();
        if (sql != null) {
            try {
                sql.close();
            } catch (SQLException e) {
                log(null, e);
            }
        }
    }

    /** 
     * Handles the HTTP <code>GET</code> method.
     * @param request servlet request
     * @param response servlet response
     * @throws ServletException if a servlet-specific error occurs
     * @throws IOException if an I/O error occurs
     */
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        if (request.getCharacterEncoding() == null) {
            request.setCharacterEncoding("UTF-8");
        }
        String uri = request.getRequestURI();
        if (uri.equals("/")) {
            blogs.doGet(sql, sqlSearch, request, response);
        } else if (uri.equals("/chats")) {
            chats.doGet(sql, request, response);
        } else if (uri.equals("/photos")) {
            photos.doGet(sql, request, response);
        } else if (uri.equals("/map")) {
            map.doGet(sql, request, response);
        } else if (uri.equals("/post")) {
            pagesNewMessage.doGetNewMessage(sql, request, response);
        } else if (uri.equals("/login")) {
            if (request.getQueryString() == null) {
                login.doGetLoginForm(sql, request, response);
            } else {
                login.doGetLogin(sql, request, response);
            }
        } else if (uri.equals("/logout")) {
            login.doGetLogout(sql, request, response);
        } else if (uri.equals("/settings")) {
            //TODO settings
        } else if (uri.matches("^/\\d+$")) {
            rootRedirects.doGetPostID(sql, request, response);
        } else if (uri.matches("^/[^/]$")) {
            rootRedirects.doGetUsername(sql, request, response);
        } else if (uri.matches("^/.+/.*")) {
            String uriparts[] = uri.split("/");
            com.juick.User user = com.juick.server.UserQueries.getUserByNick(sql, uriparts[1]);
            if (user != null && user.UName.equals(uriparts[1])) {
                if (uriparts.length == 2) { // http://juick.com/username/
                    pagesUser.doGetBlog(sql, sqlSearch, request, response, user);
                } else if (uriparts[2].equals("info")) {
                    pagesUser.doGetInfo(sql, request, response, user);
                } else {
                    int mid = 0;
                    try {
                        mid = Integer.parseInt(uriparts[2]);
                    } catch (NumberFormatException e) {
                    }
                    if (mid > 0) {
                        com.juick.User author = com.juick.server.MessagesQueries.getMessageAuthor(sql, mid);
                        if (author != null) {
                            if (!author.UName.equals(user.UName)) {
                                Utils.sendPermanentRedirect(response, "/" + author.UName + "/" + mid);
                            } else {
                                pagesUserThread.doGetThread(sql, request, response, user, mid);
                            }
                        } else {
                            response.sendError(404);
                        }
                    } else {
                        response.sendError(404);
                    }
                }
            } else if (user != null) {
                Utils.sendPermanentRedirect(response, "/" + user.UName + "/" + (uriparts.length > 2 ? uriparts[2] : ""));
            } else {
                response.sendError(404);
            }
        } else {
            response.sendError(404);
        }
    }

    /** 
     * Handles the HTTP <code>POST</code> method.
     * @param request servlet request
     * @param response servlet response
     * @throws ServletException if a servlet-specific error occurs
     * @throws IOException if an I/O error occurs
     */
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        String uri = request.getRequestURI();
        if (uri.equals("/post")) {
            pagesNewMessage.doPostNewMessage(sql, request, response);
        } else if (uri.equals("/login")) {
            login.doPostLogin(sql, request, response);
        } else if (uri.equals("/settings")) {
        } else {
            response.sendError(405);
        }
    }
}
