import de.skyrising.xmpp.Jid;
import org.junit.Assert;
import org.junit.Test;

import java.text.ParseException;

public class JidTests {

    @Test
    public void testEmptyJid() {
        try {
            new Jid("");
        } catch (ParseException e) {
            return;
        }
        Assert.fail("Empty jid valid");
    }

    @Test
    public void testEmptyUserJid() {
        try {
            new Jid("@host/res");
        } catch (ParseException e) {
            return;
        }
        Assert.fail("Empty jid user valid");
    }

    @Test
    public void testEmptyHostJid() {
        try {
            new Jid("user@/res");
        } catch (ParseException e) {
            return;
        }
        Assert.fail("Empty jid host valid");
    }

    @Test
    public void testDoubleResourceJid() {
        try {
            new Jid("user@host/res/res2");
        } catch (ParseException e) {
            return;
        }
        Assert.fail("Double resource jid is valid");
    }

    @Test
    public void testJidWithoutResource() {
        try {
            Jid jid = new Jid("user@host");
            Assert.assertEquals(jid.user, "user");
            Assert.assertEquals(jid.host, "host");
            Assert.assertNull(jid.resource);
        } catch (ParseException e) {
            Assert.fail();
        }
    }

    @Test
    public void testJidWithResource() {
        try {
            Jid jid = new Jid("user@host/res");
            Assert.assertEquals(jid.user, "user");
            Assert.assertEquals(jid.host, "host");
            Assert.assertEquals(jid.resource, "res");
        } catch (ParseException e) {
            Assert.fail();
        }
    }

    @Test
    public void testServerJid() {
        try {
            Jid jid = new Jid("host");
            Assert.assertNull(jid.user);
            Assert.assertEquals(jid.host, "host");
            Assert.assertNull(jid.resource);
        } catch (ParseException e) {
            Assert.fail();
        }
    }

    @Test
    public void testJidConstuctors() {
        try {
            Assert.assertEquals(new Jid("host"), new Jid(null, "host", null));
            Assert.assertEquals(new Jid("user@host"), new Jid("user", "host", null));
            Assert.assertEquals(new Jid("user@host/res"), new Jid("user", "host", "res"));
            Assert.assertEquals(new Jid("host"), new Jid("", "host", ""));
        } catch (ParseException e) {
            Assert.fail();
        }
    }
}
