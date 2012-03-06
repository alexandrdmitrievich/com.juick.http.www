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
import com.juick.server.UserQueries;
import java.io.IOException;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
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
public class UserThread {

    public String mediaOut(String type, int mid, int rid, /*int size, */int link = 0)
    {
        String ret=""; // TODO доделать 
         if (type != null) {
                    if (type.equals("jpg")) {
                        if(link == 0) 
                            {
                                ret+=("    <div class=\"msg-media\"><img src=\"http://i.juick.com/photos-512/" + mid + "-" + rid + ".jpg\" alt=\"\"/></div>");
                            }
                        else 
                            {
                             ret+=("    <div class=\"msg-media\"><a href=\"http://i.juick.com/photos-1024/" + mid + ".jpg\"><img src=\"http://i.juick.com/photos-512/" + mid + ".jpg\" alt=\"\"/></a></div>");
                            }
                    
                    } else {
                        ret+=("    <div class=\"msg-media\"><div id=\"video-" + mid + "-" + rid + "\"><b>Attachment: <a href=\"http://i.juick.com/video/" +mid+ "-" + rid + ".mp4\">Video</a></b></div></div>");
                        ret+=("    <script type=\"text/javascript\">");
                        ret+=("    inlinevideo('" +mid + "-" + rid + "');");
                        ret+=("    </script>");
                    }
                }
        
    }

    protected void doGetThread(Connection sql, HttpServletRequest request, HttpServletResponse response, com.juick.User user, int MID) throws ServletException, IOException {
        com.juick.User visitor = Utils.getVisitorUser(sql, request);
        Locale locale = request.getLocale();

        if (!MessagesQueries.canViewThread(sql, MID, visitor != null ? visitor.UID : 0)) {
            response.sendError(403);
            return;
        }

        boolean listview = false;
        String paramView = request.getParameter("view");
        
        if(visitor == null) 
        {
            listview = false;
        }
        else
        {
            listview = true;
            if (paramView.equals("list")) {
                UserQueries.setUserOptionInt(sql, visitor.UID, "repliesview", 1);
            }
            else if (paramView.equals("tree")) {
                UserQueries.setUserOptionInt(sql, visitor.UID, "repliesview", 0);
            }
            

        }
    /*    if (paramView != null) {
            if (paramView.equals("list")) {
                listview = true;
                if (visitor != null) {
                    UserQueries.setUserOptionInt(sql, visitor.UID, "repliesview", 1);
                }
            } else if (paramView.equals("tree") && visitor != null) {
                UserQueries.setUserOptionInt(sql, visitor.UID, "repliesview", 0);
            }
        } else if (visitor != null && UserQueries.getUserOptionInt(sql, visitor.UID, "repliesview", 0) == 1) {
            listview = true;
        }*/

        String title = "@" + user.UName + " - #" + MID;

        response.setContentType("text/html; charset=UTF-8");
        PrintWriter out = response.getWriter();
        try {
            PageTemplates.pageHead(out, title, null);
            PageTemplates.pageNavigation(out, locale, visitor);
            PageTemplates.pageUserTitle(out, sql, locale, user, visitor);


            out.println("<div id=\"wrapper\">");
            out.println("<div id=\"content\" style=\"margin-left: 0; width: 100%\">");

            printMessage(out, sql, MID, locale);

            printReplies(out, sql, MID, locale, listview);

            out.println("</div>");
            
            out.println("</div>");

            out.println("<script type=\"text/javascript\">");
            out.println("$(document).ready(unfoldReply);");
            out.println("$(window).bind('hashchange',unfoldReply);");
            out.println("</script>");

            PageTemplates.pageFooter(request, out, locale, visitor);
        } finally {
            out.close();
        }
    }

