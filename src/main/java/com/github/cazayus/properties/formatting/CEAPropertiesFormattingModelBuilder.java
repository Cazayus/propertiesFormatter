package com.github.cazayus.properties.formatting;

import org.jetbrains.annotations.NotNull;

import com.intellij.formatting.FormattingContext;
import com.intellij.formatting.FormattingModel;
import com.intellij.formatting.FormattingModelBuilder;
import com.intellij.lang.ASTNode;
import com.intellij.psi.PsiElement;
import com.intellij.psi.codeStyle.CodeStyleSettings;
import com.intellij.psi.formatter.FormattingDocumentModelImpl;
import com.intellij.psi.formatter.PsiBasedFormattingModel;
import com.intellij.psi.impl.source.SourceTreeToPsiMap;
import com.intellij.psi.impl.source.tree.TreeElement;
import com.intellij.psi.impl.source.tree.TreeUtil;

public final class CEAPropertiesFormattingModelBuilder implements FormattingModelBuilder {
	@Override
	public @NotNull FormattingModel createModel(@NotNull FormattingContext formattingContext) {
		PsiElement psiElement = formattingContext.getPsiElement();
		CodeStyleSettings codeStyleSettings = formattingContext.getCodeStyleSettings();
		ASTNode root = TreeUtil.getFileElement((TreeElement) SourceTreeToPsiMap.psiElementToTree(psiElement));
		FormattingDocumentModelImpl documentModel = FormattingDocumentModelImpl.createOn(psiElement.getContainingFile());
		return new PsiBasedFormattingModel(psiElement.getContainingFile(), new CEAPropertiesRootBlock(root, codeStyleSettings), documentModel);
	}
}
