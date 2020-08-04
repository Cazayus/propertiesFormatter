// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.github.cazayus.properties.parsing;

import com.intellij.lang.cacheBuilder.DefaultWordsScanner;
import com.intellij.lang.properties.parsing.PropertiesTokenTypes;
import com.intellij.psi.tree.TokenSet;

public class CEAPropertiesWordsScanner extends DefaultWordsScanner {
	public CEAPropertiesWordsScanner() {
		super(new CEAPropertiesLexer(), TokenSet.create(PropertiesTokenTypes.KEY_CHARACTERS), PropertiesTokenTypes.COMMENTS,
				TokenSet.create(PropertiesTokenTypes.VALUE_CHARACTERS));
	}
}
