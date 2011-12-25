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
import java.sql.Connection;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 *
 * @author Ugnich Anton
 */
public class RootRedirects {

    protected void doGetPostID(Connection sql, HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        String strID = request.getRequestURI().substring(1);
        int mid = Integer.parseInt(strID);
        if (mid > 0) {
            com.juick.User author = com.juick.server.MessagesQueries.getMessageAuthor(sql, mid);
            if (author != null) {
                Utils.sendPermanentRedirect(response, "/" + author.UName + "/" + mid);
                return;
            }
        }
        response.sendError(404);
    }

    protected void doGetUsername(Connection sql, HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        com.juick.User user = com.juick.server.UserQueries.getUserByNick(sql, request.getRequestURI().substring(1));
        if (user != null) {
            Utils.sendPermanentRedirect(response, "/" + user.UName + "/");
        } else {
            response.sendError(404);
        }
    }
}