    public static void printMessage(PrintWriter out, Connection sql, int mid, Locale locale) {
        ResourceBundle rb = ResourceBundle.getBundle("Global", locale);

        PreparedStatement stmt = null;
        ResultSet rs = null;
        try { 
            stmt = sql.prepareStatement("SELECT STRAIGHT_JOIN messages.message_id,messages.user_id,users.nick,messages_txt.tags,messages.readonly,messages.privacy,messages_txt.txt,TIMESTAMPDIFF(MINUTE,messages.ts,NOW()),messages.ts,messages.replies,messages.attach,messages.place_id,places.name,messages.lat,messages.lon FROM ((messages INNER JOIN messages_txt ON messages.message_id=messages_txt.message_id) INNER JOIN users ON messages.user_id=users.id) LEFT JOIN places ON messages.place_id=places.place_id WHERE messages.message_id=?");
            stmt.setInt(1, mid);
            rs = stmt.executeQuery();
            if (rs.first()) {
                int uid = rs.getInt(2);
                String uname = rs.getString(3);
                String tags = "";
                String txt = rs.getString(7);
                // timediff
                // timestamp
                // replies
                // attach
                // pid
                // pname
                // lat
                // lon

                boolean cancomment = true;
                
                if(rs.getString(4)!= null){
                    tags = PageTemplates.formatTags(tags);
                }

                if (rs.getInt(5) == 1) {
                    tags += " *readonly";
                    cancomment = false;
                }
                switch (rs.getInt(6)) {
                    case 2:
                        tags += " *public";
                        break;
                    case -1:
                        tags += " *friends";
                        break;
                    case -2:
                        tags += " *private";
                        break;
                }

                txt = PageTemplates.formatMessage(txt);

                out.println("<ul>");
                out.println("  <li id=\"msg-" + mid + "\" class=\"msg\" style=\"border: 0\">");

                if (rs.getString(11) != null) {
                    if (rs.getString(11).equals("jpg")) {
                        out.println("    <div class=\"msg-media\"><a href=\"http://i.juick.com/photos-1024/" + mid + ".jpg\"><img src=\"http://i.juick.com/photos-512/" + mid + ".jpg\" alt=\"\"/></a></div>");
                    } else {
                        out.println("    <div class=\"msg-media\"><div id=\"video-" + mid + "\"><b>Attachment: <a href=\"http://i.juick.com/video/" + mid + ".mp4\">Video</a></b></div></div>");
                        out.println("    <script type=\"text/javascript\">");
                        out.println("    inlinevideo(" + mid + ");");
                        out.println("    </script>");
                    }
                }

                out.println("    <div class=\"msg-avatar\"><a href=\"/" + uname + "/\"><img src=\"http://i.juick.com/a/" + uid + ".png\" alt=\"" + uname + "\"/></a></div>");
                out.println("    <div class=\"msg-ts\"><a href=\"/" + uname + "/" + mid + "\">" + PageTemplates.formatDate(rs.getInt(8), rs.getString(9), locale) + "</a><div class=\"msg-menu\"><a href=\"#\" onclick=\"$('#msg-menu-" + mid + "').toggle('blind'); return false\"><img src=\"http://static.juick.com/message-menu-icon.png\"></a><ul id=\"msg-menu-" + mid + "\">");
                out.println("      <li><a href=\"/post?body=%21%20%23" + mid + "\">" + rb.getString("Recommend message") + "</a></li>");
                out.println("      <li><a href=\"/post?body=%40" + uname + "%20\">" + rb.getString("Send private message") + "</a></li>");
                out.println("      <li><a href=\"/post?body=BL%20%40" + uname + "\">" + rb.getString("Block user") + "</a></li>");
                out.println("    </ul></div></div>");
                out.println("    <div class=\"msg-header\"><a href=\"/" + uname + "/\">@" + uname + "</a>:" + tags + "</div>");
                out.println("    <div class=\"msg-txt\">" + txt + "</div>");

                if (cancomment) {
                    out.println("    <form action=\"/post\" method=\"POST\" enctype=\"multipart/form-data\"><input type=\"hidden\" name=\"mid\" value=\"" + mid + "\"/>");
                    out.println("      <div class=\"msg-comment\"><textarea name=\"body\" rows=\"1\" class=\"reply\" placeholder=\"Add a comment...\" onkeypress=\"postformListener(this.form,event)\"></textarea><input type=\"submit\" value=\"OK\"/></div>");
                    out.println("    </form>");
                }

                out.println("  </li>");
                out.println("</ul>");
            }
        } catch (SQLException e) {
            System.err.println(e);
        } finally {
            Utils.finishSQL(rs, stmt);
        }

    }

