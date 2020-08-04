/*
 * Copyright 2000-2015 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.github.cazayus.properties.inspection;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.jetbrains.annotations.NotNull;

import com.intellij.codeInsight.actions.ReformatCodeProcessor;
import com.intellij.codeInspection.LocalInspectionTool;
import com.intellij.codeInspection.LocalQuickFix;
import com.intellij.codeInspection.ProblemDescriptor;
import com.intellij.codeInspection.ProblemHighlightType;
import com.intellij.codeInspection.ProblemsHolder;
import com.intellij.lang.properties.IProperty;
import com.intellij.lang.properties.PropertiesImplUtil;
import com.intellij.lang.properties.ResourceBundle;
import com.intellij.lang.properties.psi.PropertiesElementFactory;
import com.intellij.lang.properties.psi.PropertiesFile;
import com.intellij.lang.properties.psi.PropertiesList;
import com.intellij.lang.properties.psi.PropertyKeyValueFormat;
import com.intellij.lang.properties.psi.codeStyle.PropertiesCodeStyleSettings;
import com.intellij.lang.properties.psi.impl.PropertiesFileImpl;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Comparing;
import com.intellij.psi.PsiComment;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiElementVisitor;
import com.intellij.psi.PsiFile;
import com.intellij.psi.util.PsiTreeUtil;

/**
 * @author Dmitry Batkovich
 */
public final class UnsortedPropertiesFileInspection extends LocalInspectionTool {
	static final String LINE_SEPARATOR = "\n";
	static final String TWO_LINE_SEPARATOR = LINE_SEPARATOR + LINE_SEPARATOR;
	static final Logger LOG = Logger.getInstance(UnsortedPropertiesFileInspection.class);
	private static final String MESSAGE_TEMPLATE_WHOLE_RESOURCE_BUNDLE = "Property keys of resource bundle '%s' aren't CEA sorted";

	@NotNull
	@Override
	public PsiElementVisitor buildVisitor(@NotNull ProblemsHolder holder, boolean isOnTheFly) {
		return new PsiElementVisitor() {
			@Override
			public void visitFile(PsiFile file) {
				PropertiesFile propertiesFile = PropertiesImplUtil.getPropertiesFile(file);
				if (!(propertiesFile instanceof PropertiesFileImpl)) {
					return;
				}
				ResourceBundle resourceBundle = propertiesFile.getResourceBundle();
				String resourceBundleBaseName = resourceBundle.getBaseName();
				if (!isResourceBundleAlphaSortedExceptOneFile(resourceBundle, propertiesFile)) {
					List<PropertiesFile> allFiles = resourceBundle.getPropertiesFiles();
					holder.registerProblem(file, String.format(MESSAGE_TEMPLATE_WHOLE_RESOURCE_BUNDLE, resourceBundleBaseName),
							ProblemHighlightType.GENERIC_ERROR_OR_WARNING, new CEAPropertiesSorterQuickFix(allFiles.toArray(new PropertiesFile[0])));
					return;
				}
				if (!isCeaSorted(propertiesFile)) {
					holder.registerProblem(file, "Properties file is CEA unsorted", ProblemHighlightType.GENERIC_ERROR_OR_WARNING,
							new CEAPropertiesSorterQuickFix(propertiesFile));
				}
			}
		};
	}

	static boolean isResourceBundleAlphaSortedExceptOneFile(@NotNull ResourceBundle resourceBundle, @NotNull PropertiesFile exceptedFile) {
		for (PropertiesFile file : resourceBundle.getPropertiesFiles()) {
			if (!(file instanceof PropertiesFileImpl)) {
				return true;
			}
			if (!file.equals(exceptedFile) && !isCeaSorted(file)) {
				return false;
			}
		}
		return true;
	}

	static String getFirstKeyPart(@NotNull String fullKey) {
		return fullKey.substring(0, fullKey.indexOf('.'));
	}

