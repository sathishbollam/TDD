package com.pn.tdd.StringCalculator;

public class StringCalculator {

	public static final int add(String numbers) {
		String[] numbersArray = numbers.split(",");
		System.out.println("Length: "+numbersArray.length);
		System.out.println("Values: "+numbers);
		if(numbersArray.length > 2){
			throw new RuntimeException("Up to 2 numbers separated by comma (,) are allowed");
		} else {
			for(String number : numbersArray){
				if (!number.isEmpty()) {
					Integer.parseInt(number);
				}
			}
		}
		
		return 0;
	}

}
