package chess.qa;

import java.io.File;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import javax.xml.parsers.DocumentBuilderFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * Читает XML-отчёт JaCoCo и сохраняет метрики кода в последний запуск:
 * покрытие инструкций, ветвлений и строк, цикломатическую сложность.
 */
public final class MetricsRecorder {

    /** Счётчик JaCoCo [missed, covered] у элемента (только прямые потомки). */
    static int[] counter(Element parent, String type) {
        NodeList list = parent.getChildNodes();
        for (int i = 0; i < list.getLength(); i++) {
            Node n = list.item(i);
            if (n instanceof Element e && e.getTagName().equals("counter")
                    && e.getAttribute("type").equals(type)) {
                return new int[] {Integer.parseInt(e.getAttribute("missed")),
                                  Integer.parseInt(e.getAttribute("covered"))};
            }
        }
        return new int[] {0, 0};
    }

    static double percent(int[] c) {
        int total = c[0] + c[1];
        return total == 0 ? 100.0 : Math.round(1000.0 * c[1] / total) / 10.0;
    }

    public static void main(String[] args) throws Exception {
        var f = DocumentBuilderFactory.newInstance();
        f.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
        Document doc = f.newDocumentBuilder().parse(new File(args[0]));
        Element report = doc.getDocumentElement();

        double instr = percent(counter(report, "INSTRUCTION"));
        double branch = percent(counter(report, "BRANCH"));
        double line = percent(counter(report, "LINE"));
        int[] cx = counter(report, "COMPLEXITY");

        try (Connection c = Db.connect()) {
            long runId;
            try (Statement s = c.createStatement();
                 ResultSet r = s.executeQuery("SELECT MAX(id) FROM test_run")) {
                r.next();
                runId = r.getLong(1);
            }
            int maxCx = 0;
            try (PreparedStatement ps = c.prepareStatement(
                    "INSERT INTO method_metric VALUES (?, ?, ?, ?, ?)")) {
                NodeList classes = report.getElementsByTagName("class");
                for (int i = 0; i < classes.getLength(); i++) {
                    Element cls = (Element) classes.item(i);
                    String cname = cls.getAttribute("name").replace('/', '.');
                    NodeList methods = cls.getElementsByTagName("method");
                    for (int j = 0; j < methods.getLength(); j++) {
                        Element m = (Element) methods.item(j);
                        int[] mc = counter(m, "COMPLEXITY");
                        int complexity = mc[0] + mc[1];
                        maxCx = Math.max(maxCx, complexity);
                        ps.setLong(1, runId);
                        ps.setString(2, cname);
                        ps.setString(3, m.getAttribute("name"));
                        ps.setInt(4, complexity);
                        ps.setDouble(5, percent(counter(m, "LINE")));
                        ps.addBatch();
                    }
                }
                ps.executeBatch();
            }
            try (PreparedStatement ps = c.prepareStatement("""
                    UPDATE test_run SET instruction_coverage = ?, branch_coverage = ?,
                           line_coverage = ?, complexity = ?, max_complexity = ?
                    WHERE id = ?""")) {
                ps.setDouble(1, instr);
                ps.setDouble(2, branch);
                ps.setDouble(3, line);
                ps.setInt(4, cx[0] + cx[1]);
                ps.setInt(5, maxCx);
                ps.setLong(6, runId);
                ps.executeUpdate();
            }
            System.out.printf("Метрики запуска #%d: инструкции %.1f%%, ветвления %.1f%%, "
                + "строки %.1f%%, сложность %d (макс. у метода %d)%n",
                runId, instr, branch, line, cx[0] + cx[1], maxCx);
        }
    }
}