	// This method checks if we have a blank line between us and the previous property
	private static boolean psiElementIsAfterABlankLine(@NotNull PsiElement psiElement) {
		PsiElement previousWhiteSpace = psiElement.getPrevSibling();
		switch (previousWhiteSpace.getText()) {
		case LINE_SEPARATOR:
			if (previousWhiteSpace.getPrevSibling() instanceof PsiComment) {
				return psiElementIsAfterABlankLine(previousWhiteSpace.getPrevSibling());
			} else {
				// We are after a property
				return false;
			}
		case TWO_LINE_SEPARATOR:
			return !(previousWhiteSpace.getPrevSibling() instanceof PsiComment);
		default:
			return false;
		}
	}

	// This method checks if we have a simple line return between us and the previous property
	private static boolean psiElementIsAfterAProperty(@NotNull PsiElement psiElement) {
		PsiElement previousWhiteSpace = psiElement.getPrevSibling();
		if (previousWhiteSpace.getText().equals(LINE_SEPARATOR)) {
			if (previousWhiteSpace.getPrevSibling() instanceof PsiComment) {
				return psiElementIsAfterAProperty(previousWhiteSpace.getPrevSibling());
			} else {
				// We are after a property
				return true;
			}
		} else {
			return false;
		}
	}

	static boolean isCeaSorted(PropertiesFile propertiesFile) {
		Iterable<IProperty> properties = new ArrayList<>(propertiesFile.getProperties());

		String previousKey = null;
		IProperty property;
		String currentKey;
		for (Iterator<IProperty> it = properties.iterator(); it.hasNext(); previousKey = currentKey) {
			property = it.next();
			currentKey = property.getKey();
			if (currentKey == null) {
				// Don't know when this can happen
				return false;
			}

			String value = property.getValue();
			if ((value == null) || value.trim().isEmpty()) {
				return false;
			}

			if (property.getPsiElement().getText().contains(LINE_SEPARATOR)) {
				return false;
			}

			// The first loop does nothing because of this
			if (previousKey != null) {
				String previousKeyToCompare;
				String keyToCompare;
				// When comparing two keys with and without a point, we need to compare the key without a point with the first part of the key with a point
				if (previousKey.contains(".") && !currentKey.contains(".")) {
					previousKeyToCompare = getFirstKeyPart(previousKey);
					keyToCompare = currentKey;
					// We need to have a blank line between a property with point and a property without
					if (!psiElementIsAfterABlankLine(property.getPsiElement())) {
						return false;
					}
				} else if (!previousKey.contains(".") && currentKey.contains(".")) {
					previousKeyToCompare = previousKey;
					keyToCompare = getFirstKeyPart(currentKey);
					// We need to have a blank line between a property with point and a property without
					if (!psiElementIsAfterABlankLine(property.getPsiElement())) {
						return false;
					}
				} else if (previousKey.contains(".") && currentKey.contains(".")) {
					// If both keys have points, we compare the full key
					previousKeyToCompare = previousKey;
					keyToCompare = currentKey;
					if (!getFirstKeyPart(previousKey).equals(getFirstKeyPart(currentKey))) {
						// We need to have a blank line between 2 properties with point if the first part is not the same
						if (!psiElementIsAfterABlankLine(property.getPsiElement())) {
							return false;
						}
					} else {
						// We need to have no blank line between 2 properties with point if the first part is the same
						if (!psiElementIsAfterAProperty(property.getPsiElement())) {
							return false;
						}
					}
				} else {
					// If both keys have no points, we compare the full key
					previousKeyToCompare = previousKey;
					keyToCompare = currentKey;
					// We need to have a blank line between 2 properties without a point
					if (!psiElementIsAfterABlankLine(property.getPsiElement())) {
						return false;
					}
				}

				// We just checked that we have the correct number of blank lines before our property.
				// We now compare it with the previous one to check if they are correctly ordered
				if (previousKeyToCompare.compareTo(keyToCompare) > 0) {
					return false;
				}
			}
		}
		return true;
	}

