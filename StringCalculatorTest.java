package pn.tdd.StringCalculator;

import org.junit.Assert;
import org.junit.Test;

import com.pn.tdd.StringCalculator.StringCalculator;

public class StringCalculatorTest {
	
	@Test( expected = RuntimeException.class)
	public final void whenMoreThanTwoNumbersExceptionIsThrown(){
		StringCalculator.add("1,2,3");
	}
	
	@Test
	public final void whenTwoNumbersUsedNoExceptionIsThrown(){
		StringCalculator.add("1,2");
		Assert.assertTrue(true);
	}
	
	@Test( expected = RuntimeException.class)
	public final void whenOtherThanNumberExceptionIsThrown(){
		StringCalculator.add("1,X");		
	}
	
	@Test
	public final void whenEmptyStringIsUsedThenReturnValueIs0() {
	    Assert.assertEquals(0, StringCalculator.add(""));
	}

}
