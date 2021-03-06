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

import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Locale;
import java.util.ResourceBundle;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.servlet.http.HttpServletRequest;
import ru.sape.Sape;

/**
 *
 * @author Ugnich Anton
 */
public class PageTemplates {

    public static Sape sape = null;

    public static void pageHead(PrintWriter out, String title, String headers) {
        out.println("<!DOCTYPE html>");
        out.println("<html>");
        out.println("<head>");
        out.println("  <meta charset=\"utf-8\"/>");
        out.println("  <title>" + title + "</title>");
        out.println("  <link rel=\"stylesheet\" href=\"http://static.juick.com/style3.css?20120306\"/>");
        out.println("  <link rel=\"icon\" type=\"image/png\" href=\"http://static.juick.com/favicon.png\"/>");
        out.println("  <script type=\"text/javascript\" src=\"https://ajax.googleapis.com/ajax/libs/jquery/1.6.4/jquery.min.js\"></script>");
        out.println("  <script type=\"text/javascript\" src=\"https://ajax.googleapis.com/ajax/libs/jqueryui/1.8.16/jquery-ui.min.js\"></script>");
        out.println("  <script type=\"text/javascript\" src=\"https://ajax.googleapis.com/ajax/libs/swfobject/2.2/swfobject.js\"></script>");
        out.println("  <script type=\"text/javascript\" src=\"http://static.juick.com/scripts3.js?20120306\"></script>");
        out.println("  <script type=\"text/javascript\" src=\"http://static.juick.com/js/jquery.autoresize.js\"></script>");
        if (headers != null) {
            out.println(headers);
        }
        out.println("</head>");
        out.println();
        out.println("<body>");
    }

    public static void pageNavigation(PrintWriter out, Locale loc, com.juick.User user) {
        ResourceBundle rb = ResourceBundle.getBundle("Global", loc);
        out.println("<div id=\"header\">");
        out.println("<div id=\"logo\"><a href=\"" + (user != null ? "/?show=my" : "/") + "\"><img src=\"http://static.juick.com/logo3.png\" width=\"120\" height=\"40\" alt=\"Juick\"/></a></div>");
        out.println("  <ul id=\"nav\">");
        out.println("    <li><a href=\"/\">" + rb.getString("Blogs") + "</a></li>");
//        out.println("    <li><a href=\"/chats\">" + rb.getString("Chats") + "</a></li>");
        out.println("    <li><a href=\"/photos\">" + rb.getString("Photos") + "</a></li>");
        out.println("    <li><a href=\"/map\">" + rb.getString("Map") + "</a></li>");
        out.println("  </ul>");
        out.println("  <ul id=\"nav-right\">");
        if (user != null) {
            out.println("    <li><a href=\"/post\">" + rb.getString("Post") + "</a></li>");
            out.println("    <li><a href=\"#\" onclick=\"$('#nav-menu').toggle('blind'); return false\"><img src=\"http://i.juick.com/as/" + user.UID + ".png\" alt=\"" + user.UName + "\"/>" + user.UName + "</a><ul id=\"nav-menu\">");
            out.println("      <li><a href=\"/" + user.UName + "/\">" + rb.getString("Blog") + "</a></li>");
            out.println("      <li><a href=\"/settings\">" + rb.getString("Settings") + "</a></li>");
            out.println("      <li><a href=\"/logout\">" + rb.getString("Logout") + "</a></li>");
            out.println("    </ul></li>");
        } else {
            out.println("    <li><a href=\"/login\">" + rb.getString("Login") + "</a></li>");
        }
        out.println("   </ul>");
        out.println("</div>");
    }

    public static void pageTitle(PrintWriter out, String title) {
        out.println("<div id=\"title\">");
        out.println("  <h1>" + title + "</h1>");
        out.println("</div>");
    }

