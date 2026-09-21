package salve.presentation.ui;

import android.app.Activity;
import android.os.Bundle;
import android.text.InputFilter;
import android.text.InputType;
import android.widget.*;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Locale;
import salve.core.finance.BusinessFinance;

/** Read-only local analysis: no company data, banking access or forecast is assumed. */
public final class BusinessFinanceActivity extends Activity {
    private EditText price, variable, fixed, units, currency;
    private TextView result;
    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);setTitle("Finanzas del negocio");
        ScrollView scroll = new ScrollView(this);
        LinearLayout root = new LinearLayout(this); root.setOrientation(LinearLayout.VERTICAL);
        int pad = (int)(20 * getResources().getDisplayMetrics().density);root.setPadding(pad,pad,pad,pad);scroll.addView(root);
        text(root,"Analiza un producto o servicio",22);
        text(root,"Usa la misma moneda y el mismo periodo para todos los datos. Introduce importes sin impuestos indirectos y sin separadores de miles. Puedes usar coma o punto decimal. Los cálculos se hacen en este dispositivo.",15);
        currency = field(root,"Moneda (por ejemplo EUR, USD o COP)",false,3,101);
        currency.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_CHARACTERS);
        price = field(root,"Precio de venta por unidad",true,20,102);
        variable = field(root,"Coste variable por unidad",true,20,103);
        fixed = field(root,"Costes fijos del periodo",true,20,104);
        units = field(root,"Unidades vendidas o previstas en ese periodo",false,10,105);
        Button calculate = new Button(this);calculate.setText("Calcular escenario");root.addView(calculate);
        result = text(root,"Introduce tus cifras para calcular el resultado.",16);
        result.setTextIsSelectable(true);result.setAccessibilityLiveRegion(android.view.View.ACCESSIBILITY_LIVE_REGION_POLITE);
        calculate.setOnClickListener(v -> calculate());
        android.text.TextWatcher changed = new android.text.TextWatcher() {
            public void beforeTextChanged(CharSequence s,int start,int count,int after) { }
            public void onTextChanged(CharSequence s,int start,int before,int count) {
                result.setText("Datos modificados. Pulsa Calcular escenario para actualizar el resultado.");
            }
            public void afterTextChanged(android.text.Editable text) { }
        };
        for (EditText input : new EditText[]{price,variable,fixed,units,currency}) input.addTextChangedListener(changed);
        text(root,"El escenario supone precio y coste unitario constantes y un solo producto. No incluye costes que no hayas introducido, intereses ni impuesto sobre beneficios. Las ventas previstas son una hipótesis tuya; el resultado no mide caja disponible.",14);
        setContentView(scroll);
    }
    private EditText field(LinearLayout root,String label,boolean decimal,int limit,int id) {
        text(root,label,16);EditText field = new EditText(this);field.setId(id);field.setSingleLine(true);
        field.setContentDescription(label);field.setFilters(new InputFilter[]{new InputFilter.LengthFilter(limit)});
        field.setInputType(InputType.TYPE_CLASS_NUMBER | (decimal ? InputType.TYPE_NUMBER_FLAG_DECIMAL : 0));
        if (decimal) field.setKeyListener(android.text.method.DigitsKeyListener.getInstance("0123456789.,"));
        root.addView(field);return field;
    }
    private TextView text(LinearLayout root,String text,int size) {TextView view = new TextView(this);view.setText(text);view.setTextSize(size);view.setPadding(0,10,0,10);root.addView(view);return view;}
    private void calculate() {
        try {
            String code = currency.getText().toString().trim().toUpperCase(Locale.ROOT);
            try { java.util.Currency.getInstance(code); } catch(IllegalArgumentException invalid) { throw new IllegalArgumentException("Indica una moneda válida de tres letras, como EUR, USD o COP."); }
            BusinessFinance.Result r = BusinessFinance.calculate(BusinessFinance.parseAmount(price.getText().toString()),
                    BusinessFinance.parseAmount(variable.getText().toString()),BusinessFinance.parseAmount(fixed.getText().toString()),BusinessFinance.parseUnits(units.getText().toString()));
            String balance = r.breakEvenUnits == null ? "No se alcanza vendiendo más con estos costes y precio."
                    : r.breakEvenUnits.toPlainString() + " unidades";
            result.setText("Escenario calculado con tus datos (totales redondeados a la moneda)\nIngresos: " + money(r.revenue,code)
                    + "\nCostes variables: " + money(r.variableCosts,code)
                    + "\nContribución por unidad: " + r.contributionPerUnit.stripTrailingZeros().toPlainString() + " " + code
                    + "\nMargen de contribución: " + (r.contributionPercent == null ? "no definido con precio cero" : r.contributionPercent.toPlainString()+" %")
                    + "\nResultado operativo estimado: " + money(r.operatingResult,code)
                    + "\nPunto de equilibrio: " + balance
                    + "\n\nFórmulas: contribución = precio − coste variable unitario; resultado = contribución × unidades − costes fijos. Punto de equilibrio = costes fijos ÷ contribución, redondeado hacia arriba."
                    + (r.contributionPerUnit.signum()<0 ? "\nCada unidad adicional aumenta la pérdida con estos supuestos." : ""));
        } catch(IllegalArgumentException invalid) { result.setText(invalid.getMessage()); }
    }
    @Override protected void onRestoreInstanceState(Bundle state) {super.onRestoreInstanceState(state);result.setText(state.getString("analysis", "Introduce tus cifras para calcular el resultado."));}
    @Override protected void onSaveInstanceState(Bundle state) {state.putString("analysis", result.getText().toString());super.onSaveInstanceState(state);}
    private static String money(BigDecimal value,String code) {int digits=Math.max(0,java.util.Currency.getInstance(code).getDefaultFractionDigits());return value.setScale(digits,RoundingMode.HALF_UP).toPlainString()+" "+code;}
}
