package salve.core.finance;

import java.io.File;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.charset.StandardCharsets;
import org.junit.Test;
import static org.junit.Assert.*;

public class PersonalBudgetStoreTest {
    @Test public void persistsAcrossInstancesAndRemovesPendingDataOnClear() throws Exception {
        File dir=Files.createTempDirectory("salve-budget").toFile();File file=new File(dir,"budget.json");
        try {
            PersonalBudgetStore store=new PersonalBudgetStore(file);
            assertNull(store.load().monthlyIncome);
            PersonalBudget budget=new PersonalBudget().withMonthlyIncome(new BigDecimal("1800"),"EUR")
                    .withExpense("Alquiler",new BigDecimal("650"),PersonalBudget.Category.NEED,"EUR");
            store.save(budget);
            assertEquals(budget.toJson(),new PersonalBudgetStore(file).load().toJson());
            Files.writeString(new File(dir,"budget.json.pending").toPath(),"incomplete update");
            store.clear();assertFalse(file.exists());assertFalse(new File(dir,"budget.json.pending").exists());
            assertNull(store.load().monthlyIncome);
        } finally {for(File child:dir.listFiles())child.delete();dir.delete();}
    }
    @Test public void corruptFileIsRejectedAndPreserved() throws Exception {
        File file=File.createTempFile("salve-budget", ".json");
        try {
            Files.writeString(file.toPath(),"{broken");
            assertThrows(java.io.IOException.class,()->new PersonalBudgetStore(file).load());
            assertEquals("{broken",Files.readString(file.toPath()));
            Files.write(file.toPath(),new byte[]{(byte)0xff});
            assertThrows(java.io.IOException.class,()->new PersonalBudgetStore(file).load());
        } finally {file.delete();}
    }
    @Test public void pendingUncommittedWriteDoesNotReplaceCurrentBudget() throws Exception {
        File dir=Files.createTempDirectory("salve-budget").toFile();File file=new File(dir,"budget.json");
        try {
            PersonalBudgetStore store=new PersonalBudgetStore(file);
            PersonalBudget original=new PersonalBudget().withMonthlyIncome(new BigDecimal("1000"),"EUR");store.save(original);
            Files.writeString(new File(dir,"budget.json.pending").toPath(),"{unfinished",StandardCharsets.UTF_8);
            assertEquals(original.toJson(),store.load().toJson());
            PersonalBudget updated=original.withMonthlyIncome(new BigDecimal("1200"),"EUR");store.save(updated);
            assertEquals(updated.toJson(),store.load().toJson());assertFalse(new File(dir,"budget.json.pending").exists());
        } finally {for(File child:dir.listFiles())child.delete();dir.delete();}
    }
    @Test public void failedDestinationDoesNotCreateSuccessOrDestroyOtherFile() throws Exception {
        File file=File.createTempFile("salve-budget-parent", ".txt");
        try {Files.writeString(file.toPath(),"keep");assertThrows(java.io.IOException.class,()->new PersonalBudgetStore(new File(file,"budget.json")).save(new PersonalBudget()));assertEquals("keep",Files.readString(file.toPath()));}
        finally {file.delete();}
    }
}
