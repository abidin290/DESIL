package id.desa.dokumentasirumah;
import org.junit.Test;
import static org.junit.Assert.*;
public class FormRulesTest {
    @Test public void rejectsMissingIdentityOrAnyPhoto() {
        boolean[] all={true,true,true,true,true};
        assertFalse(FormRules.ready("  ",all));
        assertFalse(FormRules.ready(null,all));
        for(int i=0;i<5;i++){ boolean[] partial=all.clone(); partial[i]=false; assertFalse(FormRules.ready("Budi",partial)); }
        assertTrue(FormRules.ready(" Budi Santoso ",all));
        assertTrue(FormRules.ready("Budi",new boolean[]{true,true,true,true,true,false,false,false}));
        assertFalse(FormRules.ready("Budi",new boolean[4]));
    }
}
