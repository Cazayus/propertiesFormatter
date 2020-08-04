package com.github.cazayus.properties.formatting;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.intellij.formatting.Alignment;
import com.intellij.lang.ASTNode;
import com.intellij.lang.properties.formatting.PropertyBlock;

public final class CEAPropertyBlock extends PropertyBlock {
	CEAPropertyBlock(@NotNull ASTNode node, @Nullable Alignment alignment) {
		super(node, alignment);
	}
}