	private static final class CEAPropertiesSorterQuickFix implements LocalQuickFix {
		private final PropertiesFile[] myFilesToSort;

		private CEAPropertiesSorterQuickFix(PropertiesFile... toSort) {
			myFilesToSort = toSort;
		}

		@NotNull
		@Override
		public String getFamilyName() {
			return "Sort resource bundle files";
		}

		@Override
		public void applyFix(@NotNull Project project, @NotNull ProblemDescriptor descriptor) {
			boolean force = myFilesToSort.length == 1;
			for (PropertiesFile file : myFilesToSort) {
				if (!force && isCeaSorted(file)) {
					continue;
				}
				try {
					ReformatCodeProcessor reformatCodeProcessor = new ReformatCodeProcessor(file.getContainingFile(), false);
					reformatCodeProcessor.runWithoutProgress();
					sortPropertiesFile(file);
				} catch (Exception e) {
					LOG.error(e.getMessage(), e);
				}
			}
		}

		private static void sortPropertiesFile(PropertiesFile file) {
			List<IProperty> properties = new ArrayList<>(file.getProperties());

			properties.sort((p1, p2) -> Comparing.compare(p1.getKey(), p2.getKey()));
			char delimiter = PropertiesCodeStyleSettings.getInstance(file.getProject()).getDelimiter();
			StringBuilder rawText = new StringBuilder();
			for (int i = 0; i < properties.size(); i++) {
				IProperty property = properties.get(i);
				String value = property.getValue();
				String commentAboveProperty = property.getDocCommentText();
				if (commentAboveProperty != null) {
					rawText.append(commentAboveProperty).append(LINE_SEPARATOR);
				}
				String key = property.getKey();
				String propertyText;
				if (key != null) {
					propertyText = PropertiesElementFactory.getPropertyText(key, (value != null) ? value : "", delimiter, null, PropertyKeyValueFormat.FILE);
					rawText.append(propertyText);
					if (i <= (properties.size() - 2)) {
						String nextKey = properties.get(i + 1).getKey();
						if (nextKey != null) {
							// If one key has a point and not the other, we add a blank line
							if (key.contains(".") && !nextKey.contains(".")) {
								rawText.append(TWO_LINE_SEPARATOR);
							} else if (!key.contains(".") && nextKey.contains(".")) {
								rawText.append(TWO_LINE_SEPARATOR);
							} else if (key.contains(".") && nextKey.contains(".")) {
								// If both keys have points, we might add a blank line
								if (!getFirstKeyPart(key).equals(getFirstKeyPart(nextKey))) {
									rawText.append(TWO_LINE_SEPARATOR);
								} else {
									rawText.append(LINE_SEPARATOR);
								}
							} else {
								// If both keys have no points, we add a blank line
								rawText.append(TWO_LINE_SEPARATOR);
							}
						} else {
							rawText.append(LINE_SEPARATOR);
						}
					}
				}
			}

			PropertiesFile fakeFile = PropertiesElementFactory.createPropertiesFile(file.getProject(), rawText.toString());

			PropertiesList propertiesList = PsiTreeUtil.findChildOfType(file.getContainingFile(), PropertiesList.class);
			LOG.assertTrue(propertiesList != null);
			PropertiesList fakePropertiesList = PsiTreeUtil.findChildOfType(fakeFile.getContainingFile(), PropertiesList.class);
			LOG.assertTrue(fakePropertiesList != null);
			propertiesList.replace(fakePropertiesList);
		}
	}

	@Override
	@NotNull
	public String getDisplayName() {
		return "CEA Unsorted Properties File or Resource Bundle";
	}

	@Override
	@NotNull
	public String getShortName() {
		return "CeaUnsortedPropertiesFile";
	}
}
