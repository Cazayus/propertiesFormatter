// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.github.cazayus.properties.parsing;

import org.jetbrains.annotations.NotNull;

import com.intellij.lexer.Lexer;
import com.intellij.psi.impl.cache.impl.OccurrenceConsumer;
import com.intellij.psi.impl.cache.impl.id.LexerBasedIdIndexer;
import com.intellij.psi.impl.cache.impl.idCache.PropertiesFilterLexer;

/**
 * @author Maxim.Mossienko
 */
public class CEAPropertiesIdIndexer extends LexerBasedIdIndexer {
	@NotNull
	@Override
	public Lexer createLexer(@NotNull final OccurrenceConsumer consumer) {
		return createIndexingLexer(consumer);
	}

	static Lexer createIndexingLexer(OccurrenceConsumer consumer) {
		return new PropertiesFilterLexer(new CEAPropertiesLexer(), consumer);
	}
}
