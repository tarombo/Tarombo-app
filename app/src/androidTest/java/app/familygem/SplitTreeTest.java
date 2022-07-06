package app.familygem;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import android.content.Context;

import androidx.test.internal.runner.junit4.AndroidJUnit4ClassRunner;
import androidx.test.platform.app.InstrumentationRegistry;

import com.familygem.utility.FamilyGemTreeInfoModel;
import com.google.gson.Gson;

import org.apache.commons.io.FileUtils;
import org.folg.gedcom.model.EventFact;
import org.folg.gedcom.model.Family;
import org.folg.gedcom.model.Gedcom;
import org.folg.gedcom.model.Name;
import org.folg.gedcom.model.Person;
import org.folg.gedcom.parser.JsonParser;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

@RunWith(AndroidJUnit4ClassRunner.class)
public class SplitTreeTest {
    Context testContext; // Contesto del test (per accedere alle risorse in /assets)

    private String getJson(String filename) throws IOException {
        testContext = InstrumentationRegistry.getInstrumentation().getContext();
        InputStream inputStream = testContext.getAssets().open(filename);
        InputStreamReader inputStreamReader = new InputStreamReader(inputStream, StandardCharsets.UTF_8);
        StringBuilder text = new StringBuilder();

        BufferedReader br = new BufferedReader(inputStreamReader);
        String line;
        while( (line = br.readLine()) != null ) {
            text.append(line);
            text.append('\n');
        }
        br.close();

        String json = text.toString();
        return json;
    }

    @Test
    public void convertJsonGedcom() throws IOException {
        String json = getJson("T_tree_v1.json");
        System.out.println(json);
        Gedcom gedcom = new JsonParser().fromJson(json);
        assertNotNull(gedcom.getHeader());
    }

    // T = T1 + T2
//    Gedcom T1;
//    Gedcom T2;


    @Test
    public void T_Test() throws  IOException {
        Gson gson = new Gson();
        String jsonInfo = getJson("T_setting_v1.json");
        FamilyGemTreeInfoModel treeInfoModel = gson.fromJson(jsonInfo, FamilyGemTreeInfoModel.class);
        Settings.Tree tree = new Settings.Tree(1, treeInfoModel.title, null, treeInfoModel.persons, treeInfoModel.generations, treeInfoModel.root, null, 0, "");

        String json = getJson("T_tree_v1.json");
        Gedcom gedcom = new JsonParser().fromJson(json);
        String fulcrumId = "I5*5ba2f623-430c-4eef-ad41-1dff3c17218b";
        Person fulcrum = gedcom.getPerson(fulcrumId);
        System.out.println("fulcrum:" + getName(fulcrum) + " id:" + fulcrum.getId());

        TreeSplitter.SplitterResult result = TreeSplitter.split(gedcom, tree, fulcrum);

        File dir = InstrumentationRegistry.getInstrumentation().getTargetContext().getDir("tmp1", Context.MODE_PRIVATE);
        if (!dir.exists())
            dir.mkdir();
        JsonParser jp = new JsonParser();

        // create T1.json
        String jsonT1 = jp.toJson(result.T1);
        File T1file = new File(dir, "T1.json");
        FileUtils.writeStringToFile(T1file, jsonT1, "UTF-8");

        // create remaining T.json
        String jsonT = jp.toJson(gedcom);
        File Tfile = new File(dir, "T.json");
        FileUtils.writeStringToFile(Tfile, jsonT, "UTF-8");


        assertEquals(12, gedcom.getPeople().size());
        assertEquals(12, tree.persons);
        assertEquals(9, result.T1.getPeople().size());
        assertEquals(9, result.personsT1);
        assertEquals(5, gedcom.getFamilies().size());
        assertEquals(2, result.T1.getFamilies().size());
        assertEquals(3, result.generationsT1);
    }

    @Test
    public void SimpsonsTest() throws  IOException {
        Gson gson = new Gson();
        String jsonInfo = getJson("simpsons_setting.json");
        FamilyGemTreeInfoModel treeInfoModel = gson.fromJson(jsonInfo, FamilyGemTreeInfoModel.class);
        Settings.Tree tree = new Settings.Tree(1, treeInfoModel.title, null, treeInfoModel.persons, treeInfoModel.generations, treeInfoModel.root, null, 0, "");

        String json = getJson("simpsons_tree.json");
        Gedcom gedcom = new JsonParser().fromJson(json);
        String fulcrumId = "I2";
        Person fulcrum = gedcom.getPerson(fulcrumId);
        System.out.println("fulcrum:" + getName(fulcrum) + " id:" + fulcrum.getId());

        TreeSplitter.SplitterResult result = TreeSplitter.split(gedcom, tree, fulcrum);
        System.out.println("T root:" + tree.root);

        File dir = InstrumentationRegistry.getInstrumentation().getTargetContext().getDir("tmp1", Context.MODE_PRIVATE);
        if (!dir.exists())
            dir.mkdir();
        JsonParser jp = new JsonParser();

        // create T1.json
        String jsonT1 = jp.toJson(result.T1);
        File T1file = new File(dir, "T1.json");
        FileUtils.writeStringToFile(T1file, jsonT1, "UTF-8");

        // create remaining T.json
        String jsonT = jp.toJson(gedcom);
        File Tfile = new File(dir, "T.json");
        FileUtils.writeStringToFile(Tfile, jsonT, "UTF-8");


        assert(true);
    }


    private String getName(Person person) {
        for (Name name : person.getNames()) {
            return  name.getValue();
        }
        return "";
    }
}
