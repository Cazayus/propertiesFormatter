// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.github.cazayus.properties.parsing;

import org.jetbrains.annotations.NotNull;

import com.intellij.lang.properties.PropertiesHighlighter;
import com.intellij.lexer.Lexer;

public class CEAPropertiesHighlighter extends PropertiesHighlighter {
	@Override
	@NotNull
	public Lexer getHighlightingLexer() {
		return new CEAPropertiesHighlightingLexer();
	}
}