    public static void pageUserTitle(PrintWriter out, Connection sql, Locale loc, com.juick.User user, com.juick.User visitor) {
        ResourceBundle rb = ResourceBundle.getBundle("User", loc);

        // Full name and description
        String fullname = null;
        String description = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;
        try {
            stmt = sql.prepareStatement("SELECT fullname,descr FROM usersinfo WHERE user_id=?");
            stmt.setInt(1, user.UID);
            rs = stmt.executeQuery();
            if (rs.first()) {
                fullname = rs.getString(1) + " (" + user.UName + ")";
                description = rs.getString(2);
            }
        } catch (SQLException e) {
            System.err.println(e);
        } finally {
            Utils.finishSQL(rs, stmt);
        }
        if (fullname == null) {
            fullname = user.UName;
        }
        if (description == null) {
            description = "";
        }

        // I read
        int iread = 0;
        try {
            stmt = sql.prepareStatement("SELECT COUNT(*) FROM subscr_users WHERE suser_id=?");
            stmt.setInt(1, user.UID);
            rs = stmt.executeQuery();
            if (rs.first()) {
                iread = rs.getInt(1);
            }
        } catch (SQLException e) {
            System.err.println(e);
        } finally {
            Utils.finishSQL(rs, stmt);
        }

        // My readers
        int myreaders = 0;
        try {
            stmt = sql.prepareStatement("SELECT COUNT(*) FROM subscr_users WHERE user_id=?");
            stmt.setInt(1, user.UID);
            rs = stmt.executeQuery();
            if (rs.first()) {
                myreaders = rs.getInt(1);
            }
        } catch (SQLException e) {
            System.err.println(e);
        } finally {
            Utils.finishSQL(rs, stmt);
        }

        // Messages
        int messages = 0;
        try {
            stmt = sql.prepareStatement("SELECT COUNT(*) FROM messages WHERE user_id=?");
            stmt.setInt(1, user.UID);
            rs = stmt.executeQuery();
            if (rs.first()) {
                messages = rs.getInt(1);
            }
        } catch (SQLException e) {
            System.err.println(e);
        } finally {
            Utils.finishSQL(rs, stmt);
        }

        // Replies
        int replies = 0;
        try {
            stmt = sql.prepareStatement("SELECT COUNT(*) FROM replies WHERE user_id=?");
            stmt.setInt(1, user.UID);
            rs = stmt.executeQuery();
            if (rs.first()) {
                replies = rs.getInt(1);
            }
        } catch (SQLException e) {
            System.err.println(e);
        } finally {
            Utils.finishSQL(rs, stmt);
        }

        out.println("<div id=\"title\"><div id=\"title-container\">");
        out.println("  <div id=\"title-av\"><a href=\"/" + user.UName + "/\"><img src=\"http://i.juick.com/a/" + user.UID + ".png\" width=\"96\" height=\"96\" alt=\"" + user.UName + "\"/></a></div>");
        out.println("  <div id=\"title-stats\"><ul>");
        out.println("    <li><a href=\"/" + user.UName + "/friends\">" + rb.getString("(Stats) I read") + ": " + iread + "</a></li>");
        out.println("    <li><a href=\"/" + user.UName + "/readers\">" + rb.getString("(Stats) My readers") + ": " + myreaders + "</a></li>");
        out.println("    <li>" + rb.getString("(Stats) Messages") + ": " + messages + "</li>");
        out.println("    <li>" + rb.getString("(Stats) Replies") + ": " + replies + "</li>");
        out.println("  </ul></div>");
        out.println("  <div id=\"title-username\"><h1>" + fullname + "</h1><p>" + description + "</p></div>");
        out.println("</div></div>");
        out.println();
    }

