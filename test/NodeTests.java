import de.skyrising.xmpp.xml.XMLNode;
import org.junit.Assert;
import org.junit.Test;

public class NodeTests {

    @Test
    public void testNodeEquality() {
        XMLNode node1 = new XMLNode("type");
        XMLNode node2 = new XMLNode("type");
        XMLNode node3 = new XMLNode("other");
        Assert.assertEquals(node1, node2);
        Assert.assertNotEquals(node1, node3);
        Assert.assertNotEquals(node2, node3);
        node1.setAttribute("attr", "value");
        Assert.assertNotEquals(node1, node2);
        node2.setAttribute("attr", "value");
        Assert.assertEquals(node1, node2);
    }
}
