package com.raredev.vcspace.ui.panels.editor;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import com.blankj.utilcode.util.FileIOUtils;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.raredev.vcspace.databinding.LayoutEditorPanelBinding;
import com.raredev.vcspace.editor.IDECodeEditor;
import com.raredev.vcspace.models.DocumentModel;
import com.raredev.vcspace.res.R;
import com.raredev.vcspace.task.TaskExecutor;
import com.raredev.vcspace.ui.panels.Panel;
import io.github.rosemoe.sora.langs.textmate.VCSpaceTMLanguage;
import io.github.rosemoe.sora.langs.textmate.provider.TextMateProvider;
import io.github.rosemoe.sora.text.Cursor;

public class EditorPanel extends Panel {

  private LayoutEditorPanelBinding binding;
  private DocumentModel document;

  public EditorPanel(Context context, DocumentModel document) {
    super(context);
    this.document = document;
    binding = LayoutEditorPanelBinding.inflate(LayoutInflater.from(getContext()));

    binding.editor.setColorScheme(TextMateProvider.getColorScheme());
    binding.editor.setDocument(document);
    setLoading(true);

    var modified = document.isModified();

    TaskExecutor.executeAsync(
        () -> {
          String content = "";
          if (document.getContent() != null) {
            content = new String(document.getContent());
          } else {
            content = FileIOUtils.readFile2String(document.getPath());
          }
          return content;
        },
        (result) -> {
          binding.editor.setText((String) result, null);
          if (!modified) document.markUnmodified();
          binding.editor.configureEditor();
          postRead();
        });
    setContentView(binding.getRoot());
  }

  @Override
  public void destroy() {
    binding.editor.release();
  }

  @Override
  public void unselected() {
  }

  @Override
  public void selected() {
    if (binding != null) binding.editor.requestFocus();
  }

  private void postRead() {
    binding.editor.setEditorLanguage(createLanguage());
    //binding.editor.setCursorPosition(document.getPositionLine(), document.getPositionColumn());
    setLoading(false);
  }

  public void reloadFile(Runnable post) {
    if (document.isModified()) {
      new MaterialAlertDialogBuilder(getContext())
          .setTitle(R.string.menu_reload)
          .setMessage(R.string.discard_changes)
          .setNegativeButton(R.string.cancel, null)
          .setPositiveButton(
              android.R.string.ok,
              (dpg, witch) -> {
                reloadFileHandler(post);
              })
          .show();
    } else {
      reloadFileHandler(post);
    }
  }

  private void reloadFileHandler(Runnable post) {
    setLoading(true);
    TaskExecutor.executeAsync(
        () -> {
          return FileIOUtils.readFile2String(document.getPath());
        },
        (result) -> {
          binding.editor.setText((String) result, null);
          document.markUnmodified();
          setLoading(false);
          post.run();
        });
  }

  public void reloadEditor() {
    getEditor().setColorScheme(TextMateProvider.getColorScheme());
    getEditor().setEditorLanguage(createLanguage());
  }

  public void saveDocument() {
    if (document.isModified()) {
      FileIOUtils.writeFileFromString(document.getPath(), getCode());
      document.markUnmodified();
    }
  }

  public String getCode() {
    return binding.editor.getText().toString();
  }

  public void undo() {
    if (binding.editor.canUndo()) {
      binding.editor.undo();
    }
  }

  public void redo() {
    if (binding.editor.canRedo()) {
      binding.editor.redo();
    }
  }

  public void setDocument(DocumentModel document) {
    binding.editor.setDocument(document);
    this.document = document;
  }

  public DocumentModel getDocument() {
    return document;
  }

  public IDECodeEditor getEditor() {
    return binding.editor;
  }

  public void setLoading(boolean loading) {
    binding.circularProgressIndicator.setVisibility(loading ? View.VISIBLE : View.GONE);
  }

  public VCSpaceTMLanguage createLanguage() {
    try {
      return TextMateProvider.createLanguage(getEditor(), document.getName());
    } catch (Exception e) {
      return null;
    }
  }
}