    public static void pageFooter(HttpServletRequest request, PrintWriter out, Locale loc, com.juick.User visitor) {
        ResourceBundle rb = ResourceBundle.getBundle("Global", loc);
        out.println("<div id=\"fwrapper\"><div id=\"footer\">");
        out.println("  <div id=\"footer-right\"><a href=\"/help/contacts\">" + rb.getString("Contacts") + "</a> &#183; <a href=\"/help/\">" + rb.getString("Help") + "</a></div>");
        out.print("  <div id=\"footer-left\">juick.com &copy; 2008-2012");
        if (sape != null && (visitor == null || visitor.UID == 1)) {
            String links = sape.getPageLinks(request.getRequestURI(), request.getCookies()).render();
            if (links != null && !links.isEmpty()) {
                out.print("<br/>" + rb.getString("Sponsored by") + ": " + links);
            }
        }
        out.println("</div>");
        out.println("</div></div>");
    }

    public static String formatTags(String tags) {
        String ret = "";
        String tagsarr[] = tags.split(" ");
        for (int i = 0; i < tagsarr.length; i++) {
            String tag = tagsarr[i];
            tag = tag.replaceAll("<", "&lt;");
            tag = tag.replaceAll(">", "&gt;");
            try {
                ret += " *<a href=\"/?tag=" + URLEncoder.encode(tagsarr[i], "utf-8") + "\">" + tag + "</a>";
            } catch (UnsupportedEncodingException e) {
            }
        }

        return ret;
    }

    public static String formatDate(int minsago, String fulldate, Locale loc) {
        if (minsago < 1) {
            return "now";
        } else if (minsago < 60) {
            return minsago + " minute" + ((minsago % 10 == 1) ? "" : "s") + " ago";
        } else if (minsago < 1440) {
            int hours = (minsago / 60);
            return hours + " hour" + ((hours % 10 == 1) ? "" : "s") + " ago";
        } else if (minsago < 20160) {
            int days = (minsago / 1440);
            return days + " day" + ((days % 10 == 1) ? "" : "s") + " ago";
        } else {
            return fulldate;
        }
    }

    public static String formatReplies(int replies, Locale loc) {
        return replies + " repl" + (replies % 10 == 1 ? "y" : "ies");
    }
    private static Pattern regexLinks2 = Pattern.compile("((?<=\\s)|(?<=\\A))([\\[\\{]|&lt;)((?:ht|f)tps?://(?:www\\.)?([^\\/\\s\\\"\\)\\!]+)/?(?:[^\\]\\}](?<!&gt;))*)([\\]\\}]|&gt;)");

