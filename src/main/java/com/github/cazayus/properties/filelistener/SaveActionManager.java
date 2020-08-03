package com.github.cazayus.properties.filelistener;

import static java.util.Collections.synchronizedList;

import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Level;
import org.jetbrains.annotations.NotNull;

import com.github.cazayus.properties.inspection.CeaUnsortedPropertiesFileInspection;
import com.github.cazayus.properties.inspection.InspectionProcessor;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.fileEditor.FileDocumentManagerListener;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectManager;
import com.intellij.openapi.roots.ProjectRootManager;
import com.intellij.psi.PsiDocumentManager;
import com.intellij.psi.PsiFile;
import com.intellij.util.PsiErrorElementUtil;

/**
 * Only contains one inspection <br>
 * The psi files seems to be shared between projects, so we need to check if the file is physically <br>
 * in that project before reformating, or else the file is formatted twice and intellij will ask to <br>
 * confirm unlocking of non-project file in the other project, see {@link #isPsiFileEligible(Project, PsiFile)}.
 */
public final class SaveActionManager implements FileDocumentManagerListener {
	private static final Logger LOGGER = Logger.getInstance(SaveActionManager.class);
	private static final List<Runnable> runningProcessors = synchronizedList(new ArrayList<>());

	static {
		LOGGER.setLevel(Level.DEBUG);
	}

	@Override
	public void beforeDocumentSaving(@NotNull Document document) {
		LOGGER.debug("Running SaveActionManager on " + document);
		for (Project project : ProjectManager.getInstance().getOpenProjects()) {
			PsiFile psiFile = PsiDocumentManager.getInstance(project).getPsiFile(document);
			processPsiFile(project, psiFile);
		}
	}

	private static void processPsiFile(Project project, PsiFile psiFile) {
		if (isPsiFileEligible(project, psiFile)) {
			actuallyProcessPsiFile(project, psiFile);
		}
	}

	private static boolean isPsiFileEligible(Project project, PsiFile psiFile) {
		return psiFile != null && isProjectValid(project) && isPsiFileInProject(project, psiFile) && isPsiFileHasErrors(project, psiFile)
				&& isPsiFileFresh(psiFile) && isPsiFileValid(psiFile);
	}

	private static boolean isProjectValid(Project project) {
		return project.isInitialized() && !project.isDisposed();
	}

	private static boolean isPsiFileInProject(Project project, PsiFile file) {
		boolean inProject = ProjectRootManager.getInstance(project).getFileIndex().isInContent(file.getVirtualFile());
		if (!inProject) {
			LOGGER.debug("File " + file.getVirtualFile().getCanonicalPath() + " not in current project " + project);
		}
		return inProject;
	}

	private static boolean isPsiFileHasErrors(Project project, PsiFile psiFile) {
		return !PsiErrorElementUtil.hasErrors(project, psiFile.getVirtualFile());
	}

	private static boolean isPsiFileFresh(PsiFile psiFile) {
		return psiFile.getModificationStamp() != 0;
	}

	private static boolean isPsiFileValid(PsiFile psiFile) {
		return psiFile.isValid();
	}

	private static void actuallyProcessPsiFile(Project project, PsiFile psiFile) {
		Runnable processor = getSaveActionsProcessors(project, psiFile);
		LOGGER.debug("Running processors " + processor + ", file " + psiFile + ", project " + project);
		runProcessor(processor);
	}

	private static void runProcessor(Runnable processor) {
		if (runningProcessors.contains(processor)) {
			return;
		}
		try {
			runningProcessors.add(processor);
			processor.run();
		} finally {
			runningProcessors.remove(processor);
		}
	}

	private static Runnable getSaveActionsProcessors(Project project, PsiFile psiFile) {
		return new InspectionProcessor(project, psiFile, new CeaUnsortedPropertiesFileInspection());
	}
}