    public static void printReplies(PrintWriter out, Connection sql, int mid, Locale locale, boolean listview) {
        ResourceBundle rbuser = ResourceBundle.getBundle("User", locale);
        ArrayList<com.juick.Message> replies = new ArrayList<com.juick.Message>();

        PreparedStatement stmt = null;
        ResultSet rs = null;
        
        boolean added = false;
        
        int i = 0
        
        try {
            stmt = sql.prepareStatement("SELECT replies.reply_id,replies.replyto,replies.user_id,users.nick,replies.txt,TIMESTAMPDIFF(MINUTE,replies.ts,NOW()),replies.ts,replies.attach FROM replies INNER JOIN users ON replies.user_id=users.id WHERE replies.message_id=? ORDER BY replies.reply_id ASC");
            stmt.setInt(1, mid);
            rs = stmt.executeQuery();
            rs.beforeFirst();
            while (rs.next()) {
                com.juick.Message msg = new com.juick.Message();
                msg.MID = mid;
                msg.RID = rs.getInt(1);
                msg.ReplyTo = rs.getInt(2);
                msg.User = new com.juick.User();
                msg.User.UID = rs.getInt(3);
                msg.User.UName = rs.getString(4);
                msg.Text = PageTemplates.formatMessage(rs.getString(5));
                msg.MinutesAgo = rs.getInt(6);
                msg.TimestampString = rs.getString(7);
                msg.AttachmentType = rs.getString(8);

                replies.add(msg);

                if (msg.ReplyTo > 0) {
                    added = false;
                    for (i = 0; i < replies.size(),added != true; i++) {  
                            if(replies.get(i).RID == msg.ReplyTo)
                            {
                                replies.get(i).childs.add(msg);
                                added = true;
                            }
                            //break;
                    }
                    if (added == false) {
                        msg.ReplyTo = 0;
                    }
                }
            }
        } catch (SQLException e) {
            System.err.println(e);
        } finally {
            Utils.finishSQL(rs, stmt);
        }

        if (!replies.isEmpty()) {

            out.println("<div class=\"title2\">");
            out.print("  <div class=\"title2-right\">");
            if (listview) {
                out.print("<a href=\"?view=tree\">" + rbuser.getString("View as tree") + "</a>");
            } else {
                out.print("<a href=\"#\" onclick=\"$('#replies>li').show(); $('#replies .msg-comments').hide(); return false\">" + rbuser.getString("Expand all") + "</a> &#183; <a href=\"?view=list\">" + rbuser.getString("View as list") + "</a>");
            }
            out.print("</div>");
            out.println("  <h2>Replies (" + replies.size() + ")</h2>");
            out.println("</div>");

            out.println("<ul id=\"replies\">");
            if (listview) {
                printList(out, replies, locale);
            } else {
                printTree(out, replies, locale);
            }
            out.println("</ul>");
        }

        for (int i = 0; i < replies.size(); i++) {
            replies.get(i).cleanupChilds();
        }
        replies.clear();
    }

    public static void printTree(PrintWriter out, ArrayList<com.juick.Message> replies,  Locale locale, int ReplyTo = 0, int margin = 0) {
        ResourceBundle rb = ResourceBundle.getBundle("Global", locale);

        for (int i = 0; i < replies.size(); i++) {
            com.juick.Message msg = replies.get(i);
            if (msg.ReplyTo == ReplyTo) {

                out.print("  <li id=\"" + msg.RID + "\" class=\"msg\" style=\"");
                if (i == 0) {
                    out.print("border: 0;");
                }
                if (margin > 0) {
                    out.print("margin-left: " + margin + "px;display:none;");
                }
                out.println("\">");
                if (msg.AttachmentType != null) {
                    if (msg.AttachmentType.equals("jpg")) {
                        out.println("    <div class=\"msg-media\"><img src=\"http://i.juick.com/photos-512/" + msg.MID + "-" + msg.RID + ".jpg\" alt=\"\"/></div>");
                    } else {
                        out.println("    <div class=\"msg-media\"><div id=\"video-" + msg.MID + "-" + msg.RID + "\"><b>Attachment: <a href=\"http://i.juick.com/video/" + msg.MID + "-" + msg.RID + ".mp4\">Video</a></b></div></div>");
                        out.println("    <script type=\"text/javascript\">");
                        out.println("    inlinevideo('" + msg.MID + "-" + msg.RID + "');");
                        out.println("    </script>");
                    }
                }
                out.println("    <div class=\"msg-avatar\"><a href=\"/" + msg.User.UName + "/\"><img src=\"http://i.juick.com/a/" + msg.User.UID + ".png\" alt=\"" + msg.User.UName + "\"/></a></div>");
                out.println("    <div class=\"msg-ts\"><a href=\"/" + msg.MID + "#" + msg.RID + "\">" + PageTemplates.formatDate(msg.MinutesAgo, msg.TimestampString, locale) + "</a><div class=\"msg-menu\"><a href=\"#\" onclick=\"$('#msg-menu-" + msg.MID + "-" + msg.RID + "').toggle('blind'); return false\"><img src=\"http://static.juick.com/message-menu-icon.png\"/></a><ul id=\"msg-menu-" + msg.MID + "-" + msg.RID + "\">");
                out.println("      <li><a href=\"/post?body=%40" + msg.User.UName + "%20\">" + rb.getString("Send private message") + "</a></li>");
                out.println("      <li><a href=\"/post?body=BL%20%40" + msg.User.UName + "\">" + rb.getString("Block user") + "</a></li>");
                out.println("    </ul></div></div>");
                out.println("    <div class=\"msg-header\"><a href=\"/" + msg.User.UName + "/\">@" + msg.User.UName + "</a>:</div>");
                out.println("    <div class=\"msg-txt\">" + msg.Text + "</div>");
                out.println("    <div class=\"msg-links\"><a href=\"#\" onclick=\"return showCommentForm(" + msg.MID + "," + msg.RID + ")\">" + rb.getString("Comment") + "</a></div>");
                out.println("    <div class=\"msg-comment\" style=\"display: none\"></div>");
                if (ReplyTo == 0) {
                    int childs = msg.getChildsCount() - 1;
                    if (childs > 0) {
                        out.println("    <div class=\"msg-comments\"><a href=\"#\" onclick=\"return showMoreReplies(" + msg.RID + ")\">" + PageTemplates.formatReplies(childs, locale) + " more</a></div>");
                    }
                }
                out.println("  </li>");

                printTree(out, replies, msg.RID, margin + 20, locale);
            }
        }
    }

