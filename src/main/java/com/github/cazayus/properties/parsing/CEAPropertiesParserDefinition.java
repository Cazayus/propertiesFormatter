package com.github.cazayus.properties.parsing;

import org.jetbrains.annotations.NotNull;

import com.intellij.lang.properties.parsing.PropertiesParserDefinition;
import com.intellij.lexer.Lexer;
import com.intellij.openapi.project.Project;

public class CEAPropertiesParserDefinition extends PropertiesParserDefinition {
	@Override
	@NotNull
	public Lexer createLexer(Project project) {
		return new CEAPropertiesLexer();
	}
}
