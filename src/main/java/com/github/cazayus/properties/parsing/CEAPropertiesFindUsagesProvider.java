package com.github.cazayus.properties.parsing;

import com.intellij.lang.cacheBuilder.WordsScanner;
import com.intellij.lang.properties.findUsages.PropertiesFindUsagesProvider;

public class CEAPropertiesFindUsagesProvider extends PropertiesFindUsagesProvider {
	@Override
	public WordsScanner getWordsScanner() {
		return new CEAPropertiesWordsScanner();
	}
}