    public static String formatMessage(String msg) {
        msg = msg.replaceAll("&", "&amp;");
        msg = msg.replaceAll("<", "&lt;");
        msg = msg.replaceAll(">", "&gt;");

        // --
        // &mdash;
        msg = msg.replaceAll("((?<=\\s)|(?<=\\A))\\-\\-?((?=\\s)|(?=\\Z))", "$1&mdash;$2");

        // http://juick.com/last?page=2
        // <a href="http://juick.com/last?page=2" rel="nofollow">juick.com</a>
        msg = msg.replaceAll("((?<=\\s)|(?<=\\A))((?:ht|f)tps?://(?:www\\.)?([^\\/\\s\\n\\\"]+)/?[^\\s\\n\\\"]*)", "$1<a href=\"$2\" rel=\"nofollow\">$3</a>");

        // #12345
        // <a href="http://juick.com/12345">#12345</a>
        msg = msg.replaceAll("((?<=\\s)|(?<=\\A)|(?<=\\p{Punct}))#(\\d+)((?=\\s)|(?=\\Z)|(?=\\))|(?=\\.)|(?=\\,))", "$1<a href=\"http://juick.com/$2\">#$2</a>$3");

        // #12345/65
        // <a href="http://juick.com/12345#65">#12345/65</a>
        msg = msg.replaceAll("((?<=\\s)|(?<=\\A)|(?<=\\p{Punct}))#(\\d+)/(\\d+)((?=\\s)|(?=\\Z)|(?=\\p{Punct}))", "$1<a href=\"http://juick.com/$2#$3\">#$2/$3</a>$4");

        // *bold*
        // <b>bold</b>
        msg = msg.replaceAll("((?<=\\s)|(?<=\\A)|(?<=\\p{Punct}))\\*([^\\*\\n<>]+)\\*((?=\\s)|(?=\\Z)|(?=\\p{Punct}))", "$1<b>$2</b>$3");

        // /italic/
        // <i>italic</i>
        msg = msg.replaceAll("((?<=\\s)|(?<=\\A))/([^\\/\\n<>]+)/((?=\\s)|(?=\\Z)|(?=\\p{Punct}))", "$1<i>$2</i>$3");

        // _underline_
        // <span class="u">underline</span>
        msg = msg.replaceAll("((?<=\\s)|(?<=\\A))_([^\\_\\n<>]+)_((?=\\s)|(?=\\Z)|(?=\\p{Punct}))", "$1<span class=\"u\">$2</span>$3");

        // /12
        // <a href="#12">/12</a>
        msg = msg.replaceAll("((?<=\\s)|(?<=\\A))\\/(\\d+)((?=\\s)|(?=\\Z)|(?=\\p{Punct}))", "$1<a href=\"#$2\">/$2</a>$3");

        // @username@jabber.org
        // <a href="http://juick.com/username@jabber.org/">@username@jabber.org</a>
        msg = msg.replaceAll("((?<=\\s)|(?<=\\A))@([\\w\\-\\.]+@[\\w\\-\\.]+)((?=\\s)|(?=\\Z)|(?=\\p{Punct}))", "$1<a href=\"http://juick.com/$2/\">@$2</a>$3");

        // @username
        // <a href="http://juick.com/username/">@username</a>
        msg = msg.replaceAll("((?<=\\s)|(?<=\\A))@([\\w\\-]{2,16})((?=\\s)|(?=\\Z)|(?=\\p{Punct}))", "$1<a href=\"http://juick.com/$2/\">@$2</a>$3");

        // (http://juick.com/last?page=2)
        // (<a href="http://juick.com/last?page=2" rel="nofollow">juick.com</a>)
        Matcher m = regexLinks2.matcher(msg);
        StringBuffer sb = new StringBuffer();
        while (m.find()) {
            String url = m.group(3).replace(" ", "%20").replaceAll("\\s+", "");
            m.appendReplacement(sb, "$1$2<a href=\"" + url + "\" rel=\"nofollow\">$4</a>$5");
        }
        m.appendTail(sb);
        msg = sb.toString();

        // > citate
        msg = msg.replaceAll("(?:(?<=\\n)|(?<=\\A))&gt; *(.*)?(\\n|(?=\\Z))", "<blockquote>$1</blockquote>");
        msg = msg.replaceAll("</blockquote><blockquote>", "\n");

        msg = msg.replaceAll("\n", "<br/>\n");
        return msg;
    }

