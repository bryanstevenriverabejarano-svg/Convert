package salve.core.finance;

import java.io.File;
import java.io.IOException;

/** Serializes the read/interpret/write cycle. A successful reply is emitted only after saving. */
public final class PersonalBudgetService {
    private static final Object STORAGE_LOCK=new Object();
    private final PersonalBudgetStore store;
    private final PersonalBudgetConversation conversation=new PersonalBudgetConversation();
    public PersonalBudgetService(File file){store=new PersonalBudgetStore(file);}
    public void cancelPending(){conversation.resetPending();}
    public boolean handles(String input){return conversation.handles(input)||FinanceConversationPolicy.isPrivateBudgetInput(input);}
    public String respond(String input) { synchronized(STORAGE_LOCK) { return respondLocked(input); } }
    private String respondLocked(String input) {
        PersonalBudget budget;
        try {budget=store.load();}
        catch(IOException|RuntimeException unreadable) {
            // Explicit deletion remains possible even if the saved file was corrupted.
            PersonalBudgetConversation.Response reset=conversation.handle(input,new PersonalBudget());
            if(reset!=null&&reset.clearAll) {
                try {store.clear();conversation.resetPending();return reset.reply;}
                catch(IOException failure){return "No pude borrar el presupuesto. Los datos no se han eliminado.";}
            }
            conversation.resetPending();
            return "No puedo leer el presupuesto guardado y lo he conservado sin reemplazarlo. Puedes intentar de nuevo o decir ‘borra mi presupuesto’ para empezar desde cero.";
        }
        try {
            PersonalBudgetConversation.Response response=conversation.handle(input,budget);
            if(response==null)return "Puedo guardar tu sueldo neto mensual y gastos mensuales, y calcular tu presupuesto personal. Dime el importe, la moneda y el periodo; no he guardado datos de este mensaje.";
            if(response.clearAll){store.clear();conversation.resetPending();}
            else if(response.changed)store.save(response.nextBudget);
            return response.reply;
        } catch(IOException saveFailed) {
            conversation.resetPending();return "No pude guardar el cambio. El presupuesto anterior se conserva; vuelve a intentarlo.";
        } catch(IllegalArgumentException invalid) {
            conversation.resetPending();return "No he guardado ese cambio: faltan datos o el importe no es válido. Indica sueldo neto mensual o un gasto mensual con su moneda.";
        }
    }
}
