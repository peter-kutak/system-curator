package com.github.peter_kutak;

import com.sun.jna.LastErrorException;
import com.sun.jna.NativeLong;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.URLEncoder;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.function.Consumer;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.apache.commons.codec.DecoderException;
import org.apache.commons.codec.binary.Hex;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.tuple.Pair;

/**
 *
 */
@SuppressWarnings("serial")
@WebServlet("/journal")
public class JournalServlet extends HttpServlet {

  static class MenuComparator implements Comparator {
    public int compare(Object o1, Object o2) {
      return ((Map.Entry<String, String>)o1).getValue().compareTo(((Map.Entry<String, String>)o2).getValue());
    }
  }

  /**
   *
   */
  static byte[] initByteArray(String data) {
    byte [] result = null;
    try {
      result = Hex.decodeHex(data);
    } catch (DecoderException e) {
      //toto sa nestane
    }
    return result;
  }

  static String encodeURIComponent(String value) {
    //aj tak to nekoduje podla rfc3986 takze to volam zbytocne
    String result = null;
    try {
      result = URLEncoder.encode(value, "UTF-8");
    } catch (java.io.UnsupportedEncodingException e) {
      //toto sa nestane
    }
    return result;
  }

  //my app generated uuid ce74fb5f-fa74-4968-979f-824c61695ecc
  static String APP_ID = "ce74fb5f-fa74-4968-979f-824c61695ecc";

  static String STYLE_DARK = ""
      + "* {"
      +           "background-color: dimgray;"
      +           "color: lightgray;"
      +           "text-shadow: 1px 1px 2px #000;}"
      + "th {background-color: black;}"
      + ".tablerow tr:nth-child(odd) td {background-color: #1f1f1f;}"
      + ".tablerow tr:hover td {background-color: #0f0f0f;}"
      + ".tablerow td {"
      +      "unicode-bidi: embed;"
      +      "font-family: monospace;"
      +      "font-size: 120%;"
      +      "white-space: pre;"
      +      "}"
      + "input, select, button {"
      +           "border-color: black; color:black; "
      +           "text-shadow: 1px 1px 2px lightgray; "
      +           "background-color: gray;"
      +           "}"
      + "pre {background-color: inherit;"
      +           "}";

  static String STYLE_LIGHT = "";
  static String PAGE_HEADER = "<html><head><title>journal</title>"
      + "<style type=text/css>" + STYLE_DARK + "</style>"
      + "</head><body>";

  static String PAGE_FOOTER = "</body></html>";

  /**
   *
   */
  private String getParam(HttpServletRequest req, String name) {
    Map<String, String[]> parameters = req.getParameterMap();
    if (parameters == null) { return null; }
    String[] a =  parameters.get(name);
    if (a == null) { return null; }
    return a[0];
  }

  /**
   *
   */
  String priority(String prio) {
    //CODE PRIORITY SEVERITY
    //0    emerg    System is unusable
    //1    alert    Action must be taken immediately
    //2    crit     Critical condition
    //3    err      Non-critical error condition
    //4    warning  Warning condition
    //5    notice   Normal but significant event
    //6    info     Informational event
    //7    debug    Debugging-level message
    int p = Integer.parseInt(prio);
    if (p == 6) { return "info"; }
    if (p == 5) { return "notice"; }
    if (p == 4) { return "warning"; }
    if (p == 3) { return "err"; }
    if (p == 2) { return "crit"; }
    if (p == 1) { return "alert"; }
    if (p == 0) { return "emerg"; }
    if (p == 7) { return "debug"; }
    return "unkn";
  }

  /**
   *
   */
  void printTableRow(PrintWriter writer, Journal j, ZoneId tz, DateTimeFormatter tf, String href) {
    writer.println("<table class=\"tablerow\">");
    writer.println("<tr><th>wallclock</th><th>priority</th><th>hostname</th><th>message</th></tr>");
    long count = 0;
    boolean cont = false;
    //#define SD_JOURNAL_FOREACH(j)
    if (j.seek_head() < 0) {
      //nothing to do
    } else {
      while (j.next() > 0) {
        writer.print("<tr><td>");
        String cursor = j.get_cursor();
        System.out.println(cursor);
        writer.print("<a href=\"" + href + "&cursor=" + encodeURIComponent(cursor) + "\">");
        writer.print(j.get_realtime().atZone(tz).format(tf));
        writer.print("</a>");
        writer.print("</td><td>");
        writer.print(priority(j.get_data("PRIORITY")));
        writer.print("</td><td>");
        writer.print(j.get_data("_HOSTNAME"));
        writer.print("</td><td>");
        writer.print(j.get_data("MESSAGE"));
        writer.println("</td></tr>");
        count++;
        if (count > 20) {
          cont = true;
          break;
        }
      }
    }
    writer.println("</table>");
    if (cont) {
      writer.println("to be continued");
    }
  }

