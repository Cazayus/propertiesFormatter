package com.github.cazayus.properties.parsing;

import com.intellij.lang.properties.parsing.PropertiesTokenTypes;
import com.intellij.lexer.LayeredLexer;
import com.intellij.lexer.StringLiteralLexer;
import com.intellij.psi.tree.IElementType;

public class CEAPropertiesHighlightingLexer extends LayeredLexer {
	public CEAPropertiesHighlightingLexer() {
		super(new CEAPropertiesLexer());
		registerSelfStoppingLayer(new StringLiteralLexer(StringLiteralLexer.NO_QUOTE_CHAR, PropertiesTokenTypes.VALUE_CHARACTERS, true, "#!=:"),
				new IElementType[] { PropertiesTokenTypes.VALUE_CHARACTERS }, IElementType.EMPTY_ARRAY);
		registerSelfStoppingLayer(new StringLiteralLexer(StringLiteralLexer.NO_QUOTE_CHAR, PropertiesTokenTypes.KEY_CHARACTERS, true, "#!=: "),
				new IElementType[] { PropertiesTokenTypes.KEY_CHARACTERS }, IElementType.EMPTY_ARRAY);
	}
}
