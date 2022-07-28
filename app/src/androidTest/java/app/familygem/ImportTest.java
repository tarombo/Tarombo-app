package app.familygem;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import android.content.Context;

import androidx.test.platform.app.InstrumentationRegistry;

import com.familygem.utility.Helper;

import org.folg.gedcom.model.ChildRef;
import org.folg.gedcom.model.Family;
import org.folg.gedcom.model.Gedcom;
import org.folg.gedcom.model.ParentFamilyRef;
import org.folg.gedcom.model.Person;
import org.folg.gedcom.model.SpouseFamilyRef;
import org.folg.gedcom.model.SpouseRef;
import org.folg.gedcom.parser.JsonParser;
import org.folg.gedcom.parser.ModelParser;
import org.junit.Test;
import org.xml.sax.SAXParseException;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Optional;
import java.util.UUID;

public class ImportTest {
    Context testContext; // Contesto del test (per accedere alle risorse in /assets)

    private Gedcom getGedcom(String filename) throws IOException, SAXParseException {
        testContext = InstrumentationRegistry.getInstrumentation().getContext();
        InputStream inputStream = testContext.getAssets().open(filename);
        return new ModelParser().parseGedcom(inputStream);

    }

    @Test
    public void convertFileToGedcom() throws IOException, SAXParseException {
        String fileName = "TreeImport.ged";
        Gedcom gedcom = getGedcom(fileName);

        JsonParser jp = new JsonParser();
        String json = jp.toJson(gedcom);
        System.out.println(json);
        assertNotNull(gedcom.getHeader());
        Helper.makeGuidGedcom(gedcom);
        String json2 = jp.toJson(gedcom);
        System.out.println(json2);
    }
}
