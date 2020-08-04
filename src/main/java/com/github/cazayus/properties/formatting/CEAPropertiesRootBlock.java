package com.github.cazayus.properties.formatting;

import java.util.ArrayList;
import java.util.List;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.intellij.formatting.Alignment;
import com.intellij.formatting.Alignment.Anchor;
import com.intellij.formatting.Block;
import com.intellij.formatting.Spacing;
import com.intellij.lang.ASTNode;
import com.intellij.lang.properties.PropertiesLanguage;
import com.intellij.lang.properties.parsing.PropertiesElementTypes;
import com.intellij.lang.properties.parsing.PropertiesTokenTypes;
import com.intellij.lang.properties.psi.codeStyle.PropertiesCodeStyleSettings;
import com.intellij.lang.properties.psi.impl.PropertyKeyImpl;
import com.intellij.lang.properties.psi.impl.PropertyValueImpl;
import com.intellij.psi.PsiWhiteSpace;
import com.intellij.psi.codeStyle.CodeStyleSettings;
import com.intellij.psi.formatter.common.AbstractBlock;
import com.intellij.psi.tree.TokenSet;

public final class CEAPropertiesRootBlock extends AbstractBlock {

	private final CodeStyleSettings mySettings;
	private Alignment mySeparatorAlignment;

	CEAPropertiesRootBlock(@NotNull ASTNode node, CodeStyleSettings settings) {
		super(node, null, Alignment.createAlignment());
		mySettings = settings;
		mySeparatorAlignment = Alignment.createAlignment(true, Anchor.LEFT);
	}

	@Override
	protected List<Block> buildChildren() {
		List<Block> result = new ArrayList<>();
		ASTNode child = myNode.getFirstChildNode();
		while (child != null) {
			if (!(child instanceof PsiWhiteSpace)) {
				if (child.getElementType() == PropertiesElementTypes.PROPERTIES_LIST) {
					ASTNode propertyNode = child.getFirstChildNode();
					while (propertyNode != null) {
						if (propertyNode.getElementType() == PropertiesElementTypes.PROPERTY) {
							collectPropertyBlock(propertyNode, result);
						} else if (PropertiesTokenTypes.END_OF_LINE_COMMENT.equals(propertyNode.getElementType())
								|| PropertiesTokenTypes.BAD_CHARACTER.equals(propertyNode.getElementType())) {
							result.add(new CEAPropertyBlock(propertyNode, null));
						} else if ((propertyNode.getText().length() - propertyNode.getText().replace("\n", "").length()) > 1) {
							mySeparatorAlignment = Alignment.createAlignment(true, Anchor.LEFT);
						}
						propertyNode = propertyNode.getTreeNext();
					}
				} else if (PropertiesTokenTypes.BAD_CHARACTER.equals(child.getElementType())) {
					result.add(new CEAPropertyBlock(child, null));
				}
			}
			if (PropertiesTokenTypes.END_OF_LINE_COMMENT.equals(child.getElementType())) {
				result.add(new CEAPropertyBlock(child, null));
			}
			child = child.getTreeNext();
		}
		return result;
	}

	private void collectPropertyBlock(ASTNode propertyNode, List<? super Block> collector) {
		ASTNode[] nonWhiteSpaces = propertyNode.getChildren(
				TokenSet.create(PropertiesTokenTypes.KEY_CHARACTERS, PropertiesTokenTypes.KEY_VALUE_SEPARATOR, PropertiesTokenTypes.VALUE_CHARACTERS));
		for (ASTNode node : nonWhiteSpaces) {
			if (node instanceof PropertyKeyImpl) {
				collector.add(new CEAPropertyBlock(node, null));
			}
			if (PropertiesTokenTypes.KEY_VALUE_SEPARATOR.equals(node.getElementType())) {
				collector.add(new CEAPropertyBlock(node,
						mySettings.getCommonSettings(PropertiesLanguage.INSTANCE).ALIGN_GROUP_FIELD_DECLARATIONS ? mySeparatorAlignment : null));
			}
			if (node instanceof PropertyValueImpl) {
				collector.add(new CEAPropertyBlock(node, null));
			}
		}
	}

	@Nullable
	@Override
	public Spacing getSpacing(@Nullable Block child1, @NotNull Block child2) {
		if (child1 == null) {
			return null;
		}
		return mySettings.getCustomSettings(PropertiesCodeStyleSettings.class).SPACES_AROUND_KEY_VALUE_DELIMITER && (isSeparator(child1) || isSeparator(child2))
				|| isKeyValue(child1, child2) ? Spacing.createSpacing(1, 1, 0, true, 0)
						: Spacing.createSpacing(0, 0, 0, true, mySettings.getCustomSettings(PropertiesCodeStyleSettings.class).KEEP_BLANK_LINES ? 999 : 0);
	}

	private static boolean isKeyValue(Block maybeKey, Block maybeValue) {
		if (!(maybeKey instanceof CEAPropertyBlock) || !PropertiesTokenTypes.KEY_CHARACTERS.equals(((CEAPropertyBlock) maybeKey).getNode().getElementType())) {
			return false;
		}
		return maybeValue instanceof CEAPropertyBlock
				&& PropertiesTokenTypes.VALUE_CHARACTERS.equals(((CEAPropertyBlock) maybeValue).getNode().getElementType());
	}

	private static boolean isSeparator(Block block) {
		return block instanceof CEAPropertyBlock && PropertiesTokenTypes.KEY_VALUE_SEPARATOR.equals(((CEAPropertyBlock) block).getNode().getElementType());
	}

	@Override
	public boolean isLeaf() {
		return false;
	}
}
