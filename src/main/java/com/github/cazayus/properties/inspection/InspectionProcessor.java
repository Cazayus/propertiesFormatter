package com.github.cazayus.properties.inspection;

import java.util.List;

import com.intellij.codeInspection.GlobalInspectionContext;
import com.intellij.codeInspection.InspectionEngine;
import com.intellij.codeInspection.InspectionManager;
import com.intellij.codeInspection.LocalInspectionTool;
import com.intellij.codeInspection.ProblemDescriptor;
import com.intellij.codeInspection.QuickFix;
import com.intellij.codeInspection.ex.LocalInspectionToolWrapper;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.project.IndexNotReadyException;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiFile;

public final class InspectionProcessor implements Runnable {
	private final Project project;
	private final PsiFile psiFile;
	private final LocalInspectionTool inspectionTool;

	public InspectionProcessor(Project project, PsiFile psiFile, LocalInspectionTool inspectionTool) {
		this.project = project;
		this.psiFile = psiFile;
		this.inspectionTool = inspectionTool;
	}

	@Override
	public void run() {
		ApplicationManager.getApplication().invokeLater(() -> WriteCommandAction.writeCommandAction(project).run(() -> {
			InspectionManager inspectionManager = InspectionManager.getInstance(project);
			GlobalInspectionContext context = inspectionManager.createNewGlobalContext();
			LocalInspectionToolWrapper toolWrapper = new LocalInspectionToolWrapper(inspectionTool);
			List<ProblemDescriptor> problemDescriptors;
			try {
				problemDescriptors = InspectionEngine.runInspectionOnFile(psiFile, toolWrapper, context);
			} catch (IndexNotReadyException exception) {
				return;
			}
			for (ProblemDescriptor problemDescriptor : problemDescriptors) {
				QuickFix<ProblemDescriptor>[] fixes = problemDescriptor.getFixes();
				if (fixes != null) {
					writeQuickFixes(problemDescriptor, fixes);
				}
			}
		}));
	}

	private void writeQuickFixes(ProblemDescriptor problemDescriptor, QuickFix<ProblemDescriptor>[] fixes) {
		for (QuickFix<ProblemDescriptor> fix : fixes) {
			if (fix != null) {
				fix.applyFix(project, problemDescriptor);
			}
		}
	}
}