    public static void printMessages(PrintWriter out, Connection sql, ArrayList<Integer> mids, Locale locale) {
        ResourceBundle rb = ResourceBundle.getBundle("Global", locale);

        PreparedStatement stmt = null;
        ResultSet rs = null;
        try {
            stmt = sql.prepareStatement("SELECT STRAIGHT_JOIN messages.message_id,messages.user_id,users.nick,messages_txt.tags,messages.readonly,messages.privacy,messages_txt.txt,TIMESTAMPDIFF(MINUTE,messages.ts,NOW()),messages.ts,messages.replies,messages_txt.repliesby,messages.attach,messages.place_id,places.name,messages.lat,messages.lon FROM ((messages INNER JOIN messages_txt ON messages.message_id=messages_txt.message_id) INNER JOIN users ON messages.user_id=users.id) LEFT JOIN places ON messages.place_id=places.place_id WHERE messages.message_id IN (" + Utils.convertArray2String(mids) + ") ORDER BY messages.message_id DESC");
            rs = stmt.executeQuery();
            rs.beforeFirst();
            while (rs.next()) {
                int mid = rs.getInt(1);
                int uid = rs.getInt(2);
                String uname = rs.getString(3);
                String tags = rs.getString(4);
                String txt = rs.getString(7);
                // timediff
                // timestamp
                // replies
                // 11 repliesby 
                // attach
                // pid
                // pname
                // lat
                // lon

                boolean cancomment = true;

                tags = (tags != null) ? formatTags(tags) : "";
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

                txt = formatMessage(txt);

                out.print("  <li id=\"msg-" + mid + "\" class=\"msg\"");
                if (mid == mids.get(0)) {
                    out.print(" style=\"border: 0\"");
                }
                out.println(">");

                if (rs.getString(12) != null) {
                    if (rs.getString(12).equals("jpg")) {
                        out.println("    <div class=\"msg-media\"><a href=\"http://i.juick.com/photos-1024/" + mid + ".jpg\"><img src=\"http://i.juick.com/photos-512/" + mid + ".jpg\" alt=\"\"/></a></div>");
                    } else {
                        out.println("    <div class=\"msg-media\"><div id=\"video-" + mid + "\"><b>Attachment: <a href=\"http://i.juick.com/video/" + mid + ".mp4\">Video</a></b></div></div>");
                        out.println("    <script type=\"text/javascript\">");
                        out.println("    inlinevideo(" + mid + ");");
                        out.println("    </script>");
                    }
                }

                out.println("    <div class=\"msg-avatar\"><a href=\"/" + uname + "/\"><img src=\"http://i.juick.com/a/" + uid + ".png\" alt=\"" + uname + "\"/></a></div>");
                out.println("    <div class=\"msg-ts\"><a href=\"/" + uname + "/" + mid + "\">" + formatDate(rs.getInt(8), rs.getString(9), locale) + "</a><div class=\"msg-menu\"><a href=\"#\" onclick=\"$('#msg-menu-" + mid + "').toggle('blind'); return false\"><img src=\"http://static.juick.com/message-menu-icon.png\"></a><ul id=\"msg-menu-" + mid + "\">");
                out.println("      <li><a href=\"/post?body=%21%20%23" + mid + "\">" + rb.getString("Recommend message") + "</a></li>");
                out.println("      <li><a href=\"/post?body=%40" + uname + "%20\">" + rb.getString("Send private message") + "</a></li>");
                out.println("      <li><a href=\"/post?body=BL%20%40" + uname + "\">" + rb.getString("Block user") + "</a></li>");
                out.println("    </ul></div></div>");
                out.println("    <div class=\"msg-header\"><a href=\"/" + uname + "/\">@" + uname + "</a>:" + tags + "</div>");
                out.println("    <div class=\"msg-txt\">" + txt + "</div>");

                if (rs.getInt(10) > 0) {
                    String repliesby = rs.getString(11);
                    if (repliesby == null) {
                        repliesby = "...";
                    }
                    out.println("    <div class=\"msg-comments\"><a href=\"/" + uname + "/" + mid + "\">" + formatReplies(rs.getInt(10), locale) + "</a> " + rb.getString("(replies) by") + " " + repliesby + "</div>");
                } else if (cancomment) {
                    out.println("    <form action=\"/post\" method=\"POST\" enctype=\"multipart/form-data\"><input type=\"hidden\" name=\"mid\" value=\"" + mid + "\"/>");
                    out.println("      <div class=\"msg-comment\"><textarea name=\"body\" rows=\"1\" class=\"reply\" placeholder=\"Add a comment...\" onkeypress=\"postformListener(this.form,event)\"></textarea><input type=\"submit\" value=\"OK\"/></div>");
                    out.println("    </form>");
                }
                out.println("  </li>");
            }
        } catch (SQLException e) {
            System.err.println(e);
        } finally {
            Utils.finishSQL(rs, stmt);
        }
    }
}