    public static void printList(PrintWriter out, ArrayList<com.juick.Message> replies, Locale locale) {
        ResourceBundle rb = ResourceBundle.getBundle("Global", locale);

        for (int i = 0; i < replies.size(); i++) {
            com.juick.Message msg = replies.get(i);

            out.print("  <li id=\"" + msg.RID + "\" class=\"msg\"" + (i == 0 ? " style=\"border: 0\"" : "") + ">");
            if (msg.AttachmentType != null) {
                if (msg.AttachmentType.equals("jpg")) {
                    out.println("    <div class=\"msg-media\"><img src=\"http://i.juick.com/photos-512/" + msg.MID + "-" + msg.RID + ".jpg\" alt=\"\"/></div>");
                } else {
                    out.println("    <div class=\"msg-media\"><div id=\"video-" + msg.MID + "-" + msg.RID + "\"><b>Attachment: <a href=\"http://i.juick.com/video/" + msg.MID + "-" + msg.RID + ".mp4\">Video</a></b></div></div>");
                    out.println("    <script type=\"text/javascript\">");
                    out.println("    inlinevideo('" + msg.MID + "-" + msg.RID + "');");
                    out.println("    </script>");
                }
            }
            out.println("    <div class=\"msg-avatar\"><a href=\"/" + msg.User.UName + "/\"><img src=\"http://i.juick.com/a/" + msg.User.UID + ".png\"></a></div>");
            out.println("    <div class=\"msg-ts\"><a href=\"/" + msg.MID + "#" + msg.RID + "\">" + PageTemplates.formatDate(msg.MinutesAgo, msg.TimestampString, locale) + "</a><div class=\"msg-menu\"><a href=\"#\" onclick=\"$('#msg-menu-" + msg.MID + "-" + msg.RID + "').toggle('blind'); return false\"><img src=\"http://static.juick.com/message-menu-icon.png\"></a><ul id=\"msg-menu-" + msg.MID + "\">");
            out.println("      <li><a href=\"/post?body=%40" + msg.User.UName + "%20\">" + rb.getString("Send private message") + "</a></li>");
            out.println("      <li><a href=\"/post?body=BL%20%40" + msg.User.UName + "\">" + rb.getString("Block user") + "</a></li>");
            out.println("    </ul></div></div>");
            out.println("    <div class=\"msg-header\"><a href=\"/" + msg.User.UName + "/\">@" + msg.User.UName + "</a>:</div>");
            out.println("    <div class=\"msg-txt\">" + msg.Text + "</div>");
            out.print("    <div class=\"msg-links\">/" + msg.RID);
            if (msg.ReplyTo > 0) {
                out.print(" " + rb.getString("in reply to") + " <a href=\"#" + msg.ReplyTo + "\">/" + msg.ReplyTo + "</a>");
            }
            out.println(" &#183; <a href=\"#\" onclick=\"return showCommentForm(" + msg.MID + "," + msg.RID + ")\">" + rb.getString("Comment") + "</a></div>");
            out.println("    <div class=\"msg-comment\" style=\"display: none\"></div>");
            out.println("  </li>");
        }
    }
}
