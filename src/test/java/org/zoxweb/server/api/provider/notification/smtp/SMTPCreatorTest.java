package org.zoxweb.server.api.provider.notification.smtp;

import org.junit.jupiter.api.Test;
import org.zoxweb.shared.api.APIConfigInfo;
import org.zoxweb.shared.api.APIServiceType;
import org.zoxweb.shared.filters.FilterType;
import org.zoxweb.shared.util.DynamicEnumMap;
import org.zoxweb.shared.util.GetNameValue;
import org.zoxweb.shared.util.NVGenericMap;
import org.zoxweb.shared.util.NVInt;
import org.zoxweb.shared.util.NVPair;

import static org.junit.jupiter.api.Assertions.*;

/**
 * {@link SMTPCreator#createEmptyConfigInfo()}: the SMTP parameters land in the config's typed
 * properties, and the provider reads the port back regardless of how it is stored.
 */
public class SMTPCreatorTest {

    @Test
    public void emptyConfigCarriesEveryParamAsTypedProperty() {
        APIConfigInfo config = new SMTPCreator().createEmptyConfigInfo();

        assertEquals(SMTPCreator.API_NAME, config.getAPITypeName());
        assertEquals("2026", config.getVersion());
        assertArrayEquals(new APIServiceType[]{APIServiceType.EMAIL_NOTIFICATION}, config.getServiceTypes());

        NVGenericMap props = config.getProperties();
        assertEquals(SMTPCreator.Param.values().length, props.size());
        for (SMTPCreator.Param p : SMTPCreator.Param.values()) {
            assertNotNull(props.get(p.getName()), p.getName() + " missing");
        }

        GetNameValue<?> port = props.get(SMTPCreator.Param.PORT.getName());
        assertTrue(port instanceof NVInt, "Port is typed");
        assertEquals(Integer.valueOf(465), port.getValue());
        assertEquals(465L, props.getValueAsLong(SMTPCreator.Param.PORT.getName()));

        GetNameValue<?> password = props.get(SMTPCreator.Param.PASSWORD.getName());
        assertTrue(password instanceof NVPair);
        assertNull(password.getValue());
        assertSame(FilterType.ENCRYPT_MASK, ((NVPair) password).getValueFilter());

        GetNameValue<?> auth = props.get(SMTPCreator.Param.AUTHENTICATION.getName());
        assertTrue(auth instanceof NVPair);
        assertTrue(((NVPair) auth).getValueFilter() instanceof DynamicEnumMap, "enum-valued param keeps its DynamicEnumMap filter");

        GetNameValue<?> host = props.get(SMTPCreator.Param.HOST.getName());
        assertTrue(host instanceof NVPair);
        assertNull(host.getValue());
    }

    @Test
    public void portReadsAsIntWhetherStoredTypedOrAsText() {
        // the provider reads the port with getValueAsInt
        NVGenericMap typed = new SMTPCreator().createEmptyConfigInfo().getProperties();
        assertEquals(465, typed.getValueAsInt(SMTPCreator.Param.PORT.getName()));
        assertEquals(465, typed.getValueAsInt(SMTPCreator.Param.PORT));

        // a config persisted before the switch holds the port as text
        NVGenericMap legacy = new NVGenericMap();
        legacy.add(new NVPair(SMTPCreator.Param.PORT.getName(), "587"));
        assertEquals(587, legacy.getValueAsInt(SMTPCreator.Param.PORT.getName()));

        // missing: default form answers, strict form fails
        NVGenericMap empty = new NVGenericMap();
        assertEquals(25, empty.getValueAsInt(SMTPCreator.Param.PORT, 25));
        assertThrows(RuntimeException.class, () -> empty.getValueAsInt(SMTPCreator.Param.PORT.getName()));

        // non-numeric text is rejected
        NVGenericMap bad = new NVGenericMap();
        bad.add(new NVPair(SMTPCreator.Param.PORT.getName(), "smtp"));
        assertThrows(NumberFormatException.class, () -> bad.getValueAsInt(SMTPCreator.Param.PORT.getName()));
    }

    @Test
    public void eachCallReturnsAnIndependentConfig() {
        SMTPCreator creator = new SMTPCreator();
        APIConfigInfo a = creator.createEmptyConfigInfo();
        APIConfigInfo b = creator.createEmptyConfigInfo();

        ((NVInt) a.getProperties().get(SMTPCreator.Param.PORT.getName())).setValue(25);
        assertEquals(Integer.valueOf(465), b.getProperties().get(SMTPCreator.Param.PORT.getName()).getValue());
    }
}