  /**
   *
   */
  Map<String, String> menuMachine(Journal j, String field) {
    Map<String, String> m = new TreeMap<String, String>();
    for (String i : j.query_unique(field)) {
      m.put(i, j.getMachineHostname(i));
    }
    return m;
  }

  /**
   *
   */
  Map<String, String> menuBoot(Journal j, String field, ZoneId tz, DateTimeFormatter tf) {
    Map<String, String> m = new TreeMap<String, String>();
    for (String i : j.query_unique(field)) {
      m.put(i, j.getBootBegin(i).atZone(tz).format(tf));
    }
    return m;
  }

  /**
   *
   */
  Map<String, String> uniqueList(Journal j, String field) {
    Map<String, String> m = new TreeMap<String, String>();
    for (String i : j.query_unique(field)) {
      m.put(i, i);
    }
    return m;
  }

  /**
   *
   */
  SortedMap<String, List<String>> uniqueMap(Journal j, String field) {
    SortedMap<String, List<String>> m = new TreeMap<String, List<String>>();
    for (String i : j.query_unique(field)) {
      String e = FilenameUtils.getExtension(i);
      List l = m.get(e);
      if (l == null) {
        l = new LinkedList<String>();
        m.put(e, l);
      }
      l.add(i);
    }
    return m;
  }

  /**
   *
   */
  void printMenuList(PrintWriter writer, Collection<Map.Entry<String, String>> m, String sel, String href) {
    List<Map.Entry<String, String>> sm = new LinkedList(m);
    sm.sort(new MenuComparator());
    for (Map.Entry<String, String> k : sm) {
      writer.println("[" + "<a href=\"" + href + "sel=" + Id128.hmac256(k.getKey(), APP_ID) + "\">"
          + k.getValue() + "</a>]");
      writer.println("<br/>");
    }
  }

  /**
   *
   */
  void printMenuTree(PrintWriter writer, SortedMap<String, List<String>> m, String sel, String href) {
    for (String k : m.keySet()) {
      writer.println("[" + "<a href=\"" + href + "sel=" + k + "\">" + k + "</a>]");
      writer.println("<br/>");
      if (k.equals(sel)) {
        List<String> l = m.get(k);
        Collections.sort(l, Comparator.naturalOrder());
        for (String v : l) {
          String label;
          if ("mount".equals(k)) {
            label = SdUtils.unescapeSdPath(v);
          } else {
            label = v;
          }
          writer.println(" - <a href=\"" + href + "sel=" + k + "&unit=" + v + "\">"
              + label + "</a>");
          writer.println("<br/>");
        }
      }
    }
  }

  /**
   *
   */
  void printCursor(PrintWriter writer, Journal j, String cursor) {
    writer.println("<table>");
    for (Pair<String, String> i : j.enumerate_available_data(cursor)) {
      writer.print("<tr>");
      writer.print("<td>");
      writer.print(i.getLeft());
      writer.print("</td>");
      writer.print("<td>");
      writer.print(i.getRight());
      writer.print("</td>");
      writer.print("</tr>");
    }
    writer.println("</table>");
  }

  /**
   *
   */
  void tabAll(HttpServletRequest req, PrintWriter writer, Journal j, ZoneId tz,
      DateTimeFormatter tf) {
    String href = "?tab=all";
    j.flush_matches();
    printTableRow(writer, j, tz, tf, href);
    String cursor = getParam(req, "cursor");
    if (cursor != null) {
      printCursor(writer, j, cursor);
    }
  }


  /**
   * Find original ID by ID secured with APP_ID.
   */
  String findOrigId(Iterable<String> ids, String securedid) {
    String result = null;
    for(String id : ids) {
      if (securedid.equals(Id128.hmac256(id, APP_ID).toString())) {
        return id;
      }
    }
    return result;
  }

  /**
   *
   */
  void tabMachine(HttpServletRequest req, PrintWriter writer, Journal j, ZoneId tz,
      DateTimeFormatter tf) {
    String href = "?tab=machine&";
    Map<String, String> m = menuMachine(j, "_MACHINE_ID");
    String sel = getParam(req, "sel");
    j.flush_matches();
    if (sel != null) {
      String origId = findOrigId(m.keySet(), sel);
      String filter = "_MACHINE_ID=" + origId;
      j.add_match(filter);
    }
    writer.println("<TABLE>");
    writer.println("<TD>");
    printMenuList(writer, m.entrySet(), sel, href);
    writer.println("</TD>");
    writer.println("<TD>");
    printTableRow(writer, j, tz, tf, href);
    writer.println("</TD>");
    writer.println("</TABLE>");
    String cursor = getParam(req, "cursor");
    if (cursor != null) {
      printCursor(writer, j, cursor);
    }
  }

