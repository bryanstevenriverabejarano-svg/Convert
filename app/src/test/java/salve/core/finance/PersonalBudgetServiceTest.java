package salve.core.finance;

import java.io.File;
import java.nio.file.Files;
import java.nio.charset.StandardCharsets;
import org.junit.Test;
import static org.junit.Assert.*;

public class PersonalBudgetServiceTest {
    @Test public void conversationPersistsReplacesAndForgetsAcrossSessions() throws Exception {
        File dir=Files.createTempDirectory("salve-budget-service").toFile();File file=new File(dir,"budget.json");
        try {
            PersonalBudgetService service=new PersonalBudgetService(file);
            assertTrue(service.handles("mi sueldo es 1800 EUR netos al mes"));
            service.respond("mi sueldo es 1800 EUR netos al mes");
            service.respond("pago 650 EUR de alquiler al mes");
            new PersonalBudgetService(file).respond("pago 700 EUR de alquiler al mes");
            PersonalBudget saved=new PersonalBudgetStore(file).load();
            assertEquals(1,saved.expenses.size());assertEquals(0,saved.expenses.get(0).amount.compareTo(new java.math.BigDecimal("700")));
            assertNotNull(new PersonalBudgetService(file).respond("cuánto puedo ahorrar"));
            service.respond("olvida mi sueldo");assertNull(new PersonalBudgetStore(file).load().monthlyIncome);
            assertEquals(1,new PersonalBudgetStore(file).load().expenses.size());
            service.respond("borra mi presupuesto");assertFalse(file.exists());
        } finally {for(File child:dir.listFiles())child.delete();dir.delete();}
    }
    @Test public void hypotheticalAndMalformedAmountsNeverOverwriteIncome() throws Exception {
        File dir=Files.createTempDirectory("salve-budget-service").toFile();File file=new File(dir,"budget.json");
        try {
            PersonalBudgetService service=new PersonalBudgetService(file);service.respond("mi sueldo es 1800 EUR netos al mes");
            String original=new String(Files.readAllBytes(file.toPath()),StandardCharsets.UTF_8);
            for(String input:new String[]{"si mi sueldo es 9000 EUR netos al mes", "mi sueldo es 1.800 EUR netos al mes", "mi sueldo es -200 EUR netos al mes"}) {
                assertTrue(service.handles(input));service.respond(input);assertEquals(original,new String(Files.readAllBytes(file.toPath()),StandardCharsets.UTF_8));
            }
        } finally {for(File child:dir.listFiles())child.delete();dir.delete();}
    }
    @Test public void corruptBudgetDoesNotBecomeEmptyAndCanBeExplicitlyDeleted() throws Exception {
        File file=File.createTempFile("salve-budget-service", ".json");
        try {
            Files.write(file.toPath(),"{broken".getBytes(StandardCharsets.UTF_8));PersonalBudgetService service=new PersonalBudgetService(file);
            assertTrue(service.respond("mi sueldo es 1800 EUR netos al mes").contains("conservado"));
            assertEquals("{broken",new String(Files.readAllBytes(file.toPath()),StandardCharsets.UTF_8));service.respond("borra mi presupuesto");assertFalse(file.exists());
        } finally {file.delete();}
    }
    @Test public void failedWriteNeverReportsSaved() throws Exception {
        File file=File.createTempFile("salve-budget-parent", ".txt");
        try {
            PersonalBudgetService service=new PersonalBudgetService(new File(file,"budget.json"));
            assertTrue(service.respond("mi sueldo es 1800 EUR netos al mes").startsWith("No pude guardar"));
        } finally {file.delete();}
    }
    @Test public void unfinishedClarificationIsNotPersistedOrReusedAfterTopicChange() throws Exception {
        File dir=Files.createTempDirectory("salve-budget-service").toFile();File file=new File(dir,"budget.json");
        try {
            PersonalBudgetService service=new PersonalBudgetService(file);service.respond("cobro 1800");assertFalse(file.exists());
            assertTrue(service.handles("EUR netos al mes"));service.cancelPending();
            service.respond("EUR netos al mes");assertFalse(file.exists());
        } finally {for(File child:dir.listFiles())child.delete();dir.delete();}
    }
}
