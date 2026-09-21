package salve.core.finance;

import java.math.BigDecimal;
import org.junit.Test;
import static org.junit.Assert.*;

public class BusinessFinanceTest {
    private static BusinessFinance.Result scenario(String price,String variable,String fixed,long units) {
        return BusinessFinance.calculate(new BigDecimal(price),new BigDecimal(variable),new BigDecimal(fixed),units);
    }
    private static void number(String expected,BigDecimal actual) {assertEquals(0,new BigDecimal(expected).compareTo(actual));}
    @Test public void profitableScenarioUsesContributionNotGrossRevenue() {
        BusinessFinance.Result r=scenario("20","10","100",50);
        number("1000",r.revenue);number("500",r.variableCosts);number("10",r.contributionPerUnit);
        number("400",r.operatingResult);number("50",r.contributionPercent);number("10",r.breakEvenUnits);
    }
    @Test public void breakEvenRoundsUpToWholeSale() {
        BusinessFinance.Result r=scenario("10","7","10",3);number("4",r.breakEvenUnits);number("-1",r.operatingResult);
        assertTrue(scenario("10","7","10",4).operatingResult.signum()>=0);
    }
    @Test public void exactSmallDecimalsDoNotCreateExtraUnit() {
        BusinessFinance.Result r=scenario("0.3","0.2","1",10);number("10",r.breakEvenUnits);number("0",r.operatingResult);
    }
    @Test public void positiveFixedCostsCannotBeRecoveredWithNonpositiveContribution() {
        assertNull(scenario("10","10","1",100).breakEvenUnits);
        assertNull(scenario("10","12","1",100).breakEvenUnits);
        number("-201",scenario("10","12","1",100).operatingResult);
    }
    @Test public void noFixedCostsNeedsNoSalesButNegativeContributionStillLosesMoney() {
        number("0",scenario("10","12","0",0).breakEvenUnits);
        number("-20",scenario("10","12","0",10).operatingResult);
    }
    @Test public void zeroPriceHasNoPercentage() {assertNull(scenario("0","0","0",0).contributionPercent);}
    @Test public void rejectsNegativeNonfiniteGroupingAndExponentInputs() {
        for(String raw:new String[]{null,"","-1","NaN","Infinity","1e3","1.000,00","1,000.00","1 000","1.23456","1000000000001"})
            assertThrows(IllegalArgumentException.class,()->BusinessFinance.parseAmount(raw));
    }
    @Test public void decimalCommaAndDotHaveSameValue() {number("12.3456",BusinessFinance.parseAmount("12,3456"));number("12.3456",BusinessFinance.parseAmount("12.3456"));}
    @Test public void rejectsFractionalOrHugeUnits() {
        for(String raw:new String[]{null,"","-1","1.2","1e3","1000000001","99999999999"})assertThrows(IllegalArgumentException.class,()->BusinessFinance.parseUnits(raw));
    }
    @Test public void boundedBigDecimalCalculationDoesNotOverflow() {
        BusinessFinance.Result r=scenario("1000000000000","0","0",1000000000);
        number("1000000000000000000000",r.revenue);
    }
    @Test public void publicApiAlsoEnforcesInputBounds() {
        assertThrows(IllegalArgumentException.class,()->scenario("-1","0","0",0));
        assertThrows(IllegalArgumentException.class,()->scenario("1","0","0",-1));
        assertThrows(IllegalArgumentException.class,()->scenario("1","0","0",1000000001));
    }
}