  /**
   *
   */
  void tabBoot(HttpServletRequest req, PrintWriter writer, Journal j, ZoneId tz,
      DateTimeFormatter tf) {
    String href = "?tab=boot&";
    Map<String, String> m = menuBoot(j, "_BOOT_ID", tz, tf);
    String sel = getParam(req, "sel");
    j.flush_matches();
    if (sel != null) {
      String origId = findOrigId(m.keySet(), sel);
      String filter = "_BOOT_ID=" + origId;
      j.add_match(filter);
    }
    writer.println("<TABLE>");
    writer.println("<TD>");
    printMenuList(writer, m.entrySet(), sel, href);
    writer.println("</TD>");
    writer.println("<TD>");
    printTableRow(writer, j, tz, tf, href);
    writer.println("</TD>");
    writer.println("</TABLE>");
    String cursor = getParam(req, "cursor");
    if (cursor != null) {
      printCursor(writer, j, cursor);
    }
  }

  /**
   *
   */
  void tabUnit(HttpServletRequest req, PrintWriter writer, Journal j, ZoneId tz,
      DateTimeFormatter tf) {
    String href = "?tab=unit&";
    SortedMap<String, List<String>> m = uniqueMap(j, "UNIT");
    writer.println("<TABLE>");
    writer.println("<TD>");
    String unitgroup = getParam(req, "sel");
    for (String k : m.keySet()) {
      writer.println("[" + "<a href=\"" + href + "sel=" + k + "\">" + k + "</a>]");
      writer.println("<br/>");
      if (k.equals(unitgroup)) {
        List<String> l = m.get(k);
        Collections.sort(l, Comparator.naturalOrder());
        for (String v : l) {
          String label;
          if ("mount".equals(k)) {
            label = SdUtils.unescapeSdPath(v);
          } else {
            label = v;
          }
          writer.println(" - <a href=\"" + href + "sel=" + k + "&unit=" + v + "\">" + label + "</a>");
          writer.println("<br/>");
        }
      }
    }
    writer.println("</TD>");
    writer.println("<TD>");
    String unit = getParam(req, "unit");
    if (unit != null) {
      String filter = "UNIT=" + unit;
      j.flush_matches();
      j.add_match(filter);
      printTableRow(writer, j, tz, tf, href + "sel=" + unitgroup + "&unit=" + unit);
    }
    writer.println("</TD>");
    writer.println("</TABLE>");
    String cursor = getParam(req, "cursor");
    if (cursor != null) {
      printCursor(writer, j, cursor);
    }
  }

  /**
   *
   */
  @Override
  protected void doGet(HttpServletRequest req, HttpServletResponse resp) 
      throws ServletException, IOException {
    Map<String, String[]> parameters = req.getParameterMap();
    resp.setContentType("text/html");
    PrintWriter writer = resp.getWriter();
    writer.println(PAGE_HEADER);
    writer.println("<h1>Journal</h1>");
//    writer.println("<pre>");
//    writer.println("boot id    :" + Id128.get_boot_id());
//    writer.println("boot id app:" + Id128.get_boot_app_specific(APP_ID));
//    writer.println("app id     :" + APP_ID);
//    String machine_id = Id128.get_machine_id();
//    writer.println("machine    :" + machine_id);
//    writer.println("machine app:" + Id128.get_machine_app_specific(APP_ID));
//    writer.println("machine app:" + Id128.hmac256(machine_id, APP_ID));
//    writer.println("</pre>");

    writer.println("[<a href=\"?tab=all\">all]</a>");
    writer.println("[<a href=\"?tab=machine\">machine</a>]");
    writer.println("[<a href=\"?tab=boot\">boot</a>]");
    writer.println("[<a href=\"?tab=unit\">unit</a>]");
    writer.println("<hr/>");
    writer.println("<br/>");
    ZoneId tz = ZoneId.of("Europe/Bratislava");
    DateTimeFormatter tf = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSSSSS");
    Journal j = new Journal();
    j.open();

    for (String field : j.enumerate_fields()) {
       //writer.println("" + field + "<br/>");
    }

    String tab = getParam(req, "tab");
    if ("machine".equals(tab)) {
      tabMachine(req, writer, j, tz, tf);
    } else if ("boot".equals(tab)) {
      tabBoot(req, writer, j, tz, tf);
    } else if ("unit".equals(tab)) {
      tabUnit(req, writer, j, tz, tf);
    } else {
      tabAll(req, writer, j, tz, tf);
    }

    j.close();
    writer.println(PAGE_FOOTER);
    writer.close();
  }

}

